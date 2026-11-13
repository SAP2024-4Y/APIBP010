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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@SuppressWarnings({"unchecked", "rawtypes"})
class PaginationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    private String getToken() {
        return rest.postForEntity(
                "/api/v1/auth/token",
                new AuthRequest("demo", "demo-password"),
                AuthResponse.class
        ).getBody().token();
    }

    @Test
    void firstPageHasFiveItemsAndHasMoreTrue() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getToken());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        Map body = rest.exchange(
                "/api/v1/doctors?limit=5&sort=id&order=asc",
                HttpMethod.GET, entity, Map.class
        ).getBody();

        List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");
        assertThat(data).hasSize(5);
        assertThat((Boolean) body.get("has_more")).isTrue();
        assertThat((String) body.get("next_cursor")).isNotBlank();
    }

    @Test
    void secondPageDoesNotOverlapWithFirstPage() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getToken());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // Page 1
        Map page1 = rest.exchange(
                "/api/v1/doctors?limit=5&sort=id&order=asc",
                HttpMethod.GET, entity, Map.class
        ).getBody();
        List<Map<String, Object>> data1 = (List<Map<String, Object>>) page1.get("data");
        Set<String> ids1 = new HashSet<>();
        data1.forEach(d -> ids1.add((String) d.get("id")));

        String cursor = (String) page1.get("next_cursor");

        // Page 2
        Map page2 = rest.exchange(
                "/api/v1/doctors?limit=5&sort=id&order=asc&after=" + cursor,
                HttpMethod.GET, entity, Map.class
        ).getBody();
        List<Map<String, Object>> data2 = (List<Map<String, Object>>) page2.get("data");
        Set<String> ids2 = new HashSet<>();
        data2.forEach(d -> ids2.add((String) d.get("id")));

        // No doctor should appear on both pages
        Set<String> overlap = new HashSet<>(ids1);
        overlap.retainAll(ids2);
        assertThat(overlap).isEmpty();
    }

    @Test
    void paginatingThroughAllDoctorsYields20UniqueResultsAndHasMoreFalseOnLastPage() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getToken());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        Set<String> allIds = new HashSet<>();
        String cursor = null;
        boolean hasMore = true;

        while (hasMore) {
            String url = "/api/v1/doctors?limit=5&sort=id&order=asc"
                    + (cursor != null ? "&after=" + cursor : "");

            Map body = rest.exchange(url, HttpMethod.GET, entity, Map.class).getBody();
            List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");

            // Each page must have at least 1 and at most 5 items
            assertThat(data).hasSizeBetween(1, 5);

            List<String> pageIds = new ArrayList<>();
            data.forEach(d -> pageIds.add((String) d.get("id")));

            // No duplicates within this page
            assertThat(new HashSet<>(pageIds)).hasSameSizeAs(pageIds);

            // No overlap with previously seen IDs
            Set<String> overlap = new HashSet<>(allIds);
            overlap.retainAll(new HashSet<>(pageIds));
            assertThat(overlap).isEmpty();

            allIds.addAll(pageIds);

            hasMore = Boolean.TRUE.equals(body.get("has_more"));
            cursor = hasMore ? (String) body.get("next_cursor") : null;
        }

        // V3 seeds exactly 20 doctors
        assertThat(allIds).hasSize(20);
    }
}
