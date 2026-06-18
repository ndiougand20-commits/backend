package com.rezo.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class UserMediaApiIntegrationTest {

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
    void shouldReturn401OnProtectedUserMediaEndpointsWithoutAuthentication() throws Exception {
        MockMultipartFile photo = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1, 2, 3});
        MockMultipartFile justificatif = new MockMultipartFile("file", "cv.pdf", "application/pdf", new byte[]{1, 2, 3});
        String randomMediaId = UUID.randomUUID().toString();

        mockMvc.perform(get("/api/users/me/media"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Non authentifie"));

        mockMvc.perform(multipart("/api/users/me/media/photos").file(photo))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Non authentifie"));

        mockMvc.perform(multipart("/api/users/me/media/justificatifs")
                        .file(justificatif)
                        .param("category", "CV"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Non authentifie"));

        mockMvc.perform(delete("/api/users/me/media/{id}", randomMediaId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Non authentifie"));
    }

    @Test
    void shouldEnforceRoleCategoryAndOwnershipOnUserMedia() throws Exception {
        String studentEmail = "integration.media.student." + UUID.randomUUID() + "@rezo.test";
        String lyceenEmail = "integration.media.lyceen." + UUID.randomUUID() + "@rezo.test";

        String studentId = signupAndReturnUserId(studentEmail, "ETUDIANT",
                "{\"niveauEtude\":\"Licence 3\",\"domaine\":\"Informatique\"}");
        String lyceenId = signupAndReturnUserId(lyceenEmail, "LYCEEN",
                "{\"classeActuelle\":\"Terminale\",\"serieOrientation\":\"S\"}");

        MockMultipartFile pdf = new MockMultipartFile("file", "bulletin.pdf", "application/pdf", new byte[]{10, 20, 30});

        mockMvc.perform(multipart("/api/users/me/media/justificatifs")
                        .file(pdf)
                        .param("category", "BULLETIN")
                        .principal(studentId::toString))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Categorie non autorisee pour ce role"));

        MockMultipartFile photo = new MockMultipartFile("file", "avatar.png", "image/png", new byte[]{11, 22, 33});

        MvcResult uploadResponse = mockMvc.perform(multipart("/api/users/me/media/photos")
                        .file(photo)
                        .principal(studentId::toString))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("PHOTO"))
                .andReturn();

        String uploadedMediaId = objectMapper.readTree(uploadResponse.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(delete("/api/users/me/media/{id}", uploadedMediaId)
                        .principal(lyceenId::toString))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Media introuvable"));

        mockMvc.perform(delete("/api/users/me/media/{id}", uploadedMediaId)
                        .principal(studentId::toString))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Media supprime avec succes"));
    }

    private String signupAndReturnUserId(String email, String role, String profilJson) throws Exception {
        String payload = """
                {
                  "email": "%s",
                  "password": "Password123!",
                  "role": "%s",
                  "prenom": "Media",
                  "nom": "Integration",
                  "profil": %s
                }
                """.formatted(email, role, profilJson);

        String response = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value(role))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("userId").asText();
    }
}
