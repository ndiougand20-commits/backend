package com.rezo.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rezo.entities.Message;
import com.rezo.entities.Offer;
import com.rezo.entities.Pack;
import com.rezo.entities.User;
import com.rezo.entities.enums.UserRole;
import com.rezo.repositories.MessageRepository;
import com.rezo.repositories.OfferRepository;
import com.rezo.repositories.ProfileSwipeRepository;
import com.rezo.repositories.ProfileRepository;
import com.rezo.repositories.SwipeRepository;
import com.rezo.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Field;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MessageControllerTest {

    @Mock private MessageRepository messageRepository;
    @Mock private UserRepository userRepository;
    @Mock private OfferRepository offerRepository;
    @Mock private ProfileRepository profileRepository;
    @Mock private SwipeRepository swipeRepository;
    @Mock private ProfileSwipeRepository profileSwipeRepository;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private final UUID senderId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID receiverId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private final UUID offerId = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private final UUID messageId = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @BeforeEach
    void setUp() {
        MessageController controller = new MessageController(
                messageRepository,
                userRepository,
                offerRepository,
                profileRepository,
                swipeRepository,
                profileSwipeRepository
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    void sendMessageShouldCreateMessageForAuthenticatedUser() throws Exception {
        User sender = buildUser(senderId, UserRole.ETUDIANT, "PREMIUM", "MESSAGERIE_ILLIMITEE");
        User receiver = buildUser(receiverId, UserRole.ENTREPRISE, "BUSINESS", "MESSAGERIE_ILLIMITEE");
        Offer offer = new Offer();
        setField(offer, "id", offerId);

        when(userRepository.findByIdWithPack(senderId)).thenReturn(Optional.of(sender));
        when(userRepository.findByIdWithPack(receiverId)).thenReturn(Optional.of(receiver));
        when(offerRepository.findById(offerId)).thenReturn(Optional.of(offer));
        when(swipeRepository.existsCandidateLikeOnOwnerOffers(senderId, receiverId, com.rezo.entities.enums.SwipeAction.LIKE)).thenReturn(true);
        when(profileSwipeRepository.existsBySwiperIdAndTargetUserIdAndAction(receiverId, senderId, com.rezo.entities.enums.SwipeAction.LIKE)).thenReturn(true);
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message message = invocation.getArgument(0);
            setField(message, "id", messageId);
            setField(message, "createdAt", LocalDateTime.of(2026, 4, 8, 10, 30));
            return message;
        });

        mockMvc.perform(post("/api/messages")
                        .principal(principal(senderId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverId": "%s",
                                  "content": "Bonjour, je suis interesse par votre offre",
                                  "relatedOfferId": "%s"
                                }
                                """.formatted(receiverId, offerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(messageId.toString()))
                .andExpect(jsonPath("$.senderId").value(senderId.toString()))
                .andExpect(jsonPath("$.receiverId").value(receiverId.toString()))
                .andExpect(jsonPath("$.relatedOfferId").value(offerId.toString()))
                .andExpect(jsonPath("$.isRead").value(false));
    }

    @Test
    void sendMessageShouldRejectSelfMessage() throws Exception {
        User sender = buildUser(senderId, UserRole.ETUDIANT, "PREMIUM", "MESSAGERIE_ILLIMITEE");
        when(userRepository.findByIdWithPack(senderId)).thenReturn(Optional.of(sender));

        mockMvc.perform(post("/api/messages")
                        .principal(principal(senderId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverId": "%s",
                                  "content": "Bonjour moi-meme"
                                }
                                """.formatted(senderId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("Vous ne pouvez pas vous envoyer un message a vous-meme"));
    }

    @Test
    void sendMessageShouldReturn403WhenPackDoesNotAllowMessaging() throws Exception {
        User sender = buildUser(senderId, UserRole.ETUDIANT, "FREE", "MATCHING_BASIC");
        when(userRepository.findByIdWithPack(senderId)).thenReturn(Optional.of(sender));

        mockMvc.perform(post("/api/messages")
                        .principal(principal(senderId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverId": "%s",
                                  "content": "Bonjour"
                                }
                                """.formatted(receiverId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.message").value("Votre pack actuel ne permet pas d'utiliser la messagerie"));
    }

    @Test
    void getConversationShouldReturnMessagesBetweenTwoUsers() throws Exception {
        User current = buildUser(senderId, UserRole.ETUDIANT, "PREMIUM", "MESSAGERIE_ILLIMITEE");
        User other = buildUser(receiverId, UserRole.ENTREPRISE, "BUSINESS", "MESSAGERIE_ILLIMITEE");

        when(userRepository.findByIdWithPack(senderId)).thenReturn(Optional.of(current));
        when(userRepository.findByIdWithPack(receiverId)).thenReturn(Optional.of(other));
        when(messageRepository.findConversation(senderId, receiverId)).thenReturn(List.of(
                buildMessage(messageId, current, other, false, "Bonjour", LocalDateTime.of(2026, 4, 8, 9, 0)),
                buildMessage(UUID.fromString("55555555-5555-5555-5555-555555555555"), other, current, true, "Rebonjour", LocalDateTime.of(2026, 4, 8, 9, 5))
        ));

        mockMvc.perform(get("/api/messages/conversation/{userId}", receiverId)
                        .principal(principal(senderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Bonjour"))
                .andExpect(jsonPath("$[1].content").value("Rebonjour"));
    }

    @Test
    void listMessagesShouldSupportReadFilter() throws Exception {
        User current = buildUser(senderId, UserRole.ETUDIANT, "PREMIUM", "MESSAGERIE_ILLIMITEE");
        User other = buildUser(receiverId, UserRole.ENTREPRISE, "BUSINESS", "MESSAGERIE_ILLIMITEE");

        when(userRepository.findByIdWithPack(senderId)).thenReturn(Optional.of(current));
        when(messageRepository.findAllForUser(senderId, false)).thenReturn(List.of(
                buildMessage(messageId, other, current, false, "Message non lu", LocalDateTime.of(2026, 4, 8, 11, 0))
        ));

        mockMvc.perform(get("/api/messages")
                        .param("read", "false")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "asc")
                        .principal(principal(senderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].content").value("Message non lu"))
                .andExpect(jsonPath("$.items[0].isRead").value(false))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void markReadShouldWorkForReceiver() throws Exception {
        User sender = buildUser(senderId, UserRole.ENTREPRISE, "BUSINESS", "MESSAGERIE_ILLIMITEE");
        User receiver = buildUser(receiverId, UserRole.ETUDIANT, "PREMIUM", "MESSAGERIE_ILLIMITEE");
        Message message = buildMessage(messageId, sender, receiver, false, "A lire", LocalDateTime.of(2026, 4, 8, 12, 0));

        when(userRepository.findByIdWithPack(receiverId)).thenReturn(Optional.of(receiver));
        when(messageRepository.findByIdWithUsersAndOffer(messageId)).thenReturn(Optional.of(message));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/messages/{id}/read", messageId)
                        .principal(principal(receiverId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(messageId.toString()))
                .andExpect(jsonPath("$.isRead").value(true));
    }

    @Test
    void deleteMessageShouldReturn403WhenUserIsNotSender() throws Exception {
        User sender = buildUser(senderId, UserRole.ENTREPRISE, "BUSINESS", "MESSAGERIE_ILLIMITEE");
        User receiver = buildUser(receiverId, UserRole.ETUDIANT, "PREMIUM", "MESSAGERIE_ILLIMITEE");
        Message message = buildMessage(messageId, sender, receiver, false, "Bonjour", LocalDateTime.of(2026, 4, 8, 12, 30));

        when(userRepository.findByIdWithPack(receiverId)).thenReturn(Optional.of(receiver));
        when(messageRepository.findByIdWithUsersAndOffer(messageId)).thenReturn(Optional.of(message));

        mockMvc.perform(delete("/api/messages/{id}", messageId)
                        .principal(principal(receiverId)))
                .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.message").value("Vous ne pouvez supprimer que vos propres messages"));
    }

    @Test
    void deleteMessageShouldWorkForSender() throws Exception {
        User sender = buildUser(senderId, UserRole.ENTREPRISE, "BUSINESS", "MESSAGERIE_ILLIMITEE");
        User receiver = buildUser(receiverId, UserRole.ETUDIANT, "PREMIUM", "MESSAGERIE_ILLIMITEE");
        Message message = buildMessage(messageId, sender, receiver, false, "Bonjour", LocalDateTime.of(2026, 4, 8, 12, 30));

        when(userRepository.findByIdWithPack(senderId)).thenReturn(Optional.of(sender));
        when(messageRepository.findByIdWithUsersAndOffer(messageId)).thenReturn(Optional.of(message));
        when(messageRepository.deleteByIdAndSenderId(messageId, senderId)).thenReturn(1);

        mockMvc.perform(delete("/api/messages/{id}", messageId)
                        .principal(principal(senderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Message supprime avec succes"));

        verify(messageRepository).deleteByIdAndSenderId(messageId, senderId);
    }

    @Test
    void listMessagesShouldReturn401WhenPrincipalIsMissing() throws Exception {
        mockMvc.perform(get("/api/messages"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.message").value("Non authentifie"));
    }

    private Principal principal(UUID userId) {
        return userId::toString;
    }

    private User buildUser(UUID userId, UserRole role, String packNom, String features) {
        Pack pack = new Pack();
        pack.setNom(packNom);
        pack.setFeatures(features);

        User user = new User();
        setField(user, "id", userId);
        user.setEmail("user-" + userId + "@rezo.com");
        user.setPrenom("Test");
        user.setNom("User");
        user.setRole(role);
        user.setPack(pack);
        return user;
    }

    private Message buildMessage(UUID id, User sender, User receiver, boolean isRead, String content, LocalDateTime createdAt) {
        Message message = new Message();
        setField(message, "id", id);
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(content);
        message.setRead(isRead);
        setField(message, "createdAt", createdAt);
        return message;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
