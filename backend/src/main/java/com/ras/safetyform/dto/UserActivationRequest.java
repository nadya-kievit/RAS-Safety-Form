package com.ras.safetyform.dto;

import jakarta.validation.constraints.NotNull;

public record UserActivationRequest(@NotNull Boolean active) {
}
