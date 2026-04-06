package com.brandextractor.infrastructure.vision;

import com.brandextractor.domain.evidence.VisualEvidence;
import com.brandextractor.domain.ports.VisualAnalysisPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * No-op {@link VisualAnalysisPort} active when {@code vertexai.enabled=false} (the default).
 *
 * <p>Returns {@code null} so {@link com.brandextractor.application.extraction.FileExtractionService}
 * skips adding visual evidence, and the pipeline continues with OCR and flyer evidence only.
 * Set {@code VERTEXAI_ENABLED=true} to activate the real {@link GeminiVisualAnalysisAdapter}.
 */
@Component
@ConditionalOnProperty(name = "vertexai.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpVisualAnalysisAdapter implements VisualAnalysisPort {

    @Override
    public VisualEvidence analyse(byte[] imageBytes, String mimeType) {
        return null;
    }
}
