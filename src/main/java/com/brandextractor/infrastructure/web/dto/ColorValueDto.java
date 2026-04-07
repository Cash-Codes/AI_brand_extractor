package com.brandextractor.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "A brand colour with its confidence score and evidence provenance")
public record ColorValueDto(

        @Schema(description = "Hex colour in uppercase #RRGGBB format",
                example = "#1A2B3C",
                pattern = "^#[0-9A-F]{6}$")
        String value,

        @Schema(description = "AI confidence that this is the correct colour, in the range [0.0, 1.0]",
                example = "0.92")
        double confidence,

        @Schema(description = "IDs of the evidence items this colour was sourced from",
                example = "[\"w-1\", \"f-1\"]")
        List<String> evidenceRefs) {}
