package com.bucketly.api.controller;

import com.bucketly.api.config.SecurityConfig;
import com.bucketly.api.dto.DreamRequest;
import com.bucketly.api.dto.DreamResponse;
import com.bucketly.api.exception.GlobalExceptionHandler;
import com.bucketly.api.exception.ResourceNotFoundException;
import com.bucketly.api.security.JwtService;
import com.bucketly.api.security.UserPrincipal;
import com.bucketly.api.service.DreamService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {DreamController.class, GlobalExceptionHandler.class})
@Import(SecurityConfig.class)
class DreamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DreamService dreamService;

    @MockitoBean
    private JwtService jwtService;

    private final UserPrincipal user1 = new UserPrincipal(1L, "user1@bucketly.local");
    private final UserPrincipal user2 = new UserPrincipal(2L, "user2@bucketly.local");

    @Test
    @DisplayName("GET /api/dreams returns 200 OK with dreams list for authenticated user")
    void getAllDreams_shouldReturn200() throws Exception {
        DreamResponse response = new DreamResponse(
                1L,
                "Witness Northern Lights",
                "Rent a glass igloo",
                "Travel & Nature",
                "Winter 2026",
                "In Progress",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        when(dreamService.getAllDreams(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/dreams")
                        .with(user(user1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Witness Northern Lights"))
                .andExpect(jsonPath("$[0].category").value("Travel & Nature"));

        verify(dreamService).getAllDreams(1L);
    }

    @Test
    @DisplayName("GET /api/dreams returns 401 Unauthorized when unauthenticated")
    void getAllDreams_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/dreams"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("GET /api/dreams/{id} returns 200 OK when dream belongs to current user")
    void getDreamById_whenFound_shouldReturn200() throws Exception {
        DreamResponse response = new DreamResponse(
                1L,
                "Witness Northern Lights",
                "Rent a glass igloo",
                "Travel & Nature",
                "Winter 2026",
                "In Progress",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        when(dreamService.getDreamById(1L, 1L)).thenReturn(response);

        mockMvc.perform(get("/api/dreams/1")
                        .with(user(user1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Witness Northern Lights"))
                .andExpect(jsonPath("$.status").value("In Progress"));

        verify(dreamService).getDreamById(1L, 1L);
    }

    @Test
    @DisplayName("GET /api/dreams/{id} returns 404 Not Found when dream is missing, foreign-owned, or legacy")
    void getDreamById_whenNotFoundOrForeignOwned_shouldReturn404() throws Exception {
        when(dreamService.getDreamById(999L, 1L)).thenThrow(new ResourceNotFoundException("Dream not found with id: 999"));

        mockMvc.perform(get("/api/dreams/999")
                        .with(user(user1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Dream not found with id: 999"));
    }

    @Test
    @DisplayName("POST /api/dreams returns 201 Created on valid request and passes userId")
    void createDream_whenValid_shouldReturn201() throws Exception {
        DreamResponse created = new DreamResponse(
                2L,
                "Run Tokyo Marathon",
                "Abbott lottery",
                "Fitness & Health",
                "Spring 2027",
                "Not Started",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        when(dreamService.createDream(any(DreamRequest.class), eq(1L))).thenReturn(created);

        String json = """
                {
                    "title": "Run Tokyo Marathon",
                    "description": "Abbott lottery",
                    "category": "Fitness & Health",
                    "targetDate": "Spring 2027",
                    "status": "Not Started"
                }
                """;

        mockMvc.perform(post("/api/dreams")
                        .with(user(user1))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.title").value("Run Tokyo Marathon"))
                .andExpect(jsonPath("$.category").value("Fitness & Health"));

        verify(dreamService).createDream(any(DreamRequest.class), eq(1L));
    }

    @Test
    @DisplayName("POST /api/dreams returns 400 Bad Request when title is blank")
    void createDream_whenTitleBlank_shouldReturn400() throws Exception {
        String invalidJson = """
                {
                    "title": "",
                    "category": "Fitness & Health"
                }
                """;

        mockMvc.perform(post("/api/dreams")
                        .with(user(user1))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.validationErrors.title").exists());
    }

    @Test
    @DisplayName("PUT /api/dreams/{id} returns 200 OK on valid update by owner")
    void updateDream_whenFound_shouldReturn200() throws Exception {
        DreamResponse updated = new DreamResponse(
                1L,
                "Northern Lights Updated",
                "Updated igloo reservation",
                "Travel & Nature",
                "Winter 2026",
                "Completed",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        when(dreamService.updateDream(eq(1L), any(DreamRequest.class), eq(1L))).thenReturn(updated);

        String updateJson = """
                {
                    "title": "Northern Lights Updated",
                    "description": "Updated igloo reservation",
                    "category": "Travel & Nature",
                    "targetDate": "Winter 2026",
                    "status": "Completed"
                }
                """;

        mockMvc.perform(put("/api/dreams/1")
                        .with(user(user1))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Northern Lights Updated"))
                .andExpect(jsonPath("$.status").value("Completed"));

        verify(dreamService).updateDream(eq(1L), any(DreamRequest.class), eq(1L));
    }

    @Test
    @DisplayName("PUT /api/dreams/{id} returns 404 when dream belongs to another user or is missing")
    void updateDream_whenNotFoundOrForeignOwned_shouldReturn404() throws Exception {
        when(dreamService.updateDream(eq(1L), any(DreamRequest.class), eq(2L)))
                .thenThrow(new ResourceNotFoundException("Dream not found with id: 1"));

        String updateJson = """
                {
                    "title": "Attempting cross-user edit",
                    "category": "Travel"
                }
                """;

        mockMvc.perform(put("/api/dreams/1")
                        .with(user(user2))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));

        verify(dreamService).updateDream(eq(1L), any(DreamRequest.class), eq(2L));
    }

    @Test
    @DisplayName("DELETE /api/dreams/{id} returns 204 No Content when deleted by owner")
    void deleteDream_whenFound_shouldReturn204() throws Exception {
        doNothing().when(dreamService).deleteDream(1L, 1L);

        mockMvc.perform(delete("/api/dreams/1")
                        .with(user(user1))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(dreamService).deleteDream(1L, 1L);
    }

    @Test
    @DisplayName("DELETE /api/dreams/{id} returns 404 when dream belongs to another user or is missing")
    void deleteDream_whenNotFoundOrForeignOwned_shouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("Dream not found with id: 1"))
                .when(dreamService).deleteDream(1L, 2L);

        mockMvc.perform(delete("/api/dreams/1")
                        .with(user(user2))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));

        verify(dreamService).deleteDream(1L, 2L);
    }
}
