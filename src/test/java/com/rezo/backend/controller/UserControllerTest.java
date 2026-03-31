package com.rezo.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rezo.backend.dto.user.UpdateUserRequest;
import com.rezo.entities.Company;
import com.rezo.entities.Pack;
import com.rezo.entities.Profile;
import com.rezo.entities.User;
import com.rezo.entities.enums.CompanySize;
import com.rezo.entities.enums.UserRole;
import com.rezo.repositories.CompanyRepository;
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

import java.lang.reflect.Field;
import java.security.Principal;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock private UserRepository userRepository;
    @Mock private ProfileRepository profileRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private SchoolRepository schoolRepository;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private final UUID userId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    @BeforeEach
    void setUp() {
        UserController controller = new UserController(
                userRepository, profileRepository, companyRepository, schoolRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    private Principal principal() {
        return () -> userId.toString();
    }

    private User buildUser(UserRole role) {
        Pack pack = new Pack();
        pack.setNom("FREE");
        User user = new User();
        setField(user, "id", userId);
        user.setEmail("alice@rezo.com");
        user.setPrenom("Alice");
        user.setNom("Dupont");
        user.setRole(role);
        user.setPack(pack);
        return user;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    // ─── GET /api/users/me ───────────────────────────────────────────────

    @Test
    void getMeShouldReturnUserWithProfile() throws Exception {
        User user = buildUser(UserRole.ETUDIANT);
        Profile profile = new Profile();
        profile.setNiveauEtude("M1");
        profile.setDomaine("Informatique");
        profile.setCompetences(Set.of("Java", "SQL"));

        when(userRepository.findByIdWithPack(userId)).thenReturn(Optional.of(user));
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        mockMvc.perform(get("/api/users/me").principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@rezo.com"))
                .andExpect(jsonPath("$.role").value("ETUDIANT"))
                .andExpect(jsonPath("$.profil.niveauEtude").value("M1"))
                .andExpect(jsonPath("$.profil.domaine").value("Informatique"));
    }

    @Test
    void getMeShouldReturnUserWithCompanyProfile() throws Exception {
        User user = buildUser(UserRole.ENTREPRISE);
        Company company = new Company();
        company.setRaisonSociale("TechCorp");
        company.setSecteurActivite("IT");
        company.setTaille(CompanySize.PME);
        company.setDescription("Entreprise tech");

        when(userRepository.findByIdWithPack(userId)).thenReturn(Optional.of(user));
        when(companyRepository.findByUserId(userId)).thenReturn(Optional.of(company));

        mockMvc.perform(get("/api/users/me").principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ENTREPRISE"))
                .andExpect(jsonPath("$.profil.raisonSociale").value("TechCorp"))
                .andExpect(jsonPath("$.profil.taille").value("PME"));
    }

    @Test
    void getMeShouldReturn404WhenUserNotFound() throws Exception {
        when(userRepository.findByIdWithPack(userId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/me").principal(principal()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Utilisateur introuvable"));
    }

    // ─── PUT /api/users/me ───────────────────────────────────────────────

    @Test
    void updateMeShouldUpdateUserFields() throws Exception {
        User user = buildUser(UserRole.ETUDIANT);
        when(userRepository.findByIdWithPack(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        UpdateUserRequest request = new UpdateUserRequest();
        request.setPrenom("Bob");
        request.setNom("Martin");

        mockMvc.perform(put("/api/users/me")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prenom").value("Bob"))
                .andExpect(jsonPath("$.nom").value("Martin"));
    }

    @Test
    void updateMeShouldReturn409WhenEmailAlreadyUsed() throws Exception {
        User user = buildUser(UserRole.ETUDIANT);
        when(userRepository.findByIdWithPack(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("taken@rezo.com")).thenReturn(true);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("taken@rezo.com");

        mockMvc.perform(put("/api/users/me")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email deja utilise"));
    }

    @Test
    void updateMeShouldReturn400WhenPrenomBlank() throws Exception {
        User user = buildUser(UserRole.ETUDIANT);
        when(userRepository.findByIdWithPack(userId)).thenReturn(Optional.of(user));

        UpdateUserRequest request = new UpdateUserRequest();
        request.setPrenom("   ");

        mockMvc.perform(put("/api/users/me")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Le prenom ne peut pas etre vide"));
    }

    @Test
    void updateMeShouldUpdateProfileFields() throws Exception {
        User user = buildUser(UserRole.ETUDIANT);
        Profile profile = new Profile();
        profile.setUser(user);
        profile.setNiveauEtude("L3");
        profile.setDomaine("Informatique");

        when(userRepository.findByIdWithPack(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(Profile.class))).thenAnswer(i -> i.getArgument(0));

        UpdateUserRequest request = new UpdateUserRequest();
        request.setProfil(Map.of("niveauEtude", "M1", "domaine", "Data Science"));

        mockMvc.perform(put("/api/users/me")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profil.niveauEtude").value("M1"))
                .andExpect(jsonPath("$.profil.domaine").value("Data Science"));
    }

    // ─── DELETE /api/users/me ────────────────────────────────────────────

    @Test
    void deleteMeShouldDeleteUser() throws Exception {
        when(userRepository.existsById(userId)).thenReturn(true);
        when(userRepository.deleteByIdDirect(userId)).thenReturn(1);

        mockMvc.perform(delete("/api/users/me").principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Compte supprime avec succes"));

        verify(profileRepository).deleteAllByUserId(userId);
        verify(companyRepository).deleteAllByUserId(userId);
        verify(schoolRepository).deleteAllByUserId(userId);
        verify(userRepository).deleteByIdDirect(userId);
    }

    @Test
    void deleteMeShouldReturn404WhenUserNotFound() throws Exception {
        when(userRepository.existsById(userId)).thenReturn(false);

        mockMvc.perform(delete("/api/users/me").principal(principal()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Utilisateur introuvable"));
    }
}
