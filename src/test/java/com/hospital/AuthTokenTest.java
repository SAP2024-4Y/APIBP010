package com.hospital;

import com.hospital.auth.AuthRequest;
import com.hospital.auth.AuthResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

class AuthTokenTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void validCredentialsReturnToken() {
        var response = rest.postForEntity(
                "/api/v1/auth/token",
                new AuthRequest("demo", "demo-password"),
                AuthResponse.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isNotBlank();
    }

    @Test
    void wrongPasswordReturns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var response = rest.postForEntity(
                "/api/v1/auth/token",
                new HttpEntity<>(new AuthRequest("demo", "wrong"), headers),
                String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void missingTokenOnProtectedEndpointReturns401() {
        var response = rest.postForEntity(
                "/api/v1/appointments",
                new HttpEntity<>(null, new HttpHeaders()),
                String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }
}
