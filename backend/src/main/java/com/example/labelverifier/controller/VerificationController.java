package com.example.labelverifier.controller;

import com.example.labelverifier.dto.DetectedLabelFields;
import com.example.labelverifier.dto.OcrResult;
import com.example.labelverifier.dto.ValidationResult;
import com.example.labelverifier.exception.InvalidImageException;
import com.example.labelverifier.service.*;
import com.example.labelverifier.dto.FieldCheck;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/verifications")
public class VerificationController {

    private final ValidationService validationService;
    private final JsonMapper objectMapper;
    private final OcrService ocrService;
    private final LabelFieldExtractor labelFieldExtractor;
    private final GovernmentWarningValidator governmentWarningValidator;
    private final NetContentsValidator netContentsValidator;
    private final ClassTypeValidator classTypeValidator;

    public VerificationController(
            ValidationService validationService,
            JsonMapper objectMapper,
            OcrService ocrService,
            LabelFieldExtractor labelFieldExtractor,
            GovernmentWarningValidator governmentWarningValidator,
            NetContentsValidator netContentsValidator,
            ClassTypeValidator classTypeValidator) {
        this.validationService = validationService;
        this.objectMapper = objectMapper;
        this.ocrService = ocrService;
        this.labelFieldExtractor = labelFieldExtractor;
        this.governmentWarningValidator = governmentWarningValidator;
        this.netContentsValidator = netContentsValidator;
        this.classTypeValidator = classTypeValidator;
    }

    @PostMapping(value = "/ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OcrResult> testOcr(
            @RequestPart("image") MultipartFile image)
            throws IOException {

        if (image.isEmpty()) {
            throw new InvalidImageException("Image is required");
        }

        BufferedImage bufferedImage =
                ImageIO.read(image.getInputStream());

        if (bufferedImage == null) {
            throw new InvalidImageException("Unsupported image format");
        }

        OcrResult result = ocrService.extract(bufferedImage);

        return ResponseEntity.ok(result);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ValidationResult> verify(
            @RequestPart("image") MultipartFile image,
            @RequestPart("application") String application)
            throws IOException {

        if (image.isEmpty()) {
            throw new InvalidImageException("Image is required");
        }

        BufferedImage bufferedImage =
                ImageIO.read(image.getInputStream());

        if (bufferedImage == null) {
            throw new InvalidImageException("Unsupported image format");
        }

        JsonNode data = objectMapper.readTree(application);

        String expectedBrand =
                data.path("brandName").asText("");

        String expectedClassType =
                data.path("classType").asText("");

        String expectedAbv =
                data.path("alcoholContent").asText("");

        String expectedNetContents = data.path("netContents").asText("");

        OcrResult ocrResult =
                ocrService.extract(bufferedImage);

        DetectedLabelFields fields =
                labelFieldExtractor.extract(
                        ocrResult.fullText(),
                        expectedBrand,
                        expectedClassType
                );

        ValidationResult basicResult =
                validationService.validate(
                        expectedBrand,
                        fields.brandName(),
                        expectedAbv,
                        fields.alcoholContent()
                );

        List<FieldCheck> checks = new ArrayList<>(basicResult.checks());

        checks.addAll(governmentWarningValidator.validate(fields.governmentWarning()));

        checks.add(netContentsValidator.validate(expectedNetContents, fields.netContents()));

        checks.add(classTypeValidator.validate(expectedClassType, fields.classType()));

        String overallStatus;

        if (checks.stream().anyMatch(c -> "FAIL".equals(c.status()))) {
            overallStatus = "FAIL";

        } else if (checks.stream().anyMatch(c -> "NEEDS_REVIEW".equals(c.status()))) {
            overallStatus = "NEEDS_REVIEW";
        } else {
            overallStatus = "PASS";
        }

        ValidationResult finalResult =
                new ValidationResult(overallStatus, checks);

        return ResponseEntity.ok(finalResult);
    }
}
