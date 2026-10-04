package com.ras.safetyform.dto;

public record ChecklistAnswerResponse(
        Integer id,
        Integer safetyFormId,
        Integer checklistItemId,
        boolean response) {
}
