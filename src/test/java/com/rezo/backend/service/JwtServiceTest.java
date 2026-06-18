package com.rezo.backend.service;

import com.rezo.entities.User;
import com.rezo.entities.enums.UserRole;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = "rezo_secret_key_must_be_at_least_32_chars_long_for_hs256";

    @Test
    void accessTokenShouldBeValidOnlyAsAccessToken() {
        JwtService jwtService = new JwtService(SECRET, 60_000L, 604_800_000L);
        User user = buildUser();

        String accessToken = jwtService.generateAccessToken(user);

        assertNotNull(accessToken);
        assertTrue(jwtService.isAccessTokenValid(accessToken));
        assertFalse(jwtService.isRefreshTokenValid(accessToken));
        assertEquals("ACCESS", jwtService.extractTokenType(accessToken));
        assertEquals(user.getId().toString(), jwtService.extractSubject(accessToken));
    }

    @Test
    void refreshTokenShouldBeValidOnlyAsRefreshToken() {
        JwtService jwtService = new JwtService(SECRET, 60_000L, 604_800_000L);
        User user = buildUser();

        String refreshToken = jwtService.generateRefreshToken(user);

        assertNotNull(refreshToken);
        assertTrue(jwtService.isRefreshTokenValid(refreshToken));
        assertFalse(jwtService.isAccessTokenValid(refreshToken));
        assertEquals("REFRESH", jwtService.extractTokenType(refreshToken));
    }

    @Test
    void malformedTokenShouldBeRejected() {
        JwtService jwtService = new JwtService(SECRET, 60_000L, 604_800_000L);

        assertFalse(jwtService.isAccessTokenValid("bad-token"));
        assertFalse(jwtService.isRefreshTokenValid("bad-token"));
    }

    private User buildUser() {
        User user = new User();
        user.setEmail("test@rezo.sn");
        user.setRole(UserRole.ETUDIANT);
        setPrivateField(user, "id", UUID.randomUUID());
        return user;
    }

    private void setPrivateField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Impossible d'initialiser le champ " + fieldName, exception);
        }
    }
}
