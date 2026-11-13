package com.hospital.common;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class CursorUtils {

    private CursorUtils() {}

    public static String encode(String raw) {
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static String decode(String cursor) {
        return new String(Base64.getDecoder().decode(cursor), StandardCharsets.UTF_8);
    }
}
