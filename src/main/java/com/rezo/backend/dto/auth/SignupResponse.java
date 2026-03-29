package com.rezo.backend.dto.auth;

import java.util.UUID;

public class SignupResponse {

    private final String message;
    private final UUID userId;
    private final String email;
    private final String role;
    private final String profileType;

    public SignupResponse(String message, UUID userId, String email, String role, String profileType) {
        this.message = message;
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.profileType = profileType;
    }

    public String getMessage() {
        return message;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getProfileType() {
        return profileType;
    }
}
