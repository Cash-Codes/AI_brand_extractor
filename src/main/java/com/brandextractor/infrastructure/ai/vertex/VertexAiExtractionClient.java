package com.brandextractor.infrastructure.ai.vertex;

import com.brandextractor.infrastructure.ai.client.AiExtractionRequest;
import com.brandextractor.infrastructure.ai.client.AiExtractionResponse;
import com.brandextractor.infrastructure.ai.client.BrandExtractionAiClient;
import com.brandextractor.support.error.AiProviderException;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Vertex AI Gemini implementation of {@link BrandExtractionAiClient}.
 *
 * <p>Workflow per call:
 * <ol>
 *   <li>Build a {@link GenerativeModel} with a system instruction, generation config
 *       (JSON MIME type + response schema), and temperature=0 for determinism.</li>
 *   <li>Build the user prompt string from normalised evidence and candidates.</li>
 *   <li>Call {@code generateContent} and extract the text response.</li>
 *   <li>Parse and validate via {@link VertexAiResponseParser}.</li>
 *   <li>On {@link MalformedAiResponseException}: retry up to
 *       {@code vertexai.maxParseRetries} times with a correction suffix appended.</li>
 *   <li>On exhausted retries or {@link IOException}: throw {@link AiProviderException}.</li>
 * </ol>
 */
@Component
@ConditionalOnProperty(name = "vertexai.enabled", havingValue = "true")
public class VertexAiExtractionClient implements BrandExtractionAiClient {

    private static final Logger log = LoggerFactory.getLogger(VertexAiExtractionClient.class);

    private static final String RETRY_SUFFIX =
            "\n\nYour previous response did not conform to the required JSON schema. " +
            "Return ONLY valid JSON matching the schema exactly. Do not add markdown fences.";

    @Lazy
    @Autowired
    private VertexAI vertexAI;

    private final VertexAiExtractionProperties props;
    private final VertexAiPromptFactory        promptFactory;
    private final VertexAiResponseParser       responseParser;

    public VertexAiExtractionClient(
            VertexAiExtractionProperties props,
            VertexAiPromptFactory promptFactory,
            VertexAiResponseParser responseParser) {
        this.props          = props;
        this.promptFactory  = promptFactory;
        this.responseParser = responseParser;
    }

    @Override
    public AiExtractionResponse extract(AiExtractionRequest request) {
        Content systemInstruction = ContentMaker.fromString(promptFactory.buildSystemInstruction());
        GenerativeModel model = new GenerativeModel(props.getModelId(), vertexAI)
                .withSystemInstruction(systemInstruction)
                .withGenerationConfig(promptFactory.buildGenerationConfig(props));

        String userPrompt = promptFactory.buildUserContent(request);
        MalformedAiResponseException lastParseError = null;

        for (int attempt = 0; attempt <= props.getMaxParseRetries(); attempt++) {
            String prompt = attempt == 0 ? userPrompt : userPrompt + RETRY_SUFFIX;
            try {
                GenerateContentResponse response = model.generateContent(prompt);
                String rawJson = ResponseHandler.getText(response);
                log.debug("Vertex AI raw response (attempt {}): {}", attempt + 1, rawJson);

                return responseParser.parse(rawJson);

            } catch (MalformedAiResponseException e) {
                lastParseError = e;
                log.warn("Attempt {}/{} — malformed AI response: {}",
                        attempt + 1, props.getMaxParseRetries() + 1, e.getMessage());
            } catch (IOException e) {
                throw new AiProviderException(
                        "Vertex AI call failed on attempt " + (attempt + 1) + ": " + e.getMessage(), e);
            }
        }

        throw new AiProviderException(
                "Vertex AI returned malformed JSON after " + (props.getMaxParseRetries() + 1) +
                " attempts. Last error: " + lastParseError.getMessage(), lastParseError);
    }
}
