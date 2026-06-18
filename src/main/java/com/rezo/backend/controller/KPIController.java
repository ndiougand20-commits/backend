package com.rezo.backend.controller;

import com.rezo.backend.service.MatchingInstrumentationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint d'accès aux KPIs de matching.
 * Utilisé pour mesurer la pertinence, taux de conversion et efficacité de l'algorithme.
 */
@RestController
@RequestMapping("/api/kpi")
@SecurityRequirement(name = "bearer-jwt")
public class KPIController {

    private final MatchingInstrumentationService instrumentationService;

    public KPIController(MatchingInstrumentationService instrumentationService) {
        this.instrumentationService = instrumentationService;
    }

    @Operation(summary = "KPI de matching", description = "Retourne les statistiques d'utilisation et pertinence du matching")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "KPIs calculees"),
            @ApiResponse(responseCode = "401", description = "Non authentifie")
    })
    @GetMapping("/matching")
    public ResponseEntity<Map<String, Object>> getMatchingKPIs() {
        return ResponseEntity.ok(instrumentationService.computeKPIs());
    }

    @Operation(summary = "Export CSV des événements matching", description = "Exporte tous les événements matching en CSV pour analyse externe")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "CSV genere"),
            @ApiResponse(responseCode = "401", description = "Non authentifie")
    })
    @GetMapping("/matching/events-csv")
    public ResponseEntity<String> exportMatchingEventsCSV() {
        String csv = instrumentationService.exportToCSV();
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"matching-events.csv\"")
                .header("Content-Type", "text/csv")
                .body(csv);
    }

    @Operation(summary = "Reset des événements (dev only)", description = "Réinitialise tous les événements collectés (uniquement en dev)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Events cleared"),
            @ApiResponse(responseCode = "401", description = "Non authentifie")
    })
    @PostMapping("/matching/reset")
    public ResponseEntity<Map<String, String>> resetEvents() {
        instrumentationService.clearEvents();
        return ResponseEntity.ok(Map.of("message", "Tous les événements ont été réinitialisés"));
    }
}
