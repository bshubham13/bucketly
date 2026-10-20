package com.bucketly.api.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("User constructor and getters/setters work correctly")
    void userFields_shouldGetAndSetProperly() {
        User user = new User(1L, "user@bucketly.local", "hashedPassword123", "Jane Doe");

        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getEmail()).isEqualTo("user@bucketly.local");
        assertThat(user.getPasswordHash()).isEqualTo("hashedPassword123");
        assertThat(user.getFullName()).isEqualTo("Jane Doe");

        user.setEmail("updated@bucketly.local");
        user.setFullName("Jane Smith");
        user.setPasswordHash("newHash456");

        assertThat(user.getEmail()).isEqualTo("updated@bucketly.local");
        assertThat(user.getFullName()).isEqualTo("Jane Smith");
        assertThat(user.getPasswordHash()).isEqualTo("newHash456");
    }

    @Test
    @DisplayName("onCreate and onUpdate callbacks populate timestamps")
    void lifecycleCallbacks_shouldSetTimestamps() {
        User user = new User();
        user.onCreate();

        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
        assertThat(user.getCreatedAt()).isEqualTo(user.getUpdatedAt());

        LocalDateTime created = user.getCreatedAt();
        user.onUpdate();

        assertThat(user.getUpdatedAt()).isAfterOrEqualTo(created);
    }

    @Test
    @DisplayName("Validation succeeds on valid User")
    void validation_whenValidUser_shouldHaveNoViolations() {
        User user = new User(1L, "valid.user@bucketly.local", "secretHashValue", "Alex Mercer");

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Validation fails when email is invalid or blank")
    void validation_whenInvalidEmail_shouldHaveViolations() {
        User blankEmailUser = new User(1L, "", "secretHashValue", "Alex Mercer");
        Set<ConstraintViolation<User>> blankViolations = validator.validate(blankEmailUser);
        assertThat(blankViolations).isNotEmpty();

        User malformedEmailUser = new User(1L, "not-an-email", "secretHashValue", "Alex Mercer");
        Set<ConstraintViolation<User>> malformedViolations = validator.validate(malformedEmailUser);
        assertThat(malformedViolations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    @DisplayName("Validation fails when passwordHash or fullName is blank")
    void validation_whenRequiredFieldsBlank_shouldHaveViolations() {
        User missingFieldsUser = new User(1L, "test@bucketly.local", "", "   ");

        Set<ConstraintViolation<User>> violations = validator.validate(missingFieldsUser);

        assertThat(violations).hasSize(2);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("passwordHash"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("fullName"));
    }
}

