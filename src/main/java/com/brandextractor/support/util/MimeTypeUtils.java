package com.brandextractor.support.util;

import org.springframework.stereotype.Component;

@Component
public class MimeTypeUtils {

    private static final byte[] PNG_MAGIC  = {(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    public String detectMimeType(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return "application/octet-stream";
        }
        if (startsWith(bytes, PNG_MAGIC))  return "image/png";
        if (startsWith(bytes, JPEG_MAGIC)) return "image/jpeg";
        return "application/octet-stream";
    }

    private boolean startsWith(byte[] bytes, byte[] magic) {
        if (bytes.length < magic.length) return false;
        for (int i = 0; i < magic.length; i++) {
            if (bytes[i] != magic[i]) return false;
        }
        return true;
    }
}
