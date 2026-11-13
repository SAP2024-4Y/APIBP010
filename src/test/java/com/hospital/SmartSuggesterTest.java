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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class SmartSuggesterTest extends BaseIntegrationTest {

    // Use doctor 2 (Dr. Arjun Mehta) for this test to avoid interference with other tests
    private static final UUID DOCTOR_ID = UUID.fromString("d1000000-0000-0000-0000-000000000002");
    private static final UUID PATIENT_ID = UUID.fromString("b1000000-0000-0000-0000-000000000002");

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
    @SuppressWarnings("unchecked")
    void afterConflictSuggestionsContainFutureSlots() {
        String token = getToken();

        // Book one slot on Dec 8 (Monday) at 09:00
        OffsetDateTime start = OffsetDateTime.of(2026, 12, 8, 9, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime end = start.plusMinutes(30);

        BookAppointmentRequest firstReq = new BookAppointmentRequest(
                DOCTOR_ID, PATIENT_ID, start, end, "CONSULTATION", null);

        var firstResp = rest.postForEntity(
                "/api/v1/appointments",
                new HttpEntity<>(firstReq, authHeaders(token)),
                AppointmentDto.class);
        assertThat(firstResp.getStatusCode().value()).isEqualTo(201);

        // Try to book the same slot again – should conflict and return 3 suggestions
        var conflictResp = rest.postForEntity(
                "/api/v1/appointments",
                new HttpEntity<>(firstReq, authHeaders(token)),
                Map.class);
        assertThat(conflictResp.getStatusCode().value()).isEqualTo(409);

        Map<String, Object> body = conflictResp.getBody();
        assertThat(body.get("error")).isEqualTo("APPOINTMENT_CONFLICT");

        List<Map<String, Object>> suggestions = (List<Map<String, Object>>) body.get("suggested_alternatives");
        assertThat(suggestions).hasSize(3);

        // Each suggestion's startsAt must be after the requested endsAt
        for (Map<String, Object> suggestion : suggestions) {
            String suggestedStart = (String) suggestion.get("startsAt");
            assertThat(suggestedStart).isNotNull();
            OffsetDateTime suggestedDt = OffsetDateTime.parse(suggestedStart);
            assertThat(suggestedDt).isAfter(end);
        }
    }
}
