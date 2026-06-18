package com.rezo.backend.controller;

import com.rezo.backend.dto.common.ApiErrorResponse;
import com.rezo.backend.dto.message.MessageRequest;
import com.rezo.backend.dto.message.MessageResponse;
import com.rezo.backend.service.PackRules;
import com.rezo.entities.Message;
import com.rezo.entities.Offer;
import com.rezo.entities.Profile;
import com.rezo.entities.User;
import com.rezo.entities.enums.SwipeAction;
import com.rezo.entities.enums.UserRole;
import com.rezo.repositories.MessageRepository;
import com.rezo.repositories.OfferRepository;
import com.rezo.repositories.ProfileSwipeRepository;
import com.rezo.repositories.ProfileRepository;
import com.rezo.repositories.SwipeRepository;
import com.rezo.repositories.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
@SecurityRequirement(name = "bearer-jwt")
public class MessageController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageController.class);
    private static final int MAX_CONTENT_LENGTH = 2000;

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final OfferRepository offerRepository;
    private final ProfileRepository profileRepository;
    private final SwipeRepository swipeRepository;
    private final ProfileSwipeRepository profileSwipeRepository;

    public MessageController(
            MessageRepository messageRepository,
            UserRepository userRepository,
            OfferRepository offerRepository,
            ProfileRepository profileRepository,
            SwipeRepository swipeRepository,
            ProfileSwipeRepository profileSwipeRepository
    ) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.offerRepository = offerRepository;
        this.profileRepository = profileRepository;
        this.swipeRepository = swipeRepository;
        this.profileSwipeRepository = profileSwipeRepository;
    }

    @Operation(
            summary = "Envoyer un message direct",
            description = "Permet a un utilisateur authentifie d'envoyer un message a un autre utilisateur via son userId ou son profileId",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{" +
                                    "\"receiverId\":\"22222222-2222-2222-2222-222222222222\"," +
                                    "\"content\":\"Bonjour, votre profil m'interesse\"," +
                                    "\"relatedOfferId\":\"33333333-3333-3333-3333-333333333333\"}")
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Message envoye"),
            @ApiResponse(responseCode = "400", description = "Payload invalide"),
            @ApiResponse(responseCode = "401", description = "Non authentifie"),
            @ApiResponse(responseCode = "403", description = "Messagerie non autorisee ou destinataire non joignable")
    })
    @PostMapping
    @Transactional
    public ResponseEntity<?> sendMessage(@RequestBody MessageRequest request, Principal principal) {
        try {
            User sender = resolveAuthenticatedUser(principal);
            ensureMessagingAllowed(sender);
            UUID senderId = sender.getId();

            if (request == null) {
                throw new BadRequestException("Le corps de la requete est obligatoire");
            }
            if (request.getReceiverId() == null) {
                throw new BadRequestException("Le champ receiverId est obligatoire");
            }

            User receiver = resolveTargetUser(request.getReceiverId());
            if (senderId.equals(receiver.getId())) {
                throw new BadRequestException("Vous ne pouvez pas vous envoyer un message a vous-meme");
            }
            if (!PackRules.canUseMessaging(receiver)) {
                throw new ForbiddenException("Le destinataire n'est pas joignable via la messagerie");
            }
            if (!isMutualMatch(sender, receiver)) {
                throw new ForbiddenException("La messagerie est reservee aux profils ayant un match mutuel");
            }

            String normalizedContent = normalizeContent(request.getContent());
            Offer offer = resolveOffer(request.getRelatedOfferId());

            Message message = new Message();
            message.setSender(sender);
            message.setReceiver(receiver);
            message.setOffer(offer);
            message.setContent(normalizedContent);
            message.setRead(false);

            Message saved = messageRepository.save(message);
            LOGGER.info("Message envoye id={} senderId={} receiverId={}", saved.getId(), senderId, receiver.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(MessageResponse.from(saved));
        } catch (UnauthorizedException exception) {
            return error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", exception.getMessage());
        } catch (ForbiddenException exception) {
            return error(HttpStatus.FORBIDDEN, "FORBIDDEN", exception.getMessage());
        } catch (BadRequestException exception) {
            return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", exception.getMessage());
        }
    }

    @Operation(summary = "Recuperer une conversation", description = "Retourne tous les messages echanges avec un autre utilisateur")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conversation retournee"),
            @ApiResponse(responseCode = "401", description = "Non authentifie"),
            @ApiResponse(responseCode = "403", description = "Messagerie non autorisee"),
            @ApiResponse(responseCode = "404", description = "Destinataire introuvable")
    })
    @GetMapping("/conversation/{userId}")
    @Transactional
    public ResponseEntity<?> getConversation(
            @Parameter(description = "Identifiant user ou profile du contact") @PathVariable UUID userId,
            Principal principal
    ) {
        try {
            User currentUser = resolveAuthenticatedUser(principal);
            ensureMessagingAllowed(currentUser);
            User otherUser = resolveTargetUser(userId);

            List<MessageResponse> conversation = messageRepository.findConversation(currentUser.getId(), otherUser.getId())
                    .stream()
                    .map(MessageResponse::from)
                    .toList();
            return ResponseEntity.ok(conversation);
        } catch (UnauthorizedException exception) {
            return error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", exception.getMessage());
        } catch (ForbiddenException exception) {
            return error(HttpStatus.FORBIDDEN, "FORBIDDEN", exception.getMessage());
        } catch (BadRequestException exception) {
            return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", exception.getMessage());
        }
    }

    @Operation(summary = "Lister mes messages", description = "Retourne les messages recus/envoyes du user courant avec tri, pagination simple et filtre lu/non lu")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Messages retournes"),
            @ApiResponse(responseCode = "400", description = "Parametres invalides"),
            @ApiResponse(responseCode = "401", description = "Non authentifie"),
            @ApiResponse(responseCode = "403", description = "Messagerie non autorisee")
    })
    @GetMapping
    @Transactional
    public ResponseEntity<?> listMessages(
            @RequestParam(required = false) Boolean read,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "desc") String sort,
            Principal principal
    ) {
        try {
            if (page < 0) {
                throw new BadRequestException("Le parametre page doit etre superieur ou egal a 0");
            }
            if (size <= 0 || size > 100) {
                throw new BadRequestException("Le parametre size doit etre compris entre 1 et 100");
            }

            User currentUser = resolveAuthenticatedUser(principal);
            ensureMessagingAllowed(currentUser);

            List<Message> messages = new ArrayList<>(messageRepository.findAllForUser(currentUser.getId(), read));
            Comparator<Message> comparator = Comparator.comparing(Message::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            if (!"asc".equalsIgnoreCase(sort)) {
                comparator = comparator.reversed();
            }
            messages.sort(comparator);

            int fromIndex = Math.min(page * size, messages.size());
            int toIndex = Math.min(fromIndex + size, messages.size());
            List<MessageResponse> items = messages.subList(fromIndex, toIndex).stream()
                    .map(MessageResponse::from)
                    .toList();

            int totalElements = messages.size();
            int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("items", items);
            response.put("page", page);
            response.put("size", size);
            response.put("totalElements", totalElements);
            response.put("totalPages", totalPages);
            response.put("sort", "asc".equalsIgnoreCase(sort) ? "asc" : "desc");
            response.put("readFilter", read);
            return ResponseEntity.ok(response);
        } catch (UnauthorizedException exception) {
            return error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", exception.getMessage());
        } catch (ForbiddenException exception) {
            return error(HttpStatus.FORBIDDEN, "FORBIDDEN", exception.getMessage());
        } catch (BadRequestException exception) {
            return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", exception.getMessage());
        }
    }

    @Operation(summary = "Marquer un message comme lu", description = "Autorise le destinataire a marquer un message comme lu")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Message marque comme lu"),
            @ApiResponse(responseCode = "401", description = "Non authentifie"),
            @ApiResponse(responseCode = "403", description = "Acces refuse"),
            @ApiResponse(responseCode = "404", description = "Message introuvable")
    })
    @PutMapping("/{id}/read")
    @Transactional
    public ResponseEntity<?> markAsRead(@PathVariable UUID id, Principal principal) {
        try {
            User currentUser = resolveAuthenticatedUser(principal);
            ensureMessagingAllowed(currentUser);

            Optional<Message> optionalMessage = messageRepository.findByIdWithUsersAndOffer(id);
            if (optionalMessage.isEmpty()) {
                return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "Message introuvable");
            }

            Message message = optionalMessage.get();
            if (message.getReceiver() == null || !currentUser.getId().equals(message.getReceiver().getId())) {
                return error(HttpStatus.FORBIDDEN, "FORBIDDEN", "Seul le destinataire peut marquer ce message comme lu");
            }

            message.setRead(true);
            Message saved = messageRepository.save(message);
            LOGGER.info("Message lu id={} receiverId={}", saved.getId(), currentUser.getId());
            return ResponseEntity.ok(MessageResponse.from(saved));
        } catch (UnauthorizedException exception) {
            return error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", exception.getMessage());
        } catch (ForbiddenException exception) {
            return error(HttpStatus.FORBIDDEN, "FORBIDDEN", exception.getMessage());
        }
    }

    @Operation(summary = "Supprimer un message", description = "Suppression d'un message uniquement par son expediteur")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Message supprime"),
            @ApiResponse(responseCode = "401", description = "Non authentifie"),
            @ApiResponse(responseCode = "403", description = "Acces refuse"),
            @ApiResponse(responseCode = "404", description = "Message introuvable")
    })
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteMessage(@PathVariable UUID id, Principal principal) {
        try {
            User currentUser = resolveAuthenticatedUser(principal);
            Optional<Message> optionalMessage = messageRepository.findByIdWithUsersAndOffer(id);
            if (optionalMessage.isEmpty()) {
                return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "Message introuvable");
            }

            Message message = optionalMessage.get();
            if (message.getSender() == null || !currentUser.getId().equals(message.getSender().getId())) {
                return error(HttpStatus.FORBIDDEN, "FORBIDDEN", "Vous ne pouvez supprimer que vos propres messages");
            }

            int deleted = messageRepository.deleteByIdAndSenderId(id, currentUser.getId());
            if (deleted == 0) {
                return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "Message introuvable");
            }

            LOGGER.info("Message supprime id={} senderId={}", id, currentUser.getId());
            return ResponseEntity.ok(Map.of("message", "Message supprime avec succes"));
        } catch (UnauthorizedException exception) {
            return error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", exception.getMessage());
        }
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity
                .status(status)
                .body(ApiErrorResponse.of(status.value(), code, message, "/api/messages"));
    }

    private User resolveAuthenticatedUser(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new UnauthorizedException("Non authentifie");
        }
        UUID userId = UUID.fromString(principal.getName());
        return userRepository.findByIdWithPack(userId)
                .orElseThrow(() -> new UnauthorizedException("Utilisateur authentifie introuvable"));
    }

    private User resolveTargetUser(UUID receiverOrProfileId) {
        if (receiverOrProfileId == null) {
            throw new BadRequestException("Le champ receiverId est obligatoire");
        }

        Optional<User> userById = userRepository.findByIdWithPack(receiverOrProfileId);
        if (userById.isPresent()) {
            return userById.get();
        }

        Optional<Profile> profile = profileRepository.findByIdWithUser(receiverOrProfileId);
        if (profile.isPresent() && profile.get().getUser() != null && profile.get().getUser().getId() != null) {
            return userRepository.findByIdWithPack(profile.get().getUser().getId())
                    .orElseThrow(() -> new BadRequestException("Destinataire introuvable"));
        }

        throw new BadRequestException("Destinataire introuvable");
    }

    private void ensureMessagingAllowed(User user) {
        if (!PackRules.canUseMessaging(user)) {
            throw new ForbiddenException("Votre pack actuel ne permet pas d'utiliser la messagerie");
        }
    }

    private boolean isMutualMatch(User sender, User receiver) {
        if (sender == null || receiver == null || sender.getRole() == null || receiver.getRole() == null) {
            return false;
        }
        if (sender.getRole() == UserRole.ADMIN || receiver.getRole() == UserRole.ADMIN) {
            return true;
        }

        if (isCandidateRole(sender.getRole()) && isRecruiterRole(receiver.getRole())) {
            return swipeRepository.existsCandidateLikeOnOwnerOffers(sender.getId(), receiver.getId(), SwipeAction.LIKE)
                    && profileSwipeRepository.existsBySwiperIdAndTargetUserIdAndAction(receiver.getId(), sender.getId(), SwipeAction.LIKE);
        }

        if (isRecruiterRole(sender.getRole()) && isCandidateRole(receiver.getRole())) {
            return swipeRepository.existsCandidateLikeOnOwnerOffers(receiver.getId(), sender.getId(), SwipeAction.LIKE)
                    && profileSwipeRepository.existsBySwiperIdAndTargetUserIdAndAction(sender.getId(), receiver.getId(), SwipeAction.LIKE);
        }

        return false;
    }

    private boolean isCandidateRole(UserRole role) {
        return role == UserRole.ETUDIANT || role == UserRole.LYCEEN;
    }

    private boolean isRecruiterRole(UserRole role) {
        return role == UserRole.ECOLE || role == UserRole.ENTREPRISE;
    }

    private Offer resolveOffer(UUID relatedOfferId) {
        if (relatedOfferId == null) {
            return null;
        }
        return offerRepository.findById(relatedOfferId)
                .orElseThrow(() -> new BadRequestException("Offre liee introuvable"));
    }

    private String normalizeContent(String content) {
        if (content == null || content.isBlank()) {
            throw new BadRequestException("Le contenu du message est obligatoire");
        }
        String normalized = content.trim();
        if (normalized.length() > MAX_CONTENT_LENGTH) {
            throw new BadRequestException("Le contenu du message ne doit pas depasser 2000 caracteres");
        }
        return normalized;
    }

    private static class BadRequestException extends RuntimeException {
        private BadRequestException(String message) {
            super(message);
        }
    }

    private static class ForbiddenException extends RuntimeException {
        private ForbiddenException(String message) {
            super(message);
        }
    }

    private static class UnauthorizedException extends RuntimeException {
        private UnauthorizedException(String message) {
            super(message);
        }
    }
}
