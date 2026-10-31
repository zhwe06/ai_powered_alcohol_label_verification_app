package com.example.labelverifier.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationServiceTest {

    private final ValidationService service =
            new ValidationService();

    @Test
    void shouldPassWhenBrandAndAbvMatch() {
        var result = service.validate(
                "Old Tom Distillery",
                "OLD TOM DISTILLERY",
                "45% ABV",
                "45% Alc./Vol."
        );

        assertEquals("PASS", result.status());
    }

    @Test
    void shouldRequireReviewWhenBrandNotFound() {
        var result = service.validate(
                "Old Tom Distillery",
                null,
                "45% ABV",
                "45% Alc./Vol."
        );

        assertEquals("NEEDS_REVIEW", result.status());
    }

    @Test
    void shouldRequireReviewForCloseBrandMatch() {
        var result = service.validate(
                "Old Tom Distilllery",
                "OLD TOM DISTILLERY",
                "45% ABV",
                "45% Alc./Vol."
        );

        assertEquals("NEEDS_REVIEW", result.status());
    }

    @Test
    void shouldFailWhenAbvDiffers() {
        var result = service.validate(
                "Old Tom Distillery",
                "Old Tom Distillery",
                "45% ABV",
                "40% ABV"
        );

        assertEquals("FAIL", result.status());
    }

    @Test
    void shouldFailWhenAlcoholByVolumeDiffers() {

        var result = service.validate(
                "Old Tom Distillery",
                null,
                "45% ABV",
                "Alcohol by volume 17%"
        );

        assertEquals("FAIL", result.status());
    }
}
