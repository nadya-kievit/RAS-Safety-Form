package com.ras.safetyform.dto;

import java.time.Instant;

public record PhotoResponse(
        Integer id,
        Integer safetyFormId,
        String storagePath,
        String filename,
        String mimeType,
        Integer fileSize,
        String viewUrl,
        Instant createdAt) {
}
