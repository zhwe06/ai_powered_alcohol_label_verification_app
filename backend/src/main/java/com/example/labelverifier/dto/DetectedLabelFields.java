package com.example.labelverifier.dto;

public record DetectedLabelFields(
        String brandName,
        String alcoholContent,
        String netContents,
        String governmentWarning
) {}