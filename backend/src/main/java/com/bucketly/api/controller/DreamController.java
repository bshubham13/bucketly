package com.bucketly.api.controller;

import com.bucketly.api.dto.DreamRequest;
import com.bucketly.api.dto.DreamResponse;
import com.bucketly.api.exception.InvalidCredentialsException;
import com.bucketly.api.security.UserPrincipal;
import com.bucketly.api.service.DreamService;
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
@RequestMapping("/api/dreams")
public class DreamController {

    private final DreamService dreamService;

    public DreamController(DreamService dreamService) {
        this.dreamService = dreamService;
    }

    @GetMapping
    public ResponseEntity<List<DreamResponse>> getAllDreams(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = requireUserId(principal);
        List<DreamResponse> dreams = dreamService.getAllDreams(userId);
        return ResponseEntity.ok(dreams);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DreamResponse> getDreamById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = requireUserId(principal);
        DreamResponse dream = dreamService.getDreamById(id, userId);
        return ResponseEntity.ok(dream);
    }

    @PostMapping
    public ResponseEntity<DreamResponse> createDream(
            @Valid @RequestBody DreamRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = requireUserId(principal);
        DreamResponse createdDream = dreamService.createDream(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdDream);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DreamResponse> updateDream(
            @PathVariable Long id,
            @Valid @RequestBody DreamRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = requireUserId(principal);
        DreamResponse updatedDream = dreamService.updateDream(id, request, userId);
        return ResponseEntity.ok(updatedDream);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDream(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = requireUserId(principal);
        dreamService.deleteDream(id, userId);
        return ResponseEntity.noContent().build();
    }

    private Long requireUserId(UserPrincipal principal) {
        if (principal != null && principal.userId() != null) {
            return principal.userId();
        }
        throw new InvalidCredentialsException("Authentication required.");
    }
}

