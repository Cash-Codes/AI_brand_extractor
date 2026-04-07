package com.brandextractor.infrastructure.ai.client;

/**
 * Infrastructure-level abstraction for AI-powered brand extraction.
 *
 * <p>This is the swap boundary: replace
 * {@link com.brandextractor.infrastructure.ai.vertex.VertexAiExtractionClient}
 * with any other AI backend (OpenAI, Anthropic, local LLM, etc.) without touching
 * the domain or the {@link com.brandextractor.infrastructure.ai.vertex.VertexAiGeminiAdapter}.
 *
 * <p>Implementations must return a fully-populated, schema-valid
 * {@link AiExtractionResponse}. If the underlying model fails after retries, they
 * must throw {@link com.brandextractor.support.error.AiProviderException}.
 */
public interface BrandExtractionAiClient {

    /**
     * Performs brand extraction from the supplied normalised evidence and candidates.
     *
     * @param request structured input including evidence and pre-discovered candidates
     * @return structured, schema-validated extraction result
     * @throws com.brandextractor.support.error.AiProviderException if the AI call fails
     *         after retries or returns a permanently invalid response
     */
    AiExtractionResponse extract(AiExtractionRequest request);
}
