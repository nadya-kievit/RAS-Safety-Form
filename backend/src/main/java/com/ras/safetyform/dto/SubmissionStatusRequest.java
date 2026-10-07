package com.ras.safetyform.dto;

import jakarta.validation.constraints.NotBlank;

public record SubmissionStatusRequest(@NotBlank String status) {
}
