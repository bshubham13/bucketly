package com.bucketly.api.config;

import com.bucketly.api.controller.DreamController;
import com.bucketly.api.controller.HealthController;
import com.bucketly.api.controller.MemoryController;
import com.bucketly.api.exception.GlobalExceptionHandler;
import com.bucketly.api.security.JwtAuthenticationFilter;
import com.bucketly.api.security.JwtService;
import com.bucketly.api.service.DreamService;
import com.bucketly.api.service.MemoryService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.SecretKey;
import com.bucketly.api.dto.DreamRequest;
import com.bucketly.api.dto.DreamResponse;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {HealthController.class, DreamController.class, MemoryController.class, GlobalExceptionHandler.class})
@Import({SecurityConfig.class, SecurityConfigTest.TestSecurityOverrideConfig.class})
class SecurityConfigTest {

    @TestConfiguration
    static class TestSecurityOverrideConfig {
        @Bean
        public JwtService jwtService() {
            // Generate in-memory 256-bit key dynamically; zero secrets stored in source or test code
            SecretKey dynamicKey = Jwts.SIG.HS256.key().build();
            String testSecret = Encoders.BASE64.encode(dynamicKey.getEncoded());
            return new JwtService(testSecret, 86400000L);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private DreamService dreamService;

    @MockitoBean
    private MemoryService memoryService;

    @Test
    @DisplayName("GET /api/health is publicly accessible without authentication")
    void healthEndpoint_isPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("GET /api/health issues XSRF-TOKEN cookie via CsrfCookieFilter on safe request")
    void healthEndpoint_issuesCsrfCookie() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    @DisplayName("GET /api/dreams rejects unauthenticated requests with JSON 401 Unauthorized")
    void getDreams_whenUnauthenticated_returns401Json() throws Exception {
        mockMvc.perform(get("/api/dreams"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    @DisplayName("GET /api/dreams with valid bucketly_token cookie authenticates successfully")
    void getDreams_withValidJwtCookie_authenticatesSuccessfully() throws Exception {
        when(dreamService.getAllDreams(1L)).thenReturn(List.of());

        String validJwt = jwtService.generateToken(1L, "authorized@bucketly.local");
        Cookie authCookie = new Cookie(JwtAuthenticationFilter.COOKIE_NAME, validJwt);

        mockMvc.perform(get("/api/dreams")
                        .cookie(authCookie))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/dreams with invalid or malformed bucketly_token cookie returns 401 Unauthorized")
    void getDreams_withInvalidJwtCookie_returns401() throws Exception {
        Cookie invalidAuthCookie = new Cookie(JwtAuthenticationFilter.COOKIE_NAME, "malformed.invalid.token");

        mockMvc.perform(get("/api/dreams")
                        .cookie(invalidAuthCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("GET /api/memories rejects unauthenticated requests with JSON 401 Unauthorized")
    void getMemories_whenUnauthenticated_returns401Json() throws Exception {
        mockMvc.perform(get("/api/memories"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /api/dreams without CSRF token is rejected with 403 Forbidden")
    void postDreams_withoutCsrf_returns403() throws Exception {
        mockMvc.perform(post("/api/dreams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Visit Norway\",\"category\":\"Travel\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("POST /api/dreams with valid raw XSRF-TOKEN header and cookie succeeds CSRF verification")
    void postDreams_withValidRawCsrfToken_passesCsrfCheck() throws Exception {
        // Step 1: Obtain the CSRF token from a safe GET request
        MvcResult getResult = mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andReturn();

        jakarta.servlet.http.Cookie csrfCookie = getResult.getResponse().getCookie("XSRF-TOKEN");
        assertThat(csrfCookie).isNotNull();
        String rawCsrfToken = csrfCookie.getValue();

        when(dreamService.createDream(any(DreamRequest.class), eq(1L)))
                .thenReturn(new DreamResponse(10L, "Visit Norway", null, "Travel", null, "Not Started", LocalDateTime.now(), LocalDateTime.now()));

        // Step 2: Send mutating POST with matching X-XSRF-TOKEN header, cookie, and authenticated user
        mockMvc.perform(post("/api/dreams")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(new com.bucketly.api.security.UserPrincipal(1L, "testuser@bucketly.local")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Visit Norway\",\"category\":\"Travel\"}")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", rawCsrfToken))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("DELETE /api/dreams/{id} with mismatched CSRF token is rejected with 403 Forbidden")
    void deleteDreams_withInvalidCsrfToken_returns403() throws Exception {
        mockMvc.perform(delete("/api/dreams/1")
                        .cookie(new Cookie("XSRF-TOKEN", "correct-cookie-token"))
                        .header("X-XSRF-TOKEN", "wrong-header-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("CORS preflight for http://localhost:5173 allows credentials and required headers")
    void corsPreflight_forTrustedOrigin_allowsCredentials() throws Exception {
        mockMvc.perform(options("/api/dreams")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type,X-XSRF-TOKEN,Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS"));
    }

    @Test
    @DisplayName("CORS preflight rejects untrusted origin and does not grant credentials")
    void corsPreflight_forUntrustedOrigin_doesNotAllow() throws Exception {
        mockMvc.perform(options("/api/dreams")
                        .header("Origin", "http://untrusted-site.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }

    @Test
    @DisplayName("BCrypt PasswordEncoder bean hashes and verifies passwords correctly")
    void passwordEncoderBean_worksCorrectly() {
        String rawPassword = "SuperSecurePassword123!";
        String encoded = passwordEncoder.encode(rawPassword);

        assertThat(encoded).isNotBlank();
        assertThat(encoded).startsWith("$2a$");
        assertThat(passwordEncoder.matches(rawPassword, encoded)).isTrue();
        assertThat(passwordEncoder.matches("WrongPassword", encoded)).isFalse();
    }
}
