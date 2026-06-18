package com.rezo.backend.service;

import com.rezo.backend.model.MatchingEvent;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service d'instrumentation pour capturer les événements de matching.
 * Utile pour mesurer la pertinence, les taux de conversion et l'efficacité de l'algorithme.
 */
@Service
public class MatchingInstrumentationService {

    private final List<MatchingEvent> events = new CopyOnWriteArrayList<>();
    private final int MAX_EVENTS = 10000; // Limite pour éviter fuite mémoire

    /**
     * Enregistre un événement de recommandation (avant action).
     */
    public MatchingEvent recordRecommendation(UUID userId, String userRole, UUID targetId, 
                                               String targetType, int score, List<String> reasons) {
        MatchingEvent event = new MatchingEvent(userId, userRole, targetId, targetType, score, reasons);
        addEvent(event);
        return event;
    }

    /**
     * Met à jour un événement avec l'action utilisateur (LIKED, DISLIKED, SKIPPED).
     */
    public void recordAction(MatchingEvent event, String action) {
        if (event != null) {
            event.setAction(action);
            event.setActionAt(LocalDateTime.now());
            if (event.getCreatedAt() != null) {
                event.setResponseTimeMs(
                    java.time.temporal.ChronoUnit.MILLIS.between(
                        event.getCreatedAt(), event.getActionAt()
                    )
                );
            }
        }
    }

    private void addEvent(MatchingEvent event) {
        events.add(event);
        // Nettoyer si on dépasse la limite (FIFO)
        if (events.size() > MAX_EVENTS) {
            events.remove(0);
        }
    }

    /**
     * Récupère tous les événements pour export/analyse.
     */
    public List<MatchingEvent> getAllEvents() {
        return new ArrayList<>(events);
    }

    /**
     * Exporte les événements en format CSV pour analyse externe.
     */
    public String exportToCSV() {
        StringBuilder csv = new StringBuilder();
        csv.append("eventId,userId,userRole,targetId,targetType,score,action,createdAt,actionAt,responseTimeMs\n");
        
        for (MatchingEvent event : events) {
            csv.append(event.getEventId()).append(",")
               .append(event.getUserId()).append(",")
               .append(event.getUserRole()).append(",")
               .append(event.getTargetId()).append(",")
               .append(event.getTargetType()).append(",")
               .append(event.getScore()).append(",")
               .append(event.getAction() != null ? event.getAction() : "").append(",")
               .append(event.getCreatedAt()).append(",")
               .append(event.getActionAt() != null ? event.getActionAt() : "").append(",")
               .append(event.getResponseTimeMs())
               .append("\n");
        }
        
        return csv.toString();
    }

    /**
     * Calcule des KPIs simples à partir des événements collectés.
     */
    public Map<String, Object> computeKPIs() {
        Map<String, Object> kpis = new HashMap<>();

        long totalRecommendations = events.size();
        long totalLikes = events.stream().filter(e -> "LIKED".equals(e.getAction())).count();
        long totalDislikes = events.stream().filter(e -> "DISLIKED".equals(e.getAction())).count();
        long actedUpon = totalLikes + totalDislikes;

        kpis.put("totalRecommendations", totalRecommendations);
        kpis.put("totalActions", actedUpon);
        kpis.put("totalLikes", totalLikes);
        kpis.put("totalDislikes", totalDislikes);
        
        if (totalRecommendations > 0) {
            kpis.put("likeRate", String.format("%.2f%%", (totalLikes * 100.0) / totalRecommendations));
            kpis.put("actionRate", String.format("%.2f%%", (actedUpon * 100.0) / totalRecommendations));
        }

        // Score moyen des recommandations likées
        double avgScoreLiked = events.stream()
            .filter(e -> "LIKED".equals(e.getAction()))
            .mapToInt(MatchingEvent::getScore)
            .average()
            .orElse(0);
        kpis.put("avgScoreLiked", String.format("%.2f", avgScoreLiked));

        // Score moyen des recommandations dislikées
        double avgScoreDisliked = events.stream()
            .filter(e -> "DISLIKED".equals(e.getAction()))
            .mapToInt(MatchingEvent::getScore)
            .average()
            .orElse(0);
        kpis.put("avgScoreDisliked", String.format("%.2f", avgScoreDisliked));

        // Temps moyen de réponse
        double avgResponseTime = events.stream()
            .filter(e -> e.getAction() != null)
            .mapToLong(MatchingEvent::getResponseTimeMs)
            .average()
            .orElse(0);
        kpis.put("avgResponseTimeMs", String.format("%.0f", avgResponseTime));

        return kpis;
    }

    /**
     * Réinitialise les événements (pour tests ou nouveau cycle).
     */
    public void clearEvents() {
        events.clear();
    }

    /**
     * Retourne le nombre d'événements actuellement en mémoire.
     */
    public int getEventCount() {
        return events.size();
    }
}
