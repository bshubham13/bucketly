package com.bucketly.api.controller;

import com.bucketly.api.dto.MemoryCreateRequest;
import com.bucketly.api.dto.MemoryResponse;
import com.bucketly.api.dto.MemoryUpdateRequest;
import com.bucketly.api.exception.InvalidCredentialsException;
import com.bucketly.api.security.UserPrincipal;
import com.bucketly.api.service.MemoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/memories")
public class MemoryController {

    private final MemoryService memoryService;

    public MemoryController(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @GetMapping
    public ResponseEntity<List<MemoryResponse>> getAllMemories(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = requireUserId(principal);
        List<MemoryResponse> memories = memoryService.getAllMemories(userId);
        return ResponseEntity.ok(memories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MemoryResponse> getMemoryById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = requireUserId(principal);
        MemoryResponse memory = memoryService.getMemoryById(id, userId);
        return ResponseEntity.ok(memory);
    }

    @PostMapping
    public ResponseEntity<MemoryResponse> createMemory(
            @Valid @RequestBody MemoryCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = requireUserId(principal);
        MemoryResponse created = memoryService.createMemory(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MemoryResponse> updateMemory(
            @PathVariable Long id,
            @Valid @RequestBody MemoryUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = requireUserId(principal);
        MemoryResponse updated = memoryService.updateMemory(id, request, userId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMemory(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = requireUserId(principal);
        memoryService.deleteMemory(id, userId);
        return ResponseEntity.noContent().build();
    }

    private Long requireUserId(UserPrincipal principal) {
        if (principal != null && principal.userId() != null) {
            return principal.userId();
        }
        throw new InvalidCredentialsException("Authentication required.");
    }
}

