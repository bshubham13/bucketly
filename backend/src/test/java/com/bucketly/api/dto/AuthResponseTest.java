package com.bucketly.api.dto;

import com.bucketly.api.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AuthResponseTest {

    @Test
    @DisplayName("fromEntity maps user fields safely to AuthResponse")
    void fromEntity_mapsFieldsCorrectly() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 15, 10, 30);
        User user = new User(42L, "alex@bucketly.local", "hashedPasswordValueNeverExposed", "Alex Mercer");
        user.setCreatedAt(createdAt);

        AuthResponse response = AuthResponse.fromEntity(user);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.email()).isEqualTo("alex@bucketly.local");
        assertThat(response.fullName()).isEqualTo("Alex Mercer");
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("fromEntity handles null user gracefully")
    void fromEntity_whenNullUser_returnsNull() {
        assertThat(AuthResponse.fromEntity(null)).isNull();
    }

    @Test
    @DisplayName("AuthResponse record has no password or passwordHash fields")
    void authResponse_neverExposesCredentials() {
        RecordComponent[] components = AuthResponse.class.getRecordComponents();
        for (RecordComponent component : components) {
            String name = component.getName().toLowerCase();
            assertThat(name).doesNotContain("password");
            assertThat(name).doesNotContain("hash");
            assertThat(name).doesNotContain("secret");
            assertThat(name).doesNotContain("token");
        }

        Field[] fields = AuthResponse.class.getDeclaredFields();
        for (Field field : fields) {
            String name = field.getName().toLowerCase();
            assertThat(name).doesNotContain("password");
            assertThat(name).doesNotContain("hash");
            assertThat(name).doesNotContain("secret");
            assertThat(name).doesNotContain("token");
        }
    }
}

