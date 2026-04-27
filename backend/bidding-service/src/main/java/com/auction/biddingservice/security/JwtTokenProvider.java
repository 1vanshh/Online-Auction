package com.auction.biddingservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtTokenProvider {
    @Value("${jwt.secret}")
    private String secretKey;

    public String getEmailFromToken(String token) { return getClaims(token).getSubject(); }

    public String getRoleFromToken(String token) {
        Object role = getClaims(token).get("role");
        return role == null ? null : role.toString();
    }

    public Long getUserIdFromToken(String token) {
        Object id = getClaims(token).get("userId");
        if (id instanceof Number n) return n.longValue();
        return id == null ? null : Long.parseLong(id.toString());
    }

    private Claims getClaims(String token) {
        return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] bytes = secretKey.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) throw new IllegalStateException("JWT secret must be at least 32 bytes long");
        return Keys.hmacShaKeyFor(bytes);
    }
}
