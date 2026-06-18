package com.rezo.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rezo.entities.Message;
import com.rezo.entities.Pack;
import com.rezo.entities.User;
import com.rezo.entities.enums.UserRole;
import com.rezo.repositories.MessageRepository;
import com.rezo.repositories.OfferRepository;
import com.rezo.repositories.ProfileRepository;
import com.rezo.repositories.ProfileSwipeRepository;
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
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MessageControllerOwnershipTest {

    @Mock
    private MessageRepository messageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OfferRepository offerRepository;
    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private SwipeRepository swipeRepository;
    @Mock
    private ProfileSwipeRepository profileSwipeRepository;

    private MockMvc mockMvc;

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
    }

    @Test
    void shouldReturn403WhenNonReceiverMarksMessageAsRead() throws Exception {
        UUID currentUserId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        User currentUser = buildUser(currentUserId, UserRole.ETUDIANT);
        User sender = buildUser(senderId, UserRole.ETUDIANT);
        User receiver = buildUser(receiverId, UserRole.ETUDIANT);
        Message message = buildMessage(messageId, sender, receiver);

        when(userRepository.findByIdWithPack(eq(currentUserId))).thenReturn(Optional.of(currentUser));
        when(messageRepository.findByIdWithUsersAndOffer(eq(messageId))).thenReturn(Optional.of(message));

        mockMvc.perform(put("/api/messages/{id}/read", messageId)
                        .principal(() -> currentUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.message").value("Seul le destinataire peut marquer ce message comme lu"));

        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void shouldReturn403WhenNonSenderDeletesMessage() throws Exception {
        UUID currentUserId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        User currentUser = buildUser(currentUserId, UserRole.ETUDIANT);
        User sender = buildUser(senderId, UserRole.ETUDIANT);
        User receiver = buildUser(receiverId, UserRole.ETUDIANT);
        Message message = buildMessage(messageId, sender, receiver);

        when(userRepository.findByIdWithPack(eq(currentUserId))).thenReturn(Optional.of(currentUser));
        when(messageRepository.findByIdWithUsersAndOffer(eq(messageId))).thenReturn(Optional.of(message));

        mockMvc.perform(delete("/api/messages/{id}", messageId)
                        .principal(() -> currentUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.message").value("Vous ne pouvez supprimer que vos propres messages"));

        verify(messageRepository, never()).deleteByIdAndSenderId(any(UUID.class), any(UUID.class));
    }

    @Test
    void shouldDeleteMessageWhenCurrentUserIsSender() throws Exception {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        User sender = buildUser(senderId, UserRole.ETUDIANT);
        User receiver = buildUser(receiverId, UserRole.ETUDIANT);
        Message message = buildMessage(messageId, sender, receiver);

        when(userRepository.findByIdWithPack(eq(senderId))).thenReturn(Optional.of(sender));
        when(messageRepository.findByIdWithUsersAndOffer(eq(messageId))).thenReturn(Optional.of(message));
        when(messageRepository.deleteByIdAndSenderId(eq(messageId), eq(senderId))).thenReturn(1);

        mockMvc.perform(delete("/api/messages/{id}", messageId)
                        .principal(() -> senderId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Message supprime avec succes"));
    }

    private User buildUser(UUID id, UserRole role) {
        Pack pack = new Pack();
        pack.setNom("PREMIUM");
        pack.setFeatures("MESSAGERIE_LIMITEE,AI_CHAT_ACCESS");

        User user = new User();
        user.setEmail(id + "@rezo.sn");
        user.setPasswordHash("hash");
        user.setPrenom("User");
        user.setNom("Test");
        user.setRole(role);
        user.setPack(pack);
        setPrivateField(user, "id", id);
        return user;
    }

    private Message buildMessage(UUID id, User sender, User receiver) {
        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent("hello");
        message.setRead(false);
        setPrivateField(message, "id", id);
        setPrivateField(message, "createdAt", LocalDateTime.now());
        return message;
    }

    private void setPrivateField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Impossible d'initialiser le champ " + fieldName, exception);
        }
    }
}
