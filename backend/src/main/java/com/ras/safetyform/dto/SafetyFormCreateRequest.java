package com.ras.safetyform.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

public record SafetyFormCreateRequest(
        @NotNull @Positive Integer userId,
        @NotNull @Positive Integer siteId,
        @NotNull LocalDateTime formDate,
        String notes) {
}
