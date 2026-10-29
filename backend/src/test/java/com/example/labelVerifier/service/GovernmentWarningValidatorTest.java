package com.example.labelverifier.service;

import com.example.labelverifier.dto.FieldCheck;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GovernmentWarningValidatorTest {

    private final GovernmentWarningValidator validator =
            new GovernmentWarningValidator();

    private String officialText() throws Exception {
        var resource = new ClassPathResource(
            "regulations/government-warning.txt"
        );

        try (var input = resource.getInputStream()) {
            return new String(
                input.readAllBytes(),
                StandardCharsets.UTF_8
            );
        }
    }

    @Test
    void shouldPassWhenTextMatches() throws Exception {

        String ocrText = officialText();

        List<FieldCheck> checks =
                validator.validate(ocrText);

        assertEquals(
            "PASS",
            checks.get(0).status()
        );

        assertEquals(
            "NEEDS_REVIEW",
            checks.get(1).status()
        );
    }

    @Test
    void shouldRequireReviewWhenWarningMissing() {

        List<FieldCheck> checks =
                validator.validate(null);

        assertEquals(
            "NEEDS_REVIEW",
            checks.get(0).status()
        );
    }

    @Test
    void shouldRequireReviewWhenTextIsIncomplete()
            throws Exception {

        String incomplete = officialText()
                .replaceAll("(?s)\\s*\\(2\\).*", "");

        List<FieldCheck> checks =
                validator.validate(incomplete);

        assertEquals(
            "NEEDS_REVIEW",
            checks.get(0).status()
        );
    }
}
