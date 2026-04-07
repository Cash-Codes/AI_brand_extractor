package com.brandextractor.infrastructure.ai.vertex;

import com.brandextractor.domain.evidence.Evidence;
import com.brandextractor.domain.model.*;
import com.brandextractor.infrastructure.ai.client.AiExtractionRequest;
import com.brandextractor.infrastructure.ai.client.AiExtractionResponse;
import com.brandextractor.infrastructure.ai.client.BrandExtractionAiClient;
import com.brandextractor.infrastructure.discovery.DeterministicCandidateDiscoveryAdapter;
import com.brandextractor.domain.ports.AIAnalysisPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Adapts the domain {@link AIAnalysisPort} to the infrastructure {@link BrandExtractionAiClient}.
 *
 * <p>Responsibilities:
 * <ol>
 *   <li>Run deterministic candidate discovery over the evidence list.</li>
 *   <li>Package evidence + candidates into an {@link AiExtractionRequest}.</li>
 *   <li>Delegate to the AI client and map the response to an {@link ExtractionResult}.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class VertexAiGeminiAdapter implements AIAnalysisPort {

    private final BrandExtractionAiClient               aiClient;
    private final DeterministicCandidateDiscoveryAdapter candidateDiscovery;

    @Override
    public ExtractionResult analyse(List<Evidence> evidence) {
        var candidates = candidateDiscovery.discover(evidence);

        var request = new AiExtractionRequest(
                evidence,
                candidates.brandNames(),
                candidates.taglines(),
                candidates.summaries(),
                candidates.colors(),
                candidates.assets(),
                candidates.links());

        AiExtractionResponse ai = aiClient.extract(request);
        return toResult(ai, evidence);
    }

    // -------------------------------------------------------------------------

    private static ExtractionResult toResult(AiExtractionResponse ai, List<Evidence> evidence) {
        var brandProfile = new BrandProfile(
                new Confident<>(ai.brandName(), ai.brandNameConfidence()),
                new Confident<>(ai.tagline(),   ai.taglineConfidence()),
                new Confident<>(ai.summary(),   ai.summaryConfidence()),
                ai.toneKeywords() != null ? ai.toneKeywords() : List.of());

        var colors = new ColorSelection(
                colorValue(ai.primaryColor()),
                colorValue(ai.secondaryColor()),
                colorValue(ai.textColor()));

        var assets = new AssetSelection(
                ai.logoUrl()      != null ? List.of(assetItem(ai.logoUrl(),      AssetRole.PRIMARY_LOGO)) : List.of(),
                ai.heroImageUrl() != null ? List.of(assetItem(ai.heroImageUrl(), AssetRole.HERO_IMAGE))   : List.of());

        var links = contactLinks(ai.contactLinks());

        List<ExtractionWarning> warnings = ai.warnings() != null
                ? ai.warnings().stream().map(ExtractionWarning::new).toList()
                : List.of();

        return new ExtractionResult(
                UUID.randomUUID(),
                ExtractionInputType.URL,
                null, null,
                brandProfile, colors, assets, links,
                new ConfidenceScore(ai.overallConfidence()),
                warnings,
                List.of(),
                0, 0, 0, false,
                evidence);
    }

    private static ColorValue colorValue(String hex) {
        if (hex == null || hex.isBlank()) return null;
        return new ColorValue(hex, 1.0, List.of());
    }

    private static AssetItem assetItem(String url, AssetRole role) {
        return new AssetItem(url, role, 1.0, 0, 0, null, List.of());
    }

    private static ContactLinks contactLinks(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return new ContactLinks(null, null, null, null, null, null, null, null, null);
        }
        return new ContactLinks(
                map.get("website"),
                map.get("instagram"),
                map.get("linkedin"),
                map.get("email"),
                map.get("twitter"),
                map.get("facebook"),
                map.get("tiktok"),
                map.get("youtube"),
                map.get("phone"));
    }
}
