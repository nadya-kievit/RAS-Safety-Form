package com.ras.safetyform.dto;

import java.util.List;

public record ChecklistResponse(
        Integer id,
        String name,
        List<ChecklistItemResponse> items) {
}
