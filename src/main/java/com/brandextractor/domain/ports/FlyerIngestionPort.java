package com.brandextractor.domain.ports;

import com.brandextractor.domain.evidence.FlyerEvidence;

public interface FlyerIngestionPort {
    FlyerEvidence ingest(byte[] imageBytes, String mimeType, String sourceLabel);
}
