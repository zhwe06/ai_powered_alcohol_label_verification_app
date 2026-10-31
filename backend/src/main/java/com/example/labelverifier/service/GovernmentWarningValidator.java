package com.example.labelverifier.service;

import com.example.labelverifier.dto.FieldCheck;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GovernmentWarningValidator {

    private static final String WARNING_TITLE =
            "GOVERNMENT WARNING:";

    private static final Pattern TITLE_PATTERN =
            Pattern.compile("GOVERNMENT\\s+WARNING\\s*:", Pattern.CASE_INSENSITIVE);

    private final String requiredWarning;

    public GovernmentWarningValidator() {
        try {
            var resource = new ClassPathResource(
                    "regulations/government-warning.txt"
            );

            try (var input = resource.getInputStream()) {
                requiredWarning = normalize(
                        new String(
                                input.readAllBytes(),
                                StandardCharsets.UTF_8
                        )
                );
            }

            // The extractor may include text after the warning.
            // Check the complete required text at its beginning.
            if (!requiredWarning.startsWith(WARNING_TITLE)
                    || !requiredWarning.contains("(1)")
                    || !requiredWarning.contains("(2)")) {
                throw new IllegalStateException(
                        "Invalid government warning reference"
                );
            }

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Cannot load government warning reference", e
            );
        }
    }

    public List<FieldCheck> validate(String detectedWarning) {

        String textStatus;
        String textReason;

        if (detectedWarning == null
                || detectedWarning.isBlank()) {

            textStatus = "NEEDS_REVIEW";
            textReason =
                    "Warning not detected in OCR text";

        } else {
            String normalized = normalize(detectedWarning);

            boolean matches = normalized.equals(requiredWarning)
                    || normalized.startsWith(requiredWarning + " ");

            Matcher title = TITLE_PATTERN.matcher(normalized);

            if (title.lookingAt()
                    && !title.group().equals(title.group().toUpperCase(Locale.ROOT))) {
                textStatus = "FAIL";
                textReason = "\"GOVERNMENT WARNING:\" must be in all capital letters";
            } else if (matches) {

                textStatus = "PASS";
                textReason =
                        "Complete required warning text recognized";
            } else {
                textStatus = "NEEDS_REVIEW";
                textReason =
                        "OCR text does not exactly match the required warning";
            }
        }

        FieldCheck textCheck = new FieldCheck(
                "governmentWarningText",
                requiredWarning,
                detectedWarning,
                textStatus,
                textReason
        );

        FieldCheck formattingCheck = new FieldCheck(
                "governmentWarningFormatting",
                "Required capitalization, bold styling, " +
                        "type size, contrast and placement",
                null,
                "NEEDS_REVIEW",
                "Formatting cannot be verified from " +
                        "plain OCR text; manual review required"
        );

        return List.of(textCheck, formattingCheck);
    }

    private String normalize(String text) {
        return text.strip()
                .replaceAll("\\s+", " ");
    }
}
