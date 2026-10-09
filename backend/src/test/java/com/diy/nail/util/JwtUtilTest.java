package com.diy.nail.util;

import cn.hutool.jwt.JWTUtil;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private static final String SECRET = "test-only-random-jwt-secret-123456";

    @Test
    void createdTokenHasClaimsAndExpiration() {
        String token = JwtUtil.createToken(7L, "CUSTOMER", SECRET, 60_000);

        assertTrue(JwtUtil.verify(token, SECRET));
        assertEquals(7L, JwtUtil.getUserId(token));
        assertEquals("CUSTOMER", JwtUtil.getRole(token));
        long issuedAt = Long.parseLong(JWTUtil.parseToken(token).getPayload("iat").toString());
        long expiresAt = Long.parseLong(JWTUtil.parseToken(token).getPayload("exp").toString());
        assertEquals(60, expiresAt - issuedAt);
    }

    @Test
    void expiredOrUndatedTokenIsRejected() {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> expired = new HashMap<>();
        expired.put("userId", 7L);
        expired.put("role", "CUSTOMER");
        expired.put("iat", now - 60);
        expired.put("exp", now - 1);
        String expiredToken = JWTUtil.createToken(expired, SECRET.getBytes(StandardCharsets.UTF_8));
        String undatedToken = JWTUtil.createToken(Map.of("userId", 7L),
                SECRET.getBytes(StandardCharsets.UTF_8));

        assertFalse(JwtUtil.verify(expiredToken, SECRET));
        assertFalse(JwtUtil.verify(undatedToken, SECRET));
    }

    @Test
    void invalidSignatureAndInvalidLifetimeAreRejected() {
        String token = JwtUtil.createToken(7L, "CUSTOMER", SECRET, 60_000);

        assertFalse(JwtUtil.verify(token, "another-random-jwt-secret-123456"));
        assertThrows(IllegalArgumentException.class,
                () -> JwtUtil.createToken(7L, "CUSTOMER", SECRET, 0));
    }

    @Test
    void malformedIdentityClaimsAreRejected() {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> invalidUser = new HashMap<>();
        invalidUser.put("userId", "not-a-number");
        invalidUser.put("role", "CUSTOMER");
        invalidUser.put("iat", now);
        invalidUser.put("exp", now + 60);
        Map<String, Object> missingRole = new HashMap<>(invalidUser);
        missingRole.put("userId", 7L);
        missingRole.remove("role");

        assertFalse(JwtUtil.verify(sign(invalidUser), SECRET));
        assertFalse(JwtUtil.verify(sign(missingRole), SECRET));
    }

    @Test
    void weakSecretIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> JwtUtil.createToken(7L, "CUSTOMER", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", 60_000));
    }

    private String sign(Map<String, Object> payload) {
        return JWTUtil.createToken(payload, SECRET.getBytes(StandardCharsets.UTF_8));
    }
}
