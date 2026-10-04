package com.ras.safetyform.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SafetyFormResponse(
        Integer id,
        Integer userId,
        Integer siteId,
        LocalDate formDate,
        String status,
        String notes,
        LocalDateTime submittedAt,
        LocalDateTime updatedAt,
        UserResponse user,
        SiteResponse site) {
}
