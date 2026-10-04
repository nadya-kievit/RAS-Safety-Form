package com.ras.safetyform.dto;

public record ChecklistItemResponse(
        Integer id,
        Integer safetyChecklistId,
        String item) {
}
