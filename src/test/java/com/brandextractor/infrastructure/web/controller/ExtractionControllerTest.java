package com.brandextractor.infrastructure.web.controller;

import com.brandextractor.application.extraction.FileExtractionUseCase;
import com.brandextractor.application.extraction.UrlExtractionUseCase;
import com.brandextractor.domain.model.*;
import com.brandextractor.infrastructure.web.mapper.ExtractionResultMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExtractionController.class)
class ExtractionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UrlExtractionUseCase urlExtractionUseCase;

    @MockBean
    private FileExtractionUseCase fileExtractionUseCase;

    @MockBean
    private ExtractionResultMapper mapper;

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
    void extractFile_withNoFile_returns400() throws Exception {
        mockMvc.perform(multipart("/api/v1/extractions/file"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void extractUrl_withIncludeEvidence_callsMapperWithTrue() throws Exception {
        var stubResult = stubExtractionResult();
        when(urlExtractionUseCase.extract("https://example.com")).thenReturn(stubResult);
        when(mapper.toResponse(any(), eq(true))).thenReturn(null);

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
        when(mapper.toResponse(any(), eq(false))).thenReturn(null);

        mockMvc.perform(post("/api/v1/extractions/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\"}"))
                .andExpect(status().isOk());

        verify(mapper).toResponse(stubResult, false);
    }

    // ---- helper ----

    private ExtractionResult stubExtractionResult() {
        var brandProfile = new BrandProfile(
                new Confident<>("Acme", 0.94),
                new Confident<>("Bold", 0.87),
                new Confident<>("A studio.", 0.91),
                List.of());
        var colorValue = new ColorValue("#000000", 0.9, List.of());
        var colors     = new ColorSelection(colorValue, null, null);
        var assets     = new AssetSelection(List.of(), List.of());
        var links      = new ContactLinks(null, null, null, null, null, null, null, null, null);
        var confidence = new ConfidenceScore(0.9);
        return new ExtractionResult(
                UUID.randomUUID(), ExtractionInputType.URL,
                "https://example.com", "https://example.com/",
                brandProfile, colors, assets, links, confidence,
                List.of(), List.of(), 0, 0, 0, false,
                List.of());
    }
}
