package com.rezo.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rezo.backend.dto.company.CompanyRequest;
import com.rezo.entities.Company;
import com.rezo.entities.User;
import com.rezo.entities.enums.CompanySize;
import com.rezo.entities.enums.UserRole;
import com.rezo.repositories.CompanyRepository;
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
class CompanyControllerTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private UserRepository userRepository;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private final UUID ownerId = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private final UUID otherUserId = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private final UUID companyId = UUID.fromString("66666666-6666-6666-6666-666666666666");

    @BeforeEach
    void setUp() {
        CompanyController controller = new CompanyController(companyRepository, userRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void listCompaniesShouldBePublic() throws Exception {
        Company company = buildCompany(buildUser(ownerId, UserRole.ENTREPRISE));
        when(companyRepository.findAllWithUser()).thenReturn(List.of(company));

        mockMvc.perform(get("/api/companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].raisonSociale").value("TechCorp"))
                .andExpect(jsonPath("$[0].taille").value("PME"));
    }

    @Test
    void createCompanyShouldWorkForEntrepriseRole() throws Exception {
        User user = buildUser(ownerId, UserRole.ENTREPRISE);
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(user));
        when(companyRepository.findByUserId(ownerId)).thenReturn(Optional.empty());
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> {
            Company company = invocation.getArgument(0);
            setField(company, "id", companyId);
            return company;
        });

        CompanyRequest request = new CompanyRequest();
        request.setRaisonSociale("TechCorp");
        request.setSecteurActivite("IT");
        request.setTaille("PME");
        request.setDescription("Entreprise tech");
        request.setSiteWeb("https://company.rezo.com");

        mockMvc.perform(post("/api/companies")
                        .principal(principal(ownerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(companyId.toString()))
                .andExpect(jsonPath("$.raisonSociale").value("TechCorp"));
    }

    @Test
    void createCompanyShouldReturn403ForWrongRole() throws Exception {
        User user = buildUser(ownerId, UserRole.ECOLE);
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(user));

        CompanyRequest request = new CompanyRequest();
        request.setRaisonSociale("TechCorp");
        request.setSecteurActivite("IT");
        request.setTaille("PME");
        request.setDescription("Entreprise tech");

        mockMvc.perform(post("/api/companies")
                        .principal(principal(ownerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Seuls les utilisateurs ENTREPRISE peuvent creer une fiche entreprise"));
    }

    @Test
    void updateCompanyShouldReturn403WhenNotOwner() throws Exception {
        User loggedUser = buildUser(ownerId, UserRole.ENTREPRISE);
        User otherUser = buildUser(otherUserId, UserRole.ENTREPRISE);
        Company company = buildCompany(otherUser);
        setField(company, "id", companyId);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(loggedUser));
        when(companyRepository.findByIdWithUser(companyId)).thenReturn(Optional.of(company));

        CompanyRequest request = new CompanyRequest();
        request.setDescription("Updated description");

        mockMvc.perform(put("/api/companies/{id}", companyId)
                        .principal(principal(ownerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Vous ne pouvez modifier que votre propre fiche entreprise"));
    }

    @Test
    void deleteCompanyShouldWorkForOwner() throws Exception {
        User user = buildUser(ownerId, UserRole.ENTREPRISE);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(user));
        when(companyRepository.findOwnerUserIdById(companyId)).thenReturn(Optional.of(ownerId));
        when(companyRepository.deleteByIdDirect(companyId)).thenReturn(1);

        mockMvc.perform(delete("/api/companies/{id}", companyId)
                        .principal(principal(ownerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Entreprise supprimee avec succes"));

        verify(companyRepository).deleteByIdDirect(companyId);
    }

    @Test
    void createCompanyShouldReturn400ForInvalidSiteWeb() throws Exception {
        User user = buildUser(ownerId, UserRole.ENTREPRISE);
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(user));
        when(companyRepository.findByUserId(ownerId)).thenReturn(Optional.empty());

        CompanyRequest request = new CompanyRequest();
        request.setRaisonSociale("TechCorp");
        request.setSecteurActivite("IT");
        request.setTaille("PME");
        request.setDescription("Entreprise tech");
        request.setSiteWeb("notaurl");

        mockMvc.perform(post("/api/companies")
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

    private Company buildCompany(User user) {
        Company company = new Company();
        company.setUser(user);
        company.setRaisonSociale("TechCorp");
        company.setSecteurActivite("IT");
        company.setTaille(CompanySize.PME);
        company.setDescription("Entreprise tech");
        company.setSiteWeb("https://company.rezo.com");
        return company;
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
