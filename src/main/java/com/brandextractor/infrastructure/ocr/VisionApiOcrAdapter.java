package com.brandextractor.infrastructure.ocr;

import com.brandextractor.domain.evidence.OcrEvidence;
import com.brandextractor.domain.ports.OcrPort;

/**
 * @deprecated Superseded by {@link OcrPortAdapter} + {@link VertexOcrClient}.
 *             Kept for reference; not registered as a Spring bean.
 */
@Deprecated
public class VisionApiOcrAdapter implements OcrPort {

    @Override
    public OcrEvidence extractText(byte[] imageBytes, String mimeType) {
        throw new UnsupportedOperationException("Superseded by OcrPortAdapter");
    }
}
