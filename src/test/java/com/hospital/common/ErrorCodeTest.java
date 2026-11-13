package com.hospital.common;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class ErrorCodeTest {

    @ParameterizedTest
    @CsvSource({
        "APPOINTMENT_CONFLICT,   409",
        "SLOT_UNAVAILABLE,       409",
        "PATIENT_NOT_FOUND,      404",
        "DOCTOR_NOT_FOUND,       404",
        "HOSPITAL_NOT_FOUND,     404",
        "INVALID_DATE_RANGE,     400",
        "UNAUTHORIZED,           401",
        "FORBIDDEN,              403",
        "RATE_LIMITED,           429",
        "OUTSIDE_WORKING_HOURS,  422",
        "MINIMUM_BUFFER_VIOLATION, 422"
    })
    void eachCodeCarriesCorrectHttpStatus(String codeName, int expectedStatus) {
        ErrorCode code = ErrorCode.valueOf(codeName.trim());
        assertThat(code.httpStatus).isEqualTo(expectedStatus);
    }
}
