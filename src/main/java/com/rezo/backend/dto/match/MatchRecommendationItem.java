package com.rezo.backend.dto.match;

import com.rezo.backend.dto.offer.OfferResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MatchRecommendationItem {

    private UUID offerId;
    private int score;
    private List<String> reasons = new ArrayList<>();
    private OfferResponse offer;

    public UUID getOfferId() {
        return offerId;
    }

    public void setOfferId(UUID offerId) {
        this.offerId = offerId;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons;
    }

    public OfferResponse getOffer() {
        return offer;
    }

    public void setOffer(OfferResponse offer) {
        this.offer = offer;
    }
}
