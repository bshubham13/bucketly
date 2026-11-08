package com.bucketly.api.service;

import com.bucketly.api.dto.MemoryCreateRequest;
import com.bucketly.api.dto.MemoryResponse;
import com.bucketly.api.dto.MemoryUpdateRequest;
import com.bucketly.api.dto.PhotoDownload;
import com.bucketly.api.entity.Dream;
import com.bucketly.api.entity.Memory;
import com.bucketly.api.exception.DuplicateResourceException;
import com.bucketly.api.exception.InvalidPhotoException;
import com.bucketly.api.exception.ResourceNotFoundException;
import com.bucketly.api.repository.DreamRepository;
import com.bucketly.api.repository.MemoryRepository;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@Transactional
public class MemoryService {

    private final MemoryRepository memoryRepository;
    private final DreamRepository dreamRepository;
    private final PhotoStorage photoStorage;

    public MemoryService(MemoryRepository memoryRepository, DreamRepository dreamRepository, PhotoStorage photoStorage) {
        this.memoryRepository = memoryRepository;
        this.dreamRepository = dreamRepository;
        this.photoStorage = photoStorage;
    }

    @Transactional(readOnly = true)
    public List<MemoryResponse> getAllMemories(Long userId) {
        return memoryRepository.findAllByUserId(userId)
                .stream()
                .map(MemoryResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public MemoryResponse getMemoryById(Long id, Long userId) {
        Memory memory = findOwnedMemory(id, userId);
        return MemoryResponse.fromEntity(memory);
    }

    public MemoryResponse createMemory(MemoryCreateRequest request, Long userId) {
        Dream dream = dreamRepository.findByIdAndUserId(request.dreamId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Dream not found with id: " + request.dreamId()));

        if (memoryRepository.existsByDreamId(request.dreamId())) {
            throw new DuplicateResourceException("A memory already exists for dream with id: " + request.dreamId());
        }

        Memory memory = new Memory();
        memory.setDream(dream);
        memory.setCompletedDate(request.completedDate());
        memory.setStory(request.story() != null ? request.story().trim() : null);
        memory.setPhotoPath(request.photoPath() != null ? request.photoPath().trim() : null);
        memory.setOriginalFilename(request.originalFilename() != null ? request.originalFilename().trim() : null);

        // Keep Dream status consistent: marking a Dream completed upon memory creation
        if (!"Completed".equalsIgnoreCase(dream.getStatus())) {
            dream.setStatus("Completed");
            dreamRepository.save(dream);
        }

        Memory savedMemory = memoryRepository.save(memory);
        return MemoryResponse.fromEntity(savedMemory);
    }

    public MemoryResponse updateMemory(Long id, MemoryUpdateRequest request, Long userId) {
        Memory existingMemory = findOwnedMemory(id, userId);

        existingMemory.setCompletedDate(request.completedDate());
        existingMemory.setStory(request.story() != null ? request.story().trim() : null);

        // A normal memory edit must preserve existing photo metadata when photo fields are omitted.
        // Do not allow ordinary JSON updates to arbitrarily assign a filesystem storage key or bypass the upload endpoint.
        if (request.photoPath() != null && !request.photoPath().isBlank()
                && !request.photoPath().trim().equals(existingMemory.getPhotoPath())) {
            throw new InvalidPhotoException("Photo cannot be assigned via memory update. Use the dedicated photo upload endpoint.");
        }

        Memory updatedMemory = memoryRepository.save(existingMemory);
        return MemoryResponse.fromEntity(updatedMemory);
    }

    public MemoryResponse uploadPhoto(Long id, MultipartFile file, Long userId) {
        Memory memory = findOwnedMemory(id, userId);

        String oldPhotoKey = memory.getPhotoPath();
        String originalFilename = sanitizeFilename(file != null ? file.getOriginalFilename() : null);

        // Store new photo on disk (validates image format, decodability, and <= 5MB limit)
        String newStorageKey = photoStorage.store(file);

        memory.setPhotoPath(newStorageKey);
        memory.setOriginalFilename(originalFilename);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    if (oldPhotoKey != null && !oldPhotoKey.isBlank()) {
                        try {
                            photoStorage.delete(oldPhotoKey);
                        } catch (Exception ignored) {
                        }
                    }
                }

                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        try {
                            photoStorage.delete(newStorageKey);
                        } catch (Exception ignored) {
                        }
                    }
                }
            });
        } else {
            if (oldPhotoKey != null && !oldPhotoKey.isBlank()) {
                photoStorage.delete(oldPhotoKey);
            }
        }

        Memory savedMemory = memoryRepository.save(memory);
        return MemoryResponse.fromEntity(savedMemory);
    }

    @Transactional(readOnly = true)
    public PhotoDownload getPhoto(Long id, Long userId) {
        Memory memory = findOwnedMemory(id, userId);
        String photoPath = memory.getPhotoPath();
        if (photoPath == null || photoPath.isBlank()) {
            throw new ResourceNotFoundException("No photo associated with memory id: " + id);
        }

        Resource resource = photoStorage.loadAsResource(photoPath);
        String contentType = photoStorage.getContentType(photoPath);

        return new PhotoDownload(resource, contentType);
    }

    /**
     * Removes the photo from an existing memory.
     * Clears only photoPath and originalFilename, preserving all other memory attributes.
     * Physical file deletion is scheduled to execute only after the database transaction commits.
     * If no photo is associated with the memory, the operation is idempotent.
     */
    public MemoryResponse deletePhoto(Long id, Long userId) {
        Memory memory = findOwnedMemory(id, userId);
        String existingPhotoKey = memory.getPhotoPath();

        if (existingPhotoKey == null || existingPhotoKey.isBlank()) {
            return MemoryResponse.fromEntity(memory);
        }

        memory.setPhotoPath(null);
        memory.setOriginalFilename(null);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        photoStorage.delete(existingPhotoKey);
                    } catch (Exception ignored) {
                    }
                }
            });
        } else {
            photoStorage.delete(existingPhotoKey);
        }

        Memory savedMemory = memoryRepository.save(memory);
        return MemoryResponse.fromEntity(savedMemory);
    }

    /**
     * Deletes a memory record.
     * Business rule: Deleting a memory removes only the Memory reflection record from the Memory Wall.
     * It does NOT change the associated Dream status—a Dream that was Completed remains Completed.
     * Physical photo is deleted only after the database transaction commits.
     */
    public void deleteMemory(Long id, Long userId) {
        Memory existingMemory = findOwnedMemory(id, userId);
        String photoPath = existingMemory.getPhotoPath();

        memoryRepository.delete(existingMemory);

        if (photoPath != null && !photoPath.isBlank()) {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            photoStorage.delete(photoPath);
                        } catch (Exception ignored) {
                        }
                    }
                });
            } else {
                photoStorage.delete(photoPath);
            }
        }
    }

    private Memory findOwnedMemory(Long id, Long userId) {
        return memoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Memory not found with id: " + id));
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }
        String clean = filename.trim();
        int lastUnixSlash = clean.lastIndexOf('/');
        int lastWinSlash = clean.lastIndexOf('\\');
        int lastSlash = Math.max(lastUnixSlash, lastWinSlash);
        if (lastSlash >= 0 && lastSlash < clean.length() - 1) {
            clean = clean.substring(lastSlash + 1);
        }
        clean = clean.replaceAll("[\\p{Cntrl}]", "").trim();
        if (clean.isBlank()) {
            return null;
        }
        if (clean.length() > 255) {
            clean = clean.substring(0, 255);
        }
        return clean;
    }
}


