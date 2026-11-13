package com.hospital.common;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class CursorUtilsTest {

    @Test
    void encodeProducesKnownBase64() {
        assertThat(CursorUtils.encode("hello")).isEqualTo("aGVsbG8=");
    }

    @Test
    void decodeRevertsEncode() {
        String raw = "some-uuid|fieldValue";
        assertThat(CursorUtils.decode(CursorUtils.encode(raw))).isEqualTo(raw);
    }

    @Test
    void roundTripWithPipeAndSpecialCharacters() {
        String raw = "550e8400-e29b-41d4-a716-446655440000|Dr. Smith";
        assertThat(CursorUtils.decode(CursorUtils.encode(raw))).isEqualTo(raw);
    }

    @Test
    void encodeEmptyStringRoundTrips() {
        assertThat(CursorUtils.decode(CursorUtils.encode(""))).isEqualTo("");
    }

    @Test
    void encodedValueDiffersFromRaw() {
        String raw = "test";
        assertThat(CursorUtils.encode(raw)).isNotEqualTo(raw);
    }
}
