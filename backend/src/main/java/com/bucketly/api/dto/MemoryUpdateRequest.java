package com.bucketly.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record MemoryUpdateRequest(
        @NotNull(message = "Completion date is required")
        @PastOrPresent(message = "Completion date cannot be in the future")
        LocalDate completedDate,

        @Size(max = 4000, message = "Story must not exceed 4000 characters")
        String story,

        @Size(max = 255, message = "Photo path must not exceed 255 characters")
        String photoPath,

        @Size(max = 255, message = "Original filename must not exceed 255 characters")
        String originalFilename
) {
}
