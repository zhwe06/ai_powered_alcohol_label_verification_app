package com.example.labelverifier.service;

import com.example.labelverifier.dto.OcrResult;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class Tess4JOcrService implements OcrService {

    private final String dataPath;

    public Tess4JOcrService(
            @Value("${ocr.tesseract.data-path}")
            String dataPath) {

        this.dataPath = dataPath;
    }

    @Override
    public OcrResult extract(BufferedImage image) {

        if (image == null) {
            throw new IllegalArgumentException(
                    "Image cannot be null"
            );
        }

        Path trainedData = Path.of(
                dataPath, "eng.traineddata"
        );

        if (!Files.isRegularFile(trainedData)) {
            throw new IllegalStateException(
                    "Missing Tesseract language data: "
                            + trainedData.toAbsolutePath()
            );
        }

        try {
            Tesseract tesseract = new Tesseract();

            tesseract.setDatapath(dataPath);
            tesseract.setLanguage("eng");
            tesseract.setPageSegMode(3);

            String text = tesseract.doOCR(image);

            return new OcrResult(
                    text == null ? "" : text.strip(),
                    "tesseract"
            );

        } catch (TesseractException e) {
            throw new IllegalStateException(
                    "Tesseract OCR processing failed", e
            );
        }
    }
}
