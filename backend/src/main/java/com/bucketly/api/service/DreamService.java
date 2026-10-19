package com.bucketly.api.service;

import com.bucketly.api.dto.DreamRequest;
import com.bucketly.api.dto.DreamResponse;
import com.bucketly.api.entity.Dream;
import com.bucketly.api.exception.ResourceNotFoundException;
import com.bucketly.api.repository.DreamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DreamService {

    private final DreamRepository dreamRepository;

    public DreamService(DreamRepository dreamRepository) {
        this.dreamRepository = dreamRepository;
    }

    @Transactional(readOnly = true)
    public List<DreamResponse> getAllDreams() {
        return dreamRepository.findAll()
                .stream()
                .map(DreamResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public DreamResponse getDreamById(Long id) {
        Dream dream = dreamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dream not found with id: " + id));
        return DreamResponse.fromEntity(dream);
    }

    public DreamResponse createDream(DreamRequest request) {
        Dream dream = new Dream();
        dream.setTitle(request.title().trim());
        dream.setDescription(request.description() != null ? request.description().trim() : null);
        dream.setCategory(request.category().trim());
        dream.setTargetDate(request.targetDate() != null ? request.targetDate().trim() : null);
        dream.setStatus(request.status() != null && !request.status().isBlank() ? request.status().trim() : "Not Started");

        Dream savedDream = dreamRepository.save(dream);
        return DreamResponse.fromEntity(savedDream);
    }

    public DreamResponse updateDream(Long id, DreamRequest request) {
        Dream existingDream = dreamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dream not found with id: " + id));

        existingDream.setTitle(request.title().trim());
        existingDream.setDescription(request.description() != null ? request.description().trim() : null);
        existingDream.setCategory(request.category().trim());
        existingDream.setTargetDate(request.targetDate() != null ? request.targetDate().trim() : null);
        if (request.status() != null && !request.status().isBlank()) {
            existingDream.setStatus(request.status().trim());
        }

        Dream updatedDream = dreamRepository.save(existingDream);
        return DreamResponse.fromEntity(updatedDream);
    }

    public void deleteDream(Long id) {
        Dream existingDream = dreamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dream not found with id: " + id));
        dreamRepository.delete(existingDream);
    }
}

