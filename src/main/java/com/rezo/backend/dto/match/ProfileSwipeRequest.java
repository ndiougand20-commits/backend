package com.rezo.backend.dto.match;

import java.util.UUID;

public class ProfileSwipeRequest {

    private UUID targetUserId;
    private String action;

    public UUID getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(UUID targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
