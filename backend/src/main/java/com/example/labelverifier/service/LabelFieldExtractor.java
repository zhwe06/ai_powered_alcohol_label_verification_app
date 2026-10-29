package com.example.labelverifier.service;

import com.example.labelverifier.dto.DetectedLabelFields;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LabelFieldExtractor {

    private static final Pattern ABV_PATTERN =
            Pattern.compile(
                "\\b(\\d{1,2}(?:\\.\\d+)?)\\s*%\\s*" +
                "(?:ABV|ALC\\.?\\s*/?\\s*VOL\\.?)",
                Pattern.CASE_INSENSITIVE
            );
    private static final Pattern ABV_PREFIX_PATTERN =
        Pattern.compile(
            "(?:ALCOHOL\\s+BY\\s+VOLUME|" +
            "ALC\\.?\\s*/?\\s*VOL\\.?)" +
            "\\s*[:\\-]?\\s*" +
            "(\\d{1,3}(?:\\.\\d+)?)\\s*%",
            Pattern.CASE_INSENSITIVE
        );

    private static final Pattern VOLUME_PATTERN =
            Pattern.compile(
                "\\b(\\d+(?:\\.\\d+)?)\\s*" +
                "(mL|L|FL\\.?\\s*OZ\\.?)" +
                "(?![A-Za-z])",
                Pattern.CASE_INSENSITIVE
            );

    private static final Pattern WARNING_PATTERN =
            Pattern.compile(
                "GOVERNMENT\\s+WARNING\\s*:",
                Pattern.CASE_INSENSITIVE
            );

    public DetectedLabelFields extract(
            String fullText,
            String expectedBrand) {

        if (fullText == null || fullText.isBlank()) {
            return new DetectedLabelFields(
                null, null, null, null
            );
        }

        String brand = extractBrand(
            fullText, expectedBrand
        );

        String abv = extractAbv(fullText);

        String volume = findFirst(
            VOLUME_PATTERN, fullText
        );

        String warning = extractWarning(fullText);

        return new DetectedLabelFields(
            brand, abv, volume, warning
        );
    }

    private String extractBrand(
            String fullText,
            String expectedBrand) {

        if (expectedBrand == null ||
                expectedBrand.isBlank()) {
            return null;
        }

        String normalizedExpected =
                normalize(expectedBrand);

        for (String line : fullText.split("\\R")) {
            String normalizedLine = normalize(line);

            if (normalizedLine.equals(normalizedExpected)) {
                return line.trim();
            }
        }

        return null;
    }

    private String extractAbv(String text) {

        Matcher matcher =
            ABV_PATTERN.matcher(text);

        if (matcher.find()) {
            return matcher.group().trim();
        }

        matcher = ABV_PREFIX_PATTERN.matcher(text);

        if (matcher.find()) {
            return matcher.group().trim();
        }

        return null;
    }

    private String findFirst(
            Pattern pattern,
            String text) {

        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group().trim();
        }

        return null;
    }

    private String extractWarning(String text) {

        Matcher matcher =
                WARNING_PATTERN.matcher(text);

        if (!matcher.find()) {
            return null;
        }

        // Keep a short text excerpt for later validation.
        int start = matcher.start();
        int end = Math.min(
            text.length(), start + 500
        );

        return text.substring(start, end).trim();
    }

    private String normalize(String text) {

        return text.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }
}
