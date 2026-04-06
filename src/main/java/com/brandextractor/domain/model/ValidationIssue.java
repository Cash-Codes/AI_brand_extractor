package com.brandextractor.domain.model;

public record ValidationIssue(String code, String message, Severity severity) {

    public enum Severity { ERROR, WARNING, INFO }
}
