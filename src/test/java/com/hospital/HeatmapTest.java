package com.hospital;

import com.hospital.auth.AuthRequest;
import com.hospital.auth.AuthResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the 7×24 availability heatmap for a doctor.
 *
 * V4 seeds ALL 20 doctors with available time slots for Dec 2026 Mon–Fri,
 * 09:00–16:30 in 30-min increments. Doctor 001 (ACTIVE) is used here.
 *
 * 2026-W50 = Dec 7 (Mon) through Dec 13 (Sun):
 *   Mon-Fri (Dec 7-11) → seeded slots present → grid hours 9-16 = 1
 *   Sat-Sun (Dec 12-13) → no slots seeded   → all hours = 0
 *   Hours 0-8 and 17-23 → no slots          → 0
 */
@Tag("integration")
@SuppressWarnings({"unchecked", "rawtypes"})
class HeatmapTest extends BaseIntegrationTest {

    private static final String DOCTOR_ID = "d1000000-0000-0000-0000-000000000001";
    private static final String WEEK = "2026-W50";

    @Autowired
    private TestRestTemplate rest;

    private HttpEntity<Void> authEntity() {
        String token = rest.postForEntity(
                "/api/v1/auth/token",
                new AuthRequest("demo", "demo-password"),
                AuthResponse.class
        ).getBody().token();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    private Map fetchGrid() {
        String url = "/api/v1/doctors/" + DOCTOR_ID + "/heatmap?week=" + WEEK;
        ResponseEntity<Map> resp =
                rest.exchange(url, HttpMethod.GET, authEntity(), Map.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        return (Map) resp.getBody().get("grid");
    }

    @Test
    void gridContainsAllSevenDayKeys() {
        Map grid = fetchGrid();
        assertThat(grid.keySet())
                .containsExactlyInAnyOrder("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN");
    }

    @Test
    void eachDayHas24HourSlots() {
        Map grid = fetchGrid();
        grid.values().forEach(hours ->
                assertThat((List<?>) hours).hasSize(24));
    }

    @Test
    void weekdayHours9To16ShowAvailableInDecember() {
        Map grid = fetchGrid();
        Set<String> weekdays = Set.of("MON", "TUE", "WED", "THU", "FRI");
        for (String day : weekdays) {
            List<Integer> hours = (List<Integer>) grid.get(day);
            for (int h = 9; h <= 16; h++) {
                assertThat(hours.get(h))
                        .as("%s hour %d should be 1 (slot seeded in V4)", day, h)
                        .isEqualTo(1);
            }
        }
    }

    @Test
    void weekdayHoursOutsideWorkingHoursAreZero() {
        Map grid = fetchGrid();
        Set<String> weekdays = Set.of("MON", "TUE", "WED", "THU", "FRI");
        for (String day : weekdays) {
            List<Integer> hours = (List<Integer>) grid.get(day);
            for (int h = 0; h < 9; h++) {
                assertThat(hours.get(h)).as("%s hour %d should be 0", day, h).isEqualTo(0);
            }
            for (int h = 17; h < 24; h++) {
                assertThat(hours.get(h)).as("%s hour %d should be 0", day, h).isEqualTo(0);
            }
        }
    }

    @Test
    void weekendHoursAreAllZero() {
        Map grid = fetchGrid();
        for (String day : new String[]{"SAT", "SUN"}) {
            List<Integer> hours = (List<Integer>) grid.get(day);
            hours.forEach(h ->
                    assertThat(h).as("%s should have no slots", day).isEqualTo(0));
        }
    }

    @Test
    void responseContainsCorrectDoctorIdAndWeek() {
        String url = "/api/v1/doctors/" + DOCTOR_ID + "/heatmap?week=" + WEEK;
        ResponseEntity<Map> resp =
                rest.exchange(url, HttpMethod.GET, authEntity(), Map.class);
        Map body = resp.getBody();
        assertThat(body.get("doctorId")).isEqualTo(DOCTOR_ID);
        assertThat(body.get("week")).isEqualTo(WEEK);
    }

    @Test
    void invalidWeekFormatReturns400() {
        String url = "/api/v1/doctors/" + DOCTOR_ID + "/heatmap?week=2026W50";
        ResponseEntity<Map> resp =
                rest.exchange(url, HttpMethod.GET, authEntity(), Map.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(400);
    }
}
