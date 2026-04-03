package com.rezo.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rezo.entities.Pack;
import com.rezo.entities.User;
import com.rezo.entities.enums.UserRole;
import com.rezo.repositories.PackRepository;
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
import java.math.BigDecimal;
import java.security.Principal;
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
class PackControllerTest {

    @Mock private PackRepository packRepository;
    @Mock private UserRepository userRepository;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private final UUID adminId = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private final UUID userId = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private final UUID packId = UUID.fromString("30000000-0000-0000-0000-000000000003");

    @BeforeEach
    void setUp() {
        PackController controller = new PackController(packRepository, userRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    void listPacksShouldBePublic() throws Exception {
        Pack pack = buildPack("FREE", "TOUS", "MATCHING_BASIC,MESSAGERIE_LIMITEE");
        setField(pack, "id", packId);
        when(packRepository.findAll()).thenReturn(List.of(pack));

        mockMvc.perform(get("/api/packs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nom").value("FREE"))
                .andExpect(jsonPath("$[0].features[0]").value("MATCHING_BASIC"));
    }

    @Test
    void createPackShouldWorkForAdmin() throws Exception {
        when(userRepository.findById(adminId)).thenReturn(Optional.of(buildUser(adminId, UserRole.ADMIN)));
        when(packRepository.findByNomIgnoreCase("BUSINESS_PLUS")).thenReturn(Optional.empty());
        when(packRepository.save(any(Pack.class))).thenAnswer(invocation -> {
            Pack pack = invocation.getArgument(0);
            setField(pack, "id", packId);
            return pack;
        });

        String body = """
                {
                  "nom": "BUSINESS_PLUS",
                  "description": "Pack business",
                  "prix": 29.99,
                  "cible": "ENTREPRISE,ECOLE",
                  "features": ["OFFERS_PUBLISH", "MESSAGERIE_ILLIMITEE", "AI_CHAT_ACCESS"]
                }
                """;

        mockMvc.perform(post("/api/packs")
                        .principal(principal(adminId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nom").value("BUSINESS_PLUS"))
                .andExpect(jsonPath("$.features[0]").value("OFFERS_PUBLISH"));
    }

    @Test
    void createPackShouldReturn403ForNonAdmin() throws Exception {
        when(userRepository.findById(userId)).thenReturn(Optional.of(buildUser(userId, UserRole.ETUDIANT)));

        mockMvc.perform(post("/api/packs")
                        .principal(principal(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"PREMIUM\",\"prix\":9.99,\"cible\":\"TOUS\",\"features\":[\"AI_CHAT_ACCESS\"]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Seul un administrateur peut gerer les packs"));
    }

    @Test
    void deletePackShouldReturn409WhenAssignedToUsers() throws Exception {
        Pack pack = buildPack("FREE", "TOUS", "MATCHING_BASIC");
        setField(pack, "id", packId);

        when(userRepository.findById(adminId)).thenReturn(Optional.of(buildUser(adminId, UserRole.ADMIN)));
        when(packRepository.findById(packId)).thenReturn(Optional.of(pack));
        when(packRepository.countUsersByPackId(packId)).thenReturn(2L);

        mockMvc.perform(delete("/api/packs/{id}", packId).principal(principal(adminId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Suppression impossible: ce pack est actuellement attribue a des utilisateurs"));
    }

    private Pack buildPack(String nom, String cible, String features) {
        Pack pack = new Pack();
        pack.setNom(nom);
        pack.setDescription("Description pack");
        pack.setPrix(new BigDecimal("9.99"));
        pack.setCible(cible);
        pack.setFeatures(features);
        return pack;
    }

    private User buildUser(UUID id, UserRole role) {
        User user = new User();
        setField(user, "id", id);
        user.setEmail("admin@rezo.com");
        user.setPrenom("Admin");
        user.setNom("User");
        user.setRole(role);
        return user;
    }

    private Principal principal(UUID id) {
        return id::toString;
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
