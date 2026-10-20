package com.bucketly.api.dto;

import com.bucketly.api.entity.User;

import java.time.LocalDateTime;

public record AuthResponse(
        Long id,
        String email,
        String fullName,
        LocalDateTime createdAt
) {
    public static AuthResponse fromEntity(User user) {
        if (user == null) {
            return null;
        }
        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getCreatedAt()
        );
    }
}

