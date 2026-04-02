package com.rezo.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rezo.backend.dto.school.SchoolRequest;
import com.rezo.entities.School;
import com.rezo.entities.User;
import com.rezo.entities.enums.SchoolStatus;
import com.rezo.entities.enums.UserRole;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SchoolControllerTest {

    @Mock
    private SchoolRepository schoolRepository;

    @Mock
    private UserRepository userRepository;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private final UUID ownerId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID otherUserId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private final UUID schoolId = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @BeforeEach
    void setUp() {
        SchoolController controller = new SchoolController(schoolRepository, userRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void listSchoolsShouldBePublic() throws Exception {
        School school = buildSchool(buildUser(ownerId, UserRole.ECOLE));
        when(schoolRepository.findAllWithUser()).thenReturn(List.of(school));

        mockMvc.perform(get("/api/schools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nomEtablissement").value("ESGI"))
                .andExpect(jsonPath("$[0].statut").value("PUBLIC"));
    }

    @Test
    void createSchoolShouldWorkForEcoleRole() throws Exception {
        User user = buildUser(ownerId, UserRole.ECOLE);
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(user));
        when(schoolRepository.findByUserId(ownerId)).thenReturn(Optional.empty());
        when(schoolRepository.save(any(School.class))).thenAnswer(invocation -> {
            School school = invocation.getArgument(0);
            setField(school, "id", schoolId);
            return school;
        });

        SchoolRequest request = new SchoolRequest();
        request.setNomEtablissement("ESGI");
        request.setStatut("PUBLIC");
        request.setDomaines(List.of("Informatique", "Data"));
        request.setSiteWeb("https://school.rezo.com");

        mockMvc.perform(post("/api/schools")
                        .principal(principal(ownerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(schoolId.toString()))
                .andExpect(jsonPath("$.nomEtablissement").value("ESGI"));
    }

    @Test
    void createSchoolShouldReturn403ForWrongRole() throws Exception {
        User user = buildUser(ownerId, UserRole.ETUDIANT);
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(user));

        SchoolRequest request = new SchoolRequest();
        request.setNomEtablissement("ESGI");
        request.setStatut("PUBLIC");
        request.setDomaines(List.of("Informatique"));

        mockMvc.perform(post("/api/schools")
                        .principal(principal(ownerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Seuls les utilisateurs ECOLE peuvent creer une fiche ecole"));
    }

    @Test
    void updateSchoolShouldReturn403WhenNotOwner() throws Exception {
        User loggedUser = buildUser(ownerId, UserRole.ECOLE);
        User otherUser = buildUser(otherUserId, UserRole.ECOLE);
        School school = buildSchool(otherUser);
        setField(school, "id", schoolId);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(loggedUser));
        when(schoolRepository.findByIdWithUser(schoolId)).thenReturn(Optional.of(school));

        SchoolRequest request = new SchoolRequest();
        request.setNomEtablissement("Updated School");

        mockMvc.perform(put("/api/schools/{id}", schoolId)
                        .principal(principal(ownerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Vous ne pouvez modifier que votre propre fiche ecole"));
    }

    @Test
    void deleteSchoolShouldWorkForOwner() throws Exception {
        User user = buildUser(ownerId, UserRole.ECOLE);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(user));
        when(schoolRepository.findOwnerUserIdById(schoolId)).thenReturn(Optional.of(ownerId));
        when(schoolRepository.deleteByIdDirect(schoolId)).thenReturn(1);

        mockMvc.perform(delete("/api/schools/{id}", schoolId)
                        .principal(principal(ownerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Ecole supprimee avec succes"));

        verify(schoolRepository).deleteByIdDirect(schoolId);
    }

    @Test
    void createSchoolShouldReturn400ForInvalidWebsite() throws Exception {
        User user = buildUser(ownerId, UserRole.ECOLE);
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(user));
        when(schoolRepository.findByUserId(ownerId)).thenReturn(Optional.empty());

        SchoolRequest request = new SchoolRequest();
        request.setNomEtablissement("ESGI");
        request.setStatut("PUBLIC");
        request.setDomaines(List.of("Informatique"));
        request.setSiteWeb("not-a-valid-url");

        mockMvc.perform(post("/api/schools")
                        .principal(principal(ownerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("siteWeb doit etre une URL valide (http/https)"));
    }

    private Principal principal(UUID userId) {
        return userId::toString;
    }

    private User buildUser(UUID userId, UserRole role) {
        User user = new User();
        setField(user, "id", userId);
        user.setEmail("owner@rezo.com");
        user.setPrenom("Owner");
        user.setNom("Test");
        user.setRole(role);
        return user;
    }

    private School buildSchool(User user) {
        School school = new School();
        school.setUser(user);
        school.setNomEtablissement("ESGI");
        school.setStatut(SchoolStatus.PUBLIC);
        school.setDomaines(Set.of("Informatique"));
        school.setDiplomesDelivres(Set.of("Master"));
        school.setSiteWeb("https://school.rezo.com");
        return school;
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
}
