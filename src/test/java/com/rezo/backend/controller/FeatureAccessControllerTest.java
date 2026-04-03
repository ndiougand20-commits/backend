package com.rezo.backend.controller;

import com.rezo.entities.Pack;
import com.rezo.entities.User;
import com.rezo.entities.enums.UserRole;
import com.rezo.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Field;
import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FeatureAccessControllerTest {

    @Mock private UserRepository userRepository;

    private MockMvc mockMvc;
    private final UUID userId = UUID.fromString("50000000-0000-0000-0000-000000000005");

    @BeforeEach
    void setUp() {
        FeatureAccessController controller = new FeatureAccessController(userRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void messagingAccessShouldReturn403WhenPackIsInsufficient() throws Exception {
        when(userRepository.findByIdWithPack(userId)).thenReturn(Optional.of(buildUser("FREE", "MATCHING_BASIC")));

        mockMvc.perform(get("/api/features/messaging/access").principal(principal()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Votre pack actuel ne permet pas d'utiliser la messagerie"));
    }

    @Test
    void chatAiAccessShouldReturn200WhenPackAllowsIt() throws Exception {
        when(userRepository.findByIdWithPack(userId)).thenReturn(Optional.of(buildUser("PREMIUM", "AI_CHAT_ACCESS,MESSAGERIE_ILLIMITEE")));

        mockMvc.perform(get("/api/features/chat-ai/access").principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true))
                .andExpect(jsonPath("$.packNom").value("PREMIUM"));
    }

    private User buildUser(String packNom, String features) {
        Pack pack = new Pack();
        pack.setNom(packNom);
        pack.setFeatures(features);

        User user = new User();
        setField(user, "id", userId);
        user.setEmail("user@rezo.com");
        user.setPrenom("Test");
        user.setNom("User");
        user.setRole(UserRole.ETUDIANT);
        user.setPack(pack);
        return user;
    }

    private Principal principal() {
        return userId::toString;
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
