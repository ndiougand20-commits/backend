package com.rezo.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rezo.entities.School;
import com.rezo.entities.User;
import com.rezo.repositories.SchoolRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class SchoolApiIntegrationTest {

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
    private UserRepository userRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void shouldReturn401OnProtectedSchoolEndpointsWithoutAuthentication() throws Exception {
        String createPayload = """
                {
                  "nomEtablissement": "Rezo School Sans Auth",
                  "statut": "PUBLIC",
                  "domaines": ["Informatique"]
                }
                """;

        String randomSchoolId = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/schools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Non authentifie"));

        mockMvc.perform(put("/api/schools/{id}", randomSchoolId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Non authentifie"));

        mockMvc.perform(delete("/api/schools/{id}", randomSchoolId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Non authentifie"));
    }

    @Test
    void shouldEnforceOwnershipWhenUpdatingAndDeletingSchool() throws Exception {
        signupEcole("integration.school.owner@rezo.test");
        signupEcole("integration.school.other@rezo.test");

        UUID ownerUserId = findUserIdByEmail("integration.school.owner@rezo.test");
        UUID otherUserId = findUserIdByEmail("integration.school.other@rezo.test");

        School ownerSchool = schoolRepository.findByUserId(ownerUserId)
                .orElseThrow(() -> new IllegalStateException("Fiche ecole introuvable pour " + ownerUserId));
        String schoolId = ownerSchool.getId().toString();

        String updatePayload = """
                {
                  "description": "Description ecole modifiee"
                }
                """;

        mockMvc.perform(put("/api/schools/{id}", schoolId)
                        .principal(otherUserId::toString)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Vous ne pouvez modifier que votre propre fiche ecole"));

        mockMvc.perform(delete("/api/schools/{id}", schoolId)
                        .principal(otherUserId::toString))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Vous ne pouvez supprimer que votre propre fiche ecole"));

        mockMvc.perform(delete("/api/schools/{id}", schoolId)
                        .principal(ownerUserId::toString))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Ecole supprimee avec succes"));
    }

    private void signupEcole(String email) throws Exception {
        String signupPayload = """
                {
                  "email": "%s",
                  "password": "Password123!",
                  "role": "ECOLE",
                  "prenom": "School",
                  "nom": "Integration",
                  "profil": {
                    "nomEtablissement": "Ecole Integration",
                    "statut": "PUBLIC",
                    "domaines": ["Informatique"],
                    "diplomesDelivres": ["Licence"]
                  }
                }
                """.formatted(email);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("ECOLE"));

        assertThat(userRepository.findByEmail(email)).isPresent();
    }

    private UUID findUserIdByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable pour " + email));
        return user.getId();
    }
}
