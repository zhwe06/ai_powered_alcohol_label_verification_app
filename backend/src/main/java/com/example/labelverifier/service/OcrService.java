package com.example.labelverifier.service;

import com.example.labelverifier.dto.OcrResult;
import java.awt.image.BufferedImage;

public interface OcrService {
    OcrResult extract(BufferedImage image);
}
