
package com.example.labelverifier.controller;

import com.example.labelverifier.dto.DetectedLabelFields;
import com.example.labelverifier.dto.OcrResult;
import com.example.labelverifier.dto.ValidationResult;
import com.example.labelverifier.service.GovernmentWarningValidator;
import com.example.labelverifier.dto.FieldCheck;
import com.example.labelverifier.service.NetContentsValidator;
import com.example.labelverifier.service.LabelFieldExtractor;
import com.example.labelverifier.service.NetContentsValidator;
import com.example.labelverifier.service.ValidationService;
import com.example.labelverifier.service.OcrService;
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

    public VerificationController(
            ValidationService validationService,
            JsonMapper objectMapper,
            OcrService ocrService,
            LabelFieldExtractor labelFieldExtractor,
            GovernmentWarningValidator governmentWarningValidator,
            NetContentsValidator netContentsValidator) {
        this.validationService = validationService;
        this.objectMapper = objectMapper;
        this.ocrService = ocrService;
        this.labelFieldExtractor = labelFieldExtractor;
        this.governmentWarningValidator = governmentWarningValidator;
        this.netContentsValidator = netContentsValidator;
    }

    @PostMapping(value = "/ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> testOcr(
        @RequestPart("image") MultipartFile image)
        throws IOException {

        if (image.isEmpty()) {
            return ResponseEntity.badRequest()
                .body("Image is required");
        }

        BufferedImage bufferedImage =
            ImageIO.read(image.getInputStream());

        if (bufferedImage == null) {
            return ResponseEntity.badRequest()
                .body("Unsupported image format");
        }

        OcrResult result = ocrService.extract(bufferedImage);

        return ResponseEntity.ok(result);
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

        BufferedImage bufferedImage =
            ImageIO.read(image.getInputStream());

        if (bufferedImage == null) {
            return ResponseEntity.badRequest()
                .body("Unsupported image format");
        }

        JsonNode data = objectMapper.readTree(application);

        String expectedBrand =
            data.path("brandName").asText("");

        String expectedAbv =
            data.path("alcoholContent").asText("");

        OcrResult ocrResult =
            ocrService.extract(bufferedImage);

        DetectedLabelFields fields =
            labelFieldExtractor.extract(
                    ocrResult.fullText(),
                    expectedBrand
            );

        ValidationResult basicResult =
            validationService.validate(
                expectedBrand,
                fields.brandName(),
                expectedAbv,
                fields.alcoholContent()
            );

        List<FieldCheck> checks = new ArrayList<>(basicResult.checks());

        checks.addAll( governmentWarningValidator.validate( fields.governmentWarning()));

        String expectedNetContents = data.path("netContents").asText("");

        checks.add(netContentsValidator.validate(expectedNetContents, fields.netContents()));

        String overallStatus;

        if (checks.stream().anyMatch( c -> "FAIL".equals(c.status()))) {
            overallStatus = "FAIL";

        } else if (checks.stream().anyMatch( c -> "NEEDS_REVIEW".equals(c.status()))) {
            overallStatus = "NEEDS_REVIEW";
        } else {
            overallStatus = "PASS";
        }

        ValidationResult finalResult =
            new ValidationResult(overallStatus, checks);

        return ResponseEntity.ok(finalResult);
    }
}
