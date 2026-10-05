package com.ras.safetyform.dto;

import java.time.LocalDateTime;

public record UserResponse(
        Integer id,
        String firstName,
        String lastName,
        String username,
        String role,
        boolean active,
        LocalDateTime createdAt) {
}
