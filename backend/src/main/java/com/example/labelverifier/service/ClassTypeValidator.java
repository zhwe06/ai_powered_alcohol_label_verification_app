package com.example.labelverifier.service;

import com.example.labelverifier.dto.FieldCheck;
import com.example.labelverifier.util.TextNormalizer;
import org.springframework.stereotype.Service;

@Service
public class ClassTypeValidator {
    public FieldCheck validate(String expected, String detected) {
        String status;
        String reason;

        if (expected == null || expected.isBlank()
                || detected == null || detected.isBlank()) {
            status = "NEEDS_REVIEW";
            reason = "Class/type not found in label text";
        } else if (TextNormalizer.containsPhrase(detected, expected)) {
            status = "PASS";
            reason = "Class/type found on label";
        } else {
            status = "NEEDS_REVIEW";
            reason = "Close match found — please confirm";
        }

        return new FieldCheck("classType", expected, detected, status, reason);
    }
}
