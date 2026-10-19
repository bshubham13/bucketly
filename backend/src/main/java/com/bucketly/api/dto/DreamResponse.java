package com.bucketly.api.dto;

import com.bucketly.api.entity.Dream;

import java.time.LocalDateTime;

public record DreamResponse(
        Long id,
        String title,
        String description,
        String category,
        String targetDate,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static DreamResponse fromEntity(Dream dream) {
        return new DreamResponse(
                dream.getId(),
                dream.getTitle(),
                dream.getDescription(),
                dream.getCategory(),
                dream.getTargetDate(),
                dream.getStatus(),
                dream.getCreatedAt(),
                dream.getUpdatedAt()
        );
    }
}

