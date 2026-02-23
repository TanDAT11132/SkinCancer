package com.skincancer.backend.service;

import com.skincancer.backend.dto.response.AuthResponse;
import com.skincancer.backend.entity.Role;
import com.skincancer.backend.entity.UserEntity;
import com.skincancer.backend.repository.UserRepository;
import com.skincancer.backend.security.GoogleTokenInfo;
import com.skincancer.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse loginWithGoogle(String idToken) {
        GoogleTokenInfo info = googleTokenVerifier.verify(idToken);

        UserEntity user = userRepository.findByGoogleSub(info.sub())
                .orElseGet(UserEntity::new);

        boolean isNew = user.getUserId() == null;
        if (isNew) {
            user.setGoogleSub(info.sub());
            user.setCreatedAt(LocalDateTime.now());
            user.setRole(Role.USER);
        }

        user.setEmail(info.email());
        if (user.getFullName() == null) {
            if (info.name() != null && !info.name().isBlank()) {
                user.setFullName(info.name());
            } else {
                user.setFullName(info.email());
            }
        }
        user.setUpdatedAt(LocalDateTime.now());

        user = userRepository.save(user);

        String token = jwtService.generateToken(user.getUserId(), user.getEmail(), user.getRole());
        boolean profileCompleted = googleTokenVerifier.isProfileCompleted(user);
        return new AuthResponse(token, user.getUserId(), user.getEmail(), user.getRole(), profileCompleted);
    }
}
