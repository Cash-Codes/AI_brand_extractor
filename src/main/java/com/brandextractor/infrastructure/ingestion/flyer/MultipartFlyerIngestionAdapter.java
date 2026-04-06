package com.brandextractor.infrastructure.ingestion.flyer;

import com.brandextractor.domain.evidence.FlyerEvidence;
import com.brandextractor.domain.ports.FlyerIngestionPort;
import org.springframework.stereotype.Component;

@Component
public class MultipartFlyerIngestionAdapter implements FlyerIngestionPort {

    @Override
    public FlyerEvidence ingest(byte[] imageBytes, String mimeType, String sourceLabel) {
        throw new UnsupportedOperationException("Flyer ingestion not yet implemented");
    }
}
