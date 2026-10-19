package com.bucketly.api.service;

import com.bucketly.api.dto.DreamRequest;
import com.bucketly.api.dto.DreamResponse;
import com.bucketly.api.entity.Dream;
import com.bucketly.api.exception.ResourceNotFoundException;
import com.bucketly.api.repository.DreamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DreamServiceTest {

    @Mock
    private DreamRepository dreamRepository;

    @InjectMocks
    private DreamService dreamService;

    private Dream sampleDream;

    @BeforeEach
    void setUp() {
        sampleDream = new Dream(
                1L,
                "Northern Lights in Tromso",
                "Rent a glass igloo and watch aurora",
                "Travel & Nature",
                "Winter 2026",
                "In Progress"
        );
        sampleDream.setCreatedAt(LocalDateTime.now());
        sampleDream.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("getAllDreams should return list of DreamResponse DTOs")
    void getAllDreams_shouldReturnList() {
        when(dreamRepository.findAll()).thenReturn(List.of(sampleDream));

        List<DreamResponse> result = dreamService.getAllDreams();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Northern Lights in Tromso");
        assertThat(result.get(0).category()).isEqualTo("Travel & Nature");
        verify(dreamRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getDreamById should return DreamResponse when dream exists")
    void getDreamById_whenFound_shouldReturnDreamResponse() {
        when(dreamRepository.findById(1L)).thenReturn(Optional.of(sampleDream));

        DreamResponse result = dreamService.getDreamById(1L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.title()).isEqualTo("Northern Lights in Tromso");
        verify(dreamRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getDreamById should throw ResourceNotFoundException when dream is missing")
    void getDreamById_whenNotFound_shouldThrowException() {
        when(dreamRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dreamService.getDreamById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verify(dreamRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("createDream should save and return new Dream with default status if null")
    void createDream_shouldSaveAndReturnResponse() {
        DreamRequest request = new DreamRequest(
                "Run Tokyo Marathon",
                "Qualify for Abbott lottery",
                "Fitness & Health",
                "Spring 2027",
                null
        );

        when(dreamRepository.save(any(Dream.class))).thenAnswer(invocation -> {
            Dream toSave = invocation.getArgument(0);
            toSave.setId(2L);
            toSave.setCreatedAt(LocalDateTime.now());
            toSave.setUpdatedAt(LocalDateTime.now());
            return toSave;
        });

        DreamResponse created = dreamService.createDream(request);

        assertThat(created.id()).isEqualTo(2L);
        assertThat(created.title()).isEqualTo("Run Tokyo Marathon");
        assertThat(created.status()).isEqualTo("Not Started");
        verify(dreamRepository, times(1)).save(any(Dream.class));
    }

    @Test
    @DisplayName("updateDream should update fields and return updated DreamResponse")
    void updateDream_whenFound_shouldUpdateAndReturnResponse() {
        DreamRequest updateRequest = new DreamRequest(
                "Northern Lights in Tromso (Updated)",
                "Updated igloo reservation",
                "Travel & Nature",
                "Winter 2026",
                "Completed"
        );

        when(dreamRepository.findById(1L)).thenReturn(Optional.of(sampleDream));
        when(dreamRepository.save(any(Dream.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DreamResponse updated = dreamService.updateDream(1L, updateRequest);

        assertThat(updated.title()).isEqualTo("Northern Lights in Tromso (Updated)");
        assertThat(updated.status()).isEqualTo("Completed");
        verify(dreamRepository, times(1)).findById(1L);
        verify(dreamRepository, times(1)).save(sampleDream);
    }

    @Test
    @DisplayName("updateDream should throw ResourceNotFoundException when dream is missing")
    void updateDream_whenNotFound_shouldThrowException() {
        DreamRequest updateRequest = new DreamRequest(
                "Title", "Desc", "Cat", "Date", "Status"
        );
        when(dreamRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dreamService.updateDream(999L, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verify(dreamRepository, times(1)).findById(999L);
        verify(dreamRepository, never()).save(any(Dream.class));
    }

    @Test
    @DisplayName("deleteDream should remove dream when dream exists")
    void deleteDream_whenFound_shouldDelete() {
        when(dreamRepository.findById(1L)).thenReturn(Optional.of(sampleDream));

        dreamService.deleteDream(1L);

        verify(dreamRepository, times(1)).findById(1L);
        verify(dreamRepository, times(1)).delete(sampleDream);
    }

    @Test
    @DisplayName("deleteDream should throw ResourceNotFoundException when dream is missing")
    void deleteDream_whenNotFound_shouldThrowException() {
        when(dreamRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dreamService.deleteDream(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verify(dreamRepository, times(1)).findById(999L);
        verify(dreamRepository, never()).delete(any(Dream.class));
    }
}

