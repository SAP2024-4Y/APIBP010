package com.hospital;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class RateLimitTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void sixtyFirstRequestReturns429WithRateLimitHeaders() {
        // Use a dedicated virtual IP so this test's bucket is isolated from
        // other integration tests (which all arrive as 127.0.0.1).
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Forwarded-For", "10.0.0.99");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // Exhaust the 60-token bucket
        for (int i = 1; i <= 60; i++) {
            ResponseEntity<String> resp =
                    rest.exchange("/health", HttpMethod.GET, entity, String.class);
            assertThat(resp.getStatusCode().value())
                    .as("Request %d should succeed before limit", i)
                    .isEqualTo(200);
        }

        // 61st request must be rejected
        ResponseEntity<String> limited =
                rest.exchange("/health", HttpMethod.GET, entity, String.class);

        assertThat(limited.getStatusCode().value()).isEqualTo(429);
        assertThat(limited.getHeaders().getFirst("RateLimit-Limit")).isEqualTo("60");
        assertThat(limited.getHeaders().getFirst("RateLimit-Remaining")).isEqualTo("0");
        assertThat(limited.getHeaders().getFirst("RateLimit-Reset")).isNotNull();
    }
}
