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
import io.swagger.v3.oas.annotations.media.ExampleObject;
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

@Tag(name = "Extractions",
     description = "Extract structured brand profiles from public URLs or uploaded image files")
@RestController
@RequestMapping("/api/v1/extractions")
@RequiredArgsConstructor
public class ExtractionController {

    private static final String PROBLEM_JSON = "application/problem+json";

    private static final String URL_REQUEST_EXAMPLE = """
            {
              "url": "https://www.acmestudio.com"
            }
            """;

    private static final String RESPONSE_EXAMPLE = """
            {
              "requestId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              "inputType": "URL",
              "source": {
                "original": "https://www.acmestudio.com",
                "resolved": "https://www.acmestudio.com/"
              },
              "brandProfile": {
                "brandName": "Acme Studio",
                "brandNameConfidence": 0.94,
                "tagline": "Crafting brands that endure",
                "taglineConfidence": 0.87,
                "summary": "Acme Studio is a full-service branding agency.",
                "summaryConfidence": 0.91,
                "toneKeywords": ["bold", "minimal", "modern"]
              },
              "colors": {
                "primary":   { "value": "#1A2B3C", "confidence": 0.92, "evidenceRefs": ["w-1"] },
                "secondary": { "value": "#FF6600", "confidence": 0.80, "evidenceRefs": ["w-1"] }
              },
              "assets": {
                "logos": [{
                  "url": "https://www.acmestudio.com/assets/logo.png",
                  "role": "PRIMARY_LOGO",
                  "confidence": 0.90,
                  "width": 400, "height": 100,
                  "mimeType": "image/png",
                  "evidenceRefs": ["w-1"]
                }],
                "heroImages": []
              },
              "links": {
                "instagram": "https://www.instagram.com/acmestudio",
                "email":     "mailto:hello@acmestudio.com"
              },
              "confidence": { "overall": 0.88 },
              "evidenceSummary": {
                "textEvidenceCount": 1,
                "imageEvidenceCount": 0,
                "ocrBlockCount": 0,
                "usedScreenshot": false
              }
            }
            """;

    private static final String VALIDATION_ERROR_EXAMPLE = """
            {
              "type":     "/errors/validation-error",
              "title":    "Validation Failed",
              "status":   400,
              "detail":   "One or more fields failed validation.",
              "errors":   [{ "field": "url", "message": "url must start with http:// or https://" }]
            }
            """;

    private static final String EXTRACTION_ERROR_EXAMPLE = """
            {
              "type":   "/errors/extraction-error",
              "title":  "Extraction Failed",
              "status": 422,
              "detail": "Brand name could not be extracted from the provided input."
            }
            """;

    private static final String AI_ERROR_EXAMPLE = """
            {
              "type":   "/errors/ai-provider-error",
              "title":  "AI Provider Error",
              "status": 502,
              "detail": "The AI provider returned an error. Please retry."
            }
            """;

    private final UrlExtractionUseCase urlExtractionUseCase;
    private final FileExtractionUseCase fileExtractionUseCase;
    private final ExtractionResultMapper mapper;

    // -------------------------------------------------------------------------
    // POST /url
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Extract brand from URL",
            description = """
                    Fetches the page at the given URL, analyses it with Vertex AI Gemini, and \
                    returns a structured brand profile including colours, assets, links, and \
                    confidence scores.

                    Pass `?include=evidence` to attach the full raw evidence payload (website \
                    HTML signals, candidate colours, etc.) to the response for debugging.
                    """)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "URL to extract brand data from",
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = UrlExtractionRequest.class),
                    examples = @ExampleObject(
                            name = "Typical request",
                            value = URL_REQUEST_EXAMPLE)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Extraction successful",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ExtractionResponse.class),
                            examples = @ExampleObject(name = "Brand profile", value = RESPONSE_EXAMPLE))),
            @ApiResponse(responseCode = "400", description = "Validation error — blank or non-HTTP URL",
                    content = @Content(mediaType = PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(name = "Invalid URL", value = VALIDATION_ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "422", description = "Extraction could not produce a usable result",
                    content = @Content(mediaType = PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(name = "Extraction failed", value = EXTRACTION_ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "502", description = "Vertex AI / Gemini returned an error",
                    content = @Content(mediaType = PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(name = "AI provider error", value = AI_ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "500", description = "Unexpected internal error",
                    content = @Content(mediaType = PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/url")
    public ResponseEntity<ExtractionResponse> extractUrl(
            @Valid @RequestBody UrlExtractionRequest request,
            @Parameter(
                    description = "Pass `evidence` to include the full raw evidence payload in the response",
                    example = "evidence")
            @RequestParam(name = "include", defaultValue = "") String include) {

        boolean includeEvidence = "evidence".equals(include);
        var result = urlExtractionUseCase.extract(request.url());
        return ResponseEntity.ok(mapper.toResponse(result, includeEvidence));
    }

    // -------------------------------------------------------------------------
    // POST /file
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Extract brand from uploaded image file",
            description = """
                    Accepts a JPEG or PNG brand asset (logo, flyer, stationery scan), runs OCR \
                    and colour extraction, analyses the result with Vertex AI Gemini, and returns \
                    the extracted brand profile. Maximum file size is 10 MB.

                    Pass `?include=evidence` to attach the full raw evidence payload (OCR blocks, \
                    dominant colours, flyer metadata) for debugging.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Extraction successful",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ExtractionResponse.class),
                            examples = @ExampleObject(name = "Brand profile", value = RESPONSE_EXAMPLE))),
            @ApiResponse(responseCode = "400",
                    description = "Missing or unreadable file part, or unsupported MIME type declared by client",
                    content = @Content(mediaType = PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "413", description = "File exceeds the 10 MB limit",
                    content = @Content(mediaType = PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "415", description = "Content-Type is not image/jpeg or image/png",
                    content = @Content(mediaType = PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Extraction could not produce a usable result",
                    content = @Content(mediaType = PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(name = "Extraction failed", value = EXTRACTION_ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "502", description = "Vertex AI / Gemini returned an error",
                    content = @Content(mediaType = PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(name = "AI provider error", value = AI_ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "500", description = "Unexpected internal error",
                    content = @Content(mediaType = PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExtractionResponse> extractFile(
            @Parameter(
                    description = "JPEG or PNG image to extract brand data from. Maximum size 10 MB.",
                    required = true,
                    content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE))
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "Human-readable label for this source, e.g. the original filename. Optional.")
            @RequestPart(value = "sourceLabel", required = false) String sourceLabel,
            @Parameter(
                    description = "Pass `evidence` to include the full raw evidence payload in the response",
                    example = "evidence")
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
