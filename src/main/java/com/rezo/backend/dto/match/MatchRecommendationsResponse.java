package com.rezo.backend.dto.match;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MatchRecommendationsResponse {

    private List<MatchRecommendationItem> recommendations = new ArrayList<>();
    private SuggestedPackResponse suggestedPack;
    private Map<String, Object> trace = new LinkedHashMap<>();

    public List<MatchRecommendationItem> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<MatchRecommendationItem> recommendations) {
        this.recommendations = recommendations;
    }

    public SuggestedPackResponse getSuggestedPack() {
        return suggestedPack;
    }

    public void setSuggestedPack(SuggestedPackResponse suggestedPack) {
        this.suggestedPack = suggestedPack;
    }

    public Map<String, Object> getTrace() {
        return trace;
    }

    public void setTrace(Map<String, Object> trace) {
        this.trace = trace;
    }
}
