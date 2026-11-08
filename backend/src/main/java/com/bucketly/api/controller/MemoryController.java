package com.bucketly.api.controller;

import com.bucketly.api.dto.MemoryCreateRequest;
import com.bucketly.api.dto.MemoryResponse;
import com.bucketly.api.dto.MemoryUpdateRequest;
import com.bucketly.api.dto.PhotoDownload;
import com.bucketly.api.exception.InvalidCredentialsException;
import com.bucketly.api.security.UserPrincipal;
import com.bucketly.api.service.MemoryService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    @GetMapping("/{id}/photo")
    public ResponseEntity<Resource> getPhoto(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = requireUserId(principal);
        PhotoDownload download = memoryService.getPhoto(id, userId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600, must-revalidate")
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(download.resource());
    }

    @PostMapping
    public ResponseEntity<MemoryResponse> createMemory(
            @Valid @RequestBody MemoryCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = requireUserId(principal);
        MemoryResponse created = memoryService.createMemory(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MemoryResponse> uploadPhoto(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = requireUserId(principal);
        MemoryResponse response = memoryService.uploadPhoto(id, file, userId);
        return ResponseEntity.ok(response);
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

    @DeleteMapping("/{id}/photo")
    public ResponseEntity<MemoryResponse> deletePhoto(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = requireUserId(principal);
        MemoryResponse response = memoryService.deletePhoto(id, userId);
        return ResponseEntity.ok(response);
    }

    private Long requireUserId(UserPrincipal principal) {
        if (principal != null && principal.userId() != null) {
            return principal.userId();
        }
        throw new InvalidCredentialsException("Authentication required.");
    }
}

