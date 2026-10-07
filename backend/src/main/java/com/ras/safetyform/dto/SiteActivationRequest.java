package com.ras.safetyform.dto;

import jakarta.validation.constraints.NotNull;

public record SiteActivationRequest(@NotNull Boolean active) {
}
