package com.brandextractor.domain.ports;

import com.brandextractor.domain.evidence.OcrEvidence;

public interface OcrPort {
    OcrEvidence extractText(byte[] imageBytes, String mimeType);
}
