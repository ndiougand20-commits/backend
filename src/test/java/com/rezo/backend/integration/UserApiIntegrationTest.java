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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class UserApiIntegrationTest {

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
    void shouldReturn401OnProtectedUserEndpointsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Non authentifie"));

        mockMvc.perform(get("/api/users/me/stats"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Non authentifie"));

        String updatePayload = """
                {
                  "prenom": "Updated",
                  "nom": "User"
                }
                """;

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Non authentifie"));

        String packUpdatePayload = """
                {
                  "packId": "%s"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(put("/api/users/me/pack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(packUpdatePayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Non authentifie"));
    }
}
