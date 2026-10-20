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

class DreamTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Dream constructors, User association, and accessors work properly")
    void dreamFields_shouldGetAndSetProperly() {
        User user = new User(1L, "user@bucketly.local", "hash", "Jane Doe");
        Dream dream = new Dream(10L, user, "Learn Skydiving", "Adventure", "Travel & Adventure", "2026-12-31", "In Progress");

        assertThat(dream.getId()).isEqualTo(10L);
        assertThat(dream.getUser()).isEqualTo(user);
        assertThat(dream.getTitle()).isEqualTo("Learn Skydiving");
        assertThat(dream.getDescription()).isEqualTo("Adventure");
        assertThat(dream.getCategory()).isEqualTo("Travel & Adventure");
        assertThat(dream.getTargetDate()).isEqualTo("2026-12-31");
        assertThat(dream.getStatus()).isEqualTo("In Progress");

        User newUser = new User(2L, "other@bucketly.local", "hash2", "John Smith");
        dream.setUser(newUser);
        dream.setTitle("Updated Title");

        assertThat(dream.getUser()).isEqualTo(newUser);
        assertThat(dream.getTitle()).isEqualTo("Updated Title");
    }

    @Test
    @DisplayName("Legacy constructor without User sets user to null (unowned legacy dream)")
    void legacyConstructor_shouldAllowNullUser() {
        Dream legacyDream = new Dream(5L, "Visit Kyoto", "Visit ancient temples", "Travel", "2027-04-01", "Not Started");

        assertThat(legacyDream.getId()).isEqualTo(5L);
        assertThat(legacyDream.getUser()).isNull();
        assertThat(legacyDream.getTitle()).isEqualTo("Visit Kyoto");
        assertThat(legacyDream.getStatus()).isEqualTo("Not Started");
    }

    @Test
    @DisplayName("onCreate and onUpdate lifecycle callbacks populate timestamps and default status")
    void lifecycleCallbacks_shouldSetTimestampsAndDefaultStatus() {
        Dream dream = new Dream();
        dream.setTitle("Run Marathon");
        dream.setCategory("Fitness");
        dream.setStatus(null);

        dream.onCreate();

        assertThat(dream.getCreatedAt()).isNotNull();
        assertThat(dream.getUpdatedAt()).isNotNull();
        assertThat(dream.getCreatedAt()).isEqualTo(dream.getUpdatedAt());
        assertThat(dream.getStatus()).isEqualTo("Not Started");

        LocalDateTime created = dream.getCreatedAt();
        dream.onUpdate();

        assertThat(dream.getUpdatedAt()).isAfterOrEqualTo(created);
    }

    @Test
    @DisplayName("Validation fails when mandatory fields are missing")
    void validation_shouldFailOnBlankTitleOrCategory() {
        Dream invalidDream = new Dream();
        invalidDream.setTitle("");
        invalidDream.setCategory("   ");

        Set<ConstraintViolation<Dream>> violations = validator.validate(invalidDream);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("title"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("category"));
    }

    @Test
    @DisplayName("Validation succeeds when valid Dream is provided with or without User")
    void validation_shouldPassWithOrWithoutUser() {
        Dream dreamWithoutUser = new Dream();
        dreamWithoutUser.setTitle("Valid Title");
        dreamWithoutUser.setCategory("Personal");
        dreamWithoutUser.setStatus("Not Started");

        Set<ConstraintViolation<Dream>> violations1 = validator.validate(dreamWithoutUser);
        assertThat(violations1).isEmpty();

        User user = new User(1L, "user@bucketly.local", "hash", "Jane Doe");
        dreamWithoutUser.setUser(user);

        Set<ConstraintViolation<Dream>> violations2 = validator.validate(dreamWithoutUser);
        assertThat(violations2).isEmpty();
    }
}

