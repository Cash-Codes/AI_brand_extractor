package com.brandextractor.integration;

import com.brandextractor.domain.evidence.WebsiteEvidence;
import com.brandextractor.domain.ports.WebsiteIngestionPort;
import com.brandextractor.infrastructure.ai.client.AiExtractionResponse;
import com.brandextractor.infrastructure.ai.client.BrandExtractionAiClient;
import com.brandextractor.infrastructure.ocr.OcrClient;
import com.brandextractor.infrastructure.ocr.OcrClientResponse;
import com.brandextractor.infrastructure.screenshot.ScreenshotClient;
import com.brandextractor.support.error.AiProviderException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Validates that the real {@link com.brandextractor.support.error.GlobalExceptionHandler}
 * produces correctly structured RFC 7807 ProblemDetail responses for every error path.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ExtractionErrorHandlingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BrandExtractionAiClient aiClient;

    @MockitoBean
    private OcrClient ocrClient;

    @MockitoBean
    private ScreenshotClient screenshotClient;

    @MockitoBean
    private WebsiteIngestionPort websiteIngestionPort;

    @BeforeEach
    void setUpDefaults() {
        when(ocrClient.extract(any(), any())).thenReturn(OcrClientResponse.empty());
        when(screenshotClient.capture(any())).thenReturn(Optional.empty());
        when(websiteIngestionPort.ingest(any())).thenReturn(minimalWebsiteEvidence());
    }

    // =========================================================================
    // 400 — request-level validation
    // =========================================================================

    @Test
    void urlEndpoint_missingUrlField_returns400ProblemDetail() throws Exception {
        mockMvc.perform(post("/api/v1/extractions/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.type").value(containsString("validation-error")))
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].field").isString())
                .andExpect(jsonPath("$.errors[0].message").isString())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.instance").isNotEmpty());
    }

    @Test
    void urlEndpoint_malformedJson_returns400ProblemDetail() throws Exception {
        mockMvc.perform(post("/api/v1/extractions/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-valid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Malformed Request Body"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void fileEndpoint_missingFilePart_returns400ProblemDetail() throws Exception {
        mockMvc.perform(multipart("/api/v1/extractions/file"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Missing Request Part"))
                .andExpect(jsonPath("$.status").value(400));
    }

    // =========================================================================
    // 415 — unsupported media type
    // =========================================================================

    @Test
    void urlEndpoint_wrongContentType_returns415ProblemDetail() throws Exception {
        mockMvc.perform(post("/api/v1/extractions/url")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("https://example.com"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Unsupported Media Type"))
                .andExpect(jsonPath("$.status").value(415));
    }

    // =========================================================================
    // 422 — extraction business logic failure
    // =========================================================================

    @Test
    void urlEndpoint_aiThrowsExtractionException_returns422ProblemDetail() throws Exception {
        when(aiClient.extract(any()))
                .thenThrow(new com.brandextractor.support.error.ExtractionException("DNS resolution failed"));

        mockMvc.perform(post("/api/v1/extractions/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"http://localhost:1\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.type").value(containsString("extraction-error")))
                .andExpect(jsonPath("$.title").value("Extraction Failed"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.detail").isString())
                .andExpect(jsonPath("$.instance").value("/api/v1/extractions/url"));
    }

    @Test
    void fileEndpoint_unsupportedMimeType_returns415ProblemDetail() throws Exception {
        // Bytes {1,2,3} detected as application/octet-stream → 415
        mockMvc.perform(multipart("/api/v1/extractions/file")
                        .file(new MockMultipartFile(
                                "file", "animation.gif", "image/gif", new byte[]{1, 2, 3})))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Unsupported Media Type"))
                .andExpect(jsonPath("$.instance").value("/api/v1/extractions/file"));
    }

    @Test
    void fileEndpoint_emptyFile_returns415ProblemDetail() throws Exception {
        // Empty bytes → application/octet-stream → 415
        mockMvc.perform(multipart("/api/v1/extractions/file")
                        .file(new MockMultipartFile(
                                "file", "empty.png", "image/png", new byte[0])))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Unsupported Media Type"));
    }

    // =========================================================================
    // 502 — AI provider failure
    // =========================================================================

    @Test
    void urlEndpoint_aiProviderException_returns502ProblemDetail() throws Exception {
        when(aiClient.extract(any()))
                .thenThrow(new AiProviderException("Gemini quota exceeded"));

        mockMvc.perform(post("/api/v1/extractions/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"http://localhost:1\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.type").value(containsString("ai-provider-error")))
                .andExpect(jsonPath("$.title").value("AI Provider Error"))
                .andExpect(jsonPath("$.status").value(502));
    }

    @Test
    void fileEndpoint_aiProviderException_returns502ProblemDetail() throws Exception {
        when(aiClient.extract(any()))
                .thenThrow(new AiProviderException("Model unavailable"));

        mockMvc.perform(multipart("/api/v1/extractions/file")
                        .file(new MockMultipartFile(
                                "file", "logo.png", "image/png", realPngBytes())))
                .andExpect(status().isBadGateway())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title").value("AI Provider Error"))
                .andExpect(jsonPath("$.status").value(502));
    }

    // =========================================================================
    // ProblemDetail contract — shared fields always present
    // =========================================================================

    @Test
    void allErrorResponses_includeRequiredProblemDetailFields() throws Exception {
        // Use a simple 400 to check the shared contract
        mockMvc.perform(post("/api/v1/extractions/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.type").isString())
                .andExpect(jsonPath("$.title").isString())
                .andExpect(jsonPath("$.status").isNumber())
                .andExpect(jsonPath("$.detail").isString())
                .andExpect(jsonPath("$.instance").isString())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private static byte[] realPngBytes() {
        try {
            BufferedImage img = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
            img.setRGB(0, 0, 0xFF0000);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "png", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test PNG", e);
        }
    }

    private static WebsiteEvidence minimalWebsiteEvidence() {
        return new WebsiteEvidence(
                "w-1", "WEBSITE",
                "https://www.acmestudio.com", "https://www.acmestudio.com/",
                "Acme Studio", null, "Hello world.", List.of("Acme Studio"),
                null, List.of(), List.of(), List.of(),
                null, null, null, "Acme Studio",
                null, null, 1.0, java.time.Instant.now());
    }

    @SuppressWarnings("unused")
    private static AiExtractionResponse goldenAiResponse() {
        return new AiExtractionResponse(
                "Acme Studio", 0.94,
                "Crafting brands that endure", 0.87,
                "A full-service branding agency.", 0.91,
                List.of("bold"),
                "#1A2B3C", null, null,
                null, null,
                Map.of(),
                0.88, List.of());
    }
}
