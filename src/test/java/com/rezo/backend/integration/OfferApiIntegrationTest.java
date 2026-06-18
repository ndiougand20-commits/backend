package com.rezo.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rezo.entities.Pack;
import com.rezo.entities.User;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class OfferApiIntegrationTest {

    @TestConfiguration
    static class JacksonTestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }

    private static final String OWNER_EMAIL = "integration.owner@rezo.test";
    private static final String OTHER_EMAIL = "integration.other@rezo.test";
    private static final String PASSWORD = "Password123!";

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PackRepository packRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void shouldReturn401OnProtectedOfferEndpointsWithoutAuthentication() throws Exception {
        String createOfferPayload = """
                {
                  "titre": "Stage Sans Auth",
                  "description": "Offre test securite",
                  "type": "STAGE",
                  "domaine": "Informatique",
                  "location": "Dakar"
                }
                """;

        String randomOfferId = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/offers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createOfferPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        mockMvc.perform(delete("/api/offers/{id}", randomOfferId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/offers/{id}/liked-by", randomOfferId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldCreateOfferAndEnforceOwnershipOnOfferEndpoints() throws Exception {
        ensurePackForOfferManagement();

        signupEntreprise(OWNER_EMAIL);
        signupEntreprise(OTHER_EMAIL);
        upgradeUserPackForOffers(OWNER_EMAIL);

        UUID ownerUserId = findUserIdByEmail(OWNER_EMAIL);
        UUID otherUserId = findUserIdByEmail(OTHER_EMAIL);

        String createOfferPayload = """
                {
                  \"titre\": \"Stage Backend Integration\",
                  \"description\": \"Offre de test integration\",
                  \"type\": \"STAGE\",
                  \"domaine\": \"Informatique\",
                  \"location\": \"Dakar\",
                  \"competencesRequises\": [\"Java\", \"Spring\"],
                  \"datePublication\": \"2026-06-01T10:00:00\",
                  \"dateDebut\": \"2026-07-01T10:00:00\",
                  \"dateFin\": \"2026-09-01T10:00:00\"
                }
                """;

        String createResponse = mockMvc.perform(post("/api/offers")
                        .principal(ownerUserId::toString)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createOfferPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerType").value("ENTREPRISE"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String offerId = objectMapper.readTree(createResponse).get("id").asText();

        mockMvc.perform(get("/api/offers/{id}/liked-by", offerId)
                        .principal(otherUserId::toString))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.message").value("Acces reserve au proprietaire de l'offre"));

        mockMvc.perform(delete("/api/offers/{id}", offerId)
                        .principal(otherUserId::toString))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.message").value("Vous ne pouvez supprimer que votre propre offre"));

        mockMvc.perform(delete("/api/offers/{id}", offerId)
                        .principal(ownerUserId::toString))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Offre supprimee avec succes"));
    }

    private void ensurePackForOfferManagement() {
        packRepository.findByNomIgnoreCase("BUSINESS_INTEGRATION")
                .orElseGet(() -> {
                    Pack pack = new Pack();
                    pack.setNom("BUSINESS_INTEGRATION");
                    pack.setDescription("Pack integration test");
                    pack.setPrix(BigDecimal.ZERO);
                    pack.setCible("ENTREPRISE,ECOLE");
                    pack.setFeatures("OFFERS_PUBLISH,OFFERS_MANAGE,MESSAGERIE_LIMITEE");
                    return packRepository.save(pack);
                });
    }

    private void upgradeUserPackForOffers(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable pour " + email));
        Pack pack = packRepository.findByNomIgnoreCase("BUSINESS_INTEGRATION")
                .orElseThrow(() -> new IllegalStateException("Pack BUSINESS_INTEGRATION introuvable"));
        user.setPack(pack);
        userRepository.save(user);
    }

    private void signupEntreprise(String email) throws Exception {
        String signupPayload = """
                {
                  \"email\": \"%s\",
                  \"password\": \"%s\",
                  \"role\": \"ENTREPRISE\",
                  \"prenom\": \"Owner\",
                  \"nom\": \"Integration\",
                  \"profil\": {
                    \"raisonSociale\": \"Rezo Integration Corp\",
                    \"secteurActivite\": \"IT\",
                    \"taille\": \"PME\",
                    \"description\": \"Entreprise de test\"
                  }
                }
                """.formatted(email, PASSWORD);

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
