package com.hospital.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class JwtServiceTest {

    // 32 chars = 256 bits, minimum for HS256
    private static final String SECRET = "test-secret-key-for-unit-tests!!";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 3_600_000L);
    }

    @Test
    void generateTokenReturnsNonBlankString() {
        assertThat(jwtService.generateToken("demo", "USER")).isNotBlank();
    }

    @Test
    void generateTokenProducesThreePartJwt() {
        String token = jwtService.generateToken("demo", "USER");
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void parseTokenReturnsCorrectSubject() {
        String token = jwtService.generateToken("demo", "USER");
        Claims claims = jwtService.parseToken(token);
        assertThat(claims.getSubject()).isEqualTo("demo");
    }

    @Test
    void parseTokenReturnsCorrectRole() {
        String token = jwtService.generateToken("demo", "USER");
        assertThat(jwtService.parseToken(token).get("role", String.class)).isEqualTo("USER");
    }

    @Test
    void parseTokenWithDifferentRolePreservesRole() {
        String token = jwtService.generateToken("admin", "ADMIN");
        assertThat(jwtService.parseToken(token).get("role", String.class)).isEqualTo("ADMIN");
    }

    @Test
    void expiredTokenThrowsJwtException() {
        JwtService shortLived = new JwtService(SECRET, -1L);
        String token = shortLived.generateToken("demo", "USER");
        assertThatThrownBy(() -> jwtService.parseToken(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tamperedSignatureThrowsJwtException() {
        String token = jwtService.generateToken("demo", "USER");
        String tampered = token.substring(0, token.length() - 4) + "xxxx";
        assertThatThrownBy(() -> jwtService.parseToken(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void randomStringThrowsJwtException() {
        assertThatThrownBy(() -> jwtService.parseToken("not.a.jwt"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedWithDifferentSecretThrowsJwtException() {
        JwtService otherService = new JwtService("completely-different-secret-key!!", 3_600_000L);
        String token = otherService.generateToken("demo", "USER");
        assertThatThrownBy(() -> jwtService.parseToken(token))
                .isInstanceOf(JwtException.class);
    }
}
