package com.hospital.common;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleReturnsCorrectHttpStatusFromErrorCode() {
        ResponseEntity<Map<String, Object>> response =
                handler.handle(new DomainException(ErrorCode.DOCTOR_NOT_FOUND, "Doctor not found"));
        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void handleBodyContainsErrorCodeName() {
        ResponseEntity<Map<String, Object>> response =
                handler.handle(new DomainException(ErrorCode.APPOINTMENT_CONFLICT, "Conflict"));
        assertThat(response.getBody()).containsEntry("error", "APPOINTMENT_CONFLICT");
    }

    @Test
    void handleBodyContainsMessage() {
        ResponseEntity<Map<String, Object>> response =
                handler.handle(new DomainException(ErrorCode.DOCTOR_NOT_FOUND, "Doctor not found"));
        assertThat(response.getBody()).containsEntry("message", "Doctor not found");
    }

    @Test
    void handleWithExtrasIncludesSuggestedAlternatives() {
        DomainException ex = new DomainException(
                ErrorCode.APPOINTMENT_CONFLICT, "Conflict", List.of("slot-2", "slot-3"));
        ResponseEntity<Map<String, Object>> response = handler.handle(ex);
        assertThat(response.getBody()).containsKey("suggested_alternatives");
    }

    @Test
    void handleWithoutExtrasExcludesSuggestedAlternatives() {
        ResponseEntity<Map<String, Object>> response =
                handler.handle(new DomainException(ErrorCode.UNAUTHORIZED, "Not authorised"));
        assertThat(response.getBody()).doesNotContainKey("suggested_alternatives");
    }

    @Test
    void handleReturns401ForUnauthorized() {
        assertThat(handler.handle(new DomainException(ErrorCode.UNAUTHORIZED, "x"))
                .getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void handleReturns403ForForbidden() {
        assertThat(handler.handle(new DomainException(ErrorCode.FORBIDDEN, "x"))
                .getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void handleReturns409ForAppointmentConflict() {
        assertThat(handler.handle(new DomainException(ErrorCode.APPOINTMENT_CONFLICT, "x"))
                .getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void handleReturns422ForOutsideWorkingHours() {
        assertThat(handler.handle(new DomainException(ErrorCode.OUTSIDE_WORKING_HOURS, "x"))
                .getStatusCode().value()).isEqualTo(422);
    }

    @Test
    void handleReturns429ForRateLimited() {
        assertThat(handler.handle(new DomainException(ErrorCode.RATE_LIMITED, "x"))
                .getStatusCode().value()).isEqualTo(429);
    }
}
