package com.rezo.repositories;

import com.rezo.entities.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    List<RefreshToken> findByUserIdAndRevokedFalse(UUID userId);
}