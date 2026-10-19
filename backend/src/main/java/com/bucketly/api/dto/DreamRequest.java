package com.bucketly.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DreamRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,

        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description,

        @NotBlank(message = "Category is required")
        @Size(max = 100, message = "Category must not exceed 100 characters")
        String category,

        @Size(max = 100, message = "Target date must not exceed 100 characters")
        String targetDate,

        @Size(max = 50, message = "Status must not exceed 50 characters")
        String status
) {
}

