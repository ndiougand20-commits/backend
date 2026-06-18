package com.rezo.backend.service;

import com.rezo.entities.RefreshToken;
import com.rezo.entities.User;
import com.rezo.repositories.RefreshTokenRepository;
import com.rezo.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            JwtService jwtService
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public String issueRefreshToken(User user) {
        String token = jwtService.generateRefreshToken(user);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(token);
        refreshToken.setExpiresAt(jwtService.computeRefreshExpiryDateTime());
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);
        return token;
    }

    @Transactional
    public TokenPair rotateRefreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token manquant");
        }

        if (!jwtService.isRefreshTokenValid(rawRefreshToken)) {
            throw new IllegalArgumentException("Refresh token invalide ou expire");
        }

        RefreshToken storedToken = refreshTokenRepository
                .findByTokenAndRevokedFalse(rawRefreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token inconnu ou revoque"));

        if (storedToken.getExpiresAt() != null && storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            revoke(storedToken);
            throw new IllegalArgumentException("Refresh token expire");
        }

        String subject = jwtService.extractSubject(rawRefreshToken);
        if (storedToken.getUser() == null || storedToken.getUser().getId() == null ||
                !storedToken.getUser().getId().toString().equals(subject)) {
            revoke(storedToken);
            throw new IllegalArgumentException("Refresh token invalide");
        }

        User user = userRepository.findByIdWithPack(storedToken.getUser().getId())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        revoke(storedToken);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = issueRefreshToken(user);
        return new TokenPair(accessToken, refreshToken);
    }

    @Transactional
    public void revokeByToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenAndRevokedFalse(rawRefreshToken)
                .ifPresent(this::revoke);
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        List<RefreshToken> activeTokens = refreshTokenRepository.findByUserIdAndRevokedFalse(userId);
        for (RefreshToken token : activeTokens) {
            revoke(token);
        }
    }

    private void revoke(RefreshToken token) {
        token.setRevoked(true);
        token.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(token);
    }

    public record TokenPair(String accessToken, String refreshToken) {
    }
}