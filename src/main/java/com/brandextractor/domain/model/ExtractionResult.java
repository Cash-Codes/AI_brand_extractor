package com.brandextractor.domain.model;

import com.brandextractor.domain.evidence.Evidence;

import java.util.List;
import java.util.UUID;

public record ExtractionResult(
        UUID requestId,
        ExtractionInputType inputType,
        String originalSource,
        String resolvedSource,
        BrandProfile brandProfile,
        ColorSelection colors,
        AssetSelection assets,
        ContactLinks links,
        ConfidenceScore confidence,
        List<ExtractionWarning> warnings,
        List<ValidationIssue> validationIssues,
        int textEvidenceCount,
        int imageEvidenceCount,
        int ocrBlockCount,
        boolean usedScreenshot,
        List<Evidence> evidence) {}   // always populated internally; serialised on demand
