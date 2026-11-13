package com.hospital;

import com.hospital.auth.AuthRequest;
import com.hospital.auth.AuthResponse;
import com.hospital.hospital.dto.HospitalDto;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests PostGIS-backed geospatial queries against hospitals seeded in V2.
 *
 * V2 spatial layout (verified from centre 77.2090, 28.6139 — AIIMS Delhi):
 *   5 km radius  → 3 ACTIVE hospitals (001 AIIMS, 002 Safdarjung, 003 RML)
 *   50 km radius → 6 ACTIVE hospitals (adds 004 Max Saket, 005 Apollo Noida, 006 Medanta)
 *                  hospital 007 (Fortis Faridabad, 26 km) is INACTIVE — excluded
 *
 * Bounding box [minLon=77.18, minLat=28.59, maxLon=77.25, maxLat=28.65] captures
 * exactly the 3-hospital Delhi cluster and excludes hospital 004 (lat 28.525).
 */
@Tag("integration")
class GeospatialTest extends BaseIntegrationTest {

    // AIIMS Delhi — centre of the Delhi hospital cluster in V2
    private static final double DELHI_LAT = 28.6139;
    private static final double DELHI_LON = 77.2090;

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

    @Test
    void radiusOf5kmReturnsExactlyThreeActiveHospitals() {
        String url = String.format(
                "/api/v1/hospitals/nearby?latitude=%s&longitude=%s&radius_km=5",
                DELHI_LAT, DELHI_LON);

        ResponseEntity<HospitalDto[]> resp =
                rest.exchange(url, HttpMethod.GET, authEntity(), HospitalDto[].class);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        List<HospitalDto> hospitals = Arrays.asList(resp.getBody());
        assertThat(hospitals).hasSize(3);
    }

    @Test
    void radiusOf5kmReturnsOnlyActiveHospitals() {
        String url = String.format(
                "/api/v1/hospitals/nearby?latitude=%s&longitude=%s&radius_km=5",
                DELHI_LAT, DELHI_LON);

        ResponseEntity<HospitalDto[]> resp =
                rest.exchange(url, HttpMethod.GET, authEntity(), HospitalDto[].class);

        Arrays.asList(resp.getBody())
                .forEach(h -> assertThat(h.status()).isEqualTo("ACTIVE"));
    }

    @Test
    void radiusOf50kmReturnsSixActiveHospitalsExcludingInactive() {
        String url = String.format(
                "/api/v1/hospitals/nearby?latitude=%s&longitude=%s&radius_km=50",
                DELHI_LAT, DELHI_LON);

        ResponseEntity<HospitalDto[]> resp =
                rest.exchange(url, HttpMethod.GET, authEntity(), HospitalDto[].class);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        List<HospitalDto> hospitals = Arrays.asList(resp.getBody());

        // 6 active hospitals within 50 km; hospital 007 (INACTIVE, 26 km) must be absent
        assertThat(hospitals).hasSize(6);
        hospitals.forEach(h -> assertThat(h.status()).isEqualTo("ACTIVE"));
    }

    @Test
    void largerRadiusReturnsMoreHospitalsThanSmallerRadius() {
        HttpEntity<Void> auth = authEntity();

        String url5 = String.format(
                "/api/v1/hospitals/nearby?latitude=%s&longitude=%s&radius_km=5",
                DELHI_LAT, DELHI_LON);
        String url50 = String.format(
                "/api/v1/hospitals/nearby?latitude=%s&longitude=%s&radius_km=50",
                DELHI_LAT, DELHI_LON);

        int count5 = rest.exchange(url5, HttpMethod.GET, auth, HospitalDto[].class)
                .getBody().length;
        int count50 = rest.exchange(url50, HttpMethod.GET, auth, HospitalDto[].class)
                .getBody().length;

        assertThat(count50).isGreaterThan(count5);
    }

    @Test
    void boundingBoxQueryReturnsHospitalsWithinBox() {
        // Tight box around the Delhi 3-hospital cluster — excludes hospital 004 (lat 28.525)
        String url = "/api/v1/hospitals/search"
                + "?min_lat=28.59&min_lon=77.18&max_lat=28.65&max_lon=77.25";

        ResponseEntity<HospitalDto[]> resp =
                rest.exchange(url, HttpMethod.GET, authEntity(), HospitalDto[].class);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).hasSize(3);
    }

    @Test
    void boundingBoxQueryWithNonMatchingBoxReturnsEmpty() {
        // South Atlantic — no hospitals seeded there
        String url = "/api/v1/hospitals/search"
                + "?min_lat=-40.0&min_lon=-20.0&max_lat=-30.0&max_lon=-10.0";

        ResponseEntity<HospitalDto[]> resp =
                rest.exchange(url, HttpMethod.GET, authEntity(), HospitalDto[].class);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEmpty();
    }
}
