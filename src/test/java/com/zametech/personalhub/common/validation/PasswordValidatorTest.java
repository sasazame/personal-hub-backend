package com.zametech.personalhub.common.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordValidatorTest {

    private PasswordValidator passwordValidator;

    @BeforeEach
    void setUp() {
        passwordValidator = new PasswordValidator();
    }

    @Test
    void isValid_ValidPassword_ReturnsTrue() {
        // Given
        String validPassword = "Password123!";

        // When
        boolean result = passwordValidator.isValid(validPassword);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isValid_NullPassword_ReturnsFalse() {
        // When
        boolean result = passwordValidator.isValid(null);

        // Then
        assertThat(result).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "short1!",          // Too short
            "password123!",     // No uppercase
            "PASSWORD123!",     // No lowercase
            "Password!",        // No digit
            "Password123",      // No special character
            "        "          // Only spaces
    })
    void isValid_InvalidPasswords_ReturnsFalse(String invalidPassword) {
        // When
        boolean result = passwordValidator.isValid(invalidPassword);

        // Then
        assertThat(result).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Password123!",
            "MyP@ssw0rd",
            "Secure#Pass1",
            "Test$1234",
            "Complex!Pw9",
            "Valid@Pass123"
    })
    void isValid_ValidPasswords_ReturnsTrue(String validPassword) {
        // When
        boolean result = passwordValidator.isValid(validPassword);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void getPasswordRequirements_ReturnsCorrectMessage() {
        // When
        String requirements = passwordValidator.getPasswordRequirements();

        // Then
        assertThat(requirements).contains("at least 8 characters");
        assertThat(requirements).contains("uppercase letter");
        assertThat(requirements).contains("lowercase letter");
        assertThat(requirements).contains("digit");
        assertThat(requirements).contains("special character");
    }
}