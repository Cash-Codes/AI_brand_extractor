package com.brandextractor.infrastructure.ai.vertex;

import com.brandextractor.domain.evidence.Evidence;
import com.brandextractor.domain.model.ExtractionResult;
import com.brandextractor.domain.ports.AIAnalysisPort;
import com.google.cloud.vertexai.VertexAI;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class VertexAiGeminiAdapter implements AIAnalysisPort {

    private final VertexAI vertexAI;
    private final VertexAiProperties props;

    @Override
    public ExtractionResult analyse(List<Evidence> evidence) {
        throw new UnsupportedOperationException("Gemini extraction not yet implemented");
    }
}
