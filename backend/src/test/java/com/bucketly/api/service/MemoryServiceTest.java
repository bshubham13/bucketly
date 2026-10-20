package com.bucketly.api.service;

import com.bucketly.api.dto.MemoryCreateRequest;
import com.bucketly.api.dto.MemoryResponse;
import com.bucketly.api.dto.MemoryUpdateRequest;
import com.bucketly.api.entity.Dream;
import com.bucketly.api.entity.Memory;
import com.bucketly.api.entity.User;
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

    private User sampleUser;
    private Dream sampleDream;
    private Memory sampleMemory;

    @BeforeEach
    void setUp() {
        sampleUser = new User(1L, "owner@bucketly.local", "hash123", "Dream Owner");
        sampleDream = new Dream(
                1L,
                sampleUser,
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
    @DisplayName("getAllMemories should return list of memories belonging to authenticated user's dreams")
    void getAllMemories_shouldReturnList() {
        when(memoryRepository.findAllByUserId(1L)).thenReturn(List.of(sampleMemory));

        List<MemoryResponse> result = memoryService.getAllMemories(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(10L);
        assertThat(result.get(0).dreamId()).isEqualTo(1L);
        assertThat(result.get(0).dreamTitle()).isEqualTo("Witness Northern Lights in Tromso");
        verify(memoryRepository, times(1)).findAllByUserId(1L);
    }

    @Test
    @DisplayName("getMemoryById should return MemoryResponse when memory belongs to current user")
    void getMemoryById_whenFoundForOwner_shouldReturnMemory() {
        when(memoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleMemory));

        MemoryResponse result = memoryService.getMemoryById(10L, 1L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.story()).isEqualTo("Unbelievable green auroras over the fjords!");
        verify(memoryRepository, times(1)).findByIdAndUserId(10L, 1L);
    }

    @Test
    @DisplayName("getMemoryById should throw 404 when memory is foreign-owned, legacy, or missing")
    void getMemoryById_whenNotFoundOrForeignOwned_shouldThrowException() {
        when(memoryRepository.findByIdAndUserId(10L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memoryService.getMemoryById(10L, 2L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("10");

        verify(memoryRepository, times(1)).findByIdAndUserId(10L, 2L);
    }

    @Test
    @DisplayName("createMemory should save memory and mark Dream Completed when dream belongs to current user")
    void createMemory_whenValidForOwnedDream_shouldSaveAndMarkDreamCompleted() {
        MemoryCreateRequest request = new MemoryCreateRequest(
                1L,
                LocalDate.of(2026, 1, 15),
                "Aurora trip accomplished!",
                "uploads/aurora.jpg",
                "aurora.jpg"
        );

        when(dreamRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(sampleDream));
        when(memoryRepository.existsByDreamId(1L)).thenReturn(false);
        when(memoryRepository.save(any(Memory.class))).thenAnswer(inv -> {
            Memory m = inv.getArgument(0);
            m.setId(10L);
            m.setCreatedAt(LocalDateTime.now());
            m.setUpdatedAt(LocalDateTime.now());
            return m;
        });

        MemoryResponse created = memoryService.createMemory(request, 1L);

        assertThat(created.id()).isEqualTo(10L);
        assertThat(created.dreamId()).isEqualTo(1L);
        assertThat(sampleDream.getStatus()).isEqualTo("Completed");
        verify(dreamRepository, times(1)).findByIdAndUserId(1L, 1L);
        verify(dreamRepository, times(1)).save(sampleDream);
        verify(memoryRepository, times(1)).save(any(Memory.class));
    }

    @Test
    @DisplayName("createMemory should throw 404 when referenced Dream belongs to another user or is unassigned legacy")
    void createMemory_whenDreamForeignOwnedOrLegacy_shouldThrowException() {
        MemoryCreateRequest request = new MemoryCreateRequest(
                1L,
                LocalDate.now(),
                "Attempting to claim someone else's dream",
                null,
                null
        );

        // User 2 attempts to create memory on Dream 1 (owned by User 1) -> 404
        when(dreamRepository.findByIdAndUserId(1L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memoryService.createMemory(request, 2L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("1");

        verify(dreamRepository, times(1)).findByIdAndUserId(1L, 2L);
        verify(memoryRepository, never()).save(any(Memory.class));
    }

    @Test
    @DisplayName("createMemory should throw DuplicateResourceException when memory already exists for owned dream")
    void createMemory_whenDuplicateForOwnedDream_shouldThrowException() {
        MemoryCreateRequest request = new MemoryCreateRequest(
                1L,
                LocalDate.now(),
                "Second story",
                null,
                null
        );

        when(dreamRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(sampleDream));
        when(memoryRepository.existsByDreamId(1L)).thenReturn(true);

        assertThatThrownBy(() -> memoryService.createMemory(request, 1L))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(memoryRepository, never()).save(any(Memory.class));
    }

    @Test
    @DisplayName("updateMemory should update memory when memory belongs to current user")
    void updateMemory_whenFoundForOwner_shouldUpdate() {
        MemoryUpdateRequest request = new MemoryUpdateRequest(
                LocalDate.of(2026, 2, 20),
                "Updated personal story reflection",
                "uploads/new_aurora.jpg",
                "new_aurora.jpg"
        );

        when(memoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleMemory));
        when(memoryRepository.save(any(Memory.class))).thenAnswer(inv -> inv.getArgument(0));

        MemoryResponse updated = memoryService.updateMemory(10L, request, 1L);

        assertThat(updated.completedDate()).isEqualTo(LocalDate.of(2026, 2, 20));
        assertThat(updated.story()).isEqualTo("Updated personal story reflection");
        verify(memoryRepository, times(1)).findByIdAndUserId(10L, 1L);
        verify(memoryRepository, times(1)).save(sampleMemory);
    }

    @Test
    @DisplayName("updateMemory should throw 404 when memory belongs to another user, legacy, or missing")
    void updateMemory_whenNotFoundOrForeignOwned_shouldThrowException() {
        MemoryUpdateRequest request = new MemoryUpdateRequest(
                LocalDate.now(),
                "Story",
                null,
                null
        );

        when(memoryRepository.findByIdAndUserId(999L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memoryService.updateMemory(999L, request, 2L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verify(memoryRepository, times(1)).findByIdAndUserId(999L, 2L);
        verify(memoryRepository, never()).save(any(Memory.class));
    }

    @Test
    @DisplayName("deleteMemory should delete memory and preserve Dream status when memory belongs to current user")
    void deleteMemory_whenFoundForOwner_shouldDeleteOnlyMemoryAndKeepDreamCompleted() {
        sampleDream.setStatus("Completed");

        when(memoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleMemory));

        memoryService.deleteMemory(10L, 1L);

        assertThat(sampleDream.getStatus()).isEqualTo("Completed");
        verify(memoryRepository, times(1)).findByIdAndUserId(10L, 1L);
        verify(dreamRepository, never()).save(any(Dream.class));
        verify(memoryRepository, times(1)).delete(sampleMemory);
    }

    @Test
    @DisplayName("deleteMemory should throw 404 when memory belongs to another user, legacy, or missing")
    void deleteMemory_whenNotFoundOrForeignOwned_shouldThrowException() {
        when(memoryRepository.findByIdAndUserId(999L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memoryService.deleteMemory(999L, 2L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verify(memoryRepository, times(1)).findByIdAndUserId(999L, 2L);
        verify(memoryRepository, never()).delete(any(Memory.class));
    }
}
