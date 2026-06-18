package com.rezo.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class MatchApiIntegrationTest {

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

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void shouldReturn401OnProtectedMatchEndpointsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/match/recommendations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Non authentifie"));

        mockMvc.perform(get("/api/match/school-recommendations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Non authentifie"));

        mockMvc.perform(get("/api/match/profile-recommendations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Non authentifie"));

        mockMvc.perform(get("/api/match/mutual"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Non authentifie"));

        String swipePayload = """
                {
                  "offerId": "%s",
                  "action": "LIKE"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/match/swipe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(swipePayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Non authentifie"));

        String profileSwipePayload = """
                {
                  "targetUserId": "%s",
                  "action": "LIKE"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/match/profile-swipe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileSwipePayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Non authentifie"));
    }
}
