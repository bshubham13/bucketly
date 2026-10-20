package com.bucketly.api.service;

import com.bucketly.api.dto.AuthResponse;
import com.bucketly.api.dto.LoginRequest;
import com.bucketly.api.dto.RegisterRequest;
import com.bucketly.api.entity.User;
import com.bucketly.api.exception.DuplicateResourceException;
import com.bucketly.api.exception.InvalidCredentialsException;
import com.bucketly.api.repository.UserRepository;
import com.bucketly.api.security.JwtService;
import com.bucketly.api.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    @DisplayName("register: successful registration hashes password, persists user, and returns safe response")
    void register_successfulRegistration_persistsAndReturnsSafeResponse() {
        RegisterRequest request = new RegisterRequest("Jane Doe", "jane@bucketly.local", "plainSecret123");

        when(userRepository.existsByEmail("jane@bucketly.local")).thenReturn(false);
        when(passwordEncoder.encode("plainSecret123")).thenReturn("$2a$10$hashedBCryptValue");

        LocalDateTime now = LocalDateTime.now();
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(10L);
            u.setCreatedAt(now);
            return u;
        });

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.email()).isEqualTo("jane@bucketly.local");
        assertThat(response.fullName()).isEqualTo("Jane Doe");
        assertThat(response.createdAt()).isEqualTo(now);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getPasswordHash()).isEqualTo("$2a$10$hashedBCryptValue");
        assertThat(savedUser.getEmail()).isEqualTo("jane@bucketly.local");
    }

    @Test
    @DisplayName("register: email normalization trims and lowercases with Locale.ROOT")
    void register_emailNormalization_trimsAndLowercasesWithLocaleRoot() {
        RegisterRequest request = new RegisterRequest("  Jane Doe  ", "  JANE.DOE+Test@Bucketly.LOCAL  ", "plainSecret123");

        when(userRepository.existsByEmail("jane.doe+test@bucketly.local")).thenReturn(false);
        when(passwordEncoder.encode("plainSecret123")).thenReturn("$2a$10$hashedBCryptValue");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.register(request);

        assertThat(response.email()).isEqualTo("jane.doe+test@bucketly.local");
        assertThat(response.fullName()).isEqualTo("Jane Doe");

        verify(userRepository).existsByEmail("jane.doe+test@bucketly.local");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("jane.doe+test@bucketly.local");
        assertThat(userCaptor.getValue().getFullName()).isEqualTo("Jane Doe");
    }

    @Test
    @DisplayName("register: password is hashed with PasswordEncoder and never stored as plaintext")
    void register_passwordHashedWithBCrypt_neverStoresPlaintext() {
        RegisterRequest request = new RegisterRequest("Jane Doe", "jane@bucketly.local", "superSensitivePlaintext");

        when(userRepository.existsByEmail("jane@bucketly.local")).thenReturn(false);
        when(passwordEncoder.encode("superSensitivePlaintext")).thenReturn("$2a$10$safeBCryptHash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(request);

        verify(passwordEncoder).encode("superSensitivePlaintext");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("$2a$10$safeBCryptHash");
        assertThat(userCaptor.getValue().getPasswordHash()).doesNotContain("superSensitivePlaintext");
    }

    @Test
    @DisplayName("register: duplicate email throws DuplicateResourceException and does not save")
    void register_duplicateEmail_throwsDuplicateResourceException() {
        RegisterRequest request = new RegisterRequest("Jane Doe", "duplicate@bucketly.local", "plainSecret123");

        when(userRepository.existsByEmail("duplicate@bucketly.local")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("An account with this email already exists.");

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("login: successful login verifies password with PasswordEncoder and returns safe response")
    void login_successfulLogin_verifiesPasswordAndReturnsSafeResponse() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 10, 8, 0);
        User user = new User(5L, "user@bucketly.local", "$2a$10$hashedBCrypt", "Jane User");
        user.setCreatedAt(createdAt);

        when(userRepository.findByEmail("user@bucketly.local")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correctPassword", "$2a$10$hashedBCrypt")).thenReturn(true);

        LoginRequest request = new LoginRequest("user@bucketly.local", "correctPassword");
        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.email()).isEqualTo("user@bucketly.local");
        assertThat(response.fullName()).isEqualTo("Jane User");
        assertThat(response.createdAt()).isEqualTo(createdAt);

        verify(passwordEncoder).matches("correctPassword", "$2a$10$hashedBCrypt");
    }

    @Test
    @DisplayName("login: email is normalized before lookup")
    void login_emailNormalization_normalizesEmailBeforeLookup() {
        User user = new User(1L, "user@bucketly.local", "$2a$10$hashedBCrypt", "Jane User");
        when(userRepository.findByEmail("user@bucketly.local")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "$2a$10$hashedBCrypt")).thenReturn(true);

        LoginRequest request = new LoginRequest("   USER@bucketly.LOCAL   ", "pass");
        authService.login(request);

        verify(userRepository).findByEmail("user@bucketly.local");
    }

    @Test
    @DisplayName("login: wrong password throws InvalidCredentialsException")
    void login_wrongPassword_throwsInvalidCredentialsException() {
        User user = new User(1L, "user@bucketly.local", "$2a$10$hashedBCrypt", "Jane User");
        when(userRepository.findByEmail("user@bucketly.local")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "$2a$10$hashedBCrypt")).thenReturn(false);

        LoginRequest request = new LoginRequest("user@bucketly.local", "wrongPassword");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password.");
    }

    @Test
    @DisplayName("login: unknown email throws InvalidCredentialsException with identical generic message")
    void login_unknownEmail_throwsInvalidCredentialsException() {
        when(userRepository.findByEmail("unknown@bucketly.local")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest("unknown@bucketly.local", "anyPassword");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password.");

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("authenticate: returns UserPrincipal for valid credentials")
    void authenticate_validCredentials_returnsUserPrincipal() {
        User user = new User(77L, "user@bucketly.local", "$2a$10$hash", "Jane User");
        when(userRepository.findByEmail("user@bucketly.local")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pwd", "$2a$10$hash")).thenReturn(true);

        LoginRequest request = new LoginRequest("user@bucketly.local", "pwd");
        UserPrincipal principal = authService.authenticate(request);

        assertThat(principal).isNotNull();
        assertThat(principal.userId()).isEqualTo(77L);
        assertThat(principal.email()).isEqualTo("user@bucketly.local");
    }

    @Test
    @DisplayName("generateToken: delegates to JwtService when present")
    void generateToken_delegatesToJwtService() {
        when(jwtService.generateToken(1L, "user@bucketly.local")).thenReturn("jwt.token.val");

        String token = authService.generateToken(1L, "user@bucketly.local");

        assertThat(token).isEqualTo("jwt.token.val");
        verify(jwtService).generateToken(1L, "user@bucketly.local");
    }

    @Test
    @DisplayName("generateToken: throws IllegalStateException when JwtService is null")
    void generateToken_whenJwtServiceNull_throwsIllegalStateException() {
        AuthService serviceWithoutJwt = new AuthService(userRepository, passwordEncoder);

        assertThatThrownBy(() -> serviceWithoutJwt.generateToken(1L, "user@bucketly.local"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JwtService is not configured or available");
    }

    @Test
    @DisplayName("register: null request throws IllegalArgumentException")
    void register_nullRequest_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> authService.register(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Registration request cannot be null");
    }

    @Test
    @DisplayName("login: null request throws IllegalArgumentException")
    void login_nullRequest_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> authService.login(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Login request cannot be null");
    }

    @Test
    @DisplayName("register: blank email throws IllegalArgumentException")
    void register_blankEmail_throwsIllegalArgumentException() {
        RegisterRequest request = new RegisterRequest("Jane Doe", "   ", "pass12345");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email cannot be blank");
    }

    @Test
    @DisplayName("getUserById: returns safe AuthResponse when user exists")
    void getUserById_whenUserExists_returnsSafeResponse() {
        User user = new User(10L, "test@bucketly.local", "$2a$10$hash", "Test User");
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        AuthResponse response = authService.getUserById(10L);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.email()).isEqualTo("test@bucketly.local");
        assertThat(response.fullName()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("getUserById: throws ResourceNotFoundException when user does not exist")
    void getUserById_whenUserNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getUserById(99L))
                .isInstanceOf(com.bucketly.api.exception.ResourceNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    @DisplayName("getUserById: null id throws IllegalArgumentException")
    void getUserById_nullId_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> authService.getUserById(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User ID cannot be null");
    }
}

