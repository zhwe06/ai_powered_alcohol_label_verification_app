
package com.example.labelverifier.controller;

import com.example.labelverifier.dto.ValidationResult;
import com.example.labelverifier.service.ValidationService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/verifications")
public class VerificationController {

    private final ValidationService validationService;
    private final JsonMapper objectMapper;

    public VerificationController(
            ValidationService validationService,
            JsonMapper objectMapper) {
        this.validationService = validationService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/test")
    public String test() {
        return "VerificationController is working!";
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> verify(
            @RequestPart("image") MultipartFile image,
            @RequestPart("application") String application)
            throws IOException {

        if (image.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("Image is required");
        }

        JsonNode data = objectMapper.readTree(application);

        String brand = data.path("brandName").asText("");
        String abv = data.path("alcoholContent").asText("");

        // Temporary simulated OCR values
        String detectedBrand = "OLD TOM DISTILLERY";
        String detectedAbv = "45% Alc./Vol.";

        ValidationResult result = validationService.validate(
                brand,
                detectedBrand,
                abv,
                detectedAbv
        );

        return ResponseEntity.ok(result);
    }
}
