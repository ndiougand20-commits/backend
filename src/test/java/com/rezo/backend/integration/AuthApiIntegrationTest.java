package com.rezo.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.rezo.entities.Pack;
import com.rezo.repositories.PackRepository;
import com.rezo.repositories.RefreshTokenRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class AuthApiIntegrationTest {

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
    private PackRepository packRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        packRepository.findByNomIgnoreCase("FREE").orElseGet(() -> {
            Pack pack = new Pack();
            pack.setNom("FREE");
            pack.setDescription("Pack gratuit de test");
            pack.setPrix(BigDecimal.ZERO);
            pack.setCible("TOUS");
            pack.setFeatures("MATCHING_BASIC,MESSAGERIE_LIMITEE");
            return packRepository.save(pack);
        });
    }

    @Test
    void shouldRunAuthFlowSignupLoginRefreshAndAccessProtectedEndpoint() throws Exception {
        String signupPayload = """
                {
                  \"email\": \"integration.etudiant@rezo.test\",
                  \"password\": \"Password123!\",
                  \"role\": \"ETUDIANT\",
                  \"prenom\": \"Integration\",
                  \"nom\": \"Student\",
                  \"profil\": {
                    \"niveauEtude\": \"Licence 3\",
                    \"domaine\": \"Informatique\",
                    \"competences\": [\"Java\", \"Spring\"],
                    \"preferencesSecteur\": [\"IT\"]
                  }
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("integration.etudiant@rezo.test"))
                .andExpect(jsonPath("$.role").value("ETUDIANT"));

        String loginPayload = """
                {
                  \"email\": \"integration.etudiant@rezo.test\",
                  \"password\": \"Password123!\"
                }
                """;

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode loginJson = objectMapper.readTree(loginResponse);
        String refreshToken = loginJson.get("refreshToken").asText();

        String refreshPayload = """
                {
                  \"refreshToken\": \"%s\"
                }
                """.formatted(refreshToken);

        String refreshResponse = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode refreshJson = objectMapper.readTree(refreshResponse);
        String rotatedRefreshToken = refreshJson.get("refreshToken").asText();

        assertThat(refreshTokenRepository.findByTokenAndRevokedFalse(refreshToken)).isEmpty();
        assertThat(refreshTokenRepository.findByTokenAndRevokedFalse(rotatedRefreshToken)).isPresent();

        String oldRefreshPayload = """
                {
                  \"refreshToken\": \"%s\"
                }
                """.formatted(refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(oldRefreshPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"refreshToken\": \"%s\"
                                }
                                """.formatted(rotatedRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.refreshToken").isString());
    }
}
