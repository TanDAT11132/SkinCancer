package com.skincancer.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(
        @NotBlank String fullName,
        @NotBlank String gender,
        @Min(0) @Max(120) Integer age
) {
}




