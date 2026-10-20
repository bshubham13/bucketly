package com.bucketly.api.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Valid LoginRequest passes validation")
    void validLoginRequest_hasNoViolations() {
        LoginRequest request = new LoginRequest(
                "explorer@bucketly.local",
                "secretPassword123"
        );

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Blank or invalid email fails validation")
    void invalidEmail_failsValidation() {
        LoginRequest blankEmail = new LoginRequest("", "validPassword123");
        LoginRequest nullEmail = new LoginRequest(null, "validPassword123");
        LoginRequest malformedEmail = new LoginRequest("not-an-email", "validPassword123");

        assertThat(validator.validate(blankEmail)).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        assertThat(validator.validate(nullEmail)).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        assertThat(validator.validate(malformedEmail)).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    @DisplayName("Email exceeding 255 characters fails validation")
    void longEmail_failsValidation() {
        String longEmail = "a".repeat(250) + "@b.com";
        LoginRequest request = new LoginRequest(longEmail, "validPassword123");

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    @DisplayName("Blank password fails validation")
    void blankPassword_failsValidation() {
        LoginRequest blankPassword = new LoginRequest("user@bucketly.local", "");
        LoginRequest nullPassword = new LoginRequest("user@bucketly.local", null);
        LoginRequest whitespacePassword = new LoginRequest("user@bucketly.local", "   ");

        assertThat(validator.validate(blankPassword)).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        assertThat(validator.validate(nullPassword)).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        assertThat(validator.validate(whitespacePassword)).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("Password longer than 128 characters fails validation")
    void longPassword_failsValidation() {
        String longPassword = "p".repeat(129);
        LoginRequest request = new LoginRequest("user@bucketly.local", longPassword);

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("DTO preserves raw input without premature or silent normalization")
    void preservesRawInput() {
        String rawEmail = "  Login.User@Bucketly.LOCAL  ";
        LoginRequest request = new LoginRequest(rawEmail, "secretPassword123");

        assertThat(request.email()).isEqualTo(rawEmail);
        assertThat(request.password()).isEqualTo("secretPassword123");
    }
}

