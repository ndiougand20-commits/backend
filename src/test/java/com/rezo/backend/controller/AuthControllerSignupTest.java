package com.rezo.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rezo.backend.dto.auth.SignupRequest;
import com.rezo.entities.Pack;
import com.rezo.entities.Profile;
import com.rezo.entities.User;
import com.rezo.repositories.CompanyRepository;
import com.rezo.repositories.PackRepository;
import com.rezo.repositories.ProfileRepository;
import com.rezo.repositories.SchoolRepository;
import com.rezo.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerSignupTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PackRepository packRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private SchoolRepository schoolRepository;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(
                userRepository,
                packRepository,
                profileRepository,
                companyRepository,
                schoolRepository
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldSignupEtudiantSuccessfully() throws Exception {
        Pack free = new Pack();
        free.setNom("FREE");

        SignupRequest request = new SignupRequest();
        request.setEmail("etudiant@rezo.com");
        request.setPassword("motdepassesecret");
        request.setRole("ETUDIANT");
        request.setProfil(Map.of(
                "niveau_etude", "L3",
                "domaine", "Informatique",
                "competences", List.of("Java", "SQL"),
                "objectif", "Stage"
        ));

        when(userRepository.existsByEmail("etudiant@rezo.com")).thenReturn(false);
        when(packRepository.findByNomIgnoreCase("FREE")).thenReturn(Optional.of(free));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Inscription reussie"))
                .andExpect(jsonPath("$.email").value("etudiant@rezo.com"))
                .andExpect(jsonPath("$.role").value("ETUDIANT"))
                .andExpect(jsonPath("$.profileType").value("PROFILE"));

        verify(userRepository).save(any(User.class));
        verify(profileRepository).save(any(Profile.class));
    }

    @Test
    void shouldReturnConflictWhenEmailAlreadyExists() throws Exception {
        SignupRequest request = new SignupRequest();
        request.setEmail("used@rezo.com");
        request.setPassword("secret");
        request.setRole("ETUDIANT");
        request.setProfil(Map.of("niveau_etude", "L3", "domaine", "Info"));

        when(userRepository.existsByEmail("used@rezo.com")).thenReturn(true);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email deja utilise"));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldReturnBadRequestWhenPayloadIsIncomplete() throws Exception {
        SignupRequest request = new SignupRequest();
        request.setEmail("ecole@rezo.com");
        request.setPassword("secret");
        request.setRole("ECOLE");
        request.setProfil(Map.of("domaines", List.of("Informatique")));

        when(userRepository.existsByEmail("ecole@rezo.com")).thenReturn(false);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Champ profil obligatoire manquant: nomEtablissement/nom_etablissement"));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldDeleteUserByEmail() throws Exception {
        UUID userId = UUID.randomUUID();

        when(userRepository.findIdByEmail("delete-me@rezo.com")).thenReturn(Optional.of(userId));
        when(userRepository.deleteByIdDirect(userId)).thenReturn(1);

        mockMvc.perform(delete("/api/auth/users/by-email/delete-me@rezo.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Utilisateur supprime"));

        verify(profileRepository).deleteAllByUserId(eq(userId));
        verify(companyRepository).deleteAllByUserId(eq(userId));
        verify(schoolRepository).deleteAllByUserId(eq(userId));
        verify(userRepository).deleteByIdDirect(eq(userId));
    }

    @Test
    void shouldReturnNotFoundWhenDeletingUnknownUser() throws Exception {
        when(userRepository.findIdByEmail("absent@rezo.com")).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/auth/users/by-email/absent@rezo.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Utilisateur introuvable"));

        verify(userRepository, never()).deleteByIdDirect(any(UUID.class));
    }
}
