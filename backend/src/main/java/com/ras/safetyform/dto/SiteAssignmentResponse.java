package com.ras.safetyform.dto;

import java.time.LocalDate;

public record SiteAssignmentResponse(
        Integer id,
        Integer userId,
        Integer siteId,
        LocalDate assignmentDate,
        SiteResponse site) {
}
