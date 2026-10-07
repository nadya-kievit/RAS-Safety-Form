package com.ras.safetyform.dto;

import java.time.Instant;

public record UserResponse(
        Integer id,
        String firstName,
        String lastName,
        String username,
        String role,
        boolean mustChangePassword,
        boolean active,
        Instant createdAt) {
}
