package com.hospital;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class HealthCheckTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void healthEndpointReturnsOk() {
        var response = rest.getForEntity("/health", String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"status\"", "\"ok\"");
    }
}
