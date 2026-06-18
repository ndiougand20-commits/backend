package com.rezo.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rezo.entities.User;
import com.rezo.entities.enums.UserRole;
import com.rezo.repositories.PackRepository;
import com.rezo.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class PackApiIntegrationTest {

    @TestConfiguration
    static class JacksonTestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private PackRepository packRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void shouldReturn401OnProtectedPackEndpointsWithoutAuthentication() throws Exception {
        String payload = """
                {
                  "nom": "PACK_SANS_AUTH",
                  "description": "pack test",
                  "prix": 0,
                  "cible": "TOUS",
                  "features": ["MATCHING_BASIC"]
                }
                """;

        String randomPackId = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/packs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Non authentifie"));

        mockMvc.perform(put("/api/packs/{id}", randomPackId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Non authentifie"));

        mockMvc.perform(delete("/api/packs/{id}", randomPackId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Non authentifie"));
    }

    @Test
    void shouldAllowAdminAndRejectNonAdminOnPackManagement() throws Exception {
        String studentEmail = "integration.pack.student." + UUID.randomUUID() + "@rezo.test";
                UUID adminId = createAdminUser();
                UUID studentId = signupStudentAndGetUserId(studentEmail);

        String packName = "PACK_INT_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        String createPayload = """
                {
                  "nom": "%s",
                  "description": "pack integration",
                  "prix": 19.99,
                  "cible": "TOUS",
                  "features": ["MATCHING_BASIC", "MESSAGERIE_LIMITEE"]
                }
                """.formatted(packName);

        mockMvc.perform(post("/api/packs")
                        .principal(studentId::toString)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Seul un administrateur peut gerer les packs"));

        String createResponse = mockMvc.perform(post("/api/packs")
                        .principal(adminId::toString)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nom").value(packName))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String createdPackId = objectMapper.readTree(createResponse).get("id").asText();

        mockMvc.perform(delete("/api/packs/{id}", createdPackId)
                                                .principal(adminId::toString))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Pack supprime avec succes"));
    }

        private UUID signupStudentAndGetUserId(String email) throws Exception {
        String signupPayload = """
                {
                  "email": "%s",
                  "password": "Password123!",
                                  "role": "ETUDIANT",
                                  "prenom": "Student",
                                  "nom": "User",
                                  "profil": {"niveauEtude":"Licence 3","domaine":"Informatique"}
                }
                                """.formatted(email);

                String response = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                                .andExpect(jsonPath("$.role").value("ETUDIANT"))
                .andReturn()
                .getResponse()
                .getContentAsString();

                JsonNode json = objectMapper.readTree(response);
                return UUID.fromString(json.get("userId").asText());
    }

        private UUID createAdminUser() {
                User admin = new User();
                admin.setEmail("integration.pack.admin." + UUID.randomUUID() + "@rezo.test");
                admin.setPasswordHash("not-used");
                admin.setRole(UserRole.ADMIN);
                admin.setPrenom("Admin");
                admin.setNom("System");
                admin.setPack(packRepository.findByNomIgnoreCase("FREE")
                                .orElseThrow(() -> new IllegalStateException("Pack FREE introuvable")));
                return userRepository.save(admin).getId();
    }
}
