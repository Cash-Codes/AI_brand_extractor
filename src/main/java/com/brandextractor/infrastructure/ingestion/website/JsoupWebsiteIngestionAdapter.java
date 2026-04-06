package com.brandextractor.infrastructure.ingestion.website;

import com.brandextractor.domain.evidence.WebsiteEvidence;
import com.brandextractor.domain.ports.WebsiteIngestionPort;
import com.brandextractor.support.error.ExtractionException;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class JsoupWebsiteIngestionAdapter implements WebsiteIngestionPort {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/124.0.0.0 Safari/537.36";

    private static final int  CONNECT_TIMEOUT_MS = 15_000;
    private static final int  MAX_BODY_BYTES      = 2 * 1024 * 1024; // 2 MB
    private static final int  MAX_HEADINGS        = 20;
    private static final int  MAX_IMAGES          = 30;
    private static final int  MAX_SOCIAL_LINKS    = 20;
    private static final int  MAX_COLORS          = 30;
    private static final int  MAX_VISIBLE_TEXT    = 3_000;

    // Hex colour: #RGB or #RRGGBB  (word-boundary prevents matching longer sequences)
    private static final Pattern HEX_COLOR =
            Pattern.compile("#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{3})(?![0-9A-Fa-f])");

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

    private List<String> extractCssColors(Document doc) {
        Set<String> colors = new LinkedHashSet<>();

        // Inline style attributes
        for (Element el : doc.select("[style]")) {
            collectHexColors(el.attr("style"), colors);
            if (colors.size() >= MAX_COLORS) return List.copyOf(colors);
        }

        // <style> tag contents
        for (Element style : doc.select("style")) {
            collectHexColors(style.html(), colors);
            if (colors.size() >= MAX_COLORS) return List.copyOf(colors);
        }

        return List.copyOf(colors);
    }

    private void collectHexColors(String css, Set<String> accumulator) {
        Matcher m = HEX_COLOR.matcher(css);
        while (m.find() && accumulator.size() < MAX_COLORS) {
            accumulator.add(normalizeHex(m.group()));
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
