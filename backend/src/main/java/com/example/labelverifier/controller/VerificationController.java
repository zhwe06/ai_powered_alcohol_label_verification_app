
package com.example.labelverifier.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/verifications")
public class VerificationController {

    @PostMapping(
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> verify(
            @RequestPart("image") MultipartFile image,
            @RequestPart("application") String application) {

        if (image.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                        "error", "Image is required"
                    ));
        }

        return ResponseEntity.ok(Map.of(
            "status", "NEEDS_REVIEW",
            "message", "Image received successfully",
            "filename", image.getOriginalFilename()
        ));
    }
}
