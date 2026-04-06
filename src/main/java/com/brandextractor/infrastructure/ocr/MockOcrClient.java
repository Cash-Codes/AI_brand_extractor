package com.brandextractor.infrastructure.ocr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * No-op {@link OcrClient} active when {@code vertexai.enabled=false} (the default).
 *
 * <p>Returns an empty response so the extraction pipeline continues without OCR data.
 * Set {@code VERTEXAI_ENABLED=true} and {@code VERTEXAI_PROJECT_ID} to activate the
 * real {@link VertexOcrClient}.
 */
@Component
@ConditionalOnProperty(name = "vertexai.enabled", havingValue = "false", matchIfMissing = true)
public class MockOcrClient implements OcrClient {

    private static final Logger log = LoggerFactory.getLogger(MockOcrClient.class);

    @Override
    public OcrClientResponse extract(byte[] imageBytes, String mimeType) {
        log.debug("MockOcrClient active — returning empty OCR response. " +
                  "Set VERTEXAI_ENABLED=true to use real Vertex AI OCR.");
        return OcrClientResponse.empty();
    }
}
