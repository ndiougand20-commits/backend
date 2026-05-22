package com.rezo.backend.controller;

import com.rezo.backend.service.PackRules;
import com.rezo.entities.ChatSupport;
import com.rezo.entities.User;
import com.rezo.repositories.ChatSupportRepository;
import com.rezo.repositories.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@SecurityRequirement(name = "bearer-jwt")
public class ChatSupportController {

    private static final int MAX_MESSAGE_LENGTH = 4000;

    private final UserRepository userRepository;
    private final ChatSupportRepository chatSupportRepository;

    public ChatSupportController(
            UserRepository userRepository,
            ChatSupportRepository chatSupportRepository
    ) {
        this.userRepository = userRepository;
        this.chatSupportRepository = chatSupportRepository;
    }

    @Operation(summary = "Envoyer un message au chat IA", description = "Enregistre le message utilisateur et retourne une reponse IA (stub serveur)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reponse retournee"),
            @ApiResponse(responseCode = "400", description = "Payload invalide"),
            @ApiResponse(responseCode = "401", description = "Non authentifie"),
            @ApiResponse(responseCode = "403", description = "Chat IA non autorise")
    })
    @PostMapping
    @Transactional
    public ResponseEntity<?> sendChatMessage(@RequestBody ChatRequest request, Principal principal) {
        try {
            User user = resolveAuthenticatedUser(principal);
            ensureAiChatAllowed(user);

            if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
                throw new BadRequestException("Le champ message est obligatoire");
            }

            String normalizedMessage = request.getMessage().trim();
            if (normalizedMessage.length() > MAX_MESSAGE_LENGTH) {
                throw new BadRequestException("Le message ne doit pas depasser 4000 caracteres");
            }

            UUID sessionId = request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID();
            String context = request.getContext() != null ? request.getContext().trim() : null;

            // Stub IA serveur: a remplacer par un provider externe (OpenAI/Mistral/etc.)
            String iaResponse = generateStubResponse(normalizedMessage, context);

            ChatSupport entry = new ChatSupport();
            entry.setUser(user);
            entry.setSessionId(sessionId);
            entry.setContext(context);
            entry.setUserMessage(normalizedMessage);
            entry.setIaResponse(iaResponse);
            entry = chatSupportRepository.save(entry);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("id", entry.getId());
            body.put("sessionId", entry.getSessionId());
            body.put("userMessage", entry.getUserMessage());
            body.put("iaResponse", entry.getIaResponse());
            body.put("context", entry.getContext());
            body.put("createdAt", entry.getCreatedAt());
            return ResponseEntity.ok(body);
        } catch (UnauthorizedException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", exception.getMessage()));
        } catch (ForbiddenException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", exception.getMessage()));
        } catch (BadRequestException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @Operation(summary = "Historique chat IA", description = "Retourne l'historique du chat IA pour la session demandee")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historique retourne"),
            @ApiResponse(responseCode = "400", description = "Parametres invalides"),
            @ApiResponse(responseCode = "401", description = "Non authentifie"),
            @ApiResponse(responseCode = "403", description = "Chat IA non autorise")
    })
    @GetMapping("/history")
    @Transactional
    public ResponseEntity<?> getChatHistory(
            @RequestParam(name = "sessionId", required = false) UUID sessionId,
            @RequestParam(name = "limit", defaultValue = "100") int limit,
            Principal principal
    ) {
        try {
            if (limit <= 0 || limit > 500) {
                throw new BadRequestException("Le parametre limit doit etre compris entre 1 et 500");
            }

            User user = resolveAuthenticatedUser(principal);
            ensureAiChatAllowed(user);

            List<ChatSupport> messages = sessionId != null
                    ? chatSupportRepository.findByUserIdAndSessionIdOrderByCreatedAtAsc(user.getId(), sessionId)
                    : chatSupportRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

            if (messages.size() > limit) {
                messages = sessionId != null
                        ? messages.subList(Math.max(0, messages.size() - limit), messages.size())
                        : messages.subList(0, limit);
            }

            List<Map<String, Object>> items = messages.stream().map(this::toHistoryItem).toList();
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("items", items);
            body.put("count", items.size());
            body.put("sessionId", sessionId);
            body.put("generatedAt", LocalDateTime.now());
            return ResponseEntity.ok(body);
        } catch (UnauthorizedException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", exception.getMessage()));
        } catch (ForbiddenException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", exception.getMessage()));
        } catch (BadRequestException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    private Map<String, Object> toHistoryItem(ChatSupport entry) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", entry.getId());
        item.put("sessionId", entry.getSessionId());
        item.put("userMessage", entry.getUserMessage());
        item.put("iaResponse", entry.getIaResponse());
        item.put("context", entry.getContext());
        item.put("createdAt", entry.getCreatedAt());
        return item;
    }

    private String generateStubResponse(String message, String context) {
        String ctx = context == null || context.isBlank() ? "general" : context;
        return "Conseil REZO (stub): j'ai bien recu ton message dans le contexte '"
                + ctx
                + "'. Prochaine etape recommande: clarifier ton objectif en 1 phrase, puis lancer un matching cible.";
    }

    private void ensureAiChatAllowed(User user) {
        if (!PackRules.canUseAiChat(user)) {
            throw new ForbiddenException("Votre pack actuel ne permet pas d'utiliser le chat IA");
        }
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

    public static class ChatRequest {
        private String message;
        private UUID sessionId;
        private String context;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public UUID getSessionId() {
            return sessionId;
        }

        public void setSessionId(UUID sessionId) {
            this.sessionId = sessionId;
        }

        public String getContext() {
            return context;
        }

        public void setContext(String context) {
            this.context = context;
        }
    }

    private static class UnauthorizedException extends RuntimeException {
        private UnauthorizedException(String message) {
            super(message);
        }
    }

    private static class ForbiddenException extends RuntimeException {
        private ForbiddenException(String message) {
            super(message);
        }
    }

    private static class BadRequestException extends RuntimeException {
        private BadRequestException(String message) {
            super(message);
        }
    }
}
