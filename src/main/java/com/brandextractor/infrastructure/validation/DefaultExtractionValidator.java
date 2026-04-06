package com.brandextractor.infrastructure.validation;

import com.brandextractor.domain.model.ExtractionResult;
import com.brandextractor.domain.model.ValidationIssue;
import com.brandextractor.domain.model.ValidationIssue.Severity;
import com.brandextractor.domain.ports.ExtractionValidationPort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DefaultExtractionValidator implements ExtractionValidationPort {

    private static final double MIN_CONFIDENCE = 0.30;

    @Override
    public List<ValidationIssue> validate(ExtractionResult result) {
        List<ValidationIssue> issues = new ArrayList<>();

        var brandName = result.brandProfile() != null ? result.brandProfile().brandName() : null;
        if (brandName == null || brandName.value() == null || brandName.value().isBlank()) {
            issues.add(new ValidationIssue(
                    "MISSING_BRAND_NAME",
                    "Brand name could not be extracted from the provided input.",
                    Severity.ERROR));
        }

        if (result.colors() == null || result.colors().primary() == null) {
            issues.add(new ValidationIssue(
                    "MISSING_PRIMARY_COLOR",
                    "Primary brand colour could not be identified.",
                    Severity.WARNING));
        }

        if (result.confidence() != null && result.confidence().overall() < MIN_CONFIDENCE) {
            issues.add(new ValidationIssue(
                    "LOW_CONFIDENCE",
                    "Overall extraction confidence (%.0f%%) is below the acceptable threshold (%.0f%%)."
                            .formatted(result.confidence().overall() * 100, MIN_CONFIDENCE * 100),
                    Severity.WARNING));
        }

        return issues;
    }
}
