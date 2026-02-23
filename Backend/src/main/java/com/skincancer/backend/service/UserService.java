package com.skincancer.backend.service;

import com.skincancer.backend.dto.request.UpdateProfileRequest;
import com.skincancer.backend.dto.response.UserProfileResponse;
import com.skincancer.backend.entity.UserEntity;
import com.skincancer.backend.exception.NotFoundException;
import com.skincancer.backend.repository.UserRepository;
import com.skincancer.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse me(UserPrincipal principal) {
        UserEntity user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
        return toResponse(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(UserPrincipal principal, UpdateProfileRequest request) {
        UserEntity user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));

        user.setFullName(request.fullName());
        user.setGender(request.gender().toLowerCase());
        user.setAge(request.age());
        user.setUpdatedAt(LocalDateTime.now());

        user = userRepository.save(user);
        return toResponse(user);
    }

    private UserProfileResponse toResponse(UserEntity user) {
        boolean completed = user.getFullName() != null && user.getGender() != null && user.getAge() != null;
        return new UserProfileResponse(
                user.getUserId(),
                user.getEmail(),
                user.getFullName(),
                user.getGender(),
                user.getAge(),
                user.getRole(),
                completed
        );
    }
}
