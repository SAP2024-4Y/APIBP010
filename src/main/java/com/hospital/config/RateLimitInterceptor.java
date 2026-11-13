package com.hospital.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int LIMIT = 60;
    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        String ip = resolveIp(request);
        Bucket bucket = buckets.computeIfAbsent(ip, this::newBucket);

        var probe = bucket.tryConsumeAndReturnRemaining(1);
        long remaining = Math.max(0, probe.getRemainingTokens());
        long resetEpoch = Instant.now().plusSeconds(60).getEpochSecond();

        response.setHeader("RateLimit-Limit", String.valueOf(LIMIT));
        response.setHeader("RateLimit-Remaining", String.valueOf(remaining));
        response.setHeader("RateLimit-Reset", String.valueOf(resetEpoch));

        if (!probe.isConsumed()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter()
                    .write("{\"error\":\"RATE_LIMITED\",\"message\":\"Too many requests\"}");
            return false;
        }
        return true;
    }

    private Bucket newBucket(String ip) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(LIMIT)
                        .refillGreedy(LIMIT, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
