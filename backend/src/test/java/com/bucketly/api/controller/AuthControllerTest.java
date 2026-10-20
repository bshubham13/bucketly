package com.bucketly.api.controller;

import com.bucketly.api.config.SecurityConfig;
import com.bucketly.api.dto.AuthResponse;
import com.bucketly.api.dto.LoginRequest;
import com.bucketly.api.dto.RegisterRequest;
import com.bucketly.api.exception.DuplicateResourceException;
import com.bucketly.api.exception.GlobalExceptionHandler;
import com.bucketly.api.exception.InvalidCredentialsException;
import com.bucketly.api.exception.ResourceNotFoundException;
import com.bucketly.api.security.JwtAuthenticationFilter;
import com.bucketly.api.security.JwtService;
import com.bucketly.api.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bucketly.api.controller.HealthController;

@WebMvcTest(controllers = {AuthController.class, HealthController.class, GlobalExceptionHandler.class})
@Import({SecurityConfig.class, AuthControllerTest.TestSecurityOverrideConfig.class})
class AuthControllerTest {

    @TestConfiguration
    static class TestSecurityOverrideConfig {
        @Bean
        public JwtService jwtService() {
            // Generate in-memory ephemeral key for testing; zero hardcoded secrets
            SecretKey dynamicKey = Jwts.SIG.HS256.key().build();
            String testSecret = Encoders.BASE64.encode(dynamicKey.getEncoded());
            return new JwtService(testSecret, 86400000L);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AuthService authService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String CSRF_TOKEN_VALUE = "test-csrf-token";
    private static final Cookie CSRF_COOKIE = new Cookie("XSRF-TOKEN", CSRF_TOKEN_VALUE);

    @Test
    @DisplayName("POST /api/auth/register: valid input returns 201 Created with safe user response and no auth cookie")
    void register_validInput_returns201WithSafeResponse() throws Exception {
        RegisterRequest request = new RegisterRequest("Jane Doe", "jane@bucketly.local", "validPassword123");
        LocalDateTime now = LocalDateTime.now();
        AuthResponse response = new AuthResponse(1L, "jane@bucketly.local", "Jane Doe", now);

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("jane@bucketly.local"))
                .andExpect(jsonPath("$.fullName").value("Jane Doe"))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    @DisplayName("POST /api/auth/register: validation failure returns 400 Bad Request with field errors")
    void register_invalidInput_returns400() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest("", "not-an-email", "short");

        mockMvc.perform(post("/api/auth/register")
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.validationErrors.fullName").exists())
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists());
    }

    @Test
    @DisplayName("POST /api/auth/register: duplicate email returns 409 Conflict")
    void register_duplicateEmail_returns409() throws Exception {
        RegisterRequest request = new RegisterRequest("Jane Doe", "duplicate@bucketly.local", "validPassword123");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateResourceException("An account with this email already exists."));

        mockMvc.perform(post("/api/auth/register")
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("An account with this email already exists."));
    }

    @Test
    @DisplayName("POST /api/auth/login: valid credentials returns 200 OK with Set-Cookie HttpOnly and safe body")
    void login_validCredentials_returns200AndSetsHttpOnlyCookie() throws Exception {
        LoginRequest request = new LoginRequest("user@bucketly.local", "correctPassword123");
        LocalDateTime now = LocalDateTime.now();
        AuthResponse userResponse = new AuthResponse(5L, "user@bucketly.local", "Jane User", now);

        when(authService.login(any(LoginRequest.class))).thenReturn(userResponse);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.email").value("user@bucketly.local"))
                .andExpect(jsonPath("$.fullName").value("Jane User"))
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andReturn();

        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotNull();
        assertThat(setCookie).contains("bucketly_token=");
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("Path=/");
        assertThat(setCookie).contains("SameSite=Lax");

        // Verify body never exposes password, hash, or JWT token string
        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).doesNotContain("password");
        assertThat(responseBody).doesNotContain("hash");
        assertThat(responseBody).doesNotContain("token");
    }

    @Test
    @DisplayName("POST /api/auth/login: wrong password returns 401 Unauthorized with generic message")
    void login_wrongPassword_returns401() throws Exception {
        LoginRequest request = new LoginRequest("user@bucketly.local", "wrongPassword");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("Invalid email or password."));

        mockMvc.perform(post("/api/auth/login")
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid email or password."))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    @DisplayName("POST /api/auth/login: unknown email returns 401 Unauthorized with identical generic message")
    void login_unknownEmail_returns401() throws Exception {
        LoginRequest request = new LoginRequest("unknown@bucketly.local", "somePassword");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("Invalid email or password."));

        mockMvc.perform(post("/api/auth/login")
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid email or password."))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    @DisplayName("POST /api/auth/login: validation failure returns 400 Bad Request")
    void login_invalidInput_returns400() throws Exception {
        LoginRequest invalidRequest = new LoginRequest("", "");

        mockMvc.perform(post("/api/auth/login")
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists());
    }

    @Test
    @DisplayName("POST /api/auth/logout: clears bucketly_token cookie with Max-Age=0")
    void logout_clearsAuthCookie() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/logout")
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"))
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andReturn();

        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotNull();
        assertThat(setCookie).contains("bucketly_token=");
        assertThat(setCookie).contains("Max-Age=0");
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("Path=/");
        assertThat(setCookie).contains("SameSite=Lax");
    }

    @Test
    @DisplayName("GET /api/auth/me: valid JWT cookie resolves current user and returns 200 OK")
    void me_withValidJwtCookie_returnsCurrentUser() throws Exception {
        String token = jwtService.generateToken(42L, "alex@bucketly.local");
        Cookie authCookie = new Cookie(JwtAuthenticationFilter.COOKIE_NAME, token);

        LocalDateTime now = LocalDateTime.now();
        AuthResponse userResponse = new AuthResponse(42L, "alex@bucketly.local", "Alex Mercer", now);
        when(authService.getUserById(42L)).thenReturn(userResponse);

        mockMvc.perform(get("/api/auth/me")
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.email").value("alex@bucketly.local"))
                .andExpect(jsonPath("$.fullName").value("Alex Mercer"));
    }

    @Test
    @DisplayName("GET /api/auth/me: missing authentication returns 401 Unauthorized")
    void me_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("GET /api/auth/me: authenticated user not found in DB returns 404 without leaking internals")
    void me_whenUserDeletedFromDatabase_returns404Cleanly() throws Exception {
        String token = jwtService.generateToken(999L, "ghost@bucketly.local");
        Cookie authCookie = new Cookie(JwtAuthenticationFilter.COOKIE_NAME, token);

        when(authService.getUserById(999L))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/api/auth/me")
                        .cookie(authCookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    @DisplayName("CSRF enforcement: POST /api/auth/register without CSRF token is rejected with 403 Forbidden")
    void register_withoutCsrf_returns403() throws Exception {
        RegisterRequest request = new RegisterRequest("Jane Doe", "jane@bucketly.local", "validPassword123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("CSRF enforcement: POST /api/auth/login without CSRF token is rejected with 403 Forbidden")
    void login_withoutCsrf_returns403() throws Exception {
        LoginRequest request = new LoginRequest("user@bucketly.local", "correctPassword123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("CSRF enforcement: POST /api/auth/logout without CSRF token is rejected with 403 Forbidden")
    void logout_withoutCsrf_returns403() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("CSRF flow: valid raw XSRF-TOKEN header and cookie passes CSRF for auth endpoints")
    void register_withValidRawCsrfToken_passesCsrfCheck() throws Exception {
        MvcResult getResult = mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andReturn();

        Cookie csrfCookie = getResult.getResponse().getCookie("XSRF-TOKEN");
        if (csrfCookie == null) {
            String setCookie = getResult.getResponse().getHeader("Set-Cookie");
            if (setCookie != null && setCookie.contains("XSRF-TOKEN=")) {
                String val = setCookie.split("XSRF-TOKEN=")[1].split(";")[0];
                csrfCookie = new Cookie("XSRF-TOKEN", val);
            }
        }
        assertThat(csrfCookie)
                .withFailMessage("Cookies: " + java.util.Arrays.toString(getResult.getResponse().getCookies()) + ", Set-Cookie: " + getResult.getResponse().getHeader("Set-Cookie"))
                .isNotNull();
        String rawCsrfToken = csrfCookie.getValue();

        RegisterRequest request = new RegisterRequest("Jane Doe", "jane.csrf@bucketly.local", "validPassword123");
        LocalDateTime now = LocalDateTime.now();
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(new AuthResponse(2L, "jane.csrf@bucketly.local", "Jane Doe", now));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", rawCsrfToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("jane.csrf@bucketly.local"));
    }
}

