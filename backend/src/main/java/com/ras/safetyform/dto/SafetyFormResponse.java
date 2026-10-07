package com.ras.safetyform.dto;

import java.time.LocalDateTime;

public record SafetyFormResponse(
        Integer id,
        Integer userId,
        Integer siteId,
        LocalDateTime formDate,
        String notes,
        LocalDateTime submittedAt,
        UserResponse user,
        SiteResponse site) {
}
