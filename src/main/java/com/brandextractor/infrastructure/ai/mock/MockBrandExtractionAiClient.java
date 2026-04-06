package com.brandextractor.infrastructure.ai.mock;

import com.brandextractor.infrastructure.ai.client.AiExtractionRequest;
import com.brandextractor.infrastructure.ai.client.AiExtractionResponse;
import com.brandextractor.infrastructure.ai.client.BrandExtractionAiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Fallback {@link BrandExtractionAiClient} that returns a clearly labelled stub response
 * when {@code vertexai.enabled=false} (the default).
 *
 * <p>This bean is active whenever {@code VERTEXAI_ENABLED} is absent or {@code false},
 * allowing the service to start and respond without GCP credentials. All returned values
 * carry the prefix {@code [MOCK]} so callers can immediately identify non-production data.
 *
 * <p>To switch to real Vertex AI, set:
 * <pre>
 *   VERTEXAI_ENABLED=true
 *   VERTEXAI_PROJECT_ID=&lt;your-gcp-project&gt;
 *   GOOGLE_APPLICATION_CREDENTIALS=&lt;path-to-service-account-key&gt;
 * </pre>
 */
@Component
@ConditionalOnProperty(name = "vertexai.enabled", havingValue = "false", matchIfMissing = true)
public class MockBrandExtractionAiClient implements BrandExtractionAiClient {

    private static final Logger log = LoggerFactory.getLogger(MockBrandExtractionAiClient.class);

    @Override
    public AiExtractionResponse extract(AiExtractionRequest request) {
        log.warn("MockBrandExtractionAiClient is active — returning stub data. " +
                 "Set VERTEXAI_ENABLED=true and VERTEXAI_PROJECT_ID to use real Vertex AI.");

        // Derive the most plausible brand name from candidates so the mock is at least
        // somewhat meaningful during local development.
        String candidateName = request.brandNameCandidates().isEmpty()
                ? "Unknown Brand"
                : request.brandNameCandidates().get(0).value();

        return new AiExtractionResponse(
                "[MOCK] " + candidateName,
                0.0,
                "[MOCK] No tagline — Vertex AI not configured",
                0.0,
                "[MOCK] This is a placeholder response. " +
                "Configure VERTEXAI_ENABLED=true and VERTEXAI_PROJECT_ID to enable real extraction.",
                0.0,
                List.of("mock"),
                "#CCCCCC",
                null,
                null,
                null,
                null,
                Map.of(),
                0.0,
                List.of("Vertex AI is not configured. Set VERTEXAI_ENABLED=true to enable real extraction."));
    }
}
