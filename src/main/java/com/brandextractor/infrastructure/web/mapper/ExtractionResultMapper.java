package com.brandextractor.infrastructure.web.mapper;

import com.brandextractor.domain.model.*;
import com.brandextractor.infrastructure.web.dto.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ExtractionResultMapper {

    @Mapping(target = "inputType",            expression = "java(result.inputType().name())")
    @Mapping(target = "source.original",      source = "originalSource")
    @Mapping(target = "source.resolved",      source = "resolvedSource")
    @Mapping(target = "warnings",             expression = "java(result.warnings().stream().map(com.brandextractor.domain.model.ExtractionWarning::message).toList())")
    @Mapping(target = "validationIssues",     expression = "java(result.validationIssues().stream().map(com.brandextractor.domain.model.ValidationIssue::message).toList())")
    @Mapping(target = "evidenceSummary.textEvidenceCount",  source = "textEvidenceCount")
    @Mapping(target = "evidenceSummary.imageEvidenceCount", source = "imageEvidenceCount")
    @Mapping(target = "evidenceSummary.ocrBlockCount",      source = "ocrBlockCount")
    @Mapping(target = "evidenceSummary.usedScreenshot",     source = "usedScreenshot")
    ExtractionResponse toResponse(ExtractionResult result);

    BrandProfileDto   toBrandProfileDto(BrandProfile profile);
    ColorSelectionDto toColorSelectionDto(ColorSelection colors);
    ColorValueDto     toColorValueDto(ColorValue colorValue);
    AssetSelectionDto toAssetSelectionDto(AssetSelection assets);

    @Mapping(target = "role", expression = "java(item.role().name())")
    AssetItemDto toAssetItemDto(AssetItem item);

    ContactLinksDto toContactLinksDto(ContactLinks links);
    ConfidenceDto   toConfidenceDto(ConfidenceScore score);
}
