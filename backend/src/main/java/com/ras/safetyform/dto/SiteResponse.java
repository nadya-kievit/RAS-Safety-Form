package com.ras.safetyform.dto;

import java.time.Instant;

public record SiteResponse(
        Integer id,
        String name,
        Integer safetyChecklistId,
        boolean active,
        Instant createdAt) {
}
