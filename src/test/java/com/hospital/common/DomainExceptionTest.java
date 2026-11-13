package com.hospital.common;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class DomainExceptionTest {

    @Test
    void constructorWithCodeAndMessageSetsFields() {
        DomainException ex = new DomainException(ErrorCode.DOCTOR_NOT_FOUND, "Doctor not found");
        assertThat(ex.getCode()).isEqualTo(ErrorCode.DOCTOR_NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo("Doctor not found");
        assertThat(ex.getExtras()).isNull();
    }

    @Test
    void constructorWithExtrasSetsExtras() {
        List<String> extras = List.of("alt-slot-1", "alt-slot-2");
        DomainException ex = new DomainException(ErrorCode.APPOINTMENT_CONFLICT, "Conflict", extras);
        assertThat(ex.getExtras()).isEqualTo(extras);
    }

    @Test
    void extrasNullWhenNotProvided() {
        DomainException ex = new DomainException(ErrorCode.UNAUTHORIZED, "Denied");
        assertThat(ex.getExtras()).isNull();
    }

    @Test
    void isARuntimeException() {
        assertThat(new DomainException(ErrorCode.RATE_LIMITED, "x"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void errorCodeIsPreserved() {
        for (ErrorCode code : ErrorCode.values()) {
            assertThat(new DomainException(code, "msg").getCode()).isEqualTo(code);
        }
    }
}
