package com.brandextractor.domain.ports;

import com.brandextractor.domain.model.ExtractionResult;
import com.brandextractor.domain.model.ValidationIssue;

import java.util.List;

public interface ExtractionValidationPort {

    /**
     * Validates a completed {@link ExtractionResult} against business rules
     * (e.g. required fields present, confidence thresholds met, colour contrast
     * ratios acceptable) and returns any issues found.
     *
     * An empty list means the result is fully valid.
     */
    List<ValidationIssue> validate(ExtractionResult result);
}
