package com.rezo.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rezo.entities.Company;
import com.rezo.entities.Message;
import com.rezo.entities.Offer;
import com.rezo.entities.Pack;
import com.rezo.entities.ProfileSwipe;
import com.rezo.entities.Swipe;
import com.rezo.entities.User;
import com.rezo.entities.enums.CompanySize;
import com.rezo.entities.enums.OfferType;
import com.rezo.entities.enums.SwipeAction;
import com.rezo.entities.enums.UserRole;
import com.rezo.repositories.CompanyRepository;
import com.rezo.repositories.MessageRepository;
import com.rezo.repositories.OfferRepository;
import com.rezo.repositories.PackRepository;
import com.rezo.repositories.ProfileSwipeRepository;
import com.rezo.repositories.SwipeRepository;
import com.rezo.repositories.UserRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class MessageApiIntegrationTest {

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
    private PackRepository packRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private SwipeRepository swipeRepository;

    @Autowired
    private ProfileSwipeRepository profileSwipeRepository;

    @Autowired
    private MessageRepository messageRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

        @Test
        void shouldReturn401OnProtectedMessageEndpointsWithoutAuthentication() throws Exception {
        String randomUserId = UUID.randomUUID().toString();
        String randomMessageId = UUID.randomUUID().toString();

        String sendPayload = """
            {
              "receiverId": "%s",
              "content": "Message sans auth"
            }
            """.formatted(randomUserId);

        mockMvc.perform(get("/api/messages"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        mockMvc.perform(post("/api/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sendPayload))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/messages/conversation/{userId}", randomUserId))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        mockMvc.perform(put("/api/messages/{id}/read", randomMessageId))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        mockMvc.perform(delete("/api/messages/{id}", randomMessageId))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        }

    @Test
    void shouldEnforceOwnershipWhenDeletingMessage() throws Exception {
        Pack messagingPack = ensureMessagingPack();

        User candidate = createUser("integration.candidate@rezo.test", UserRole.ETUDIANT, messagingPack);
        User recruiter = createUser("integration.recruiter@rezo.test", UserRole.ENTREPRISE, messagingPack);
        User outsider = createUser("integration.outsider@rezo.test", UserRole.ENTREPRISE, messagingPack);

        Company recruiterCompany = createCompanyForUser(recruiter);
        Offer offer = createOfferForCompany(recruiterCompany);

        createMutualMatch(candidate, recruiter, offer);

        Message sentMessage = createMessage(candidate, recruiter, offer, "Bonjour, je suis interesse par votre offre");

        mockMvc.perform(delete("/api/messages/{id}", sentMessage.getId())
                        .principal(outsider.getId()::toString)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.message").value("Vous ne pouvez supprimer que vos propres messages"));

        assertThat(messageRepository.findById(sentMessage.getId())).isPresent();

        mockMvc.perform(delete("/api/messages/{id}", sentMessage.getId())
                        .principal(candidate.getId()::toString)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Message supprime avec succes"));

        assertThat(messageRepository.findById(sentMessage.getId())).isEmpty();
    }

        @Test
        void shouldRejectSendingMessageWithoutMutualMatch() throws Exception {
        Pack messagingPack = ensureMessagingPack();
        User candidate = createUser("integration.msg.nomatch.candidate@rezo.test", UserRole.ETUDIANT, messagingPack);
        User recruiter = createUser("integration.msg.nomatch.recruiter@rezo.test", UserRole.ENTREPRISE, messagingPack);

        String payload = """
            {
              \"receiverId\": \"%s\",
              \"content\": \"Bonjour sans match\"
            }
            """.formatted(recruiter.getId());

        mockMvc.perform(post("/api/messages")
                .principal(candidate.getId()::toString)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
            .andExpect(jsonPath("$.error.message").value("La messagerie est reservee aux profils ayant un match mutuel"));
        }

        @Test
        void shouldSendMessageWhenMutualMatchExists() throws Exception {
        Pack messagingPack = ensureMessagingPack();
        User candidate = createUser("integration.msg.match.candidate@rezo.test", UserRole.ETUDIANT, messagingPack);
        User recruiter = createUser("integration.msg.match.recruiter@rezo.test", UserRole.ENTREPRISE, messagingPack);

        Company recruiterCompany = createCompanyForUser(recruiter);
        Offer offer = createOfferForCompany(recruiterCompany);
        createMutualMatch(candidate, recruiter, offer);

        String payload = """
            {
              \"receiverId\": \"%s\",
              \"content\": \"Bonjour avec match\",
              \"relatedOfferId\": \"%s\"
            }
            """.formatted(recruiter.getId(), offer.getId());

        mockMvc.perform(post("/api/messages")
                .principal(candidate.getId()::toString)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.senderId").value(candidate.getId().toString()))
            .andExpect(jsonPath("$.receiverId").value(recruiter.getId().toString()))
            .andExpect(jsonPath("$.relatedOfferId").value(offer.getId().toString()))
            .andExpect(jsonPath("$.content").value("Bonjour avec match"));
        }

    private Pack ensureMessagingPack() {
        return packRepository.findByNomIgnoreCase("INTEGRATION_MESSAGING")
                .orElseGet(() -> {
                    Pack pack = new Pack();
                    pack.setNom("INTEGRATION_MESSAGING");
                    pack.setDescription("Pack integration messaging");
                    pack.setPrix(BigDecimal.ZERO);
                    pack.setCible("TOUS");
                    pack.setFeatures("OFFERS_PUBLISH,OFFERS_MANAGE,MESSAGERIE_ILLIMITEE");
                    return packRepository.save(pack);
                });
    }

    private User createUser(String email, UserRole role, Pack pack) {
        User user = new User();
        user.setEmail(uniqueEmail(email));
        user.setPasswordHash("hash");
        user.setPrenom("Integration");
        user.setNom("User");
        user.setRole(role);
        user.setPack(pack);
        return userRepository.save(user);
    }

    private String uniqueEmail(String baseEmail) {
        int atIndex = baseEmail.indexOf('@');
        if (atIndex <= 0) {
            return UUID.randomUUID() + "@rezo.test";
        }
        String localPart = baseEmail.substring(0, atIndex);
        String domainPart = baseEmail.substring(atIndex + 1);
        return localPart + "." + UUID.randomUUID() + "@" + domainPart;
    }

    private Company createCompanyForUser(User user) {
        Company company = new Company();
        company.setUser(user);
        company.setRaisonSociale("Integration Recruiter Corp");
        company.setSecteurActivite("IT");
        company.setTaille(CompanySize.PME);
        company.setDescription("Entreprise de test integration");
        return companyRepository.save(company);
    }

    private Offer createOfferForCompany(Company company) {
        Offer offer = new Offer();
        offer.setOwnerEntreprise(company);
        offer.setTitre("Offre integration message");
        offer.setDescription("Offre pour scenario message integration");
        offer.setType(OfferType.STAGE);
        offer.setDomaine("Informatique");
        offer.setLocation("Dakar");
        offer.setCompetencesRequises(Set.of("Java"));
        offer.setDatePublication(LocalDateTime.now().minusDays(1));
        offer.setDateDebut(LocalDateTime.now().plusDays(1));
        offer.setDateFin(LocalDateTime.now().plusDays(30));
        return offerRepository.save(offer);
    }

    private void createMutualMatch(User candidate, User recruiter, Offer offer) {
        Swipe candidateLike = new Swipe();
        candidateLike.setUser(candidate);
        candidateLike.setOffer(offer);
        candidateLike.setAction(SwipeAction.LIKE);
        swipeRepository.save(candidateLike);

        ProfileSwipe recruiterLike = new ProfileSwipe();
        recruiterLike.setSwiper(recruiter);
        recruiterLike.setTargetUser(candidate);
        recruiterLike.setAction(SwipeAction.LIKE);
        profileSwipeRepository.save(recruiterLike);
    }

    private Message createMessage(User sender, User receiver, Offer offer, String content) {
        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setOffer(offer);
        message.setContent(content);
        message.setRead(false);
        return messageRepository.save(message);
    }
}
