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

class RegisterRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Valid RegisterRequest passes validation")
    void validRegisterRequest_hasNoViolations() {
        RegisterRequest request = new RegisterRequest(
                "Jane Doe",
                "jane.doe@bucketly.local",
                "validPassword123"
        );

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Blank fullName fails validation")
    void blankFullName_failsValidation() {
        RegisterRequest blankName = new RegisterRequest("", "jane@bucketly.local", "validPassword123");
        RegisterRequest nullName = new RegisterRequest(null, "jane@bucketly.local", "validPassword123");
        RegisterRequest whitespaceName = new RegisterRequest("   ", "jane@bucketly.local", "validPassword123");

        assertThat(validator.validate(blankName)).anyMatch(v -> v.getPropertyPath().toString().equals("fullName"));
        assertThat(validator.validate(nullName)).anyMatch(v -> v.getPropertyPath().toString().equals("fullName"));
        assertThat(validator.validate(whitespaceName)).anyMatch(v -> v.getPropertyPath().toString().equals("fullName"));
    }

    @Test
    @DisplayName("FullName exceeding 100 characters fails validation")
    void longFullName_failsValidation() {
        String longName = "A".repeat(101);
        RegisterRequest request = new RegisterRequest(longName, "jane@bucketly.local", "validPassword123");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("fullName"));
    }

    @Test
    @DisplayName("Blank or invalid email fails validation")
    void invalidEmail_failsValidation() {
        RegisterRequest blankEmail = new RegisterRequest("Jane Doe", "", "validPassword123");
        RegisterRequest nullEmail = new RegisterRequest("Jane Doe", null, "validPassword123");
        RegisterRequest malformedEmail = new RegisterRequest("Jane Doe", "not-an-email", "validPassword123");

        assertThat(validator.validate(blankEmail)).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        assertThat(validator.validate(nullEmail)).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        assertThat(validator.validate(malformedEmail)).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    @DisplayName("Email exceeding 255 characters fails validation")
    void longEmail_failsValidation() {
        String longEmail = "a".repeat(250) + "@b.com";
        RegisterRequest request = new RegisterRequest("Jane Doe", longEmail, "validPassword123");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    @DisplayName("Blank password fails validation")
    void blankPassword_failsValidation() {
        RegisterRequest blankPassword = new RegisterRequest("Jane Doe", "jane@bucketly.local", "");
        RegisterRequest nullPassword = new RegisterRequest("Jane Doe", "jane@bucketly.local", null);
        RegisterRequest whitespacePassword = new RegisterRequest("Jane Doe", "jane@bucketly.local", "        ");

        assertThat(validator.validate(blankPassword)).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        assertThat(validator.validate(nullPassword)).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        assertThat(validator.validate(whitespacePassword)).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("Password shorter than 8 characters fails validation")
    void shortPassword_failsValidation() {
        RegisterRequest request = new RegisterRequest("Jane Doe", "jane@bucketly.local", "short7");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("Password longer than 128 characters fails validation")
    void longPassword_failsValidation() {
        String longPassword = "p".repeat(129);
        RegisterRequest request = new RegisterRequest("Jane Doe", "jane@bucketly.local", longPassword);

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("DTO preserves raw input without premature or silent normalization")
    void preservesRawInput() {
        String rawEmail = "  User.Name@Bucketly.LOCAL  ";
        RegisterRequest request = new RegisterRequest("  Jane Doe  ", rawEmail, "secretPassword123");

        assertThat(request.email()).isEqualTo(rawEmail);
        assertThat(request.fullName()).isEqualTo("  Jane Doe  ");
        assertThat(request.password()).isEqualTo("secretPassword123");
    }
}

