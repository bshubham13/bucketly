package com.bucketly.api.service;

import com.bucketly.api.dto.MemoryCreateRequest;
import com.bucketly.api.dto.MemoryResponse;
import com.bucketly.api.dto.MemoryUpdateRequest;
import com.bucketly.api.entity.Dream;
import com.bucketly.api.entity.Memory;
import com.bucketly.api.exception.DuplicateResourceException;
import com.bucketly.api.exception.ResourceNotFoundException;
import com.bucketly.api.repository.DreamRepository;
import com.bucketly.api.repository.MemoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class MemoryService {

    private final MemoryRepository memoryRepository;
    private final DreamRepository dreamRepository;

    public MemoryService(MemoryRepository memoryRepository, DreamRepository dreamRepository) {
        this.memoryRepository = memoryRepository;
        this.dreamRepository = dreamRepository;
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
        Memory memory = memoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Memory not found with id: " + id));
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
        Memory existingMemory = memoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Memory not found with id: " + id));

        existingMemory.setCompletedDate(request.completedDate());
        existingMemory.setStory(request.story() != null ? request.story().trim() : null);
        existingMemory.setPhotoPath(request.photoPath() != null ? request.photoPath().trim() : null);
        existingMemory.setOriginalFilename(request.originalFilename() != null ? request.originalFilename().trim() : null);

        Memory updatedMemory = memoryRepository.save(existingMemory);
        return MemoryResponse.fromEntity(updatedMemory);
    }

    /**
     * Deletes a memory record.
     * Business rule: Deleting a memory removes only the Memory reflection record from the Memory Wall.
     * It does NOT change the associated Dream status—a Dream that was Completed remains Completed.
     */
    public void deleteMemory(Long id, Long userId) {
        Memory existingMemory = memoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Memory not found with id: " + id));

        memoryRepository.delete(existingMemory);
    }
}

