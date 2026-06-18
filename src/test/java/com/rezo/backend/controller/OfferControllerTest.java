package com.rezo.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rezo.backend.dto.offer.OfferRequest;
import com.rezo.backend.service.UserMediaStorageService;
import com.rezo.entities.Company;
import com.rezo.entities.Offer;
import com.rezo.entities.School;
import com.rezo.entities.Pack;
import com.rezo.entities.User;
import com.rezo.entities.enums.CompanySize;
import com.rezo.entities.enums.OfferType;
import com.rezo.entities.enums.SchoolStatus;
import com.rezo.entities.enums.UserRole;
import com.rezo.repositories.CompanyRepository;
import com.rezo.repositories.OfferRepository;
import com.rezo.repositories.SchoolRepository;
import com.rezo.repositories.SwipeRepository;
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
import java.time.LocalDateTime;
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
class OfferControllerTest {

    @Mock
    private OfferRepository offerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private SchoolRepository schoolRepository;

    @Mock
    private SwipeRepository swipeRepository;

    @Mock
    private UserMediaStorageService userMediaStorageService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private final UUID ownerId = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private final UUID otherUserId = UUID.fromString("88888888-8888-8888-8888-888888888888");
    private final UUID offerId = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private final UUID companyId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private final UUID schoolId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @BeforeEach
    void setUp() {
        OfferController controller = new OfferController(
                offerRepository,
                userRepository,
                companyRepository,
                schoolRepository,
                swipeRepository,
                userMediaStorageService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    void listOffersShouldBePublic() throws Exception {
        Offer offer = buildCompanyOffer(buildCompany(buildUser(ownerId, UserRole.ENTREPRISE)));
        when(offerRepository.findAllWithOwners()).thenReturn(List.of(offer));

        mockMvc.perform(get("/api/offers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titre").value("Stage Java"))
                .andExpect(jsonPath("$[0].ownerType").value("ENTREPRISE"))
                .andExpect(jsonPath("$[0].ownerDisplayName").value("TechCorp"));
    }

    @Test
    void createOfferShouldWorkForEntrepriseOwner() throws Exception {
        User user = buildUser(ownerId, UserRole.ENTREPRISE);
        Company company = buildCompany(user);
        setField(company, "id", companyId);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(user));
        when(companyRepository.findByUserId(ownerId)).thenReturn(Optional.of(company));
        when(offerRepository.save(any(Offer.class))).thenAnswer(invocation -> {
            Offer offer = invocation.getArgument(0);
            setField(offer, "id", offerId);
            return offer;
        });

        OfferRequest request = validRequest();

        mockMvc.perform(post("/api/offers")
                        .principal(principal(ownerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(offerId.toString()))
                .andExpect(jsonPath("$.type").value("STAGE"))
                .andExpect(jsonPath("$.ownerType").value("ENTREPRISE"));
    }

    @Test
    void createOfferShouldReturn403ForWrongRole() throws Exception {
        User user = buildUser(ownerId, UserRole.ETUDIANT);
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/offers")
                        .principal(principal(ownerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.message").value("Seuls les utilisateurs ECOLE ou ENTREPRISE peuvent publier une offre"));
    }

    @Test
    void createOfferShouldReturn403WhenPackDoesNotAllowPublishing() throws Exception {
        User user = buildUser(ownerId, UserRole.ENTREPRISE);
        Pack freePack = new Pack();
        freePack.setNom("FREE");
        freePack.setFeatures("MATCHING_BASIC,MESSAGERIE_LIMITEE");
        user.setPack(freePack);

        Company company = buildCompany(user);
        setField(company, "id", companyId);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(user));
        when(companyRepository.findByUserId(ownerId)).thenReturn(Optional.of(company));

        mockMvc.perform(post("/api/offers")
                        .principal(principal(ownerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.message").value("Votre pack actuel ne permet pas de publier ou gerer des offres"));
    }

    @Test
    void updateOfferShouldReturn403WhenNotOwner() throws Exception {
        User loggedUser = buildUser(ownerId, UserRole.ENTREPRISE);
        User otherUser = buildUser(otherUserId, UserRole.ENTREPRISE);
        Offer offer = buildCompanyOffer(buildCompany(otherUser));
        setField(offer, "id", offerId);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(loggedUser));
        when(offerRepository.findByIdWithOwners(offerId)).thenReturn(Optional.of(offer));

        OfferRequest request = new OfferRequest();
        request.setTitre("Offre modifiee");

        mockMvc.perform(put("/api/offers/{id}", offerId)
                        .principal(principal(ownerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.message").value("Vous ne pouvez modifier que votre propre offre"));
    }

    @Test
    void deleteOfferShouldWorkForOwner() throws Exception {
        User user = buildUser(ownerId, UserRole.ENTREPRISE);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(user));
        when(offerRepository.findOwnerUserIdById(offerId)).thenReturn(Optional.of(ownerId));
        when(offerRepository.deleteByIdDirect(offerId)).thenReturn(1);

        mockMvc.perform(delete("/api/offers/{id}", offerId)
                        .principal(principal(ownerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Offre supprimee avec succes"));

        verify(offerRepository).deleteByIdDirect(offerId);
    }

    @Test
    void createOfferShouldReturn400ForInvalidDates() throws Exception {
        User user = buildUser(ownerId, UserRole.ECOLE);
        School school = buildSchool(user);
        setField(school, "id", schoolId);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(user));
        when(schoolRepository.findByUserId(ownerId)).thenReturn(Optional.of(school));

        OfferRequest request = validRequest();
        request.setDateDebut(LocalDateTime.of(2026, 6, 10, 9, 0));
        request.setDateFin(LocalDateTime.of(2026, 6, 1, 9, 0));

        mockMvc.perform(post("/api/offers")
                        .principal(principal(ownerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.message").value("dateDebut doit etre strictement avant dateFin"));
    }

    @Test
    void createOfferShouldReturn400ForInvalidType() throws Exception {
        User user = buildUser(ownerId, UserRole.ENTREPRISE);
        Company company = buildCompany(user);
        setField(company, "id", companyId);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(user));
        when(companyRepository.findByUserId(ownerId)).thenReturn(Optional.of(company));

        OfferRequest request = validRequest();
        request.setType("FORMATION");

        mockMvc.perform(post("/api/offers")
                        .principal(principal(ownerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.message").value("type doit etre STAGE ou EMPLOI"));
    }

    private OfferRequest validRequest() {
        OfferRequest request = new OfferRequest();
        request.setTitre("Stage Java");
        request.setDescription("Offre de stage backend Spring Boot");
        request.setType("STAGE");
        request.setDomaine("Informatique");
        request.setLocation("Paris");
        request.setCompetencesRequises(List.of("Java", "Spring Boot"));
        request.setDatePublication(LocalDateTime.of(2026, 4, 1, 10, 0));
        request.setDateDebut(LocalDateTime.of(2026, 5, 1, 9, 0));
        request.setDateFin(LocalDateTime.of(2026, 8, 31, 18, 0));
        return request;
    }

    private Principal principal(UUID userId) {
        return userId::toString;
    }

    private User buildUser(UUID userId, UserRole role) {
        Pack pack = new Pack();
        pack.setNom("BUSINESS_PLUS");
        pack.setFeatures("OFFERS_PUBLISH,MESSAGERIE_ILLIMITEE,AI_CHAT_ACCESS");

        User user = new User();
        setField(user, "id", userId);
        user.setEmail("owner@rezo.com");
        user.setPrenom("Owner");
        user.setNom("Test");
        user.setRole(role);
        user.setPack(pack);
        return user;
    }

    private Company buildCompany(User user) {
        Company company = new Company();
        company.setUser(user);
        company.setRaisonSociale("TechCorp");
        company.setSecteurActivite("IT");
        company.setTaille(CompanySize.PME);
        company.setDescription("Entreprise tech");
        return company;
    }

    private School buildSchool(User user) {
        School school = new School();
        school.setUser(user);
        school.setNomEtablissement("ESGI");
        school.setStatut(SchoolStatus.PUBLIC);
        school.setDomaines(Set.of("Informatique"));
        return school;
    }

    private Offer buildCompanyOffer(Company company) {
        Offer offer = new Offer();
        offer.setOwnerEntreprise(company);
        offer.setTitre("Stage Java");
        offer.setDescription("Offre de stage backend Spring Boot");
        offer.setType(OfferType.STAGE);
        offer.setDomaine("Informatique");
        offer.setLocation("Paris");
        offer.setDateDebut(LocalDateTime.of(2026, 5, 1, 9, 0));
        offer.setDateFin(LocalDateTime.of(2026, 8, 31, 18, 0));
        offer.setCompetencesRequises(Set.of("Java", "Spring Boot"));
        setField(offer, "id", offerId);
        setField(offer, "datePublication", LocalDateTime.of(2026, 4, 1, 10, 0));
        return offer;
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
