package com.brandextractor.infrastructure.web.controller;

import com.brandextractor.application.extraction.FileExtractionUseCase;
import com.brandextractor.application.extraction.UrlExtractionUseCase;
import com.brandextractor.infrastructure.web.dto.ExtractionResponse;
import com.brandextractor.infrastructure.web.dto.UrlExtractionRequest;
import com.brandextractor.infrastructure.web.mapper.ExtractionResultMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.brandextractor.support.error.ExtractionException;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/extractions")
@RequiredArgsConstructor
public class ExtractionController {

    private final UrlExtractionUseCase urlExtractionUseCase;
    private final FileExtractionUseCase fileExtractionUseCase;
    private final ExtractionResultMapper mapper;

    @PostMapping("/url")
    public ResponseEntity<ExtractionResponse> extractUrl(
            @Valid @RequestBody UrlExtractionRequest request) {
        var result = urlExtractionUseCase.extract(request.url());
        return ResponseEntity.ok(mapper.toResponse(result));
    }

    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExtractionResponse> extractFile(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "sourceLabel", required = false) String sourceLabel) {
        try {
            var result = fileExtractionUseCase.extract(
                    file.getBytes(), file.getContentType(), sourceLabel);
            return ResponseEntity.ok(mapper.toResponse(result));
        } catch (IOException e) {
            throw new ExtractionException("Failed to read uploaded file", e);
        }
    }
}
