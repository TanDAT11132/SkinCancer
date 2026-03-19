package com.skincancer.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skincancer.backend.config.FastApiProperties;
import com.skincancer.backend.config.UploadProperties;
import com.skincancer.backend.dto.response.PredictionBatchResponse;
import com.skincancer.backend.dto.response.PredictionItemResponse;
import com.skincancer.backend.entity.ImageUpload;
import com.skincancer.backend.entity.Prediction;
import com.skincancer.backend.entity.UserEntity;
import com.skincancer.backend.exception.BadRequestException;
import com.skincancer.backend.exception.ExternalServiceException;
import com.skincancer.backend.exception.NotFoundException;
import com.skincancer.backend.repository.ImageUploadRepository;
import com.skincancer.backend.repository.PredictionRepository;
import com.skincancer.backend.repository.UserRepository;
import com.skincancer.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PredictionService {

    private final UploadProperties uploadProperties;
    private final FastApiProperties fastApiProperties;
    private final UserRepository userRepository;
    private final ImageUploadRepository imageUploadRepository;
    private final PredictionRepository predictionRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public PredictionBatchResponse predict(UserPrincipal principal, List<MultipartFile> files, String clientIp) {
        log.info("[FLOW][PREDICT] Start authenticated predict userId={} files={} clientIp={}",
                principal.userId(),
                files == null ? 0 : files.size(),
                clientIp
        );
        if (files == null || files.isEmpty()) {
            throw new BadRequestException("FILES_REQUIRED", "files is required");
        }

        UserEntity user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));

        List<ImageUpload> uploads = saveUploads(user, files);

        Object fastApiRaw = callFastApi(files, null);
        List<Map<String, Object>> predictions = normalizeFastApiResponse(fastApiRaw, uploads.size());

        List<PredictionItemResponse> resultItems = new ArrayList<>();
        for (int i = 0; i < uploads.size(); i++) {
            ImageUpload upload = uploads.get(i);
            Map<String, Object> p = predictions.get(i);

            Prediction prediction = new Prediction();
            prediction.setImage(upload);
            prediction.setRequestedAt(LocalDateTime.now());
            prediction.setClientApp(null);
            prediction.setClientIp(clientIp);
            prediction.setModelName(asText(p.get("model_name")));
            prediction.setModelVersion(asText(p.get("model_version")));
            prediction.setPredictedClass(firstNonBlank(
                    asText(p.get("predicted_label")),
                    asText(p.get("predicted_class")),
                    asText(p.get("label")),
                    "unknown"
            ));
            prediction.setProbability(extractProbability(p));
            prediction.setTopKJson(writeJson(p.get("top_k")));
            prediction.setRawResponseJson(writeJson(p));

            prediction = predictionRepository.save(prediction);
            resultItems.add(toResponse(prediction));
        }

        log.info("[FLOW][PREDICT] Completed authenticated predict userId={} results={}", principal.userId(), resultItems.size());
        return new PredictionBatchResponse(resultItems);
    }

    public PredictionBatchResponse quickPredict(List<MultipartFile> files) {
        log.info("[FLOW][PREDICT] Start quick predict files={}", files == null ? 0 : files.size());
        if (files == null || files.isEmpty()) {
            throw new BadRequestException("FILES_REQUIRED", "files is required");
        }

        Object fastApiRaw = callFastApi(files, null);
        List<Map<String, Object>> predictions = normalizeFastApiResponse(fastApiRaw, files.size());

        List<PredictionItemResponse> resultItems = new ArrayList<>();
        for (Map<String, Object> p : predictions) {
            resultItems.add(new PredictionItemResponse(
                    null,
                    null,
                    null,
                    firstNonBlank(
                            asText(p.get("predicted_label")),
                            asText(p.get("predicted_class")),
                            asText(p.get("label")),
                            "unknown"
                    ),
                    extractProbability(p),
                    writeJson(p.get("top_k")),
                    writeJson(p),
                    asText(p.get("model_name")),
                    asText(p.get("model_version")),
                    LocalDateTime.now()
            ));
        }

        log.info("[FLOW][PREDICT] Completed quick predict results={}", resultItems.size());
        return new PredictionBatchResponse(resultItems);
    }

    @Transactional(readOnly = true)
    public List<PredictionItemResponse> history(UserPrincipal principal, int page, int size) {
        List<PredictionItemResponse> results = predictionRepository
                .findByImageUserUserIdOrderByRequestedAtDesc(principal.userId(), PageRequest.of(page, size))
                .stream()
                .map(this::toResponse)
                .toList();
        log.info("[FLOW][PREDICT] Load history userId={} page={} size={} returned={}", principal.userId(), page, size, results.size());
        return results;
    }

    private List<ImageUpload> saveUploads(UserEntity user, List<MultipartFile> files) {
        List<ImageUpload> uploads = new ArrayList<>();
        Path root = Paths.get(uploadProperties.rootDir());

        for (MultipartFile file : files) {
            try {
                byte[] bytes = file.getBytes();
                String ext = getExtension(file.getOriginalFilename());
                LocalDate today = LocalDate.now();
                String relDir = "%d/%02d/%02d".formatted(today.getYear(), today.getMonthValue(), today.getDayOfMonth());
                Path targetDir = root.resolve(relDir);
                Files.createDirectories(targetDir);

                String fileName = UUID.randomUUID() + (ext.isBlank() ? "" : "." + ext);
                Path target = targetDir.resolve(fileName);
                Files.write(target, bytes);

                ImageUpload upload = new ImageUpload();
                upload.setUser(user);
                upload.setFileUri(target.toString());
                upload.setFileSizeBytes((long) bytes.length);
                upload.setFileHashSha256(sha256Hex(bytes));
                upload.setUploadedAt(LocalDateTime.now());
                uploads.add(imageUploadRepository.save(upload));
            } catch (IOException e) {
                throw new ExternalServiceException("UPLOAD_SAVE_FAILED", "Cannot save upload file");
            }
        }

        return uploads;
    }

    @SuppressWarnings("unchecked")
    private Object callFastApi(List<MultipartFile> files, Integer topK) {
        int resolvedTopK = topK == null ? fastApiProperties.defaultTopK() : topK;
        String url = "%s/v1/predict?top_k=%d".formatted(fastApiProperties.baseUrl(), resolvedTopK);
        log.info("[FLOW][PREDICT] Call FastAPI url={} files={} topK={}", url, files.size(), resolvedTopK);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        for (MultipartFile file : files) {
            try {
                ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                    @Override
                    public String getFilename() {
                        return file.getOriginalFilename() == null ? "image.jpg" : file.getOriginalFilename();
                    }
                };
                body.add("files", resource);
            } catch (IOException e) {
                throw new BadRequestException("INVALID_FILE_CONTENT", "Cannot read upload file");
            }
        }

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        try {
            Object response = restTemplate.postForObject(url, request, Object.class);
            log.info("[FLOW][PREDICT] FastAPI call success");
            return response;
        } catch (RestClientException ex) {
            log.error("[FLOW][PREDICT] FastAPI call failed: {}", ex.getMessage());
            throw new ExternalServiceException("FASTAPI_CALL_FAILED", "Cannot call prediction service");
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeFastApiResponse(Object raw, int expectedSize) {
        List<Map<String, Object>> out = new ArrayList<>();

        if (raw instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> map) {
                    out.add((Map<String, Object>) map);
                }
            }
        } else if (raw instanceof Map<?, ?> mapRaw) {
            Map<String, Object> map = (Map<String, Object>) mapRaw;
            Object maybeResults = map.get("results");
            if (maybeResults instanceof List<?> list) {
                for (Object o : list) {
                    if (o instanceof Map<?, ?> item) {
                        out.add((Map<String, Object>) item);
                    }
                }
            } else {
                out.add(map);
            }
        }

        if (out.isEmpty()) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("predicted_class", "unknown");
            fallback.put("probability", 0.0);
            out.add(fallback);
        }

        while (out.size() < expectedSize) {
            out.add(out.get(out.size() - 1));
        }

        if (out.size() > expectedSize) {
            return new ArrayList<>(out.subList(0, expectedSize));
        }

        return out;
    }

    private PredictionItemResponse toResponse(Prediction p) {
        return new PredictionItemResponse(
                p.getPredictionId(),
                p.getImage().getImageId(),
                p.getImage().getFileUri(),
                p.getPredictedClass(),
                p.getProbability(),
                p.getTopKJson(),
                p.getRawResponseJson(),
                p.getModelName(),
                p.getModelVersion(),
                p.getRequestedAt()
        );
    }

    private static String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (Exception e) {
            return Base64.getEncoder().encodeToString(bytes);
        }
    }

    private static String asText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private BigDecimal extractProbability(Map<String, Object> p) {
        Object prob = p.get("probability");
        if (prob == null) {
            prob = p.get("score");
        }
        if (prob == null) {
            prob = p.get("confidence");
        }

        if (prob instanceof Number n) {
            double val = n.doubleValue();
            if (val > 1) {
                val = val / 100.0;
            }
            val = Math.max(0.0, Math.min(1.0, val));
            return BigDecimal.valueOf(val).setScale(5, RoundingMode.HALF_UP);
        }

        return BigDecimal.ZERO.setScale(5, RoundingMode.HALF_UP);
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }
}
