package com.brandextractor.infrastructure.web.dto.evidence;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = WebsiteEvidenceDto.class,    name = "WEBSITE"),
        @JsonSubTypes.Type(value = FlyerEvidenceDto.class,      name = "FLYER"),
        @JsonSubTypes.Type(value = OcrEvidenceDto.class,        name = "OCR"),
        @JsonSubTypes.Type(value = VisualEvidenceDto.class,     name = "VISUAL"),
        @JsonSubTypes.Type(value = ColorEvidenceDto.class,      name = "COLOR"),
        @JsonSubTypes.Type(value = LinkEvidenceDto.class,       name = "LINK"),
        @JsonSubTypes.Type(value = TextEvidenceDto.class,       name = "TEXT"),
        @JsonSubTypes.Type(value = ScreenshotEvidenceDto.class, name = "SCREENSHOT")
})
public sealed interface EvidenceDto
        permits WebsiteEvidenceDto, FlyerEvidenceDto, OcrEvidenceDto,
                VisualEvidenceDto, ColorEvidenceDto, LinkEvidenceDto,
                TextEvidenceDto, ScreenshotEvidenceDto {

    String id();
    String sourceType();
    String sourceReference();
}
