package com.skincancer.backend.service;

import com.skincancer.backend.entity.UserEntity;
import com.skincancer.backend.exception.ExternalServiceException;
import com.skincancer.backend.exception.UnauthorizedException;
import com.skincancer.backend.security.GoogleTokenInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class GoogleTokenVerifier {

    private static final String TOKEN_INFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token=%s";
    private final RestTemplate restTemplate;

    public GoogleTokenInfo verify(String idToken) {
        try {
            ResponseEntity<GoogleTokenInfo> response = restTemplate.exchange(
                    TOKEN_INFO_URL.formatted(idToken),
                    HttpMethod.GET,
                    null,
                    GoogleTokenInfo.class
            );

            GoogleTokenInfo body = response.getBody();
            if (body == null || body.sub() == null || body.email() == null || !body.isVerified()) {
                throw new UnauthorizedException("INVALID_GOOGLE_TOKEN", "Google token is invalid or email is not verified");
            }
            return body;
        } catch (UnauthorizedException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new ExternalServiceException("GOOGLE_VERIFY_FAILED", "Cannot verify Google token at this time");
        }
    }

    public boolean isProfileCompleted(UserEntity user) {
        return user.getFullName() != null && user.getGender() != null && user.getAge() != null;
    }
}
