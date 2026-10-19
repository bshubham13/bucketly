package com.bucketly.api.dto;

import com.bucketly.api.entity.Memory;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MemoryResponse(
        Long id,
        Long dreamId,
        String dreamTitle,
        String dreamCategory,
        LocalDate completedDate,
        String story,
        String photoPath,
        String originalFilename,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static MemoryResponse fromEntity(Memory memory) {
        return new MemoryResponse(
                memory.getId(),
                memory.getDream().getId(),
                memory.getDream().getTitle(),
                memory.getDream().getCategory(),
                memory.getCompletedDate(),
                memory.getStory(),
                memory.getPhotoPath(),
                memory.getOriginalFilename(),
                memory.getCreatedAt(),
                memory.getUpdatedAt()
        );
    }
}

