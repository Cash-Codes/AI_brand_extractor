package com.brandextractor.infrastructure.vision;

import com.brandextractor.domain.evidence.VisualEvidence;
import com.brandextractor.domain.ports.VisualAnalysisPort;
import com.brandextractor.support.error.FeatureNotAvailableException;
import org.springframework.stereotype.Component;

@Component
public class GeminiVisualAnalysisAdapter implements VisualAnalysisPort {

    @Override
    public VisualEvidence analyse(byte[] imageBytes, String mimeType) {
        throw new UnsupportedOperationException("Visual analysis not yet implemented");
    }
}
