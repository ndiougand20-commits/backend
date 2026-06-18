package com.rezo.backend.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Événement de matching capturé pour l'instrumentation KPI.
 * Utilisé pour mesurer la pertinence des recommandations et les taux de conversion.
 */
public class MatchingEvent {

    private UUID eventId;
    private UUID userId; // User qui reçoit la recommandation
    private String userRole; // ETUDIANT, LYCEEN, ECOLE, ENTREPRISE
    private UUID targetId; // Offer/School/User recommandé
    private String targetType; // OFFER, SCHOOL, PROFILE
    private int score; // Score du matching (0-100)
    private List<String> reasons; // Raisons explicitantes du score
    private String action; // VIEWED, LIKED, DISLIKED, SKIPPED
    private LocalDateTime createdAt;
    private LocalDateTime actionAt; // Quand l'utilisateur a cliqué
    private long responseTimeMs; // Temps avant action

    public MatchingEvent() {}

    public MatchingEvent(UUID userId, String userRole, UUID targetId, String targetType, 
                         int score, List<String> reasons) {
        this.eventId = UUID.randomUUID();
        this.userId = userId;
        this.userRole = userRole;
        this.targetId = targetId;
        this.targetType = targetType;
        this.score = score;
        this.reasons = reasons;
        this.createdAt = LocalDateTime.now();
    }

    // Getters et Setters
    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public UUID getTargetId() { return targetId; }
    public void setTargetId(UUID targetId) { this.targetId = targetId; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public List<String> getReasons() { return reasons; }
    public void setReasons(List<String> reasons) { this.reasons = reasons; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getActionAt() { return actionAt; }
    public void setActionAt(LocalDateTime actionAt) { this.actionAt = actionAt; }

    public long getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(long responseTimeMs) { this.responseTimeMs = responseTimeMs; }

    @Override
    public String toString() {
        return String.format(
            "MatchingEvent{userId=%s, userRole=%s, targetId=%s, targetType=%s, score=%d, action=%s, createdAt=%s}",
            userId, userRole, targetId, targetType, score, action, createdAt
        );
    }
}
