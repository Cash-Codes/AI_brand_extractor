package com.brandextractor.infrastructure.web.mapper;

import com.brandextractor.domain.evidence.*;
import com.brandextractor.domain.model.*;
import com.brandextractor.infrastructure.web.dto.*;
import com.brandextractor.infrastructure.web.dto.evidence.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ExtractionResultMapper {

    @Mapping(target = "inputType",  expression = "java(result.inputType().name())")
    @Mapping(target = "source.original", source = "result.originalSource")
    @Mapping(target = "source.resolved", source = "result.resolvedSource")
    @Mapping(target = "warnings",   expression = "java(result.warnings().stream().map(com.brandextractor.domain.model.ExtractionWarning::message).toList())")
    @Mapping(target = "validationIssues", expression = "java(result.validationIssues().stream().map(com.brandextractor.domain.model.ValidationIssue::message).toList())")
    @Mapping(target = "evidenceSummary.textEvidenceCount",  source = "result.textEvidenceCount")
    @Mapping(target = "evidenceSummary.imageEvidenceCount", source = "result.imageEvidenceCount")
    @Mapping(target = "evidenceSummary.ocrBlockCount",      source = "result.ocrBlockCount")
    @Mapping(target = "evidenceSummary.usedScreenshot",     source = "result.usedScreenshot")
    @Mapping(target = "evidence",
             expression = "java(includeEvidence ? toEvidenceDtoList(result.evidence()) : null)")
    ExtractionResponse toResponse(ExtractionResult result, boolean includeEvidence);

    @Mapping(target = "brandName",           expression = "java(profile.brandName().value())")
    @Mapping(target = "brandNameConfidence", expression = "java(profile.brandName().confidence())")
    @Mapping(target = "tagline",             expression = "java(profile.tagline().value())")
    @Mapping(target = "taglineConfidence",   expression = "java(profile.tagline().confidence())")
    @Mapping(target = "summary",             expression = "java(profile.summary().value())")
    @Mapping(target = "summaryConfidence",   expression = "java(profile.summary().confidence())")
    BrandProfileDto toBrandProfileDto(BrandProfile profile);

    ColorSelectionDto toColorSelectionDto(ColorSelection colors);
    ColorValueDto     toColorValueDto(ColorValue colorValue);
    AssetSelectionDto toAssetSelectionDto(AssetSelection assets);

    @Mapping(target = "role", expression = "java(item.role().name())")
    AssetItemDto toAssetItemDto(AssetItem item);

    ContactLinksDto toContactLinksDto(ContactLinks links);
    ConfidenceDto   toConfidenceDto(ConfidenceScore score);

    default TextBlockDto toTextBlockDto(com.brandextractor.domain.evidence.TextBlock block) {
        BoundingBoxDto bbox = block.boundingBox() == null ? null
                : new BoundingBoxDto(block.boundingBox().x(), block.boundingBox().y(),
                                     block.boundingBox().width(), block.boundingBox().height());
        return new TextBlockDto(block.text(), bbox, block.confidence());
    }

    default List<EvidenceDto> toEvidenceDtoList(List<Evidence> list) {
        return list.stream().map(this::toEvidenceDto).toList();
    }

    default EvidenceDto toEvidenceDto(Evidence evidence) {
        return switch (evidence) {
            case WebsiteEvidence e    -> new WebsiteEvidenceDto(e.id(), e.sourceType(), e.sourceReference(),
                                             e.resolvedUrl(), e.title(), e.metaDescription(), e.visibleText(),
                                             e.headings(), e.faviconUrl(), e.imageUrls(), e.socialLinks(),
                                             e.cssColorCandidates(), e.ogTitle(), e.ogDescription(),
                                             e.ogImage(), e.ogSiteName(), e.twitterCard(), e.twitterImage());
            case FlyerEvidence e      -> new FlyerEvidenceDto(e.id(), e.sourceType(), e.sourceReference(),
                                             e.mimeType(), e.width(), e.height(), e.sizeBytes(),
                                             e.dominantColors());
            case OcrEvidence e        -> new OcrEvidenceDto(e.id(), e.sourceType(), e.sourceReference(),
                                             e.blocks().stream().map(this::toTextBlockDto).toList());
            case VisualEvidence e     -> new VisualEvidenceDto(e.id(), e.sourceType(), e.sourceReference(),
                                             e.detectedLabels(), e.dominantTheme());
            case ColorEvidence e      -> new ColorEvidenceDto(e.id(), e.sourceType(), e.sourceReference(),
                                             e.hexValue(), e.frequency());
            case LinkEvidence e       -> new LinkEvidenceDto(e.id(), e.sourceType(), e.sourceReference(),
                                             e.href(), e.rel(), e.context());
            case TextEvidence e       -> new TextEvidenceDto(e.id(), e.sourceType(), e.sourceReference(),
                                             e.text(), e.textType());
            case ScreenshotEvidence e -> new ScreenshotEvidenceDto(e.id(), e.sourceType(), e.sourceReference(),
                                             e.mimeType(), e.width(), e.height());
        };
    }
}
