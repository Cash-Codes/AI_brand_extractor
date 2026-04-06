package com.brandextractor.infrastructure.ocr;

import com.brandextractor.domain.evidence.OcrEvidence;
import com.brandextractor.domain.ports.OcrPort;
import org.springframework.stereotype.Component;

@Component
public class VisionApiOcrAdapter implements OcrPort {

    @Override
    public OcrEvidence extractText(byte[] imageBytes, String mimeType) {
        throw new UnsupportedOperationException("OCR extraction not yet implemented");
    }
}
