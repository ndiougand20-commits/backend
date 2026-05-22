package com.rezo.backend.dto.match;

import java.util.UUID;

public class SwipeRequest {

    private UUID offerId;
    private String action;

    public UUID getOfferId() {
        return offerId;
    }

    public void setOfferId(UUID offerId) {
        this.offerId = offerId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
