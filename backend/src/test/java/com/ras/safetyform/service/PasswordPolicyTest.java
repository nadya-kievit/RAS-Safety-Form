package com.ras.safetyform.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    private final PasswordPolicy passwordPolicy = new PasswordPolicy();

    @Test
    void allowsPasswordsContainingTheUsername() {
        assertDoesNotThrow(() -> passwordPolicy.validate(
                "alex-password1!"));
    }

    @Test
    void rejectsShortPasswords() {
        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> passwordPolicy.validate("short1!"));

        assertEquals("Password must be at least 8 characters", exception.getMessage());
    }

    @Test
    void rejectsPasswordsWithoutANumber() {
        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> passwordPolicy.validate("strong-password!"));

        assertEquals("Password must contain at least one number", exception.getMessage());
    }

    @Test
    void rejectsPasswordsWithoutANonAlphanumericCharacter() {
        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> passwordPolicy.validate("strongpassword1"));

        assertEquals(
                "Password must contain at least one non-alphanumeric character",
                exception.getMessage());
    }

    @Test
    void rejectsPasswordsLongerThanTwentyCharacters() {
        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> passwordPolicy.validate("this-password-is-too-long1!"));

        assertEquals("Password must be no more than 20 characters", exception.getMessage());
    }
}
