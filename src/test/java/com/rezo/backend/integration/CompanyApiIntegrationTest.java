package com.rezo.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rezo.entities.Company;
import com.rezo.entities.User;
import com.rezo.repositories.CompanyRepository;
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
class CompanyApiIntegrationTest {

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
        private CompanyRepository companyRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void shouldReturn401OnProtectedCompanyEndpointsWithoutAuthentication() throws Exception {
        String createPayload = """
                {
                  "raisonSociale": "Rezo Sans Auth",
                  "secteurActivite": "IT",
                  "taille": "PME",
                  "description": "Entreprise de test"
                }
                """;

        String randomCompanyId = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Non authentifie"));

        mockMvc.perform(put("/api/companies/{id}", randomCompanyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Non authentifie"));

        mockMvc.perform(delete("/api/companies/{id}", randomCompanyId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Non authentifie"));
    }

    @Test
    void shouldEnforceOwnershipWhenUpdatingAndDeletingCompany() throws Exception {
        signupEntreprise("integration.company.owner@rezo.test");
        signupEntreprise("integration.company.other@rezo.test");

        UUID ownerUserId = findUserIdByEmail("integration.company.owner@rezo.test");
        UUID otherUserId = findUserIdByEmail("integration.company.other@rezo.test");
        Company ownerCompany = companyRepository.findByUserId(ownerUserId)
                .orElseThrow(() -> new IllegalStateException("Fiche entreprise introuvable pour " + ownerUserId));
        String companyId = ownerCompany.getId().toString();

        String updatePayload = """
                {
                  "description": "Description modifiee"
                }
                """;

        mockMvc.perform(put("/api/companies/{id}", companyId)
                        .principal(otherUserId::toString)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Vous ne pouvez modifier que votre propre fiche entreprise"));

        mockMvc.perform(delete("/api/companies/{id}", companyId)
                        .principal(otherUserId::toString))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Vous ne pouvez supprimer que votre propre fiche entreprise"));

        mockMvc.perform(delete("/api/companies/{id}", companyId)
                        .principal(ownerUserId::toString))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Entreprise supprimee avec succes"));
    }

    private void signupEntreprise(String email) throws Exception {
        String signupPayload = """
                {
                  "email": "%s",
                  "password": "Password123!",
                  "role": "ENTREPRISE",
                  "prenom": "Company",
                  "nom": "Integration",
                  "profil": {
                    "raisonSociale": "Rezo Integration Corp",
                    "secteurActivite": "IT",
                    "taille": "PME",
                    "description": "Entreprise de test"
                  }
                }
                """.formatted(email);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("ENTREPRISE"));

        assertThat(userRepository.findByEmail(email)).isPresent();
    }

    private UUID findUserIdByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable pour " + email));
        return user.getId();
    }
}
