package com.example.labelverifier.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NetContentsValidatorTest {

    private final NetContentsValidator validator =
            new NetContentsValidator();

    @Test
    void shouldPassWhenVolumesMatch() {
        var result = validator.validate(
                "750 mL", "750 mL"
        );

        assertEquals("PASS", result.status());
    }

    @Test
    void shouldConvertLitersToMilliliters() {
        var result = validator.validate(
                "1000 mL", "1 L"
        );

        assertEquals("PASS", result.status());
    }

    @Test
    void shouldFailWhenVolumesDiffer() {
        var result = validator.validate(
                "750 mL", "500 mL"
        );

        assertEquals("FAIL", result.status());
    }

    @Test
    void shouldRequireReviewWhenMissing() {
        var result = validator.validate(
                "750 mL", null
        );

        assertEquals(
                "NEEDS_REVIEW", result.status()
        );
    }
}