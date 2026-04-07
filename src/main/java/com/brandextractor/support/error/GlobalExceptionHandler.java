package com.brandextractor.support.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String PROBLEM_BASE = "/errors/";

    // -------------------------------------------------------------------------
    // 400 — validation / bad request
    // -------------------------------------------------------------------------

    /** {@code @Valid} on {@code @RequestBody} — returns field-level violation list. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<FieldViolation> violations = ex.getBindingResult().getAllErrors().stream()
                .filter(FieldError.class::isInstance)
                .map(FieldError.class::cast)
                .map(fe -> new FieldViolation(
                        fe.getField(),
                        fe.getDefaultMessage(),
                        fe.getRejectedValue() == null ? null : String.valueOf(fe.getRejectedValue())))
                .toList();

        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST,
                "Validation Failed",
                "One or more fields failed validation.",
                PROBLEM_BASE + "validation-error",
                request);
        pd.setProperty("errors", violations);
        return ResponseEntity.badRequest().body(pd);
    }

    /** {@code @Validated} method / param constraint violations. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        List<FieldViolation> violations = ex.getConstraintViolations().stream()
                .map(cv -> new FieldViolation(
                        cv.getPropertyPath().toString(),
                        cv.getMessage(),
                        cv.getInvalidValue() == null ? null : String.valueOf(cv.getInvalidValue())))
                .toList();

        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST,
                "Validation Failed",
                "One or more parameters failed validation.",
                PROBLEM_BASE + "validation-error",
                request);
        pd.setProperty("errors", violations);
        return ResponseEntity.badRequest().body(pd);
    }

    /** Malformed or unreadable JSON body. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        return ResponseEntity.badRequest().body(
                problem(HttpStatus.BAD_REQUEST,
                        "Malformed Request Body",
                        "The request body could not be parsed. Ensure the JSON is valid.",
                        PROBLEM_BASE + "malformed-body",
                        request));
    }

    /** Missing required {@code @RequestPart} (e.g. missing file in multipart). */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ProblemDetail> handleMissingPart(
            MissingServletRequestPartException ex, HttpServletRequest request) {

        return ResponseEntity.badRequest().body(
                problem(HttpStatus.BAD_REQUEST,
                        "Missing Request Part",
                        "Required part '%s' is missing from the request.".formatted(ex.getRequestPartName()),
                        PROBLEM_BASE + "missing-part",
                        request));
    }

    // -------------------------------------------------------------------------
    // 413 — payload too large
    // -------------------------------------------------------------------------

    /** Uploaded file exceeds the configured {@code spring.servlet.multipart.max-file-size}. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> handleFileTooLarge(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {

        long maxBytes = ex.getMaxUploadSize();
        String maxLabel = maxBytes > 0
                ? (maxBytes / (1024 * 1024)) + " MB"
                : "the configured limit";
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(
                problem(HttpStatus.PAYLOAD_TOO_LARGE,
                        "File Too Large",
                        "The uploaded file exceeds the maximum allowed size of " + maxLabel + ".",
                        PROBLEM_BASE + "file-too-large",
                        request));
    }

    // -------------------------------------------------------------------------
    // 415 — unsupported media type
    // -------------------------------------------------------------------------

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {

        ProblemDetail pd = problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Unsupported Media Type",
                "Content-Type '%s' is not supported here.".formatted(ex.getContentType()),
                PROBLEM_BASE + "unsupported-media-type",
                request);
        pd.setProperty("supportedMediaTypes",
                ex.getSupportedMediaTypes().stream().map(Object::toString).toList());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(pd);
    }

    // -------------------------------------------------------------------------
    // 422 — extraction business logic failure
    // -------------------------------------------------------------------------

    @ExceptionHandler(ExtractionException.class)
    public ResponseEntity<ProblemDetail> handleExtractionException(
            ExtractionException ex, HttpServletRequest request) {

        log.warn("Extraction failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
                problem(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Extraction Failed",
                        ex.getMessage(),
                        PROBLEM_BASE + "extraction-error",
                        request));
    }

    // -------------------------------------------------------------------------
    // 501 — not yet implemented
    // -------------------------------------------------------------------------

    @ExceptionHandler(FeatureNotAvailableException.class)
    public ResponseEntity<ProblemDetail> handleNotImplemented(
            FeatureNotAvailableException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(
                problem(HttpStatus.NOT_IMPLEMENTED,
                        "Not Implemented",
                        "This feature is not yet available.",
                        PROBLEM_BASE + "not-implemented",
                        request));
    }

    // -------------------------------------------------------------------------
    // 502 — upstream AI provider error
    // -------------------------------------------------------------------------

    @ExceptionHandler(AiProviderException.class)
    public ResponseEntity<ProblemDetail> handleAiProviderFailure(
            AiProviderException ex, HttpServletRequest request) {

        log.error("AI provider error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                problem(HttpStatus.BAD_GATEWAY,
                        "AI Provider Error",
                        "The AI provider returned an error. Please retry.",
                        PROBLEM_BASE + "ai-provider-error",
                        request));
    }

    // -------------------------------------------------------------------------
    // 404 — static resource not found (e.g. browser probes like /.well-known/*)
    // -------------------------------------------------------------------------

    /** Let Spring return its default 404 without ERROR-level logging. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResource() {
        return ResponseEntity.notFound().build();
    }

    // -------------------------------------------------------------------------
    // 500 — unexpected catch-all (never leaks internal details)
    // -------------------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(
            Exception ex, HttpServletRequest request) {

        String traceId = UUID.randomUUID().toString();
        log.error("Unhandled exception [traceId={}]", traceId, ex);

        ProblemDetail pd = problem(HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred. Contact support and quote the traceId.",
                PROBLEM_BASE + "internal-error",
                request);
        pd.setProperty("traceId", traceId);
        return ResponseEntity.internalServerError().body(pd);
    }

    // -------------------------------------------------------------------------
    // Shared builder
    // -------------------------------------------------------------------------

    private static ProblemDetail problem(HttpStatus status, String title, String detail,
                                         String typeUri, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setType(URI.create(typeUri));
        pd.setProperty("timestamp", Instant.now());
        pd.setInstance(URI.create(request.getRequestURI()));
        return pd;
    }
}
