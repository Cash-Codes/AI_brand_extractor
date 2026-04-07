package com.brandextractor.support.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MimeTypeUtilsTest {

    private final MimeTypeUtils utils = new MimeTypeUtils();

    @Test
    void detectMimeType_pngBytes_returnsImagePng() {
        byte[] pngMagic = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        assertThat(utils.detectMimeType(pngMagic)).isEqualTo("image/png");
    }

    @Test
    void detectMimeType_jpegBytes_returnsImageJpeg() {
        byte[] jpegMagic = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        assertThat(utils.detectMimeType(jpegMagic)).isEqualTo("image/jpeg");
    }

    @Test
    void detectMimeType_unknownBytes_returnsOctetStream() {
        byte[] unknown = {0x00, 0x01, 0x02, 0x03};
        assertThat(utils.detectMimeType(unknown)).isEqualTo("application/octet-stream");
    }

    @Test
    void detectMimeType_nullBytes_returnsOctetStream() {
        assertThat(utils.detectMimeType(null)).isEqualTo("application/octet-stream");
    }
}
