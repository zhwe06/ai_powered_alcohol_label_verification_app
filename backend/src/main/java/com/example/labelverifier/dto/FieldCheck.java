package com.example.labelverifier.dto;

public record FieldCheck(
    String field,
    String expected,
    String detected,
    String status,
    String reason
) {}
