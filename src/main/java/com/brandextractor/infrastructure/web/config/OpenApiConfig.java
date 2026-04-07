package com.brandextractor.infrastructure.web.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI brandExtractorOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Brand Extractor API")
                        .description("""
                                Multimodal brand intelligence platform powered by **Vertex AI Gemini**.

                                Give it a public URL or upload a JPEG/PNG flyer and get back a fully \
                                structured brand profile in a single API call: name, tagline, colour \
                                palette, logo and hero assets, social/contact links, and per-field \
                                confidence scores.

                                ## How it works

                                Unlike systems that send raw HTML straight to an LLM, this service uses \
                                a **deterministic-first, AI-as-selector** approach:

                                1. Rule-based services rank every signal (CSS design tokens, Open Graph \
                                tags, DOM position, pixel frequency) before the AI sees anything.
                                2. Gemini receives the pre-ranked candidates and selects the best one. \
                                It never invents values — it picks from real evidence found in the source.
                                3. Gemini's output is forced into an exact JSON schema via Vertex AI \
                                controlled generation, eliminating hallucinated fields or malformed JSON.

                                ## Optional evidence payload

                                Append `?include=evidence` to any extraction endpoint to receive the \
                                full raw evidence collected during extraction (HTML signals, OCR text \
                                blocks, dominant colour candidates, visual labels, etc.) — useful for \
                                debugging or building confidence in the results.

                                ## Confidence scores

                                Every extracted field carries a `confidence` score in `[0.0, 1.0]`. \
                                Scores below `0.30` trigger a `LOW_CONFIDENCE` validation issue in the \
                                response. Use these scores to decide how much downstream automation to \
                                trust versus route to human review.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Brand Extractor")
                                .url("https://github.com/Cash-Codes/AI_brand_extractor"))
                        .license(new License()
                                .name("Private — all rights reserved")))
                .tags(List.of(
                        new Tag()
                                .name("Extractions")
                                .description(
                                        "Core extraction endpoints. Submit a URL or an image file and " +
                                        "receive a structured brand profile with confidence scores."),
                        new Tag()
                                .name("Health")
                                .description(
                                        "Liveness probe. Returns `{\"status\": \"UP\"}` when the " +
                                        "service is running.")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local development")));
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
                    .addProperty("errors",    fieldViolationsArraySchema());

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

    /**
     * Builds the field-violations array schema without triggering raw-type warnings.
     * {@link Schema#addProperty} returns the raw {@code Schema} type in the OAS model
     * library, so chaining {@code .items()} directly on it produces an unavoidable
     * compiler warning. Building the item schema in a typed local variable sidesteps this.
     */
    private Schema<Object> fieldViolationsArraySchema() {
        Schema<Object> itemSchema = new Schema<Object>()
                .type("object")
                .addProperty("field",         new Schema<String>().type("string"))
                .addProperty("message",       new Schema<String>().type("string"))
                .addProperty("rejectedValue", new Schema<String>().type("string"));

        Schema<Object> arraySchema = new Schema<>();
        arraySchema.type("array");
        arraySchema.description("Field-level violations (validation errors only)");
        arraySchema.setItems(itemSchema);
        return arraySchema;
    }
}
