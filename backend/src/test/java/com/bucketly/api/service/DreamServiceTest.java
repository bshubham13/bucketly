package com.bucketly.api.service;

import com.bucketly.api.dto.DreamRequest;
import com.bucketly.api.dto.DreamResponse;
import com.bucketly.api.entity.Dream;
import com.bucketly.api.entity.User;
import com.bucketly.api.exception.InvalidCredentialsException;
import com.bucketly.api.exception.ResourceNotFoundException;
import com.bucketly.api.repository.DreamRepository;
import com.bucketly.api.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DreamServiceTest {

    @Mock
    private DreamRepository dreamRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DreamService dreamService;

    private User sampleUser;
    private Dream sampleDream;

    @BeforeEach
    void setUp() {
        sampleUser = new User(1L, "owner@bucketly.local", "hash123", "Dream Owner");
        sampleDream = new Dream(
                1L,
                sampleUser,
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
    @DisplayName("getAllDreams should query by userId and return only the user's dreams")
    void getAllDreams_shouldReturnListForAuthenticatedUser() {
        when(dreamRepository.findAllByUserId(1L)).thenReturn(List.of(sampleDream));

        List<DreamResponse> result = dreamService.getAllDreams(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Northern Lights in Tromso");
        assertThat(result.get(0).category()).isEqualTo("Travel & Nature");
        verify(dreamRepository, times(1)).findAllByUserId(1L);
    }

    @Test
    @DisplayName("getDreamById should return DreamResponse when dream belongs to current user")
    void getDreamById_whenFoundForOwner_shouldReturnDreamResponse() {
        when(dreamRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(sampleDream));

        DreamResponse result = dreamService.getDreamById(1L, 1L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.title()).isEqualTo("Northern Lights in Tromso");
        verify(dreamRepository, times(1)).findByIdAndUserId(1L, 1L);
    }

    @Test
    @DisplayName("getDreamById should throw 404 ResourceNotFoundException when dream is foreign-owned, legacy, or missing")
    void getDreamById_whenNotFoundOrForeignOwned_shouldThrowException() {
        // User 2 trying to access Dream 1 (owned by User 1) or legacy dream (user_id IS NULL)
        when(dreamRepository.findByIdAndUserId(1L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dreamService.getDreamById(1L, 2L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("1");

        verify(dreamRepository, times(1)).findByIdAndUserId(1L, 2L);
    }

    @Test
    @DisplayName("createDream should load authenticated User, associate it, and save new Dream")
    void createDream_shouldAssociateUserAndSave() {
        DreamRequest request = new DreamRequest(
                "Run Tokyo Marathon",
                "Qualify for Abbott lottery",
                "Fitness & Health",
                "Spring 2027",
                null
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(dreamRepository.save(any(Dream.class))).thenAnswer(invocation -> {
            Dream toSave = invocation.getArgument(0);
            toSave.setId(2L);
            toSave.setCreatedAt(LocalDateTime.now());
            toSave.setUpdatedAt(LocalDateTime.now());
            return toSave;
        });

        DreamResponse created = dreamService.createDream(request, 1L);

        assertThat(created.id()).isEqualTo(2L);
        assertThat(created.title()).isEqualTo("Run Tokyo Marathon");
        assertThat(created.status()).isEqualTo("Not Started");
        verify(userRepository, times(1)).findById(1L);
        verify(dreamRepository, times(1)).save(argThat(d -> d.getUser() != null && d.getUser().getId().equals(1L)));
    }

    @Test
    @DisplayName("createDream should throw InvalidCredentialsException (401) if authenticated User is missing in database")
    void createDream_whenUserNotFound_shouldThrowException() {
        DreamRequest request = new DreamRequest("Title", "Desc", "Cat", "Date", "Status");
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dreamService.createDream(request, 999L))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Authentication required");

        verify(dreamRepository, never()).save(any(Dream.class));
    }

    @Test
    @DisplayName("updateDream should update fields when dream belongs to current user")
    void updateDream_whenFoundForOwner_shouldUpdateAndReturnResponse() {
        DreamRequest updateRequest = new DreamRequest(
                "Northern Lights in Tromso (Updated)",
                "Updated igloo reservation",
                "Travel & Nature",
                "Winter 2026",
                "Completed"
        );

        when(dreamRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(sampleDream));
        when(dreamRepository.save(any(Dream.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DreamResponse updated = dreamService.updateDream(1L, updateRequest, 1L);

        assertThat(updated.title()).isEqualTo("Northern Lights in Tromso (Updated)");
        assertThat(updated.status()).isEqualTo("Completed");
        verify(dreamRepository, times(1)).findByIdAndUserId(1L, 1L);
        verify(dreamRepository, times(1)).save(sampleDream);
    }

    @Test
    @DisplayName("updateDream should throw 404 when dream belongs to another user, is unassigned legacy, or missing")
    void updateDream_whenNotFoundOrForeignOwned_shouldThrowException() {
        DreamRequest updateRequest = new DreamRequest("Title", "Desc", "Cat", "Date", "Status");
        when(dreamRepository.findByIdAndUserId(1L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dreamService.updateDream(1L, updateRequest, 2L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("1");

        verify(dreamRepository, times(1)).findByIdAndUserId(1L, 2L);
        verify(dreamRepository, never()).save(any(Dream.class));
    }

    @Test
    @DisplayName("deleteDream should remove dream when dream belongs to current user")
    void deleteDream_whenFoundForOwner_shouldDelete() {
        when(dreamRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(sampleDream));

        dreamService.deleteDream(1L, 1L);

        verify(dreamRepository, times(1)).findByIdAndUserId(1L, 1L);
        verify(dreamRepository, times(1)).delete(sampleDream);
    }

    @Test
    @DisplayName("deleteDream should throw 404 when dream belongs to another user, is unassigned legacy, or missing")
    void deleteDream_whenNotFoundOrForeignOwned_shouldThrowException() {
        when(dreamRepository.findByIdAndUserId(1L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dreamService.deleteDream(1L, 2L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("1");

        verify(dreamRepository, times(1)).findByIdAndUserId(1L, 2L);
        verify(dreamRepository, never()).delete(any(Dream.class));
    }
}
