package com.brandextractor.infrastructure.web.controller;

import com.brandextractor.application.extraction.FileExtractionUseCase;
import com.brandextractor.application.extraction.UrlExtractionUseCase;
import com.brandextractor.infrastructure.web.dto.ExtractionResponse;
import com.brandextractor.infrastructure.web.dto.UrlExtractionRequest;
import com.brandextractor.infrastructure.web.mapper.ExtractionResultMapper;
import com.brandextractor.support.error.ExtractionException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Tag(name = "Extractions", description = "Brand extraction from URLs and uploaded image files")
@RestController
@RequestMapping("/api/v1/extractions")
@RequiredArgsConstructor
public class ExtractionController {

    private static final String PROBLEM_JSON = "application/problem+json";

    private final UrlExtractionUseCase urlExtractionUseCase;
    private final FileExtractionUseCase fileExtractionUseCase;
    private final ExtractionResultMapper mapper;

    @Operation(
            summary = "Extract brand from URL",
            description = """
                    Fetches the page at the given URL, analyses it with Vertex AI Gemini, and
                    returns a structured brand profile including colours, assets, links, and
                    confidence scores. Pass `?include=evidence` to include the raw extraction
                    evidence in the response.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Extraction successful",
                    content = @Content(schema = @Schema(implementation = ExtractionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or malformed JSON",
                    content = @Content(mediaType = PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Extraction could not be completed",
                    content = @Content(mediaType = PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "502", description = "AI provider returned an error",
                    content = @Content(mediaType = PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected internal error",
                    content = @Content(mediaType = PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/url")
    public ResponseEntity<ExtractionResponse> extractUrl(
            @Valid @RequestBody UrlExtractionRequest request,
            @Parameter(description = "Pass `evidence` to include raw extraction evidence in the response")
            @RequestParam(name = "include", defaultValue = "") String include) {
        boolean includeEvidence = "evidence".equals(include);
        var result = urlExtractionUseCase.extract(request.url());
        return ResponseEntity.ok(mapper.toResponse(result, includeEvidence));
    }

    @Operation(
            summary = "Extract brand from uploaded file",
            description = """
                    Accepts a JPEG, PNG, WebP, or GIF image upload, analyses it with Vertex AI
                    Gemini, and returns the extracted brand data. Maximum file size is 10 MB.
                    Pass `?include=evidence` to include raw evidence in the response.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Extraction successful",
                    content = @Content(schema = @Schema(implementation = ExtractionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Missing or invalid file part",
                    content = @Content(mediaType = PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "413", description = "File exceeds 10 MB limit",
                    content = @Content(mediaType = PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "415", description = "Unsupported file media type",
                    content = @Content(mediaType = PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Extraction could not be completed",
                    content = @Content(mediaType = PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "502", description = "AI provider returned an error",
                    content = @Content(mediaType = PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected internal error",
                    content = @Content(mediaType = PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExtractionResponse> extractFile(
            @Parameter(description = "Image file to extract brand data from (JPEG/PNG/WebP/GIF, max 10 MB)",
                    required = true)
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "Optional human-readable label for this source")
            @RequestPart(value = "sourceLabel", required = false) String sourceLabel,
            @Parameter(description = "Pass `evidence` to include raw extraction evidence in the response")
            @RequestParam(name = "include", defaultValue = "") String include) {
        boolean includeEvidence = "evidence".equals(include);
        try {
            var result = fileExtractionUseCase.extract(
                    file.getBytes(), file.getContentType(), sourceLabel);
            return ResponseEntity.ok(mapper.toResponse(result, includeEvidence));
        } catch (IOException e) {
            throw new ExtractionException("Failed to read uploaded file", e);
        }
    }
}
