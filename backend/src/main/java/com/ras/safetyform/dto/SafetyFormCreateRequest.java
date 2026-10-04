package com.ras.safetyform.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record SafetyFormCreateRequest(
        @NotNull @Positive Integer userId,
        @NotNull @Positive Integer siteId,
        @NotNull LocalDate formDate,
        @Pattern(regexp = "submitted|reviewed") String status,
        String notes) {

    public SafetyFormCreateRequest {
        if (status == null || status.isBlank()) {
            status = "submitted";
        }
    }
}
