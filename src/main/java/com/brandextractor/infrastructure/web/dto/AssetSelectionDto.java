package com.brandextractor.infrastructure.web.dto;

import java.util.List;

public record AssetSelectionDto(
        List<AssetItemDto> logos,
        List<AssetItemDto> heroImages) {}
