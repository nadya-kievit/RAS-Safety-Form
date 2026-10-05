package com.ras.safetyform.dto;

import java.time.LocalDateTime;

public record PhotoResponse(
        Integer id,
        Integer safetyFormId,
        String storagePath,
        String filename,
        String mimeType,
        Integer fileSize,
        String viewUrl,
        LocalDateTime createdAt) {
}
