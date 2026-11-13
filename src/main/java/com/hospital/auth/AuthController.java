package com.hospital.auth;

import com.hospital.common.DomainException;
import com.hospital.common.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String DEMO_USER = "demo";
    private static final String DEMO_PASS = "demo-password";

    private final JwtService jwtService;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/token")
    public ResponseEntity<AuthResponse> token(@RequestBody AuthRequest request) {
        if (!DEMO_USER.equals(request.username()) || !DEMO_PASS.equals(request.password())) {
            throw new DomainException(ErrorCode.UNAUTHORIZED, "Invalid credentials");
        }
        String token = jwtService.generateToken(DEMO_USER, "USER");
        return ResponseEntity.ok(new AuthResponse(token));
    }
}
