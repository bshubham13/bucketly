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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemoryServiceTest {

    @Mock
    private MemoryRepository memoryRepository;

    @Mock
    private DreamRepository dreamRepository;

    @InjectMocks
    private MemoryService memoryService;

    private Dream sampleDream;
    private Memory sampleMemory;

    @BeforeEach
    void setUp() {
        sampleDream = new Dream(
                1L,
                "Witness Northern Lights in Tromso",
                "Rent a glass igloo and watch aurora",
                "Travel & Nature",
                "Winter 2026",
                "In Progress"
        );
        sampleDream.setCreatedAt(LocalDateTime.now());
        sampleDream.setUpdatedAt(LocalDateTime.now());

        sampleMemory = new Memory(
                10L,
                sampleDream,
                LocalDate.of(2026, 1, 15),
                "Unbelievable green auroras over the fjords!",
                "uploads/tromso.jpg",
                "aurora_tromso.jpg"
        );
        sampleMemory.setCreatedAt(LocalDateTime.now());
        sampleMemory.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("getAllMemories should return list of MemoryResponse DTOs")
    void getAllMemories_shouldReturnList() {
        when(memoryRepository.findAll()).thenReturn(List.of(sampleMemory));

        List<MemoryResponse> result = memoryService.getAllMemories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(10L);
        assertThat(result.get(0).dreamId()).isEqualTo(1L);
        assertThat(result.get(0).dreamTitle()).isEqualTo("Witness Northern Lights in Tromso");
        verify(memoryRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getMemoryById should return MemoryResponse when found")
    void getMemoryById_whenFound_shouldReturnMemory() {
        when(memoryRepository.findById(10L)).thenReturn(Optional.of(sampleMemory));

        MemoryResponse result = memoryService.getMemoryById(10L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.story()).isEqualTo("Unbelievable green auroras over the fjords!");
        verify(memoryRepository, times(1)).findById(10L);
    }

    @Test
    @DisplayName("getMemoryById should throw ResourceNotFoundException when missing")
    void getMemoryById_whenNotFound_shouldThrowException() {
        when(memoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memoryService.getMemoryById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verify(memoryRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("createMemory should save memory and mark Dream status as Completed")
    void createMemory_whenValid_shouldSaveAndMarkDreamCompleted() {
        MemoryCreateRequest request = new MemoryCreateRequest(
                1L,
                LocalDate.of(2026, 1, 15),
                "Aurora trip accomplished!",
                "uploads/aurora.jpg",
                "aurora.jpg"
        );

        when(dreamRepository.findById(1L)).thenReturn(Optional.of(sampleDream));
        when(memoryRepository.existsByDreamId(1L)).thenReturn(false);
        when(memoryRepository.save(any(Memory.class))).thenAnswer(inv -> {
            Memory m = inv.getArgument(0);
            m.setId(10L);
            m.setCreatedAt(LocalDateTime.now());
            m.setUpdatedAt(LocalDateTime.now());
            return m;
        });

        MemoryResponse created = memoryService.createMemory(request);

        assertThat(created.id()).isEqualTo(10L);
        assertThat(created.dreamId()).isEqualTo(1L);
        assertThat(sampleDream.getStatus()).isEqualTo("Completed");
        verify(dreamRepository, times(1)).save(sampleDream);
        verify(memoryRepository, times(1)).save(any(Memory.class));
    }

    @Test
    @DisplayName("createMemory should throw ResourceNotFoundException when Dream does not exist")
    void createMemory_whenDreamNotFound_shouldThrowException() {
        MemoryCreateRequest request = new MemoryCreateRequest(
                999L,
                LocalDate.now(),
                "Non-existent dream story",
                null,
                null
        );

        when(dreamRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memoryService.createMemory(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verify(memoryRepository, never()).save(any(Memory.class));
    }

    @Test
    @DisplayName("createMemory should throw DuplicateResourceException when memory already exists for dream")
    void createMemory_whenDuplicate_shouldThrowException() {
        MemoryCreateRequest request = new MemoryCreateRequest(
                1L,
                LocalDate.now(),
                "Second story",
                null,
                null
        );

        when(dreamRepository.findById(1L)).thenReturn(Optional.of(sampleDream));
        when(memoryRepository.existsByDreamId(1L)).thenReturn(true);

        assertThatThrownBy(() -> memoryService.createMemory(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(memoryRepository, never()).save(any(Memory.class));
    }

    @Test
    @DisplayName("updateMemory should update completion date and story")
    void updateMemory_whenFound_shouldUpdate() {
        MemoryUpdateRequest request = new MemoryUpdateRequest(
                LocalDate.of(2026, 2, 20),
                "Updated personal story reflection",
                "uploads/new_aurora.jpg",
                "new_aurora.jpg"
        );

        when(memoryRepository.findById(10L)).thenReturn(Optional.of(sampleMemory));
        when(memoryRepository.save(any(Memory.class))).thenAnswer(inv -> inv.getArgument(0));

        MemoryResponse updated = memoryService.updateMemory(10L, request);

        assertThat(updated.completedDate()).isEqualTo(LocalDate.of(2026, 2, 20));
        assertThat(updated.story()).isEqualTo("Updated personal story reflection");
        verify(memoryRepository, times(1)).findById(10L);
        verify(memoryRepository, times(1)).save(sampleMemory);
    }

    @Test
    @DisplayName("updateMemory should throw ResourceNotFoundException when memory missing")
    void updateMemory_whenNotFound_shouldThrowException() {
        MemoryUpdateRequest request = new MemoryUpdateRequest(
                LocalDate.now(),
                "Story",
                null,
                null
        );

        when(memoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memoryService.updateMemory(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verify(memoryRepository, never()).save(any(Memory.class));
    }

    @Test
    @DisplayName("deleteMemory should delete memory only and preserve Dream status as Completed")
    void deleteMemory_whenFound_shouldDeleteOnlyMemoryAndKeepDreamCompleted() {
        sampleDream.setStatus("Completed");

        when(memoryRepository.findById(10L)).thenReturn(Optional.of(sampleMemory));

        memoryService.deleteMemory(10L);

        assertThat(sampleDream.getStatus()).isEqualTo("Completed");
        verify(dreamRepository, never()).save(any(Dream.class));
        verify(memoryRepository, times(1)).delete(sampleMemory);
    }

    @Test
    @DisplayName("deleteMemory should throw ResourceNotFoundException when memory missing")
    void deleteMemory_whenNotFound_shouldThrowException() {
        when(memoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memoryService.deleteMemory(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verify(memoryRepository, never()).delete(any(Memory.class));
    }
}

