package com.bucketly.api.service;

import com.bucketly.api.dto.DreamRequest;
import com.bucketly.api.dto.DreamResponse;
import com.bucketly.api.entity.Dream;
import com.bucketly.api.entity.User;
import com.bucketly.api.exception.InvalidCredentialsException;
import com.bucketly.api.exception.ResourceNotFoundException;
import com.bucketly.api.repository.DreamRepository;
import com.bucketly.api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DreamService {

    private final DreamRepository dreamRepository;
    private final UserRepository userRepository;

    public DreamService(DreamRepository dreamRepository, UserRepository userRepository) {
        this.dreamRepository = dreamRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<DreamResponse> getAllDreams(Long userId) {
        return dreamRepository.findAllByUserId(userId)
                .stream()
                .map(DreamResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public DreamResponse getDreamById(Long id, Long userId) {
        Dream dream = dreamRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Dream not found with id: " + id));
        return DreamResponse.fromEntity(dream);
    }

    public DreamResponse createDream(DreamRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("Authentication required."));

        Dream dream = new Dream();
        dream.setUser(user);
        dream.setTitle(request.title().trim());
        dream.setDescription(request.description() != null ? request.description().trim() : null);
        dream.setCategory(request.category().trim());
        dream.setTargetDate(request.targetDate() != null ? request.targetDate().trim() : null);
        dream.setStatus(request.status() != null && !request.status().isBlank() ? request.status().trim() : "Not Started");

        Dream savedDream = dreamRepository.save(dream);
        return DreamResponse.fromEntity(savedDream);
    }

    public DreamResponse updateDream(Long id, DreamRequest request, Long userId) {
        Dream existingDream = dreamRepository.findByIdAndUserId(id, userId)
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

    public void deleteDream(Long id, Long userId) {
        Dream existingDream = dreamRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Dream not found with id: " + id));
        dreamRepository.delete(existingDream);
    }
}

