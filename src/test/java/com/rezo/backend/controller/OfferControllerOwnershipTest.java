package com.rezo.backend.controller;

import com.rezo.backend.service.UserMediaStorageService;
import com.rezo.entities.Company;
import com.rezo.entities.Offer;
import com.rezo.entities.Pack;
import com.rezo.entities.User;
import com.rezo.entities.enums.OfferType;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OfferControllerOwnershipTest {

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
    }

    @Test
    void shouldReturn401WhenUpdatingOfferWithoutAuth() throws Exception {
        UUID offerId = UUID.randomUUID();

        mockMvc.perform(put("/api/offers/{id}", offerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdatePayload()))
                .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.message").value("Non authentifie"));
    }

    @Test
    void shouldReturn401WhenDeletingOfferWithoutAuth() throws Exception {
        UUID offerId = UUID.randomUUID();

        mockMvc.perform(delete("/api/offers/{id}", offerId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.message").value("Non authentifie"));
    }

    @Test
    void shouldReturn401ForLikedByWithoutAuth() throws Exception {
        UUID offerId = UUID.randomUUID();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/offers/{id}/liked-by", offerId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.message").value("Non authentifie"));
    }

    @Test
    void shouldReturn403WhenUpdatingOfferNotOwnedByCurrentUser() throws Exception {
        UUID currentUserId = UUID.randomUUID();
        UUID ownerUserId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();

        User currentUser = buildBusinessUser(currentUserId);
        User ownerUser = buildBusinessUser(ownerUserId);
        Offer offer = buildOwnedOffer(offerId, ownerUser);

        when(userRepository.findById(eq(currentUserId))).thenReturn(Optional.of(currentUser));
        when(offerRepository.findByIdWithOwners(eq(offerId))).thenReturn(Optional.of(offer));

        mockMvc.perform(put("/api/offers/{id}", offerId)
                        .principal(() -> currentUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdatePayload()))
                .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.message").value("Vous ne pouvez modifier que votre propre offre"));

        verify(offerRepository, never()).save(any(Offer.class));
    }

    @Test
    void shouldReturn403WhenDeletingOfferNotOwnedByCurrentUser() throws Exception {
        UUID currentUserId = UUID.randomUUID();
        UUID ownerUserId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();

        User currentUser = buildBusinessUser(currentUserId);

        when(userRepository.findById(eq(currentUserId))).thenReturn(Optional.of(currentUser));
        when(offerRepository.findOwnerUserIdById(eq(offerId))).thenReturn(Optional.of(ownerUserId));

        mockMvc.perform(delete("/api/offers/{id}", offerId)
                        .principal(() -> currentUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.message").value("Vous ne pouvez supprimer que votre propre offre"));

        verify(offerRepository, never()).deleteByIdDirect(any(UUID.class));
    }

    @Test
    void shouldDeleteOfferWhenCurrentUserOwnsIt() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();

        User currentUser = buildBusinessUser(userId);

        when(userRepository.findById(eq(userId))).thenReturn(Optional.of(currentUser));
        when(offerRepository.findOwnerUserIdById(eq(offerId))).thenReturn(Optional.of(userId));
        when(offerRepository.deleteByIdDirect(eq(offerId))).thenReturn(1);

        mockMvc.perform(delete("/api/offers/{id}", offerId)
                        .principal(() -> userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Offre supprimee avec succes"));
    }

            @Test
            void shouldReturn403ForLikedByWhenCurrentUserIsNotOwner() throws Exception {
            UUID currentUserId = UUID.randomUUID();
            UUID ownerUserId = UUID.randomUUID();
            UUID offerId = UUID.randomUUID();

            User currentUser = buildBusinessUser(currentUserId);

            when(userRepository.findById(eq(currentUserId))).thenReturn(Optional.of(currentUser));
            when(offerRepository.findOwnerUserIdById(eq(offerId))).thenReturn(Optional.of(ownerUserId));

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .get("/api/offers/{id}/liked-by", offerId)
                    .principal(() -> currentUserId.toString())
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.message").value("Acces reserve au proprietaire de l'offre"));
            }

            @Test
            void shouldReturnLikedByPayloadWhenCurrentUserOwnsOffer() throws Exception {
            UUID ownerUserId = UUID.randomUUID();
            UUID offerId = UUID.randomUUID();
            UUID likerUserId = UUID.randomUUID();

            User ownerUser = buildBusinessUser(ownerUserId);
            User likerUser = buildBusinessUser(likerUserId);
            likerUser.setPrenom("Awa");
            likerUser.setNom("Ndiaye");

            when(userRepository.findById(eq(ownerUserId))).thenReturn(Optional.of(ownerUser));
            when(offerRepository.findOwnerUserIdById(eq(offerId))).thenReturn(Optional.of(ownerUserId));
            when(swipeRepository.findLikersByOfferId(eq(offerId), eq(com.rezo.entities.enums.SwipeAction.LIKE)))
                .thenReturn(List.of(likerUser));

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .get("/api/offers/{id}/liked-by", offerId)
                    .principal(() -> ownerUserId.toString())
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offerId").value(offerId.toString()))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.likers[0].userId").value(likerUserId.toString()))
                .andExpect(jsonPath("$.likers[0].prenom").value("Awa"))
                .andExpect(jsonPath("$.likers[0].nom").value("Ndiaye"));
            }

    private String validUpdatePayload() {
        return "{" +
                "\"titre\":\"Stage Backend Java\"," +
                "\"description\":\"Description de test\"," +
                "\"type\":\"STAGE\"," +
                "\"domaine\":\"Informatique\"," +
                "\"location\":\"Dakar\"," +
                "\"competencesRequises\":[\"Java\"]," +
                "\"dateDebut\":\"2026-07-01T10:00:00\"," +
                "\"dateFin\":\"2026-09-01T10:00:00\"" +
                "}";
    }

    private User buildBusinessUser(UUID id) {
        Pack pack = new Pack();
        pack.setNom("PREMIUM");
        pack.setFeatures("OFFERS_PUBLISH,MESSAGERIE_LIMITEE");

        User user = new User();
        user.setEmail(id + "@rezo.sn");
        user.setPasswordHash("hash");
        user.setPrenom("Owner");
        user.setNom("User");
        user.setRole(UserRole.ENTREPRISE);
        user.setPack(pack);
        setPrivateField(user, "id", id);
        return user;
    }

    private Offer buildOwnedOffer(UUID id, User ownerUser) {
        Company company = new Company();
        company.setUser(ownerUser);
        company.setRaisonSociale("Rezo Corp");
        company.setSecteurActivite("IT");
        company.setDescription("Entreprise test");

        Offer offer = new Offer();
        offer.setTitre("Offre test");
        offer.setDescription("Description test");
        offer.setType(OfferType.STAGE);
        offer.setDomaine("Informatique");
        offer.setLocation("Dakar");
        offer.setDatePublication(LocalDateTime.now());
        offer.setDateDebut(LocalDateTime.now().plusDays(1));
        offer.setDateFin(LocalDateTime.now().plusDays(30));
        offer.setCompetencesRequises(Set.of("Java"));
        offer.setOwnerEntreprise(company);
        setPrivateField(offer, "id", id);
        return offer;
    }

    private void setPrivateField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Impossible d'initialiser le champ " + fieldName, exception);
        }
    }
}
