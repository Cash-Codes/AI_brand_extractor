package com.brandextractor.domain.ports;

import com.brandextractor.domain.evidence.WebsiteEvidence;

public interface WebsiteIngestionPort {
    WebsiteEvidence ingest(String url);
}
