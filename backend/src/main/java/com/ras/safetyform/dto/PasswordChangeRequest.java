package com.ras.safetyform.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordChangeRequest(
        @NotBlank String currentPassword,
        @NotBlank String newPassword,
        @NotBlank String confirmNewPassword) {
}
