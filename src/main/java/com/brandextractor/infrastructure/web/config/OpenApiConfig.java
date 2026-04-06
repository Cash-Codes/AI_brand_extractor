package com.brandextractor.infrastructure.web.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI brandExtractorOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Brand Extractor API")
                        .description("""
                                Multimodal brand extraction service powered by Vertex AI Gemini.
                                Extracts brand profile, colour palette, asset inventory, social/contact
                                links, and confidence scores from a public URL or an uploaded image file.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Brand Extractor")
                                .email("platform@brandextractor.com")));
    }

    /**
     * Registers a reusable ProblemDetail schema as a global component so all
     * {@code application/problem+json} references resolve to the same definition.
     */
    @Bean
    public OpenApiCustomizer problemDetailCustomizer() {
        return openApi -> {
            Schema<?> problemDetailSchema = new Schema<>()
                    .type("object")
                    .description("RFC 7807 Problem Details for HTTP APIs")
                    .addProperty("type",      new Schema<>().type("string").format("uri")
                            .description("URI identifying the problem type"))
                    .addProperty("title",     new Schema<>().type("string")
                            .description("Short human-readable problem summary"))
                    .addProperty("status",    new Schema<>().type("integer")
                            .description("HTTP status code"))
                    .addProperty("detail",    new Schema<>().type("string")
                            .description("Human-readable explanation of this occurrence"))
                    .addProperty("instance",  new Schema<>().type("string").format("uri")
                            .description("URI of the specific request that caused the problem"))
                    .addProperty("timestamp", new Schema<>().type("string").format("date-time")
                            .description("Time the error occurred (ISO-8601)"))
                    .addProperty("traceId",   new Schema<>().type("string")
                            .description("Identifier for correlating this error with server logs"))
                    .addProperty("errors",    new Schema<>().type("array")
                            .description("Field-level violations (validation errors only)")
                            .items(new Schema<>()
                                    .addProperty("field",         new Schema<>().type("string"))
                                    .addProperty("message",       new Schema<>().type("string"))
                                    .addProperty("rejectedValue", new Schema<>().type("string"))));

            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            openApi.getComponents()
                    .addSchemas("ProblemDetail", problemDetailSchema);

            // Add global reusable responses
            ApiResponse problemResponse = new ApiResponse()
                    .description("Error response")
                    .content(new Content().addMediaType(
                            "application/problem+json",
                            new MediaType().schema(new Schema<>().$ref("#/components/schemas/ProblemDetail"))));

            openApi.getComponents()
                    .addResponses("Problem", problemResponse);
        };
    }
}
