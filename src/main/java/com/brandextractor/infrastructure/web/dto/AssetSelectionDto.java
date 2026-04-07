package com.brandextractor.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Logo and hero image assets identified from the source")
public record AssetSelectionDto(

        @Schema(description = "Candidate logo images, ordered by confidence (highest first)")
        List<AssetItemDto> logos,

        @Schema(description = "Candidate hero / banner images, ordered by confidence (highest first)")
        List<AssetItemDto> heroImages) {}
