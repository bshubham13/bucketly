package com.bucketly.api.service;

import com.bucketly.api.dto.AuthResponse;
import com.bucketly.api.dto.LoginRequest;
import com.bucketly.api.dto.RegisterRequest;
import com.bucketly.api.entity.User;
import com.bucketly.api.exception.DuplicateResourceException;
import com.bucketly.api.exception.InvalidCredentialsException;
import com.bucketly.api.exception.ResourceNotFoundException;
import com.bucketly.api.repository.UserRepository;
import com.bucketly.api.security.JwtService;
import com.bucketly.api.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this(userRepository, passwordEncoder, null);
    }

    @Autowired
    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       @Autowired(required = false) JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Registers a new user. Normalizes email (trim and lowercase with Locale.ROOT),
     * enforces email uniqueness, hashes the password using BCrypt, and persists the user.
     *
     * @param request registration request containing fullName, email, and password
     * @return safe AuthResponse containing public user details without secrets
     * @throws DuplicateResourceException if an account with this email already exists
     */
    public AuthResponse register(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Registration request cannot be null");
        }
        String normalizedEmail = normalizeEmail(request.email());
        String trimmedFullName = request.fullName() != null ? request.fullName().trim() : "";

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("An account with this email already exists.");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user = new User(null, normalizedEmail, passwordHash, trimmedFullName);
        User savedUser = userRepository.save(user);

        return AuthResponse.fromEntity(savedUser);
    }

    /**
     * Verifies user credentials. Normalizes email and verifies the password against
     * the stored BCrypt hash.
     *
     * @param request login request containing email and password
     * @return safe AuthResponse containing public user details without secrets
     * @throws InvalidCredentialsException if email is not found or password does not match
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Login request cannot be null");
        }
        String normalizedEmail = normalizeEmail(request.email());

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password.");
        }

        return AuthResponse.fromEntity(user);
    }

    /**
     * Authenticates credentials and returns an authenticated UserPrincipal.
     *
     * @param request login request
     * @return UserPrincipal containing verified userId and email
     */
    @Transactional(readOnly = true)
    public UserPrincipal authenticate(LoginRequest request) {
        AuthResponse response = login(request);
        return new UserPrincipal(response.id(), response.email());
    }

    /**
     * Generates a JWT token for the specified user ID and email if JwtService is configured.
     *
     * @param userId user identifier
     * @param email  user email
     * @return signed JWT token string
     */
    public String generateToken(Long userId, String email) {
        if (jwtService == null) {
            throw new IllegalStateException("JwtService is not configured or available.");
        }
        return jwtService.generateToken(userId, email);
    }

    /**
     * Resolves user details by ID for authenticated user profile retrieval.
     *
     * @param userId user identifier
     * @return safe AuthResponse containing public user details without secrets
     * @throws ResourceNotFoundException if user does not exist
     */
    @Transactional(readOnly = true)
    public AuthResponse getUserById(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return AuthResponse.fromEntity(user);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be blank");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

