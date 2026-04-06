package com.brandextractor.domain.ports;

import com.brandextractor.domain.evidence.VisualEvidence;

public interface VisualAnalysisPort {
    VisualEvidence analyse(byte[] imageBytes, String mimeType);
}
