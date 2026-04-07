package com.rezo.backend.controller;

import com.rezo.entities.Company;
import com.rezo.entities.Offer;
import com.rezo.entities.Pack;
import com.rezo.entities.Profile;
import com.rezo.entities.User;
import com.rezo.entities.enums.CompanySize;
import com.rezo.entities.enums.OfferType;
import com.rezo.entities.enums.UserRole;
import com.rezo.repositories.CompanyRepository;
import com.rezo.repositories.OfferRepository;
import com.rezo.repositories.PackRepository;
import com.rezo.repositories.ProfileRepository;
import com.rezo.repositories.SchoolRepository;
import com.rezo.repositories.SwipeRepository;
import com.rezo.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Field;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MatchControllerTest {

    @Mock private UserRepository userRepository;
    @Mock private ProfileRepository profileRepository;
    @Mock private OfferRepository offerRepository;
    @Mock private SwipeRepository swipeRepository;
    @Mock private PackRepository packRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private SchoolRepository schoolRepository;

    private MockMvc mockMvc;

    private final UUID userId = UUID.fromString("90000000-0000-0000-0000-000000000001");
    private final UUID matchingOfferId = UUID.fromString("90000000-0000-0000-0000-000000000101");
    private final UUID lowOfferId = UUID.fromString("90000000-0000-0000-0000-000000000102");
    private final UUID swipedOfferId = UUID.fromString("90000000-0000-0000-0000-000000000103");

    @BeforeEach
    void setUp() {
        MatchController controller = new MatchController(
                userRepository,
                profileRepository,
                offerRepository,
                swipeRepository,
                packRepository,
                companyRepository,
                schoolRepository
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void shouldReturnRecommendationsSortedAndExcludeAlreadySwipedOffers() throws Exception {
        User user = buildStudentUser("FREE", "MATCHING_BASIC,MESSAGERIE_LIMITEE");
        Profile profile = buildStudentProfile();

        when(userRepository.findByIdWithPack(userId)).thenReturn(Optional.of(user));
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(offerRepository.findAllWithOwners()).thenReturn(List.of(
                buildOffer(matchingOfferId, "Stage Java Spring", "Informatique", "Paris", Set.of("Java", "Spring")),
                buildOffer(lowOfferId, "Stage Marketing", "Marketing", "Lyon", Set.of("SEO")),
                buildOffer(swipedOfferId, "Stage Backend swipe", "Informatique", "Paris", Set.of("Java"))
        ));
        when(swipeRepository.findOfferIdsByUserId(userId)).thenReturn(Set.of(swipedOfferId));
        when(packRepository.findAll()).thenReturn(List.of(
                buildPack("FREE", "TOUS", "MATCHING_BASIC,MESSAGERIE_LIMITEE"),
                buildPack("PREMIUM_CANDIDAT", "ETUDIANT,LYCEEN,EMPLOI", "MATCHING_PREMIUM,MESSAGERIE_ILLIMITEE,AI_CHAT_ACCESS")
        ));

        mockMvc.perform(get("/api/match/recommendations").principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations.length()").value(2))
                .andExpect(jsonPath("$.recommendations[0].offerId").value(matchingOfferId.toString()))
                .andExpect(jsonPath("$.recommendations[0].score").isNumber())
                .andExpect(jsonPath("$.recommendations[0].score").value(org.hamcrest.Matchers.greaterThan(50)))
                .andExpect(jsonPath("$.suggestedPack.label").value("PREMIUM_CANDIDAT"))
                .andExpect(jsonPath("$.suggestedPack.reason").value(org.hamcrest.Matchers.containsString("messagerie")))
                .andExpect(jsonPath("$.trace.excludedSwipeCount").value(1));
    }

    @Test
    void shouldSuggestBusinessPackForEntrepriseUser() throws Exception {
        User user = buildBusinessUser("FREE", "MATCHING_BASIC");
        Company company = new Company();
        company.setUser(user);
        company.setRaisonSociale("RuntimeCorp");
        company.setSecteurActivite("IT");
        company.setTaille(CompanySize.PME);
        company.setDescription("Entreprise tech");
        company.setAdresse("Paris");

        when(userRepository.findByIdWithPack(userId)).thenReturn(Optional.of(user));
        when(companyRepository.findByUserId(userId)).thenReturn(Optional.of(company));
        when(offerRepository.findAllWithOwners()).thenReturn(List.of(
                buildOffer(matchingOfferId, "Offre Java", "IT", "Paris", Set.of("Java", "API"))
        ));
        when(swipeRepository.findOfferIdsByUserId(userId)).thenReturn(Set.of());
        when(packRepository.findAll()).thenReturn(List.of(
                buildPack("FREE", "TOUS", "MATCHING_BASIC"),
                buildPack("BUSINESS_PLUS", "ENTREPRISE,ECOLE", "OFFERS_PUBLISH,MESSAGERIE_ILLIMITEE,AI_CHAT_ACCESS")
        ));

        mockMvc.perform(get("/api/match/recommendations").principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestedPack.label").value("BUSINESS_PLUS"))
                .andExpect(jsonPath("$.suggestedPack.reason").value(org.hamcrest.Matchers.containsString("offres")));
    }

    @Test
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/match/recommendations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Non authentifie"));
    }

    private Principal principal() {
        return userId::toString;
    }

    private User buildStudentUser(String packName, String features) {
        return buildUser(UserRole.ETUDIANT, packName, features);
    }

    private User buildBusinessUser(String packName, String features) {
        return buildUser(UserRole.ENTREPRISE, packName, features);
    }

    private User buildUser(UserRole role, String packName, String features) {
        Pack pack = buildPack(packName, "TOUS", features);
        User user = new User();
        setField(user, "id", userId);
        user.setEmail("match@rezo.com");
        user.setPrenom("Match");
        user.setNom("User");
        user.setRole(role);
        user.setPack(pack);
        return user;
    }

    private Profile buildStudentProfile() {
        Profile profile = new Profile();
        profile.setNiveauEtude("M1");
        profile.setDomaine("Informatique");
        profile.setCompetences(Set.of("Java", "Spring", "SQL"));
        profile.setObjectif("Trouver un stage backend Java");
        profile.setPreferencesLieu(Set.of("Paris"));
        profile.setPreferencesSecteur(Set.of("Informatique", "IT"));
        return profile;
    }

    private Offer buildOffer(UUID offerId, String title, String domaine, String location, Set<String> competences) {
        Offer offer = new Offer();
        setField(offer, "id", offerId);
        offer.setTitre(title);
        offer.setDescription("Description " + title);
        offer.setType(OfferType.STAGE);
        offer.setDomaine(domaine);
        offer.setLocation(location);
        offer.setCompetencesRequises(competences);
        offer.setDatePublication(LocalDateTime.of(2026, 4, 1, 10, 0));
        offer.setDateDebut(LocalDateTime.of(2026, 6, 1, 9, 0));
        offer.setDateFin(LocalDateTime.of(2026, 8, 31, 18, 0));
        return offer;
    }

    private Pack buildPack(String name, String cible, String features) {
        Pack pack = new Pack();
        setField(pack, "id", UUID.randomUUID());
        pack.setNom(name);
        pack.setCible(cible);
        pack.setFeatures(features);
        return pack;
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
