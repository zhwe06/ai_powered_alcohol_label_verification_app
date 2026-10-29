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
    void shouldFailWhenAbvDiffers() {
        var result = service.validate(
                "Old Tom Distillery",
                "Old Tom Distillery",
                "45% ABV",
                "40% ABV"
        );

        assertEquals("FAIL", result.status());
    }
}
