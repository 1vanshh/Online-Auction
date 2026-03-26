package com.auction.authservice.security;

import com.auction.authservice.entity.Role;
import com.auction.authservice.entity.User;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "secretKey", "change-me-please-change-me-please-1");
        ReflectionTestUtils.setField(provider, "validityInMilliseconds", 3600000L);
    }

    @Test
    void shouldGenerateTokenAndExtractClaims() {
        User user = new User();
        user.setId(42L);
        user.setEmail("admin@example.com");
        user.setRole(Role.ADMIN);

        String token = provider.generateToken(user);

        assertNotNull(token);
        assertEquals("admin@example.com", provider.getEmailFromToken(token));
        assertEquals("ADMIN", provider.getRoleFromToken(token));
        assertEquals(42L, provider.getUserIdFromToken(token));
    }

    @Test
    void shouldThrowForInvalidToken() {
        assertThrows(JwtException.class, () -> provider.getEmailFromToken("not-a-jwt"));
    }
}
