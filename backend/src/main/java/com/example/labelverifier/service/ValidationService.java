package com.example.labelverifier.service;

import com.example.labelverifier.dto.FieldCheck;
import com.example.labelverifier.dto.ValidationResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ValidationService {

    public ValidationResult validate(
            String expectedBrand,
            String detectedBrand,
            String expectedAbv,
            String detectedAbv) {

        List<FieldCheck> checks = new ArrayList<>();

        boolean brandMatch = normalize(expectedBrand)
                .equals(normalize(detectedBrand));

        checks.add(new FieldCheck(
                "brandName",
                expectedBrand,
                detectedBrand,
                brandMatch ? "PASS" : "FAIL",
                brandMatch ? "Brand names match"
                           : "Brand names do not match"
        ));

        BigDecimal expected = parseAbv(expectedAbv);
        BigDecimal detected = parseAbv(detectedAbv);

        String abvStatus = expected == null || detected == null
                ? "NEEDS_REVIEW"
                : expected.compareTo(detected) == 0
                    ? "PASS" : "FAIL";

        checks.add(new FieldCheck(
                "alcoholContent",
                expectedAbv,
                detectedAbv,
                abvStatus,
                switch (abvStatus) {
                    case "PASS" -> "Alcohol contents match";
                    case "FAIL" -> "Alcohol contents differ";
                    default -> "Unable to verify alcohol content";
                }
        ));

        String overall = checks.stream()
                .anyMatch(c -> c.status().equals("FAIL"))
                ? "FAIL"
                : checks.stream().anyMatch(
                    c -> c.status().equals("NEEDS_REVIEW"))
                    ? "NEEDS_REVIEW" : "PASS";

        return new ValidationResult(overall, checks);
    }

    private String normalize(String value) {
        if (value == null) return "";
        return value.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(java.util.Locale.ROOT);
    }

    private BigDecimal parseAbv(String text) {
        if (text == null) return null;

        Matcher m = Pattern.compile(
                "(\\d+(?:\\.\\d+)?)\\s*%\\s*(?:ABV|ALC\\.?/?VOL\\.?)?",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);

        return m.find() ? new BigDecimal(m.group(1)) : null;
    }
}
