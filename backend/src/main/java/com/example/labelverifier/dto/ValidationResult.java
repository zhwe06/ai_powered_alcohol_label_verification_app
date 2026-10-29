package com.example.labelverifier.dto;

import java.util.List;

public record ValidationResult(
    String status,
    List<FieldCheck> checks
) {}