package com.ras.safetyform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record PhotoCreateRequest(
        @NotBlank String storagePath,
        @NotBlank @Size(max = 255) String filename,
        @NotBlank @Size(max = 100) String mimeType,
        @NotNull @PositiveOrZero Integer fileSize) {
}
