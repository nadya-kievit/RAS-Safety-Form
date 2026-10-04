package com.ras.safetyform.dto;

import java.time.LocalDateTime;

public record SiteResponse(
        Integer id,
        String name,
        Integer safetyChecklistId,
        boolean active,
        LocalDateTime createdAt) {
}
