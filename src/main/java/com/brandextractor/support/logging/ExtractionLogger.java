package com.brandextractor.support.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class ExtractionLogger {

    private static final Logger log = LoggerFactory.getLogger(ExtractionLogger.class);

    public void logStart(String requestId, String inputType, String source) {
        MDC.put("requestId", requestId);
        MDC.put("inputType", inputType);
        log.info("Extraction started source={}", source);
    }

    public void logComplete(String requestId, double overallConfidence) {
        log.info("Extraction complete requestId={} confidence={}", requestId, overallConfidence);
        MDC.clear();
    }

    public void logError(String requestId, Exception ex) {
        log.error("Extraction failed requestId={}", requestId, ex);
        MDC.clear();
    }
}
