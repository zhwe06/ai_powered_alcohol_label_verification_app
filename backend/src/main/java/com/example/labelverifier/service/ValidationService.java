package com.example.labelverifier.service;

import com.example.labelverifier.dto.FieldCheck;
import com.example.labelverifier.dto.ValidationResult;
import com.example.labelverifier.util.TextNormalizer;
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

        String brandStatus;
        String brandReason;

        if (expectedBrand == null || expectedBrand.isBlank()
                || detectedBrand == null || detectedBrand.isBlank()) {
            brandStatus = "NEEDS_REVIEW";
            brandReason = "Brand name not found in label text";
        } else if (TextNormalizer.containsPhrase(detectedBrand, expectedBrand)) {
            brandStatus = "PASS";
            brandReason = "Brand name found on label";
        } else {
            brandStatus = "NEEDS_REVIEW";
            brandReason = "Close match found — please confirm";
        }

        checks.add(new FieldCheck(
                "brandName",
                expectedBrand,
                detectedBrand,
                brandStatus,
                brandReason
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

    private BigDecimal parseAbv(String text) {
        if (text == null) return null;

        Pattern pattern = Pattern.compile(
                "(\\d{1,3}(?:\\.\\d+)?)\\s*%|" +
                        "(?:ALCOHOL\\s+BY\\s+VOLUME)\\s*[:\\-]?\\s*" +
                        "(\\d{1,3}(?:\\.\\d+)?)\\s*%",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(text);

        if (!matcher.find()) {
            return null;
        }

        String value = matcher.group(1) != null
                ? matcher.group(1)
                : matcher.group(2);

        BigDecimal abv = new BigDecimal(value);

        if (abv.compareTo(BigDecimal.ZERO) < 0 ||
                abv.compareTo(new BigDecimal("100")) > 0) {
            return null;
        }

        return abv;
    }
}
