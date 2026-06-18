package com.rezo.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rezo.entities.Pack;
import com.rezo.repositories.PackRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class SecurityApiIntegrationTest {

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
    void shouldReturn401WithoutTokenAnd403WhenAuthenticatedButDevEndpointDisabled() throws Exception {
        mockMvc.perform(get("/api/messages"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        String email = "integration.security." + UUID.randomUUID() + "@rezo.test";

        String signupPayload = """
                {
                  \"email\": \"%s\",
                  \"password\": \"Password123!\",
                  \"role\": \"ETUDIANT\",
                  \"prenom\": \"Integration\",
                  \"nom\": \"Security\",
                  \"profil\": {
                    \"niveauEtude\": \"Licence 3\",
                    \"domaine\": \"Informatique\"
                  }
                }
                """.formatted(email);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupPayload))
                .andExpect(status().isCreated());

        String loginPayload = """
                {
                  \"email\": \"%s\",
                  \"password\": \"Password123!\"
                }
                """.formatted(email);

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode loginJson = objectMapper.readTree(loginResponse);
        String accessToken = normalizeAccessToken(loginJson.get("token").asText());

        mockMvc.perform(delete("/api/auth/users")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

      private String normalizeAccessToken(String token) {
        if (token == null) {
          return null;
        }
        String trimmed = token.trim();
        if (trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
          return trimmed.substring(7).trim();
        }
        return trimmed;
          }
}
