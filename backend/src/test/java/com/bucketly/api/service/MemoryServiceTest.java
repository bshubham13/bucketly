package com.bucketly.api.service;

import com.bucketly.api.dto.MemoryCreateRequest;
import com.bucketly.api.dto.MemoryResponse;
import com.bucketly.api.dto.MemoryUpdateRequest;
import com.bucketly.api.dto.PhotoDownload;
import com.bucketly.api.entity.Dream;
import com.bucketly.api.entity.Memory;
import com.bucketly.api.entity.User;
import com.bucketly.api.exception.DuplicateResourceException;
import com.bucketly.api.exception.InvalidPhotoException;
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
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

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

    @Mock
    private PhotoStorage photoStorage;

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
    @DisplayName("updateMemory should update memory and preserve existing photo metadata when photo fields are omitted")
    void updateMemory_whenFoundForOwner_shouldUpdateAndPreservePhoto() {
        MemoryUpdateRequest request = new MemoryUpdateRequest(
                LocalDate.of(2026, 2, 20),
                "Updated personal story reflection",
                null,
                null
        );

        when(memoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleMemory));
        when(memoryRepository.save(any(Memory.class))).thenAnswer(inv -> inv.getArgument(0));

        MemoryResponse updated = memoryService.updateMemory(10L, request, 1L);

        assertThat(updated.completedDate()).isEqualTo(LocalDate.of(2026, 2, 20));
        assertThat(updated.story()).isEqualTo("Updated personal story reflection");
        assertThat(updated.photoPath()).isEqualTo("uploads/tromso.jpg");
        assertThat(updated.originalFilename()).isEqualTo("aurora_tromso.jpg");
        verify(memoryRepository, times(1)).findByIdAndUserId(10L, 1L);
        verify(memoryRepository, times(1)).save(sampleMemory);
    }

    @Test
    @DisplayName("updateMemory should throw InvalidPhotoException when attempting to assign arbitrary photoPath via JSON")
    void updateMemory_whenAttemptingToAssignArbitraryPhoto_shouldThrowInvalidPhotoException() {
        MemoryUpdateRequest request = new MemoryUpdateRequest(
                LocalDate.of(2026, 2, 20),
                "Updated story",
                "malicious-bypass.jpg",
                "hacked.jpg"
        );

        when(memoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleMemory));

        assertThatThrownBy(() -> memoryService.updateMemory(10L, request, 1L))
                .isInstanceOf(InvalidPhotoException.class)
                .hasMessageContaining("dedicated photo upload endpoint");

        verify(memoryRepository, never()).save(any(Memory.class));
    }

    @Test
    @DisplayName("uploadPhoto should update only photoPath and originalFilename while preserving other fields")
    void uploadPhoto_whenOwner_storesNewPhotoAndPreservesOtherFields() {
        MockMultipartFile file = new MockMultipartFile("file", "C:\\Users\\user\\photos\\tromso_aurora.png", "image/png", new byte[] {1, 2, 3});
        String newKey = "new-uuid-photo.png";

        when(memoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleMemory));
        when(photoStorage.store(file)).thenReturn(newKey);
        when(memoryRepository.save(any(Memory.class))).thenAnswer(inv -> inv.getArgument(0));

        MemoryResponse response = memoryService.uploadPhoto(10L, file, 1L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.photoPath()).isEqualTo(newKey);
        // Original filename should be sanitized (path stripped)
        assertThat(response.originalFilename()).isEqualTo("tromso_aurora.png");
        // Existing dream, story, completed date must be untouched
        assertThat(response.story()).isEqualTo("Unbelievable green auroras over the fjords!");
        assertThat(response.completedDate()).isEqualTo(LocalDate.of(2026, 1, 15));
        assertThat(response.dreamId()).isEqualTo(1L);

        verify(memoryRepository).save(sampleMemory);
    }

    @Test
    @DisplayName("uploadPhoto should throw 404 when memory is missing or belongs to another user")
    void uploadPhoto_whenForeignOrMissing_throws404() {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", new byte[] {1, 2, 3});
        when(memoryRepository.findByIdAndUserId(999L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memoryService.uploadPhoto(999L, file, 2L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verify(photoStorage, never()).store(any());
        verify(memoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("uploadPhoto should delete old photo after transaction commit")
    void uploadPhoto_transactionCommit_deletesOldPhoto() {
        MockMultipartFile file = new MockMultipartFile("file", "new.png", "image/png", new byte[] {1});
        when(memoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleMemory));
        when(photoStorage.store(file)).thenReturn("new-uuid.png");
        when(memoryRepository.save(any(Memory.class))).thenAnswer(inv -> inv.getArgument(0));

        try {
            TransactionSynchronizationManager.initSynchronization();
            memoryService.uploadPhoto(10L, file, 1L);

            // Before commit, old photo should not be deleted yet
            verify(photoStorage, never()).delete("uploads/tromso.jpg");

            // Trigger commit
            TransactionSynchronizationUtils.triggerAfterCommit();

            // After commit, old photo must be deleted
            verify(photoStorage).delete("uploads/tromso.jpg");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("uploadPhoto should delete newly created photo after transaction rollback")
    void uploadPhoto_transactionRollback_deletesNewPhoto() {
        MockMultipartFile file = new MockMultipartFile("file", "new.png", "image/png", new byte[] {1});
        when(memoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleMemory));
        when(photoStorage.store(file)).thenReturn("new-uuid.png");
        when(memoryRepository.save(any(Memory.class))).thenAnswer(inv -> inv.getArgument(0));

        try {
            TransactionSynchronizationManager.initSynchronization();
            memoryService.uploadPhoto(10L, file, 1L);

            // Trigger rollback
            TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

            // On rollback, the newly uploaded photo must be deleted, and old photo preserved
            verify(photoStorage).delete("new-uuid.png");
            verify(photoStorage, never()).delete("uploads/tromso.jpg");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("getPhoto should return PhotoDownload for owner when memory and photo exist")
    void getPhoto_whenOwnerAndPhotoExists_returnsPhotoDownload() {
        Resource mockResource = mock(Resource.class);
        when(memoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleMemory));
        when(photoStorage.loadAsResource("uploads/tromso.jpg")).thenReturn(mockResource);
        when(photoStorage.getContentType("uploads/tromso.jpg")).thenReturn("image/jpeg");

        PhotoDownload download = memoryService.getPhoto(10L, 1L);

        assertThat(download).isNotNull();
        assertThat(download.resource()).isSameAs(mockResource);
        assertThat(download.contentType()).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("getPhoto should throw 404 when memory is missing or belongs to another user")
    void getPhoto_whenForeignOrMissing_throws404() {
        when(memoryRepository.findByIdAndUserId(999L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memoryService.getPhoto(999L, 2L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("getPhoto should throw 404 when memory has no photo assigned")
    void getPhoto_whenNoPhotoAssigned_throws404() {
        sampleMemory.setPhotoPath(null);
        when(memoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleMemory));

        assertThatThrownBy(() -> memoryService.getPhoto(10L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No photo associated");
    }

    @Test
    @DisplayName("getPhoto should propagate 404 when physical file is missing from storage")
    void getPhoto_whenPhysicalFileMissing_throws404() {
        when(memoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleMemory));
        when(photoStorage.loadAsResource("uploads/tromso.jpg"))
                .thenThrow(new ResourceNotFoundException("Photo not found"));

        assertThatThrownBy(() -> memoryService.getPhoto(10L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Photo not found");
    }

    @Test
    @DisplayName("deleteMemory should delete physical photo only after transaction commit")
    void deleteMemory_whenPhotoExists_deletesPhysicalPhotoAfterCommit() {
        when(memoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleMemory));

        try {
            TransactionSynchronizationManager.initSynchronization();
            memoryService.deleteMemory(10L, 1L);

            verify(memoryRepository).delete(sampleMemory);
            // Before commit, file must not be deleted yet
            verify(photoStorage, never()).delete("uploads/tromso.jpg");

            // Trigger commit
            TransactionSynchronizationUtils.triggerAfterCommit();

            // After commit, file must be deleted
            verify(photoStorage).delete("uploads/tromso.jpg");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
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

    @Test
    @DisplayName("deletePhoto should clear photo metadata and delete physical file after transaction commit")
    void deletePhoto_whenOwnedMemoryHasPhoto_shouldClearPhotoAndSchedulePhysicalDeletion() {
        sampleMemory.setPhotoPath("photos/existing-uuid.jpg");
        sampleMemory.setOriginalFilename("my-photo.jpg");

        when(memoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleMemory));
        when(memoryRepository.save(any(Memory.class))).thenAnswer(inv -> inv.getArgument(0));

        TransactionSynchronizationManager.initSynchronization();
        try {
            MemoryResponse response = memoryService.deletePhoto(10L, 1L);

            assertThat(response.photoPath()).isNull();
            assertThat(response.originalFilename()).isNull();
            assertThat(sampleMemory.getPhotoPath()).isNull();
            assertThat(sampleMemory.getOriginalFilename()).isNull();

            // File deletion should NOT happen before transaction commit
            verify(photoStorage, never()).delete("photos/existing-uuid.jpg");

            // Simulate transaction commit
            for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
                sync.afterCommit();
            }

            verify(photoStorage, times(1)).delete("photos/existing-uuid.jpg");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("deletePhoto on transaction rollback should preserve physical file")
    void deletePhoto_whenTransactionRollsBack_shouldPreservePhysicalFile() {
        sampleMemory.setPhotoPath("photos/keep-uuid.jpg");
        sampleMemory.setOriginalFilename("keep.jpg");

        when(memoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleMemory));
        when(memoryRepository.save(any(Memory.class))).thenAnswer(inv -> inv.getArgument(0));

        TransactionSynchronizationManager.initSynchronization();
        try {
            memoryService.deletePhoto(10L, 1L);

            // Simulate transaction rollback (no afterCommit call)
            for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
                sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            }

            // Physical file must NOT be deleted
            verify(photoStorage, never()).delete("photos/keep-uuid.jpg");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("deletePhoto when memory has no photo should be idempotent and not trigger photoStorage")
    void deletePhoto_whenNoPhotoExists_shouldBeIdempotent() {
        sampleMemory.setPhotoPath(null);
        sampleMemory.setOriginalFilename(null);

        when(memoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleMemory));

        MemoryResponse response = memoryService.deletePhoto(10L, 1L);

        assertThat(response.photoPath()).isNull();
        assertThat(response.originalFilename()).isNull();
        verify(memoryRepository, never()).save(any(Memory.class));
        verify(photoStorage, never()).delete(anyString());
    }

    @Test
    @DisplayName("deletePhoto should throw 404 when memory belongs to another user or is missing")
    void deletePhoto_whenNotFoundOrForeignOwned_shouldThrowException() {
        when(memoryRepository.findByIdAndUserId(999L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memoryService.deletePhoto(999L, 2L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verify(memoryRepository, times(1)).findByIdAndUserId(999L, 2L);
        verify(memoryRepository, never()).save(any(Memory.class));
        verify(photoStorage, never()).delete(anyString());
    }
}
