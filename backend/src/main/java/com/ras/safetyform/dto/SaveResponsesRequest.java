package com.ras.safetyform.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record SaveResponsesRequest(
        @NotEmpty List<@Valid ChecklistAnswerRequest> responses) {
}
