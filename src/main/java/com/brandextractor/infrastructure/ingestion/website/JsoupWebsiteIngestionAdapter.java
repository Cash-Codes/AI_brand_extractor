package com.brandextractor.infrastructure.ingestion.website;

import com.brandextractor.domain.evidence.WebsiteEvidence;
import com.brandextractor.domain.ports.WebsiteIngestionPort;
import com.brandextractor.support.error.ExtractionException;
import com.brandextractor.support.util.ImageColorSampler;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class JsoupWebsiteIngestionAdapter implements WebsiteIngestionPort {

    private static final Logger log = LoggerFactory.getLogger(JsoupWebsiteIngestionAdapter.class);

    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/124.0.0.0 Safari/537.36";

    private static final int  CONNECT_TIMEOUT_MS     = 15_000;
    private static final int  ASSET_TIMEOUT_MS       = 5_000;  // OG image / CSS fetches
    private static final int  MAX_BODY_BYTES          = 2 * 1024 * 1024; // 2 MB
    private static final int  MAX_OG_IMAGE_BYTES      = 2 * 1024 * 1024; // 2 MB
    private static final int  MAX_CSS_BYTES           = 200 * 1024;       // 200 KB per file
    private static final int  MAX_LINKED_CSS_FILES    = 4;
    private static final int  MAX_HEADINGS            = 20;
    private static final int  MAX_IMAGES              = 30;
    private static final int  MAX_SOCIAL_LINKS        = 20;
    private static final int  MAX_COLORS              = 30;
    private static final int  MAX_VISIBLE_TEXT        = 3_000;

    // Hex colour: #RGB or #RRGGBB  (word-boundary prevents matching longer sequences)
    private static final Pattern HEX_COLOR =
            Pattern.compile("#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{3})(?![0-9A-Fa-f])");

    // rgb()/rgba() — comma-separated (legacy) and space-separated (CSS Color Level 4).
    // Groups 1, 2, 3 capture the R, G, B integer components.
    private static final Pattern RGB_COLOR = Pattern.compile(
            "rgba?\\(\\s*(\\d{1,3})\\s*[, ]\\s*(\\d{1,3})\\s*[, ]\\s*(\\d{1,3})" +
            "(?:\\s*/[^)]*|\\s*,\\s*[\\d.]+)?\\s*\\)",
            Pattern.CASE_INSENSITIVE);

    // Any CSS custom-property declaration whose value is a direct hex or rgb() colour.
    // Used inside :root / html blocks to harvest design tokens regardless of var name.
    private static final Pattern CSS_VAR_DECL = Pattern.compile(
            "--[-\\w]+\\s*:\\s*(" +
            "#(?:[0-9A-Fa-f]{6}|[0-9A-Fa-f]{3})(?![0-9A-Fa-f])" +
            "|rgba?\\(\\s*\\d{1,3}\\s*[, ]\\s*\\d{1,3}\\s*[, ]\\s*\\d{1,3}[^)]*\\)" +
            ")",
            Pattern.CASE_INSENSITIVE);

    // A :root or html block (may be multiline).
    private static final Pattern ROOT_BLOCK =
            Pattern.compile("(?::root|html)\\s*\\{([^}]+)\\}",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    // CSS var names that are unambiguously brand-specific (NOT "color" — too generic,
    // causes false positives like --text-color:#212121 winning over real brand colours).
    private static final Pattern CSS_VAR_BRAND = Pattern.compile(
            "--[-\\w]*(?:brand|primary|accent|theme|highlight)[-\\w]*"
            + "\\s*:\\s*(#(?:[0-9A-Fa-f]{6}|[0-9A-Fa-f]{3})(?![0-9A-Fa-f])"
            + "|rgba?\\(\\s*\\d{1,3}\\s*[, ]\\s*\\d{1,3}\\s*[, ]\\s*\\d{1,3}[^)]*\\))",
            Pattern.CASE_INSENSITIVE);

    private static final Set<String> SOCIAL_DOMAINS = Set.of(
            "instagram.com", "twitter.com", "x.com", "facebook.com",
            "linkedin.com", "tiktok.com", "youtube.com");

    // -------------------------------------------------------------------------
    // Port interface
    // -------------------------------------------------------------------------

    @Override
    public WebsiteEvidence ingest(String url) {
        try {
            Connection.Response response = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(CONNECT_TIMEOUT_MS)
                    .maxBodySize(MAX_BODY_BYTES)
                    .followRedirects(true)
                    .ignoreHttpErrors(false)
                    .execute();

            String resolvedUrl = response.url().toString();
            Document doc = response.parse();
            return parseDocument(url, resolvedUrl, doc);

        } catch (HttpStatusException e) {
            throw new ExtractionException(
                    "URL returned HTTP " + e.getStatusCode() + ": " + url, e);
        } catch (UnknownHostException e) {
            throw new ExtractionException(
                    "Host not found: " + url, e);
        } catch (SocketTimeoutException e) {
            throw new ExtractionException(
                    "Timed out fetching URL: " + url, e);
        } catch (IOException e) {
            throw new ExtractionException("Failed to fetch URL: " + url, e);
        }
    }

    // -------------------------------------------------------------------------
    // Package-private for unit testing
    // -------------------------------------------------------------------------

    WebsiteEvidence parseDocument(String originalUrl, String resolvedUrl, Document doc) {
        return new WebsiteEvidence(
                UUID.randomUUID().toString(),
                "WEBSITE",
                originalUrl,
                resolvedUrl,
                extractTitle(doc),
                extractMetaDescription(doc),
                extractVisibleText(doc),
                extractHeadings(doc),
                extractFavicon(doc),
                extractImageUrls(doc),
                extractSocialLinks(doc),
                extractCssColors(doc),
                extractMeta(doc, "og:title"),
                extractMeta(doc, "og:description"),
                extractMeta(doc, "og:image"),
                extractMeta(doc, "og:site_name"),
                extractMeta(doc, "twitter:card"),
                extractMeta(doc, "twitter:image"),
                1.0,
                Instant.now());
    }

    // -------------------------------------------------------------------------
    // Extraction helpers
    // -------------------------------------------------------------------------

    private String extractTitle(Document doc) {
        String title = doc.title();
        return title.isBlank() ? null : title.strip();
    }

    private String extractMetaDescription(Document doc) {
        Element meta = doc.select("meta[name=description]").first();
        if (meta != null) {
            String content = meta.attr("content").strip();
            if (!content.isBlank()) return content;
        }
        return null;
    }

    private String extractVisibleText(Document doc) {
        Document clone = doc.clone();
        clone.select("script, style, noscript, nav, footer, iframe").remove();
        String text = clone.body() != null ? clone.body().text() : "";
        if (text.length() <= MAX_VISIBLE_TEXT) return text;
        return text.substring(0, MAX_VISIBLE_TEXT) + "…";
    }

    private List<String> extractHeadings(Document doc) {
        return doc.select("h1, h2, h3").stream()
                .map(Element::text)
                .map(String::strip)
                .filter(h -> !h.isBlank())
                .limit(MAX_HEADINGS)
                .toList();
    }

    private String extractFavicon(Document doc) {
        // Prefer explicitly linked icons
        Element link = doc.select("link[rel~=icon]").first();
        if (link == null) link = doc.select("link[rel=apple-touch-icon]").first();
        if (link != null) {
            String href = link.attr("abs:href");
            if (!href.isBlank()) return href;
        }
        // Fall back to conventional /favicon.ico at the origin
        String base = doc.baseUri();
        if (!base.isBlank()) {
            try {
                java.net.URI uri = java.net.URI.create(base);
                return uri.getScheme() + "://" + uri.getHost() + "/favicon.ico";
            } catch (IllegalArgumentException ignored) {
                // malformed base URI — return null
            }
        }
        return null;
    }

    private List<String> extractImageUrls(Document doc) {
        Set<String> seen = new LinkedHashSet<>();
        for (Element img : doc.select("img")) {
            String src = img.attr("abs:src");
            if (src.isBlank()) src = img.attr("abs:data-src");
            if (src.isBlank()) src = img.attr("abs:data-lazy-src");
            if (!src.isBlank()) seen.add(src);
            if (seen.size() >= MAX_IMAGES) break;
        }
        return List.copyOf(seen);
    }

    private List<String> extractSocialLinks(Document doc) {
        Set<String> seen = new LinkedHashSet<>();
        for (Element a : doc.select("a[href]")) {
            String href = a.attr("abs:href");
            if (href.isBlank()) continue;
            if (isSocialOrContact(href)) {
                seen.add(href);
                if (seen.size() >= MAX_SOCIAL_LINKS) break;
            }
        }
        return List.copyOf(seen);
    }

    private boolean isSocialOrContact(String href) {
        if (href.startsWith("mailto:") || href.startsWith("tel:")) return true;
        try {
            String host = java.net.URI.create(href).getHost();
            if (host == null) return false;
            return SOCIAL_DOMAINS.stream().anyMatch(host::endsWith);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Builds an ordered colour candidate list from highest to lowest signal.
     *
     * <p>CSS is fetched exactly once and reused across all steps.
     *
     * <ol>
     *   <li>Non-social SVG fills in header / nav / logo</li>
     *   <li>OG image dominant colours (pixel sampling)</li>
     *   <li>{@code theme-color} / {@code msapplication-TileColor} meta</li>
     *   <li>{@code :root} / {@code html} design-token vars — all direct-value colour
     *       declarations, chromatic-filtered and ranked by usage frequency.
     *       Catches tokens like {@code --crukBrand-colors-navy-600: #00007E}
     *       without relying on var-name guessing.</li>
     *   <li>Explicitly brand-named CSS vars (safety net for non-{@code :root} declarations)</li>
     *   <li>Inline {@code style} attribute colours</li>
     *   <li>Fallback: frequency-ranked chromatic colours from all CSS</li>
     * </ol>
     */
    private List<String> extractCssColors(Document doc) {
        Set<String> colors = new LinkedHashSet<>();

        // ── Phase 1: HTML signals ─────────────────────────────────────────────
        collectSvgColors(doc, colors);

        String ogImageUrl = extractMeta(doc, "og:image");
        if (ogImageUrl != null && !ogImageUrl.isBlank()) {
            collectOgImageColors(ogImageUrl, colors);
        }

        for (String attr : List.of("theme-color", "msapplication-TileColor")) {
            Element meta = doc.select("meta[name=" + attr + "]").first();
            if (meta != null) {
                String hex = parseColorValue(meta.attr("content").strip());
                if (hex != null) colors.add(hex);
            }
        }

        // ── Phase 2: CSS signals — fetch all CSS text exactly once ────────────
        List<String> cssTexts = gatherCssTexts(doc);
        Map<String, Integer> freq = buildFrequencyMap(cssTexts);

        // :root / html design tokens: every colour var with a direct value,
        // sorted by how often that colour appears across all CSS rules.
        // Primary brand colours appear in dozens of rules; one-off colours do not.
        collectRootTokenColors(cssTexts, freq, colors);

        // Brand-keyword vars as a safety net (non-:root declarations).
        // Keywords deliberately exclude "color" — too generic, causes
        // --text-color:#212121 to pollute results.
        for (String css : cssTexts) {
            collectCssBrandVars(css, colors);
        }

        // Inline style attributes
        for (Element el : doc.select("[style]")) {
            collectAllColors(el.attr("style"), colors);
            if (colors.size() >= MAX_COLORS) return List.copyOf(colors);
        }

        // Fallback: frequency-ranked chromatic colours from all CSS
        freq.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .limit(MAX_COLORS)
                .forEach(colors::add);

        return List.copyOf(colors);
    }

    /**
     * Fetches all CSS text (inline {@code <style>} tags + linked stylesheets)
     * into a list. CSS is fetched once here and reused by every subsequent step.
     */
    private List<String> gatherCssTexts(Document doc) {
        List<String> texts = new ArrayList<>();
        for (Element style : doc.select("style")) {
            String html = style.html();
            if (!html.isBlank()) texts.add(html);
        }
        int fetched = 0;
        for (Element link : doc.select("link[rel=stylesheet][href]")) {
            if (fetched >= MAX_LINKED_CSS_FILES) break;
            String href = link.attr("abs:href");
            if (href.isBlank()) continue;
            try {
                String css = Jsoup.connect(href)
                        .userAgent(USER_AGENT)
                        .timeout(ASSET_TIMEOUT_MS)
                        .maxBodySize(MAX_CSS_BYTES)
                        .ignoreContentType(true)
                        .execute()
                        .body();
                texts.add(css);
                fetched++;
            } catch (Exception e) {
                log.debug("Could not fetch linked CSS {}: {}", href, e.getMessage());
            }
        }
        return texts;
    }

    /** Counts chromatic colour occurrences across all provided CSS texts. */
    private static Map<String, Integer> buildFrequencyMap(List<String> cssTexts) {
        Map<String, Integer> freq = new HashMap<>();
        for (String css : cssTexts) countColorFrequencies(css, freq);
        return freq;
    }

    /**
     * Extracts colour custom-property values from {@code :root} and {@code html}
     * blocks, keeps only chromatic ones, and adds them to {@code colors} sorted
     * by descending frequency of use across all CSS. The most-used colours are
     * almost certainly the brand's primary and secondary colours.
     */
    private static void collectRootTokenColors(
            List<String> cssTexts, Map<String, Integer> freq, Set<String> colors) {
        Set<String> rootColors = new LinkedHashSet<>();
        for (String css : cssTexts) {
            Matcher block = ROOT_BLOCK.matcher(css);
            while (block.find()) {
                Matcher decl = CSS_VAR_DECL.matcher(block.group(1));
                while (decl.find()) {
                    String hex = parseColorValue(decl.group(1));
                    if (hex != null && isChromatic(hex)) rootColors.add(hex);
                }
            }
        }
        rootColors.stream()
                .sorted((a, b) -> Integer.compare(
                        freq.getOrDefault(b, 0), freq.getOrDefault(a, 0)))
                .forEach(colors::add);
    }

    // ── SVG colour extraction ─────────────────────────────────────────────────

    /**
     * Extracts hex colours from inline SVG {@code fill} and {@code stroke} attributes.
     *
     * <p>Priority order:
     * <ol>
     *   <li>SVGs inside {@code header}, {@code nav}, or elements whose id/class
     *       contains "logo" — almost always the wordmark or brand icon.</li>
     *   <li>All other inline SVGs — icons, illustrations, decorative elements.</li>
     * </ol>
     * The value {@code "none"} and {@code "currentColor"} are skipped.
     */
    private static void collectSvgColors(Document doc, Set<String> colors) {
        // High-priority: logo / header SVGs (excluding social platform icons)
        for (Element svg : doc.select("header svg, nav svg, [id*=logo] svg, [class*=logo] svg")) {
            if (!isSocialIcon(svg)) collectSvgFills(svg, colors);
        }
        // Lower-priority: remaining SVGs (excluding social icons)
        for (Element svg : doc.select("svg")) {
            if (!isSocialIcon(svg)) collectSvgFills(svg, colors);
        }
    }

    /**
     * Returns {@code true} if the SVG element is inside a social-media link or
     * a share/follow widget — these carry platform brand colours (Facebook blue,
     * WhatsApp green, etc.) that are not the brand's own colours.
     */
    private static boolean isSocialIcon(Element svg) {
        // Inside a hyperlink to a social platform
        if (svg.closest(
                "a[href*='facebook'], a[href*='twitter'], a[href*='x.com'], "
                + "a[href*='instagram'], a[href*='linkedin'], a[href*='youtube'], "
                + "a[href*='tiktok'], a[href*='whatsapp'], a[href*='pinterest'], "
                + "a[href*='snapchat']") != null) return true;
        // Inside a social-share / follow widget container
        return svg.closest("[class*=social], [class*=follow], [class*=share], [class*=addthis]") != null;
    }

    private static void collectSvgFills(Element svg, Set<String> colors) {
        for (Element el : svg.select("[fill],[stroke]")) {
            addSvgColorAttr(el.attr("fill"),   colors);
            addSvgColorAttr(el.attr("stroke"), colors);
        }
        // Also check the SVG root element itself
        addSvgColorAttr(svg.attr("fill"),   colors);
        addSvgColorAttr(svg.attr("stroke"), colors);
    }

    private static void addSvgColorAttr(String value, Set<String> colors) {
        if (value == null || value.isBlank()) return;
        String v = value.strip().toLowerCase();
        if (v.equals("none") || v.equals("currentcolor") || v.equals("inherit")) return;
        String hex = normalizeHexSafe(value);
        if (hex != null) colors.add(hex);
    }

    // ── OG image sampling ────────────────────────────────────────────────────

    private void collectOgImageColors(String imageUrl, Set<String> colors) {
        try {
            byte[] bytes = Jsoup.connect(imageUrl)
                    .userAgent(USER_AGENT)
                    .timeout(ASSET_TIMEOUT_MS)
                    .maxBodySize(MAX_OG_IMAGE_BYTES)
                    .ignoreContentType(true)
                    .execute()
                    .bodyAsBytes();

            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) return;

            ImageColorSampler.dominantColors(image).forEach(colors::add);
            log.debug("OG image sampling yielded {} colours from {}", colors.size(), imageUrl);

        } catch (Exception e) {
            log.debug("Could not sample OG image colours from {}: {}", imageUrl, e.getMessage());
        }
    }

    // ── CSS parsing helpers ───────────────────────────────────────────────────

    private static void collectCssBrandVars(String css, Set<String> colors) {
        Matcher m = CSS_VAR_BRAND.matcher(css);
        while (m.find()) {
            String hex = parseColorValue(m.group(1));
            if (hex != null && isChromatic(hex)) colors.add(hex);
        }
    }

    private static void countColorFrequencies(String css, Map<String, Integer> freq) {
        Matcher hm = HEX_COLOR.matcher(css);
        while (hm.find()) {
            String hex = normalizeHex(hm.group());
            if (isChromatic(hex)) freq.merge(hex, 1, Integer::sum);
        }
        Matcher rm = RGB_COLOR.matcher(css);
        while (rm.find()) {
            String hex = rgbToHex(rm.group(1), rm.group(2), rm.group(3));
            if (hex != null && isChromatic(hex)) freq.merge(hex, 1, Integer::sum);
        }
    }

    /**
     * Returns {@code true} if the colour is likely a brand accent rather than
     * structural scaffolding. Mirrors the {@code ColorRankingService} penalty
     * thresholds so we skip neutrals before they enter the frequency table.
     *
     * <ul>
     *   <li>Near-white (lightness ≥ 0.75) — background filler</li>
     *   <li>Near-black (lightness ≤ 0.12) — text / shadow filler</li>
     *   <li>Low chroma (max−min &lt; 0.15) — greys and near-neutrals</li>
     * </ul>
     */
    private static boolean isChromatic(String hex) {
        if (hex == null || hex.length() < 7) return false;
        try {
            int r = Integer.parseInt(hex.substring(1, 3), 16);
            int g = Integer.parseInt(hex.substring(3, 5), 16);
            int b = Integer.parseInt(hex.substring(5, 7), 16);
            int max = Math.max(r, Math.max(g, b));
            int min = Math.min(r, Math.min(g, b));
            double lightness = (max + min) / 510.0;
            double chroma    = (max - min) / 255.0;
            return lightness > 0.12 && lightness < 0.75 && chroma >= 0.15;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    /** Collects all hex and rgb() colours from a CSS/style string into {@code acc}. */
    private void collectAllColors(String css, Set<String> acc) {
        Matcher hm = HEX_COLOR.matcher(css);
        while (hm.find() && acc.size() < MAX_COLORS) acc.add(normalizeHex(hm.group()));

        Matcher rm = RGB_COLOR.matcher(css);
        while (rm.find() && acc.size() < MAX_COLORS) {
            String hex = rgbToHex(rm.group(1), rm.group(2), rm.group(3));
            if (hex != null) acc.add(hex);
        }
    }

    /** Returns a normalised hex string, or {@code null} if the input isn't a valid hex colour. */
    private static String normalizeHexSafe(String raw) {
        if (raw == null || raw.isBlank()) return null;
        Matcher m = HEX_COLOR.matcher(raw.strip());
        return m.find() ? normalizeHex(m.group()) : null;
    }

    /** Parses a hex or rgb() colour value into a normalised hex string, or {@code null}. */
    private static String parseColorValue(String raw) {
        if (raw == null || raw.isBlank()) return null;
        raw = raw.strip();
        Matcher hm = HEX_COLOR.matcher(raw);
        if (hm.find()) return normalizeHex(hm.group());
        Matcher rm = RGB_COLOR.matcher(raw);
        if (rm.find()) return rgbToHex(rm.group(1), rm.group(2), rm.group(3));
        return null;
    }

    private static String rgbToHex(String r, String g, String b) {
        try {
            int ri = Integer.parseInt(r.strip());
            int gi = Integer.parseInt(g.strip());
            int bi = Integer.parseInt(b.strip());
            if (ri > 255 || gi > 255 || bi > 255) return null;
            return String.format("#%02X%02X%02X", ri, gi, bi);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * Normalises a hex colour string:
     * <ul>
     *   <li>{@code #RGB} → {@code #RRGGBB} (3-digit expansion)</li>
     *   <li>All letters uppercased</li>
     * </ul>
     */
    static String normalizeHex(String hex) {
        if (hex.length() == 4) { // #RGB
            char r = hex.charAt(1), g = hex.charAt(2), b = hex.charAt(3);
            return ("#" + r + r + g + g + b + b).toUpperCase();
        }
        return hex.toUpperCase();
    }

    private String extractMeta(Document doc, String key) {
        // OG tags use property="…", Twitter tags use name="…"
        Element el = doc.select("meta[property=" + key + "]").first();
        if (el == null) el = doc.select("meta[name=" + key + "]").first();
        if (el == null) return null;
        String content = el.attr("content").strip();
        return content.isBlank() ? null : content;
    }
}
