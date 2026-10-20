package com.bucketly.api.config;

import com.bucketly.api.controller.DreamController;
import com.bucketly.api.exception.GlobalExceptionHandler;
import com.bucketly.api.security.JwtService;
import com.bucketly.api.service.DreamService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {DreamController.class, GlobalExceptionHandler.class})
@Import({WebConfig.class, SecurityConfig.class})
class WebConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DreamService dreamService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    @DisplayName("CORS allows origin http://localhost:5173 on /api/dreams")
    void corsAllowsFrontendOrigin() throws Exception {
        when(dreamService.getAllDreams(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/dreams")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(new com.bucketly.api.security.UserPrincipal(1L, "cors@bucketly.local")))
                        .header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    @DisplayName("CORS preflight request OPTIONS succeeds for http://localhost:5173")
    void corsPreflightRequestSucceeds() throws Exception {
        mockMvc.perform(options("/api/dreams")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS"));
    }

    @Test
    @DisplayName("CORS does not allow unauthorized origins")
    void corsRejectsUnauthorizedOrigin() throws Exception {
        mockMvc.perform(get("/api/dreams")
                        .header("Origin", "http://evil-site.com"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}

