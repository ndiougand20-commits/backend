package com.rezo.backend.service;

import com.rezo.backend.model.MatchingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests pour le service d'instrumentation de matching.
 */
public class MatchingInstrumentationServiceTest {

    private MatchingInstrumentationService service;

    @BeforeEach
    public void setUp() {
        service = new MatchingInstrumentationService();
    }

    @Test
    public void testRecordRecommendation() {
        UUID userId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        List<String> reasons = Arrays.asList("Domaine match", "Secteur aligné");

        MatchingEvent event = service.recordRecommendation(
            userId, 
            "ETUDIANT", 
            targetId, 
            "OFFER", 
            75, 
            reasons
        );

        assertNotNull(event.getEventId());
        assertEquals(userId, event.getUserId());
        assertEquals("ETUDIANT", event.getUserRole());
        assertEquals(targetId, event.getTargetId());
        assertEquals("OFFER", event.getTargetType());
        assertEquals(75, event.getScore());
        assertEquals(2, event.getReasons().size());
        assertNull(event.getAction()); // Pas d'action enregistrée encore
    }

    @Test
    public void testRecordAction() {
        UUID userId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        MatchingEvent event = service.recordRecommendation(
            userId, "LYCEEN", targetId, "SCHOOL", 85, 
            Arrays.asList("Formation alignée")
        );

        service.recordAction(event, "LIKED");

        assertEquals("LIKED", event.getAction());
        assertNotNull(event.getActionAt());
        assertTrue(event.getResponseTimeMs() >= 0);
    }

    @Test
    public void testComputeKPIs() {
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();

        // Enregistrer plusieurs événements
        MatchingEvent e1 = service.recordRecommendation(
            userId1, "ETUDIANT", UUID.randomUUID(), "OFFER", 80, 
            Arrays.asList("Match")
        );
        service.recordAction(e1, "LIKED");

        MatchingEvent e2 = service.recordRecommendation(
            userId1, "ETUDIANT", UUID.randomUUID(), "OFFER", 40, 
            Arrays.asList("Mismatch")
        );
        service.recordAction(e2, "DISLIKED");

        MatchingEvent e3 = service.recordRecommendation(
            userId2, "LYCEEN", UUID.randomUUID(), "SCHOOL", 70, 
            Arrays.asList("Bonne école")
        );
        // e3 pas d'action enregistrée

        Map<String, Object> kpis = service.computeKPIs();

        assertEquals(3L, kpis.get("totalRecommendations"));
        assertEquals(1L, kpis.get("totalLikes"));
        assertEquals(1L, kpis.get("totalDislikes"));
        assertEquals(2L, kpis.get("totalActions"));
        assertTrue(kpis.get("likeRate").toString().contains("33")); // 1/3
        assertNotNull(kpis.get("avgScoreLiked"));
        assertNotNull(kpis.get("avgScoreDisliked"));
    }

    @Test
    public void testExportToCSV() {
        UUID userId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        MatchingEvent event = service.recordRecommendation(
            userId, "ETUDIANT", targetId, "OFFER", 75, 
            Arrays.asList("Compétences")
        );
        service.recordAction(event, "LIKED");

        String csv = service.exportToCSV();

        assertTrue(csv.contains("eventId,userId,userRole,targetId,targetType,score,action,createdAt,actionAt,responseTimeMs"));
        assertTrue(csv.contains("ETUDIANT"));
        assertTrue(csv.contains("OFFER"));
        assertTrue(csv.contains("LIKED"));
        assertTrue(csv.contains("75"));
    }

    @Test
    public void testClearEvents() {
        UUID userId = UUID.randomUUID();
        service.recordRecommendation(userId, "ETUDIANT", UUID.randomUUID(), "OFFER", 50, List.of());

        assertEquals(1, service.getEventCount());
        
        service.clearEvents();
        
        assertEquals(0, service.getEventCount());
        assertEquals(0L, service.computeKPIs().get("totalRecommendations"));
    }

    @Test
    public void testEventCountLimit() {
        // Enregistrer plus d'événements que MAX_EVENTS (10000)
        for (int i = 0; i < 10010; i++) {
            service.recordRecommendation(
                UUID.randomUUID(), "ETUDIANT", UUID.randomUUID(), "OFFER", 50 + (i % 50), 
                List.of("Test")
            );
        }

        // Vérifier que la limite est respectée (ne dépassera pas MAX_EVENTS)
        assertTrue(service.getEventCount() <= 10000);
    }
}
