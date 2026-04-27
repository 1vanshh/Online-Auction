package com.auction.auctionservice.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private static final String SECRET = "change-me-please-change-me-please-1";

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "secretKey", SECRET);
    }

    @Test
    void shouldExtractClaimsFromToken() {
        String token = token(42L, "seller@example.com", "USER");

        assertEquals("seller@example.com", provider.getEmailFromToken(token));
        assertEquals("USER", provider.getRoleFromToken(token));
        assertEquals(42L, provider.getUserIdFromToken(token));
    }

    @Test
    void shouldThrowForInvalidToken() {
        assertThrows(JwtException.class, () -> provider.getEmailFromToken("not-a-jwt"));
    }

    @Test
    void shouldThrowWhenSecretTooShort() {
        ReflectionTestUtils.setField(provider, "secretKey", "short-secret");
        String token = token(1L, "test@example.com", "USER");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> provider.getEmailFromToken(token));

        assertEquals("JWT secret must be at least 32 bytes long", ex.getMessage());
    }

    private String token(Long userId, String subject, String role) {
        return Jwts.builder()
                .subject(subject)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
