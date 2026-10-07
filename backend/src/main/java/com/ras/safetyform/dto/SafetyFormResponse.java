package com.ras.safetyform.dto;

import java.time.Instant;

public record SafetyFormResponse(
        Integer id,
        Integer userId,
        Integer siteId,
        Instant formDate,
        String notes,
        Instant submittedAt,
        String status,
        UserResponse user,
        SiteResponse site) {
}
