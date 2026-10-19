package com.bucketly.api.controller;

import com.bucketly.api.dto.DreamRequest;
import com.bucketly.api.dto.DreamResponse;
import com.bucketly.api.exception.GlobalExceptionHandler;
import com.bucketly.api.exception.ResourceNotFoundException;
import com.bucketly.api.service.DreamService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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

@WebMvcTest(controllers = {DreamController.class, GlobalExceptionHandler.class})
class DreamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DreamService dreamService;

    @Test
    @DisplayName("GET /api/dreams returns 200 OK with dreams list")
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
        when(dreamService.getAllDreams()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/dreams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Witness Northern Lights"))
                .andExpect(jsonPath("$[0].category").value("Travel & Nature"));
    }

    @Test
    @DisplayName("GET /api/dreams/{id} returns 200 OK when dream exists")
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
        when(dreamService.getDreamById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/dreams/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Witness Northern Lights"))
                .andExpect(jsonPath("$.status").value("In Progress"));
    }

    @Test
    @DisplayName("GET /api/dreams/{id} returns 404 Not Found when dream does not exist")
    void getDreamById_whenNotFound_shouldReturn404() throws Exception {
        when(dreamService.getDreamById(999L)).thenThrow(new ResourceNotFoundException("Dream not found with id: 999"));

        mockMvc.perform(get("/api/dreams/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Dream not found with id: 999"));
    }

    @Test
    @DisplayName("POST /api/dreams returns 201 Created on valid request")
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
        when(dreamService.createDream(any(DreamRequest.class))).thenReturn(created);

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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.title").value("Run Tokyo Marathon"))
                .andExpect(jsonPath("$.category").value("Fitness & Health"));
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.validationErrors.title").exists());
    }

    @Test
    @DisplayName("PUT /api/dreams/{id} returns 200 OK on valid update")
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
        when(dreamService.updateDream(eq(1L), any(DreamRequest.class))).thenReturn(updated);

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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Northern Lights Updated"))
                .andExpect(jsonPath("$.status").value("Completed"));
    }

    @Test
    @DisplayName("PUT /api/dreams/{id} returns 404 Not Found when dream is missing")
    void updateDream_whenNotFound_shouldReturn404() throws Exception {
        when(dreamService.updateDream(eq(999L), any(DreamRequest.class)))
                .thenThrow(new ResourceNotFoundException("Dream not found with id: 999"));

        String updateJson = """
                {
                    "title": "Non-existent",
                    "category": "Travel"
                }
                """;

        mockMvc.perform(put("/api/dreams/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("DELETE /api/dreams/{id} returns 204 No Content when successful")
    void deleteDream_whenFound_shouldReturn204() throws Exception {
        doNothing().when(dreamService).deleteDream(1L);

        mockMvc.perform(delete("/api/dreams/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/dreams/{id} returns 404 Not Found when dream does not exist")
    void deleteDream_whenNotFound_shouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("Dream not found with id: 999"))
                .when(dreamService).deleteDream(999L);

        mockMvc.perform(delete("/api/dreams/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}

