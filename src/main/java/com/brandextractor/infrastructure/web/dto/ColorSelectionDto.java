package com.brandextractor.infrastructure.web.dto;

public record ColorSelectionDto(
        ColorValueDto primary,
        ColorValueDto secondary,
        ColorValueDto text) {}
