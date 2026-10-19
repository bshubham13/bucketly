package com.bucketly.api.controller;

import com.bucketly.api.dto.MemoryCreateRequest;
import com.bucketly.api.dto.MemoryResponse;
import com.bucketly.api.dto.MemoryUpdateRequest;
import com.bucketly.api.exception.DuplicateResourceException;
import com.bucketly.api.exception.GlobalExceptionHandler;
import com.bucketly.api.exception.ResourceNotFoundException;
import com.bucketly.api.service.MemoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {MemoryController.class, GlobalExceptionHandler.class})
class MemoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemoryService memoryService;

    @Test
    @DisplayName("GET /api/memories returns 200 OK with list of memories")
    void getAllMemories_shouldReturn200() throws Exception {
        MemoryResponse response = new MemoryResponse(
                10L,
                1L,
                "Northern Lights in Tromso",
                "Travel & Nature",
                LocalDate.of(2026, 1, 15),
                "Spectacular lights!",
                "uploads/aurora.jpg",
                "aurora.jpg",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        when(memoryService.getAllMemories()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/memories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].dreamId").value(1))
                .andExpect(jsonPath("$[0].dreamTitle").value("Northern Lights in Tromso"))
                .andExpect(jsonPath("$[0].story").value("Spectacular lights!"));
    }

    @Test
    @DisplayName("GET /api/memories/{id} returns 200 OK when found")
    void getMemoryById_whenFound_shouldReturn200() throws Exception {
        MemoryResponse response = new MemoryResponse(
                10L,
                1L,
                "Northern Lights in Tromso",
                "Travel & Nature",
                LocalDate.of(2026, 1, 15),
                "Spectacular lights!",
                "uploads/aurora.jpg",
                "aurora.jpg",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        when(memoryService.getMemoryById(10L)).thenReturn(response);

        mockMvc.perform(get("/api/memories/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.dreamId").value(1))
                .andExpect(jsonPath("$.story").value("Spectacular lights!"));
    }

    @Test
    @DisplayName("GET /api/memories/{id} returns 404 Not Found when missing")
    void getMemoryById_whenNotFound_shouldReturn404() throws Exception {
        when(memoryService.getMemoryById(999L))
                .thenThrow(new ResourceNotFoundException("Memory not found with id: 999"));

        mockMvc.perform(get("/api/memories/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Memory not found with id: 999"));
    }

    @Test
    @DisplayName("POST /api/memories returns 201 Created on valid request with past date")
    void createMemory_whenValidPastDate_shouldReturn201() throws Exception {
        MemoryResponse created = new MemoryResponse(
                10L,
                1L,
                "Witness Northern Lights",
                "Travel & Nature",
                LocalDate.of(2026, 1, 15),
                "Unbelievable experience in glass igloo!",
                "uploads/tromso.jpg",
                "tromso.jpg",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        when(memoryService.createMemory(any(MemoryCreateRequest.class))).thenReturn(created);

        String json = """
                {
                    "dreamId": 1,
                    "completedDate": "2026-01-15",
                    "story": "Unbelievable experience in glass igloo!",
                    "photoPath": "uploads/tromso.jpg",
                    "originalFilename": "tromso.jpg"
                }
                """;

        mockMvc.perform(post("/api/memories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.dreamId").value(1))
                .andExpect(jsonPath("$.completedDate").value("2026-01-15"))
                .andExpect(jsonPath("$.story").value("Unbelievable experience in glass igloo!"));
    }

    @Test
    @DisplayName("POST /api/memories returns 201 Created on valid request with today's date")
    void createMemory_whenValidTodayDate_shouldReturn201() throws Exception {
        LocalDate today = LocalDate.now();
        MemoryResponse created = new MemoryResponse(
                11L,
                2L,
                "Tokyo Marathon",
                "Fitness",
                today,
                "Finished the marathon today!",
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        when(memoryService.createMemory(any(MemoryCreateRequest.class))).thenReturn(created);

        String json = String.format("""
                {
                    "dreamId": 2,
                    "completedDate": "%s",
                    "story": "Finished the marathon today!"
                }
                """, today);

        mockMvc.perform(post("/api/memories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.completedDate").value(today.toString()));
    }

    @Test
    @DisplayName("POST /api/memories returns 400 Bad Request when completedDate is in the future")
    void createMemory_whenCompletedDateInFuture_shouldReturn400() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(5);
        String json = String.format("""
                {
                    "dreamId": 1,
                    "completedDate": "%s",
                    "story": "Cannot complete in the future"
                }
                """, futureDate);

        mockMvc.perform(post("/api/memories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.validationErrors.completedDate").value("Completion date cannot be in the future"));
    }

    @Test
    @DisplayName("POST /api/memories returns 400 Bad Request when dreamId is null")
    void createMemory_whenDreamIdNull_shouldReturn400() throws Exception {
        String json = """
                {
                    "completedDate": "2026-01-15",
                    "story": "Missing dream id"
                }
                """;

        mockMvc.perform(post("/api/memories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.validationErrors.dreamId").exists());
    }

    @Test
    @DisplayName("POST /api/memories returns 400 Bad Request when completedDate is null")
    void createMemory_whenCompletedDateNull_shouldReturn400() throws Exception {
        String json = """
                {
                    "dreamId": 1,
                    "story": "Missing completed date"
                }
                """;

        mockMvc.perform(post("/api/memories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.validationErrors.completedDate").exists());
    }

    @Test
    @DisplayName("POST /api/memories returns 404 Not Found when Dream does not exist")
    void createMemory_whenDreamNotFound_shouldReturn404() throws Exception {
        when(memoryService.createMemory(any(MemoryCreateRequest.class)))
                .thenThrow(new ResourceNotFoundException("Dream not found with id: 999"));

        String json = """
                {
                    "dreamId": 999,
                    "completedDate": "2026-01-15",
                    "story": "Dream not existing"
                }
                """;

        mockMvc.perform(post("/api/memories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Dream not found with id: 999"));
    }

    @Test
    @DisplayName("POST /api/memories returns 409 Conflict when duplicate memory for dream in service")
    void createMemory_whenDuplicate_shouldReturn409() throws Exception {
        when(memoryService.createMemory(any(MemoryCreateRequest.class)))
                .thenThrow(new DuplicateResourceException("A memory already exists for dream with id: 1"));

        String json = """
                {
                    "dreamId": 1,
                    "completedDate": "2026-01-15",
                    "story": "Duplicate memory"
                }
                """;

        mockMvc.perform(post("/api/memories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("A memory already exists for dream with id: 1"));
    }

    @Test
    @DisplayName("POST /api/memories returns 409 Conflict when database unique constraint violation occurs")
    void createMemory_whenDataIntegrityViolationDuplicate_shouldReturn409() throws Exception {
        when(memoryService.createMemory(any(MemoryCreateRequest.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry '1' for key 'uk_memories_dream_id'"));

        String json = """
                {
                    "dreamId": 1,
                    "completedDate": "2026-01-15",
                    "story": "Duplicate memory under race condition"
                }
                """;

        mockMvc.perform(post("/api/memories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("A record with this unique identifier or relationship already exists."));
    }

    @Test
    @DisplayName("PUT /api/memories/{id} returns 200 OK on valid update")
    void updateMemory_whenFound_shouldReturn200() throws Exception {
        MemoryResponse updated = new MemoryResponse(
                10L,
                1L,
                "Witness Northern Lights",
                "Travel & Nature",
                LocalDate.of(2026, 2, 20),
                "Updated aurora story",
                "uploads/tromso2.jpg",
                "tromso2.jpg",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        when(memoryService.updateMemory(eq(10L), any(MemoryUpdateRequest.class))).thenReturn(updated);

        String json = """
                {
                    "completedDate": "2026-02-20",
                    "story": "Updated aurora story",
                    "photoPath": "uploads/tromso2.jpg",
                    "originalFilename": "tromso2.jpg"
                }
                """;

        mockMvc.perform(put("/api/memories/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.completedDate").value("2026-02-20"))
                .andExpect(jsonPath("$.story").value("Updated aurora story"));
    }

    @Test
    @DisplayName("PUT /api/memories/{id} returns 400 Bad Request when update completedDate is in the future")
    void updateMemory_whenCompletedDateInFuture_shouldReturn400() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(10);
        String json = String.format("""
                {
                    "completedDate": "%s",
                    "story": "Future date not allowed"
                }
                """, futureDate);

        mockMvc.perform(put("/api/memories/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.validationErrors.completedDate").value("Completion date cannot be in the future"));
    }

    @Test
    @DisplayName("PUT /api/memories/{id} returns 404 Not Found when memory is missing")
    void updateMemory_whenNotFound_shouldReturn404() throws Exception {
        when(memoryService.updateMemory(eq(999L), any(MemoryUpdateRequest.class)))
                .thenThrow(new ResourceNotFoundException("Memory not found with id: 999"));

        String json = """
                {
                    "completedDate": "2026-02-20",
                    "story": "Memory missing"
                }
                """;

        mockMvc.perform(put("/api/memories/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("DELETE /api/memories/{id} returns 204 No Content when found")
    void deleteMemory_whenFound_shouldReturn204() throws Exception {
        doNothing().when(memoryService).deleteMemory(10L);

        mockMvc.perform(delete("/api/memories/10"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/memories/{id} returns 404 Not Found when missing")
    void deleteMemory_whenNotFound_shouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("Memory not found with id: 999"))
                .when(memoryService).deleteMemory(999L);

        mockMvc.perform(delete("/api/memories/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}
