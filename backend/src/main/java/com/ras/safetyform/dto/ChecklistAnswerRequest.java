package com.ras.safetyform.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ChecklistAnswerRequest(
        @NotNull @Positive Integer checklistItemId,
        @NotNull Boolean response) {
}
