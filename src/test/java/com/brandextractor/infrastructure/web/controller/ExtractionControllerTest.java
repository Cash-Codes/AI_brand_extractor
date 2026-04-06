package com.brandextractor.infrastructure.web.controller;

import com.brandextractor.application.extraction.FileExtractionUseCase;
import com.brandextractor.application.extraction.UrlExtractionUseCase;
import com.brandextractor.infrastructure.web.mapper.ExtractionResultMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

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
}
