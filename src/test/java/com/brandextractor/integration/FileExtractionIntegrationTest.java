package com.brandextractor.integration;

import com.brandextractor.infrastructure.ai.client.AiExtractionRequest;
import com.brandextractor.infrastructure.ai.client.AiExtractionResponse;
import com.brandextractor.infrastructure.ai.client.BrandExtractionAiClient;
import com.brandextractor.infrastructure.ocr.DetectedText;
import com.brandextractor.infrastructure.ocr.OcrClient;
import com.brandextractor.infrastructure.ocr.OcrClientResponse;
import com.brandextractor.infrastructure.screenshot.ScreenshotClient;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "vertexai.project-id=test-project")
@AutoConfigureMockMvc
class FileExtractionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BrandExtractionAiClient aiClient;

    @MockitoBean
    private OcrClient ocrClient;

    @MockitoBean
    private ScreenshotClient screenshotClient;

    @BeforeEach
    void setUpDefaults() {
        when(ocrClient.extract(any(), any())).thenReturn(OcrClientResponse.empty());
        when(screenshotClient.capture(any())).thenReturn(Optional.empty());
    }

    // =========================================================================
    // Happy path
    // =========================================================================

    @Test
    void extractFile_fullPipeline_returns200WithBrandProfile() throws Exception {
        when(aiClient.extract(any())).thenReturn(goldenAiResponse());

        mockMvc.perform(multipart("/api/v1/extractions/file")
                        .file(minimalPng("logo.png")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.inputType").value("FILE"))
                .andExpect(jsonPath("$.brandProfile.brandName").value("Acme Studio"))
                .andExpect(jsonPath("$.brandProfile.brandNameConfidence").value(0.94))
                .andExpect(jsonPath("$.colors.primary.value").value("#1A2B3C"))
                .andExpect(jsonPath("$.confidence.overall").value(0.88))
                .andExpect(jsonPath("$.evidence").doesNotExist());
    }

    @Test
    void extractFile_withSourceLabel_stampsOriginalSource() throws Exception {
        when(aiClient.extract(any())).thenReturn(goldenAiResponse());

        mockMvc.perform(multipart("/api/v1/extractions/file")
                        .file(minimalPng("logo.png"))
                        .file(new MockMultipartFile(
                                "sourceLabel", "", MediaType.TEXT_PLAIN_VALUE,
                                "campaign-flyer.png".getBytes())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source.original").value("campaign-flyer.png"));
    }

    @Test
    void extractFile_withoutSourceLabel_sourceOriginalIsNull() throws Exception {
        when(aiClient.extract(any())).thenReturn(goldenAiResponse());

        mockMvc.perform(multipart("/api/v1/extractions/file")
                        .file(minimalPng("logo.png")))
                .andExpect(status().isOk())
                // null is omitted by @JsonInclude(NON_NULL)
                .andExpect(jsonPath("$.source.original").doesNotExist());
    }

    @Test
    void extractFile_withIncludeEvidence_returnsEvidencePayload() throws Exception {
        when(aiClient.extract(any())).thenReturn(goldenAiResponse());

        mockMvc.perform(multipart("/api/v1/extractions/file")
                        .file(minimalPng("logo.png"))
                        .param("include", "evidence"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evidence").isArray())
                .andExpect(jsonPath("$.evidence[0].type").value("FLYER"));
    }

    @Test
    void extractFile_evidenceSummary_countsFlyerAndOcrEvidence() throws Exception {
        when(aiClient.extract(any())).thenReturn(goldenAiResponse());
        when(ocrClient.extract(any(), any())).thenReturn(new OcrClientResponse(
                List.of(new DetectedText("Acme Studio", null, 0.99)), 100, 100));

        mockMvc.perform(multipart("/api/v1/extractions/file")
                        .file(minimalPng("logo.png")))
                .andExpect(status().isOk())
                // FlyerEvidence counts as image evidence
                .andExpect(jsonPath("$.evidenceSummary.imageEvidenceCount").value(1))
                .andExpect(jsonPath("$.evidenceSummary.ocrBlockCount").value(1))
                .andExpect(jsonPath("$.evidenceSummary.usedScreenshot").value(false));
    }

    @Test
    void extractFile_ocrTextBlocks_passedToAiClient() throws Exception {
        when(aiClient.extract(any())).thenReturn(goldenAiResponse());
        when(ocrClient.extract(any(), any())).thenReturn(new OcrClientResponse(
                List.of(
                        new DetectedText("Acme Studio", null, 0.99),
                        new DetectedText("Bold branding for bold companies", null, 0.95)),
                200, 100));

        mockMvc.perform(multipart("/api/v1/extractions/file")
                        .file(minimalPng("logo.png")))
                .andExpect(status().isOk());

        ArgumentCaptor<AiExtractionRequest> captor = ArgumentCaptor.forClass(AiExtractionRequest.class);
        verify(aiClient).extract(captor.capture());

        // Evidence list: FlyerEvidence + OcrEvidence
        assertThat(captor.getValue().evidence()).hasSize(2);
    }

    @Test
    void extractFile_jpegFile_isAccepted() throws Exception {
        when(aiClient.extract(any())).thenReturn(goldenAiResponse());

        mockMvc.perform(multipart("/api/v1/extractions/file")
                        .file(new MockMultipartFile(
                                "file", "photo.jpg", "image/jpeg", realJpegBytes())))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // Input validation
    // =========================================================================

    @Test
    void extractFile_withNoFilePart_returns400() throws Exception {
        mockMvc.perform(multipart("/api/v1/extractions/file"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void extractFile_withUnsupportedMimeType_returns415WithProblemDetail() throws Exception {
        // Controller detects MIME from bytes — {1,2,3} → application/octet-stream → 415
        mockMvc.perform(multipart("/api/v1/extractions/file")
                        .file(new MockMultipartFile(
                                "file", "animation.gif", "image/gif", new byte[]{1, 2, 3})))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Unsupported Media Type"))
                .andExpect(jsonPath("$.status").value(415));
    }

    @Test
    void extractFile_withEmptyFile_returns415WithProblemDetail() throws Exception {
        // Empty bytes → application/octet-stream → 415 before service can validate
        mockMvc.perform(multipart("/api/v1/extractions/file")
                        .file(new MockMultipartFile(
                                "file", "empty.png", "image/png", new byte[0])))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Unsupported Media Type"));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static MockMultipartFile minimalPng(String filename) {
        return new MockMultipartFile("file", filename, "image/png", realPngBytes());
    }

    private static byte[] realPngBytes() {
        try {
            BufferedImage img = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
            img.setRGB(0, 0, 0x1A2B3C);
            img.setRGB(1, 0, 0xFF6600);
            img.setRGB(0, 1, 0xFFFFFF);
            img.setRGB(1, 1, 0x000000);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "png", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test PNG", e);
        }
    }

    private static byte[] realJpegBytes() {
        try {
            BufferedImage img = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
            img.setRGB(0, 0, 0xFF0000);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "jpeg", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test JPEG", e);
        }
    }

    private static AiExtractionResponse goldenAiResponse() {
        return new AiExtractionResponse(
                "Acme Studio", 0.94,
                "Crafting brands that endure", 0.87,
                "A full-service branding agency.", 0.91,
                List.of("bold", "minimal", "modern"),
                "#1A2B3C", "#FF6600", null,
                "https://acme.com/logo.png", null,
                Map.of("instagram", "https://instagram.com/acmestudio"),
                0.88, List.of());
    }
}
