package com.example.labelverifier.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ClassTypeValidatorTest {
    private final ClassTypeValidator validator =
            new ClassTypeValidator();

    @Test
    void shouldPassWhenClassTypeFound() {
        var result = validator.validate(
                "Bourbon Whiskey", "Bourbon Whiskey"
        );

        assertEquals("PASS", result.status());
    }

    @Test
    void shouldRequireReviewWhenClassTypeMissing() {
        var result = validator.validate(
                "Bourbon Whiskey", null
        );

        assertEquals("NEEDS_REVIEW", result.status());
    }

    @Test
    void shouldRequireReviewForCloseMatch() {
        var result = validator.validate(
                "Bourbon Whiskey", "B0URBON WHISKEY"
        );

        assertEquals("NEEDS_REVIEW", result.status());
    }
}
