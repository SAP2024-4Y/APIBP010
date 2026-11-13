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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class CalendarWindowTest extends BaseIntegrationTest {

    // Use doctor 3 (Dr. Neha Kapoor) for this test
    private static final UUID DOCTOR_ID = UUID.fromString("d1000000-0000-0000-0000-000000000003");
    private static final UUID PATIENT_ID = UUID.fromString("b1000000-0000-0000-0000-000000000003");

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
    void calendarWindowReturnsOnlyAppointmentsInRange() {
        String token = getToken();

        // Book a Dec 1 appointment and a Dec 5 appointment
        OffsetDateTime dec1Start = OffsetDateTime.of(2026, 12, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime dec5Start = OffsetDateTime.of(2026, 12, 5, 10, 0, 0, 0, ZoneOffset.UTC);

        BookAppointmentRequest dec1Req = new BookAppointmentRequest(
                DOCTOR_ID, PATIENT_ID, dec1Start, dec1Start.plusMinutes(30), "CONSULTATION", null);
        BookAppointmentRequest dec5Req = new BookAppointmentRequest(
                DOCTOR_ID, PATIENT_ID, dec5Start, dec5Start.plusMinutes(30), "CONSULTATION", null);

        rest.postForEntity("/api/v1/appointments",
                new HttpEntity<>(dec1Req, authHeaders(token)), AppointmentDto.class);
        rest.postForEntity("/api/v1/appointments",
                new HttpEntity<>(dec5Req, authHeaders(token)), AppointmentDto.class);

        // Query only Dec 1
        String url = "/api/v1/doctors/" + DOCTOR_ID
                + "/appointments?starts_at=2026-12-01T00:00:00Z&ends_at=2026-12-02T00:00:00Z";
        var resp = rest.getForEntity(url, AppointmentDto[].class);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);

        List<AppointmentDto> results = Arrays.asList(resp.getBody());
        // Should only contain the Dec 1 appointment, not Dec 5
        assertThat(results).isNotEmpty();
        for (AppointmentDto appt : results) {
            assertThat(appt.startsAt()).isBefore(OffsetDateTime.of(2026, 12, 2, 0, 0, 0, 0, ZoneOffset.UTC));
        }
    }

    @Test
    void calendarWindowExcludesOutOfRangeAppointments() {
        String token = getToken();

        // Book Dec 10 appointment (outside Dec 1-2 window)
        OffsetDateTime dec10Start = OffsetDateTime.of(2026, 12, 10, 14, 0, 0, 0, ZoneOffset.UTC);
        BookAppointmentRequest dec10Req = new BookAppointmentRequest(
                DOCTOR_ID, PATIENT_ID, dec10Start, dec10Start.plusMinutes(30), "CONSULTATION", null);

        rest.postForEntity("/api/v1/appointments",
                new HttpEntity<>(dec10Req, authHeaders(token)), AppointmentDto.class);

        // Query Dec 1–2 window: should NOT include Dec 10
        String url = "/api/v1/doctors/" + DOCTOR_ID
                + "/appointments?starts_at=2026-12-01T00:00:00Z&ends_at=2026-12-02T00:00:00Z";
        var resp = rest.getForEntity(url, AppointmentDto[].class);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);

        List<AppointmentDto> results = Arrays.asList(resp.getBody());
        for (AppointmentDto appt : results) {
            assertThat(appt.startsAt())
                    .isAfterOrEqualTo(OffsetDateTime.of(2026, 12, 1, 0, 0, 0, 0, ZoneOffset.UTC))
                    .isBefore(OffsetDateTime.of(2026, 12, 2, 0, 0, 0, 0, ZoneOffset.UTC));
        }
    }

    @Test
    void invalidDateRangeReturns400() {
        String url = "/api/v1/doctors/" + DOCTOR_ID
                + "/appointments?starts_at=2026-12-02T00:00:00Z&ends_at=2026-12-01T00:00:00Z";
        var resp = rest.getForEntity(url, Object.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(400);
    }
}
