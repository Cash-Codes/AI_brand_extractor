package com.brandextractor.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Aggregated extraction confidence")
public record ConfidenceDto(

        @Schema(description = "Overall confidence score in the range [0.0, 1.0]. " +
                              "Scores below 0.30 trigger a LOW_CONFIDENCE validation issue.",
                example = "0.88")
        double overall) {}
