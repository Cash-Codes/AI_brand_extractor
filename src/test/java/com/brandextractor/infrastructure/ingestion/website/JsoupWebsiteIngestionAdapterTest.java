package com.brandextractor.infrastructure.ingestion.website;

import com.brandextractor.domain.evidence.WebsiteEvidence;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsoupWebsiteIngestionAdapterTest {

    private static final String BASE_URI = "https://example.com";
    private static final String ORIGINAL = "https://example.com";
    private static final String RESOLVED = "https://example.com/";

    private JsoupWebsiteIngestionAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JsoupWebsiteIngestionAdapter();
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private WebsiteEvidence parse(String html) {
        Document doc = Jsoup.parse(html, BASE_URI);
        return adapter.parseDocument(ORIGINAL, RESOLVED, doc);
    }

    // -------------------------------------------------------------------------
    // Title
    // -------------------------------------------------------------------------

    @Test
    void extractsTitle() {
        var ev = parse("<html><head><title>Acme Studio</title></head><body></body></html>");
        assertThat(ev.title()).isEqualTo("Acme Studio");
    }

    @Test
    void titleIsNullWhenAbsent() {
        var ev = parse("<html><body></body></html>");
        assertThat(ev.title()).isNull();
    }

    @Test
    void titleIsNullWhenBlank() {
        var ev = parse("<html><head><title>   </title></head><body></body></html>");
        assertThat(ev.title()).isNull();
    }

    // -------------------------------------------------------------------------
    // Meta description
    // -------------------------------------------------------------------------

    @Test
    void extractsMetaDescription() {
        var ev = parse("<html><head>" +
                "<meta name='description' content='A design studio'>" +
                "</head><body></body></html>");
        assertThat(ev.metaDescription()).isEqualTo("A design studio");
    }

    @Test
    void metaDescriptionIsNullWhenAbsent() {
        var ev = parse("<html><body></body></html>");
        assertThat(ev.metaDescription()).isNull();
    }

    // -------------------------------------------------------------------------
    // Headings
    // -------------------------------------------------------------------------

    @Test
    void extractsH1H2H3Headings() {
        var ev = parse("<html><body>" +
                "<h1>Alpha</h1><h2>Beta</h2><h3>Gamma</h3><h4>Ignored</h4>" +
                "</body></html>");
        assertThat(ev.headings()).containsExactly("Alpha", "Beta", "Gamma");
    }

    @Test
    void headingsLimitedTo20() {
        StringBuilder sb = new StringBuilder("<html><body>");
        for (int i = 1; i <= 25; i++) sb.append("<h1>Heading ").append(i).append("</h1>");
        sb.append("</body></html>");

        var ev = parse(sb.toString());
        assertThat(ev.headings()).hasSize(20);
    }

    @Test
    void blankHeadingsAreSkipped() {
        var ev = parse("<html><body><h1>   </h1><h2>Real</h2></body></html>");
        assertThat(ev.headings()).containsExactly("Real");
    }

    // -------------------------------------------------------------------------
    // Visible text
    // -------------------------------------------------------------------------

    @Test
    void visibleTextExcludesScriptAndStyle() {
        var ev = parse("<html><body>" +
                "<script>alert('x')</script>" +
                "<style>.foo{color:red}</style>" +
                "<p>Hello world</p>" +
                "</body></html>");
        assertThat(ev.visibleText()).contains("Hello world");
        assertThat(ev.visibleText()).doesNotContain("alert");
        assertThat(ev.visibleText()).doesNotContain(".foo");
    }

    @Test
    void visibleTextExcludesNavAndFooter() {
        var ev = parse("<html><body>" +
                "<nav>Nav content</nav>" +
                "<footer>Footer content</footer>" +
                "<main>Main content</main>" +
                "</body></html>");
        assertThat(ev.visibleText()).contains("Main content");
        assertThat(ev.visibleText()).doesNotContain("Nav content");
        assertThat(ev.visibleText()).doesNotContain("Footer content");
    }

    @Test
    void visibleTextTruncatedAt3000Chars() {
        String longText = "x".repeat(4000);
        var ev = parse("<html><body><p>" + longText + "</p></body></html>");
        assertThat(ev.visibleText()).hasSizeLessThanOrEqualTo(3001); // 3000 + ellipsis char
        assertThat(ev.visibleText()).endsWith("…");
    }

    @Test
    void visibleTextNotTruncatedWhenShort() {
        var ev = parse("<html><body><p>Short text</p></body></html>");
        assertThat(ev.visibleText()).doesNotEndWith("…");
    }

    // -------------------------------------------------------------------------
    // Image URLs
    // -------------------------------------------------------------------------

    @Test
    void extractsImageSrc() {
        var ev = parse("<html><body>" +
                "<img src='/logo.png'>" +
                "<img src='/banner.jpg'>" +
                "</body></html>");
        assertThat(ev.imageUrls())
                .contains("https://example.com/logo.png", "https://example.com/banner.jpg");
    }

    @Test
    void extractsLazyLoadedImages() {
        var ev = parse("<html><body>" +
                "<img data-src='/lazy.png'>" +
                "<img data-lazy-src='/lazy2.png'>" +
                "</body></html>");
        assertThat(ev.imageUrls())
                .contains("https://example.com/lazy.png", "https://example.com/lazy2.png");
    }

    @Test
    void imageUrlsDeduplicatedAndLimitedTo30() {
        StringBuilder sb = new StringBuilder("<html><body>");
        for (int i = 1; i <= 40; i++) sb.append("<img src='/img").append(i).append(".png'>");
        sb.append("</body></html>");

        var ev = parse(sb.toString());
        assertThat(ev.imageUrls()).hasSize(30);
        assertThat(ev.imageUrls()).doesNotHaveDuplicates();
    }

    // -------------------------------------------------------------------------
    // Social / contact links
    // -------------------------------------------------------------------------

    @Test
    void extractsSocialDomainLinks() {
        var ev = parse("<html><body>" +
                "<a href='https://instagram.com/acme'>IG</a>" +
                "<a href='https://twitter.com/acme'>TW</a>" +
                "<a href='https://linkedin.com/company/acme'>LI</a>" +
                "</body></html>");
        assertThat(ev.socialLinks())
                .contains("https://instagram.com/acme",
                          "https://twitter.com/acme",
                          "https://linkedin.com/company/acme");
    }

    @Test
    void extractsMailtoAndTelLinks() {
        var ev = parse("<html><body>" +
                "<a href='mailto:hello@acme.com'>Email</a>" +
                "<a href='tel:+15551234567'>Call</a>" +
                "</body></html>");
        assertThat(ev.socialLinks())
                .contains("mailto:hello@acme.com", "tel:+15551234567");
    }

    @Test
    void regularLinksNotIncludedInSocialLinks() {
        var ev = parse("<html><body>" +
                "<a href='https://example.com/about'>About</a>" +
                "<a href='https://notasocial.com/page'>Other</a>" +
                "</body></html>");
        assertThat(ev.socialLinks()).isEmpty();
    }

    @Test
    void socialLinksLimitedTo20() {
        StringBuilder sb = new StringBuilder("<html><body>");
        for (int i = 1; i <= 25; i++)
            sb.append("<a href='https://instagram.com/user").append(i).append("'>IG</a>");
        sb.append("</body></html>");

        var ev = parse(sb.toString());
        assertThat(ev.socialLinks()).hasSize(20);
    }

    // -------------------------------------------------------------------------
    // CSS color candidates
    // -------------------------------------------------------------------------

    @Test
    void extractsHexColorsFromInlineStyles() {
        var ev = parse("<html><body>" +
                "<div style='color: #FF0000; background: #00FF00'></div>" +
                "</body></html>");
        assertThat(ev.cssColorCandidates()).contains("#FF0000", "#00FF00");
    }

    @Test
    void extractsHexColorsFromStyleTag() {
        // Use chromatic colours — non-chromatic (low chroma / near-neutral) are filtered out
        var ev = parse("<html><head>" +
                "<style>body { color: #1E3A8A; } .btn { background: #E63946; }</style>" +
                "</head><body></body></html>");
        assertThat(ev.cssColorCandidates()).contains("#1E3A8A", "#E63946");
    }

    @Test
    void colorsDeduplicatedAndLimitedTo30() {
        StringBuilder styles = new StringBuilder("<style>");
        for (int i = 0; i < 40; i++)
            styles.append(".c").append(i).append("{color:#").append(String.format("%06X", i * 1000)).append("}");
        styles.append("</style>");

        var ev = parse("<html><head>" + styles + "</head><body></body></html>");
        assertThat(ev.cssColorCandidates()).hasSize(30);
        assertThat(ev.cssColorCandidates()).doesNotHaveDuplicates();
    }

    @Test
    void colorsAreUppercased() {
        var ev = parse("<html><body>" +
                "<div style='color: #aabbcc'></div>" +
                "</body></html>");
        assertThat(ev.cssColorCandidates()).contains("#AABBCC");
    }

    // -------------------------------------------------------------------------
    // Hex normalisation
    // -------------------------------------------------------------------------

    @Test
    void normalizeHex_expandsThreeDigitToSixDigit() {
        assertThat(JsoupWebsiteIngestionAdapter.normalizeHex("#F0A")).isEqualTo("#FF00AA");
        assertThat(JsoupWebsiteIngestionAdapter.normalizeHex("#abc")).isEqualTo("#AABBCC");
        assertThat(JsoupWebsiteIngestionAdapter.normalizeHex("#000")).isEqualTo("#000000");
        assertThat(JsoupWebsiteIngestionAdapter.normalizeHex("#fff")).isEqualTo("#FFFFFF");
    }

    @Test
    void normalizeHex_uppercasesSixDigit() {
        assertThat(JsoupWebsiteIngestionAdapter.normalizeHex("#aabbcc")).isEqualTo("#AABBCC");
        assertThat(JsoupWebsiteIngestionAdapter.normalizeHex("#1a2b3c")).isEqualTo("#1A2B3C");
        assertThat(JsoupWebsiteIngestionAdapter.normalizeHex("#FFFFFF")).isEqualTo("#FFFFFF");
    }

    @Test
    void threeDigitHexInStyleIsExpandedToSixDigit() {
        var ev = parse("<html><body><div style='color: #F3C'></div></body></html>");
        assertThat(ev.cssColorCandidates()).contains("#FF33CC");
    }

    // -------------------------------------------------------------------------
    // Favicon
    // -------------------------------------------------------------------------

    @Test
    void extractsFaviconFromLinkRelIcon() {
        var ev = parse("<html><head>" +
                "<link rel='icon' href='/favicon.png'>" +
                "</head><body></body></html>");
        assertThat(ev.faviconUrl()).isEqualTo("https://example.com/favicon.png");
    }

    @Test
    void extractsFaviconFromAppleTouchIcon() {
        var ev = parse("<html><head>" +
                "<link rel='apple-touch-icon' href='/apple-icon.png'>" +
                "</head><body></body></html>");
        assertThat(ev.faviconUrl()).isEqualTo("https://example.com/apple-icon.png");
    }

    @Test
    void faviconFallsBackToConventionalPath() {
        var ev = parse("<html><body></body></html>");
        assertThat(ev.faviconUrl()).isEqualTo("https://example.com/favicon.ico");
    }

    // -------------------------------------------------------------------------
    // Open Graph
    // -------------------------------------------------------------------------

    @Test
    void extractsOpenGraphMetadata() {
        var ev = parse("<html><head>" +
                "<meta property='og:title' content='Acme OG'>" +
                "<meta property='og:description' content='OG desc'>" +
                "<meta property='og:image' content='https://example.com/og.png'>" +
                "<meta property='og:site_name' content='Acme'>" +
                "</head><body></body></html>");
        assertThat(ev.ogTitle()).isEqualTo("Acme OG");
        assertThat(ev.ogDescription()).isEqualTo("OG desc");
        assertThat(ev.ogImage()).isEqualTo("https://example.com/og.png");
        assertThat(ev.ogSiteName()).isEqualTo("Acme");
    }

    @Test
    void ogFieldsNullWhenAbsent() {
        var ev = parse("<html><body></body></html>");
        assertThat(ev.ogTitle()).isNull();
        assertThat(ev.ogDescription()).isNull();
        assertThat(ev.ogImage()).isNull();
        assertThat(ev.ogSiteName()).isNull();
    }

    // -------------------------------------------------------------------------
    // Twitter Card
    // -------------------------------------------------------------------------

    @Test
    void extractsTwitterCardMetadata() {
        var ev = parse("<html><head>" +
                "<meta name='twitter:card' content='summary_large_image'>" +
                "<meta name='twitter:image' content='https://example.com/tw.png'>" +
                "</head><body></body></html>");
        assertThat(ev.twitterCard()).isEqualTo("summary_large_image");
        assertThat(ev.twitterImage()).isEqualTo("https://example.com/tw.png");
    }

    @Test
    void twitterFieldsNullWhenAbsent() {
        var ev = parse("<html><body></body></html>");
        assertThat(ev.twitterCard()).isNull();
        assertThat(ev.twitterImage()).isNull();
    }

    // -------------------------------------------------------------------------
    // Resolved URL and provenance
    // -------------------------------------------------------------------------

    @Test
    void preservesOriginalAndResolvedUrls() {
        var ev = parse("<html><body></body></html>");
        assertThat(ev.sourceReference()).isEqualTo(ORIGINAL);
        assertThat(ev.resolvedUrl()).isEqualTo(RESOLVED);
    }

    @Test
    void confidenceIsOne() {
        var ev = parse("<html><body></body></html>");
        assertThat(ev.confidence()).isEqualTo(1.0);
    }

    @Test
    void extractedAtIsSet() {
        var ev = parse("<html><body></body></html>");
        assertThat(ev.extractedAt()).isNotNull();
    }
}
