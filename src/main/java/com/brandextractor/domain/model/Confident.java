package com.brandextractor.domain.model;

public record Confident<T>(T value, double confidence) {}
