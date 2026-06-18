package com.rezo.backend.service;

import com.rezo.entities.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtService.class);
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";

    private final SecretKey secretKey;
    private final long expirationMs;
    private final long refreshExpirationMs;

    public JwtService(
            @Value("${rezo.jwt.secret}") String secret,
            @Value("${rezo.jwt.expiration-ms}") long expirationMs,
            @Value("${rezo.jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generateToken(User user) {
        return generateAccessToken(user);
    }

    public String generateAccessToken(User user) {
        return buildToken(user, expirationMs, ACCESS_TOKEN_TYPE, true);
    }

    public String generateRefreshToken(User user) {
        return buildToken(user, refreshExpirationMs, REFRESH_TOKEN_TYPE, false);
    }

    private String buildToken(User user, long tokenTtlMs, String tokenType, boolean includeRoleClaims) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + tokenTtlMs);

        var builder = Jwts.builder()
                .subject(user.getId().toString())
            .id(UUID.randomUUID().toString())
                .claim("email", user.getEmail())
                .claim("tokenType", tokenType)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey);

        if (includeRoleClaims) {
            builder
                    .claim("role", user.getRole().name())
                    .claim("packId", user.getPack() != null ? user.getPack().getId().toString() : null);
        }

        return builder.compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractSubject(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    public String extractTokenType(String token) {
        return extractClaims(token).get("tokenType", String.class);
    }

    public boolean isTokenValid(String token) {
        return isAccessTokenValid(token);
    }

    public boolean isAccessTokenValid(String token) {
        return isTokenValidForType(token, ACCESS_TOKEN_TYPE);
    }

    public boolean isRefreshTokenValid(String token) {
        return isTokenValidForType(token, REFRESH_TOKEN_TYPE);
    }

    private boolean isTokenValidForType(String token, String expectedType) {
        try {
            Claims claims = extractClaims(token);
            String tokenType = claims.get("tokenType", String.class);
            return claims.getExpiration().after(new Date()) && expectedType.equals(tokenType);
        } catch (JwtException | IllegalArgumentException exception) {
            LOGGER.warn("Token JWT invalide: {}", exception.getMessage());
            return false;
        }
    }

    public LocalDateTime computeRefreshExpiryDateTime() {
        return LocalDateTime.now().plusNanos(refreshExpirationMs * 1_000_000);
    }
}
