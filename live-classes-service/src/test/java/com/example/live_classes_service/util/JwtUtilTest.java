package com.example.live_classes_service.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private static final String SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3Rpbmctb25seQ==";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKeyString", SECRET);
    }

    private String buildToken(String userId, String name, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", name);
        claims.put("role", role);
        long now = System.currentTimeMillis();
        return "Bearer " + Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + 60 * 60 * 1000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    void extractUserId_success() {
        assertEquals("user-123", jwtUtil.extractUserId(buildToken("user-123", "Alice", "TRAINER")));
    }

    @Test
    void extractName_success() {
        assertEquals("Alice", jwtUtil.extractName(buildToken("user-123", "Alice", "TRAINER")));
    }

    @Test
    void extractRole_success() {
        assertEquals("LEARNER", jwtUtil.extractRole(buildToken("user-123", "Alice", "LEARNER")));
    }
}
