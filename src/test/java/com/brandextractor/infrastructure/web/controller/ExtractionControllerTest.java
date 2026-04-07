package com.brandextractor.infrastructure.web.controller;

import com.brandextractor.application.extraction.FileExtractionUseCase;
import com.brandextractor.application.extraction.UrlExtractionUseCase;
import com.brandextractor.domain.model.*;
import com.brandextractor.support.util.MimeTypeUtils;
import com.brandextractor.infrastructure.web.dto.*;
import com.brandextractor.infrastructure.web.mapper.ExtractionResultMapper;
import com.brandextractor.support.error.AiProviderException;
import com.brandextractor.support.error.ExtractionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExtractionController.class)
class ExtractionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UrlExtractionUseCase urlExtractionUseCase;

    @MockitoBean
    private FileExtractionUseCase fileExtractionUseCase;

    @MockitoBean
    private ExtractionResultMapper mapper;

    @MockitoBean
    private MimeTypeUtils mimeTypeUtils;

    @BeforeEach
    void setUp() {
        when(mimeTypeUtils.detectMimeType(any())).thenReturn("image/png");
    }

    // =========================================================================
    // POST /url — validation
    // =========================================================================

    @Test
    void extractUrl_withMissingUrl_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/extractions/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void extractUrl_withBlankUrl_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/extractions/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void extractUrl_withInvalidScheme_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/extractions/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"ftp://example.com\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void extractUrl_validationError_hasProblemDetailBody() throws Exception {
        mockMvc.perform(post("/api/v1/extractions/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    // =========================================================================
    // POST /url — happy path
    // =========================================================================

    @Test
    void extractUrl_returns200WithResponseBody() throws Exception {
        var stubResult   = stubExtractionResult();
        var stubResponse = stubExtractionResponse();
        when(urlExtractionUseCase.extract("https://example.com")).thenReturn(stubResult);
        when(mapper.toResponse(any(), eq(false))).thenReturn(stubResponse);

        mockMvc.perform(post("/api/v1/extractions/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.requestId").value(stubResponse.requestId().toString()))
                .andExpect(jsonPath("$.inputType").value("URL"))
                .andExpect(jsonPath("$.brandProfile.brandName").value("Acme Studio"))
                .andExpect(jsonPath("$.brandProfile.brandNameConfidence").value(0.94))
                .andExpect(jsonPath("$.colors.primary.value").value("#1A2B3C"))
                .andExpect(jsonPath("$.confidence.overall").value(0.88))
                .andExpect(jsonPath("$.evidenceSummary.textEvidenceCount").value(1));
    }

    @Test
    void extractUrl_responseOmitsNullFieldsFromBody() throws Exception {
        var stubResult   = stubExtractionResult();
        // Response with null warnings and evidence (both should be absent, not "null")
        var stubResponse = stubExtractionResponse();
        when(urlExtractionUseCase.extract(any())).thenReturn(stubResult);
        when(mapper.toResponse(any(), eq(false))).thenReturn(stubResponse);

        mockMvc.perform(post("/api/v1/extractions/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\"}"))
                .andExpect(status().isOk())
                // evidence field absent when not requested
                .andExpect(jsonPath("$.evidence").doesNotExist());
    }

    @Test
    void extractUrl_withIncludeEvidence_callsMapperWithTrue() throws Exception {
        var stubResult = stubExtractionResult();
        when(urlExtractionUseCase.extract("https://example.com")).thenReturn(stubResult);
        when(mapper.toResponse(any(), eq(true))).thenReturn(stubExtractionResponse());

        mockMvc.perform(post("/api/v1/extractions/url")
                        .param("include", "evidence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\"}"))
                .andExpect(status().isOk());

        verify(mapper).toResponse(stubResult, true);
    }

    @Test
    void extractUrl_withoutIncludeParam_callsMapperWithFalse() throws Exception {
        var stubResult = stubExtractionResult();
        when(urlExtractionUseCase.extract("https://example.com")).thenReturn(stubResult);
        when(mapper.toResponse(any(), eq(false))).thenReturn(stubExtractionResponse());

        mockMvc.perform(post("/api/v1/extractions/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\"}"))
                .andExpect(status().isOk());

        verify(mapper).toResponse(stubResult, false);
    }

    @Test
    void extractUrl_ignoreNonEvidenceIncludeParam() throws Exception {
        when(urlExtractionUseCase.extract(any())).thenReturn(stubExtractionResult());
        when(mapper.toResponse(any(), eq(false))).thenReturn(stubExtractionResponse());

        mockMvc.perform(post("/api/v1/extractions/url")
                        .param("include", "something-else")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\"}"))
                .andExpect(status().isOk());

        verify(mapper).toResponse(any(), eq(false));
    }

    // =========================================================================
    // POST /url — error paths
    // =========================================================================

    @Test
    void extractUrl_serviceThrowsExtractionException_returns422() throws Exception {
        when(urlExtractionUseCase.extract(any()))
                .thenThrow(new ExtractionException("DNS resolution failed"));

        mockMvc.perform(post("/api/v1/extractions/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Extraction Failed"))
                .andExpect(jsonPath("$.detail").value(containsString("DNS resolution failed")));
    }

    @Test
    void extractUrl_serviceThrowsAiProviderException_returns502() throws Exception {
        when(urlExtractionUseCase.extract(any()))
                .thenThrow(new AiProviderException("Gemini quota exceeded"));

        mockMvc.perform(post("/api/v1/extractions/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.title").value("AI Provider Error"));
    }

    // =========================================================================
    // POST /file — validation
    // =========================================================================

    @Test
    void extractFile_withNoFile_returns400() throws Exception {
        mockMvc.perform(multipart("/api/v1/extractions/file"))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // POST /file — happy path
    // =========================================================================

    @Test
    void extractFile_returns200WithResponseBody() throws Exception {
        var stubResult   = stubExtractionResult();
        var stubResponse = stubExtractionResponse();
        when(fileExtractionUseCase.extract(any(), any(), any())).thenReturn(stubResult);
        when(mapper.toResponse(any(), eq(false))).thenReturn(stubResponse);

        mockMvc.perform(multipart("/api/v1/extractions/file")
                        .file(pngFile("logo.png")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(stubResponse.requestId().toString()))
                .andExpect(jsonPath("$.brandProfile.brandName").value("Acme Studio"));
    }

    @Test
    void extractFile_passesFileBytesAndContentTypeToUseCase() throws Exception {
        when(fileExtractionUseCase.extract(any(), any(), any())).thenReturn(stubExtractionResult());
        when(mapper.toResponse(any(), eq(false))).thenReturn(stubExtractionResponse());

        mockMvc.perform(multipart("/api/v1/extractions/file")
                        .file(pngFile("brand.png")))
                .andExpect(status().isOk());

        verify(fileExtractionUseCase).extract(
                eq(new byte[]{1, 2, 3}),
                eq("image/png"),
                isNull());
    }

    @Test
    void extractFile_withSourceLabel_passesLabelToUseCase() throws Exception {
        when(fileExtractionUseCase.extract(any(), any(), any())).thenReturn(stubExtractionResult());
        when(mapper.toResponse(any(), eq(false))).thenReturn(stubExtractionResponse());

        mockMvc.perform(multipart("/api/v1/extractions/file")
                        .file(pngFile("logo.png"))
                        .file(new MockMultipartFile(
                                "sourceLabel", "", MediaType.TEXT_PLAIN_VALUE,
                                "campaign-flyer.png".getBytes())))
                .andExpect(status().isOk());

        verify(fileExtractionUseCase).extract(any(), any(), eq("campaign-flyer.png"));
    }

    @Test
    void extractFile_withIncludeEvidence_callsMapperWithTrue() throws Exception {
        var stubResult = stubExtractionResult();
        when(fileExtractionUseCase.extract(any(), any(), any())).thenReturn(stubResult);
        when(mapper.toResponse(any(), eq(true))).thenReturn(stubExtractionResponse());

        mockMvc.perform(multipart("/api/v1/extractions/file")
                        .file(pngFile("logo.png"))
                        .param("include", "evidence"))
                .andExpect(status().isOk());

        verify(mapper).toResponse(stubResult, true);
    }

    @Test
    void extractFile_withoutIncludeParam_callsMapperWithFalse() throws Exception {
        when(fileExtractionUseCase.extract(any(), any(), any())).thenReturn(stubExtractionResult());
        when(mapper.toResponse(any(), eq(false))).thenReturn(stubExtractionResponse());

        mockMvc.perform(multipart("/api/v1/extractions/file")
                        .file(pngFile("logo.png")))
                .andExpect(status().isOk());

        verify(mapper).toResponse(any(), eq(false));
    }

    // =========================================================================
    // POST /file — error paths
    // =========================================================================

    @Test
    void extractFile_serviceThrowsExtractionException_returns422() throws Exception {
        when(fileExtractionUseCase.extract(any(), any(), any()))
                .thenThrow(new ExtractionException("Unsupported MIME type 'image/gif'"));

        mockMvc.perform(multipart("/api/v1/extractions/file")
                        .file(pngFile("logo.png")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Extraction Failed"));
    }

    @Test
    void extractFile_serviceThrowsAiProviderException_returns502() throws Exception {
        when(fileExtractionUseCase.extract(any(), any(), any()))
                .thenThrow(new AiProviderException("AI unavailable"));

        mockMvc.perform(multipart("/api/v1/extractions/file")
                        .file(pngFile("logo.png")))
                .andExpect(status().isBadGateway());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static MockMultipartFile pngFile(String filename) {
        return new MockMultipartFile(
                "file", filename, "image/png", new byte[]{1, 2, 3});
    }

    private static ExtractionResult stubExtractionResult() {
        return new ExtractionResult(
                UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"),
                ExtractionInputType.URL,
                "https://example.com", "https://example.com/",
                new BrandProfile(
                        new Confident<>("Acme", 0.94),
                        new Confident<>("Bold", 0.87),
                        new Confident<>("A studio.", 0.91),
                        List.of()),
                new ColorSelection(new ColorValue("#000000", 0.9, List.of()), null, null),
                new AssetSelection(List.of(), List.of()),
                new ContactLinks(null, null, null, null, null, null, null, null, null),
                new ConfidenceScore(0.88),
                List.of(), List.of(),
                1, 0, 0, false,
                List.of());
    }

    private static ExtractionResponse stubExtractionResponse() {
        UUID id = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
        return new ExtractionResponse(
                id,
                "URL",
                new SourceDto("https://example.com", "https://example.com/"),
                new BrandProfileDto("Acme Studio", 0.94, "A tagline", 0.87,
                        "A summary.", 0.91, List.of("bold")),
                new ColorSelectionDto(
                        new ColorValueDto("#1A2B3C", 0.92, List.of()), null, null),
                new AssetSelectionDto(List.of(), List.of()),
                new ContactLinksDto(null, null, null, null, null, null, null, null, null),
                new ConfidenceDto(0.88),
                null,   // no warnings
                null,   // no validationIssues
                new EvidenceSummaryDto(1, 0, 0, false),
                null);  // evidence not requested
    }
}
