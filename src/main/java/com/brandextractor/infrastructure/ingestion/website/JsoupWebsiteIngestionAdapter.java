package com.brandextractor.infrastructure.ingestion.website;

import com.brandextractor.domain.evidence.WebsiteEvidence;
import com.brandextractor.domain.ports.WebsiteIngestionPort;
import org.springframework.stereotype.Component;

@Component
public class JsoupWebsiteIngestionAdapter implements WebsiteIngestionPort {

    @Override
    public WebsiteEvidence ingest(String url) {
        throw new UnsupportedOperationException("Website ingestion not yet implemented");
    }
}
