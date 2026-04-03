package com.rezo.backend.controller;

import com.rezo.backend.service.PackRules;
import com.rezo.entities.User;
import com.rezo.repositories.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/features")
@SecurityRequirement(name = "bearer-jwt")
public class FeatureAccessController {

    private final UserRepository userRepository;

    public FeatureAccessController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Operation(summary = "Verifier l'acces a la messagerie", description = "Controle si le pack actuel autorise la messagerie interne")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Messagerie autorisee"),
            @ApiResponse(responseCode = "401", description = "Non authentifie"),
            @ApiResponse(responseCode = "403", description = "Pack insuffisant")
    })
    @GetMapping("/messaging/access")
    public ResponseEntity<?> messagingAccess(Principal principal) {
        try {
            User user = resolveAuthenticatedUser(principal);
            if (!PackRules.canUseMessaging(user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "allowed", false,
                        "packNom", packName(user),
                        "message", "Votre pack actuel ne permet pas d'utiliser la messagerie"
                ));
            }
            return ResponseEntity.ok(Map.of(
                    "allowed", true,
                    "packNom", packName(user),
                    "message", "Messagerie autorisee pour ce pack"
            ));
        } catch (UnauthorizedException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", exception.getMessage()));
        }
    }

    @Operation(summary = "Verifier l'acces au chat IA", description = "Controle si le pack actuel autorise le chat IA/support intelligent")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chat IA autorise"),
            @ApiResponse(responseCode = "401", description = "Non authentifie"),
            @ApiResponse(responseCode = "403", description = "Pack insuffisant")
    })
    @GetMapping("/chat-ai/access")
    public ResponseEntity<?> chatAiAccess(Principal principal) {
        try {
            User user = resolveAuthenticatedUser(principal);
            if (!PackRules.canUseAiChat(user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "allowed", false,
                        "packNom", packName(user),
                        "message", "Votre pack actuel ne permet pas d'utiliser le chat IA"
                ));
            }
            return ResponseEntity.ok(Map.of(
                    "allowed", true,
                    "packNom", packName(user),
                    "message", "Chat IA autorise pour ce pack"
            ));
        } catch (UnauthorizedException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", exception.getMessage()));
        }
    }

    @Operation(summary = "Resume des droits du pack", description = "Retourne les principales capacites actives pour le front selon le pack du user")
    @GetMapping("/access-summary")
    public ResponseEntity<?> accessSummary(Principal principal) {
        try {
            User user = resolveAuthenticatedUser(principal);
            return ResponseEntity.ok(Map.of(
                    "packNom", packName(user),
                    "canViewOpportunities", PackRules.canViewOpportunities(user),
                    "canManageOffers", PackRules.canManageOffers(user),
                    "canUseMessaging", PackRules.canUseMessaging(user),
                    "canUseAiChat", PackRules.canUseAiChat(user)
            ));
        } catch (UnauthorizedException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", exception.getMessage()));
        }
    }

    private String packName(User user) {
        return user != null && user.getPack() != null ? user.getPack().getNom() : "AUCUN";
    }

    private User resolveAuthenticatedUser(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new UnauthorizedException("Non authentifie");
        }
        UUID userId = UUID.fromString(principal.getName());
        Optional<User> optionalUser = userRepository.findByIdWithPack(userId);
        if (optionalUser.isEmpty()) {
            throw new UnauthorizedException("Utilisateur authentifie introuvable");
        }
        return optionalUser.get();
    }

    private static class UnauthorizedException extends RuntimeException {
        private UnauthorizedException(String message) {
            super(message);
        }
    }
}
