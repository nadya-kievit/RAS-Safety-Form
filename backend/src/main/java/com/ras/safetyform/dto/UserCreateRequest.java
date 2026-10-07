package com.ras.safetyform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Size(max = 100) String username,
        @NotBlank String password,
        @NotBlank String confirmPassword,
        @NotBlank @Pattern(regexp = "admin|framer", message = "must be admin or framer")
                String role) {
}
