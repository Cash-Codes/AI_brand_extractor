package com.brandextractor.domain.model;

import java.util.List;

public record AssetSelection(
        List<AssetItem> logos,
        List<AssetItem> heroImages) {}
