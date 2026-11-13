package com.hospital;

import com.hospital.appointment.dto.AppointmentDto;
import com.hospital.appointment.dto.BookAppointmentRequest;
import com.hospital.auth.AuthRequest;
import com.hospital.auth.AuthResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class AppointmentConflictTest extends BaseIntegrationTest {

    private static final UUID DOCTOR_ID = UUID.fromString("d1000000-0000-0000-0000-000000000001");
    private static final UUID PATIENT_ID = UUID.fromString("b1000000-0000-0000-0000-000000000001");

    @Autowired
    private TestRestTemplate rest;

    private String getToken() {
        var resp = rest.postForEntity(
                "/api/v1/auth/token",
                new AuthRequest("demo", "demo-password"),
                AuthResponse.class);
        return resp.getBody().token();
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    void bookAppointmentSuccessfullyReturns201() {
        String token = getToken();

        // Use a date in the future with working hours
        OffsetDateTime start = OffsetDateTime.of(2026, 12, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime end = start.plusMinutes(30);

        BookAppointmentRequest req = new BookAppointmentRequest(
                DOCTOR_ID, PATIENT_ID, start, end, "CONSULTATION", null);

        var response = rest.postForEntity(
                "/api/v1/appointments",
                new HttpEntity<>(req, authHeaders(token)),
                AppointmentDto.class);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isNotNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void bookSameSlotTwiceReturns409WithSuggestions() {
        String token = getToken();

        // Use December 2 to avoid conflicts with bookAppointmentSuccessfullyReturns201
        OffsetDateTime start = OffsetDateTime.of(2026, 12, 2, 11, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime end = start.plusMinutes(30);

        BookAppointmentRequest req = new BookAppointmentRequest(
                DOCTOR_ID, PATIENT_ID, start, end, "CONSULTATION", null);

        // First booking should succeed
        var firstResp = rest.postForEntity(
                "/api/v1/appointments",
                new HttpEntity<>(req, authHeaders(token)),
                AppointmentDto.class);
        assertThat(firstResp.getStatusCode().value()).isEqualTo(201);

        // Second booking on same slot should 409
        var secondResp = rest.postForEntity(
                "/api/v1/appointments",
                new HttpEntity<>(req, authHeaders(token)),
                Map.class);
        assertThat(secondResp.getStatusCode().value()).isEqualTo(409);

        Map<String, Object> body = secondResp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("error")).isEqualTo("APPOINTMENT_CONFLICT");
        assertThat(body.get("suggested_alternatives")).isNotNull();

        var suggestions = (java.util.List<?>) body.get("suggested_alternatives");
        assertThat(suggestions).hasSize(3);
    }

    @Test
    @SuppressWarnings("unchecked")
    void suggestionsAreFutureSlots() {
        String token = getToken();

        OffsetDateTime start = OffsetDateTime.of(2026, 12, 3, 9, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime end = start.plusMinutes(30);

        BookAppointmentRequest req = new BookAppointmentRequest(
                DOCTOR_ID, PATIENT_ID, start, end, "CONSULTATION", null);

        // First booking
        rest.postForEntity(
                "/api/v1/appointments",
                new HttpEntity<>(req, authHeaders(token)),
                AppointmentDto.class);

        // Conflict booking
        var conflictResp = rest.postForEntity(
                "/api/v1/appointments",
                new HttpEntity<>(req, authHeaders(token)),
                Map.class);
        assertThat(conflictResp.getStatusCode().value()).isEqualTo(409);

        Map<String, Object> body = conflictResp.getBody();
        var suggestions = (java.util.List<Map<String, Object>>) body.get("suggested_alternatives");
        assertThat(suggestions).isNotEmpty();
        // Each suggestion start should be a non-null string (serialized OffsetDateTime)
        for (Map<String, Object> suggestion : suggestions) {
            assertThat(suggestion.get("startsAt")).isNotNull();
            assertThat(suggestion.get("endsAt")).isNotNull();
        }
    }

    @Test
    void invalidDateRangeReturns400() {
        String token = getToken();

        OffsetDateTime start = OffsetDateTime.of(2026, 12, 4, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime end = start.minusMinutes(30); // end before start

        BookAppointmentRequest req = new BookAppointmentRequest(
                DOCTOR_ID, PATIENT_ID, start, end, "CONSULTATION", null);

        var response = rest.postForEntity(
                "/api/v1/appointments",
                new HttpEntity<>(req, authHeaders(token)),
                Map.class);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void outsideWorkingHoursReturns422() {
        String token = getToken();

        // 08:00 is before working hours start
        OffsetDateTime start = OffsetDateTime.of(2026, 12, 7, 8, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime end = start.plusMinutes(30);

        BookAppointmentRequest req = new BookAppointmentRequest(
                DOCTOR_ID, PATIENT_ID, start, end, "CONSULTATION", null);

        var response = rest.postForEntity(
                "/api/v1/appointments",
                new HttpEntity<>(req, authHeaders(token)),
                Map.class);

        assertThat(response.getStatusCode().value()).isEqualTo(422);
    }
}
