package com.ras.safetyform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SiteUpdateRequest(@NotBlank @Size(max = 150) String name) {
}
