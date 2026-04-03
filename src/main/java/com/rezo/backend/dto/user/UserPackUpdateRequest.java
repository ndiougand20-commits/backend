package com.rezo.backend.dto.user;

import java.util.UUID;

public class UserPackUpdateRequest {

    private UUID packId;

    public UUID getPackId() {
        return packId;
    }

    public void setPackId(UUID packId) {
        this.packId = packId;
    }
}
