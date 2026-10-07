package com.ras.safetyform.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public record SafetyFormCreateRequest(
        @NotNull @Positive Integer userId,
        @NotNull @Positive Integer siteId,
        @NotNull Instant formDate,
        String notes,
        @NotNull @Size(max = 200) List<@NotNull Integer> checkedItemIds) {
}
