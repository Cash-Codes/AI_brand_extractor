package com.brandextractor.domain.evidence;

public sealed interface Evidence
        permits WebsiteEvidence, FlyerEvidence, OcrEvidence,
                VisualEvidence, ColorEvidence, LinkEvidence,
                TextEvidence, ScreenshotEvidence {

    String id();
    String sourceType();
    String sourceReference();
}
