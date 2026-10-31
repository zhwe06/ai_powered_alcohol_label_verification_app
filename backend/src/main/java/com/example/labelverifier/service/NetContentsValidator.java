package com.example.labelverifier.service;

import com.example.labelverifier.dto.FieldCheck;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NetContentsValidator {
    private static final Pattern VOLUME_PATTERN = Pattern.compile("\\b(\\d+(?:\\.\\d+)?)\\s*(mL|L)\\b", Pattern.CASE_INSENSITIVE);

    public FieldCheck validate(String expected, String detected) {
        BigDecimal expectedMl = parseMilliliters(expected);
        BigDecimal detectedMl = parseMilliliters(detected);

        String status;
        String reason;

        if (expectedMl == null) {
            status = "NEEDS_REVIEW";
            reason = "Expected net contents are missing or invalid";
        } else if (detectedMl == null) {
            status = "NEEDS_REVIEW";
            reason = "Net contents could not be verified form OCR";
        } else if (expectedMl.compareTo(detectedMl) == 0) {
            status = "PASS";
            reason = "Net contents match after unit conversion";
        } else {
            status = "FAIL";
            reason = "Net contents differ";
        }

        return new FieldCheck("netContents", expected, detected, status, reason);
    }

    private BigDecimal parseMilliliters(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        Matcher matcher = VOLUME_PATTERN.matcher(text);

        if (!matcher.find()) {
            return null;
        }

        BigDecimal value = new BigDecimal(matcher.group(1));

        if (value.signum() <= 0) {
            return null;
        }

        String unit = matcher.group(2).toLowerCase(Locale.ROOT);

        return unit.equals("l") ? value.multiply(new BigDecimal("1000")) : value;
    }
}
