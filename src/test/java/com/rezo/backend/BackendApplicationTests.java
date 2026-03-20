package com.rezo.backend;

import com.rezo.entities.ChatSupport;
import com.rezo.entities.Company;
import com.rezo.entities.Message;
import com.rezo.entities.Offer;
import com.rezo.entities.Pack;
import com.rezo.entities.Profile;
import com.rezo.entities.Swipe;
import com.rezo.entities.User;
import com.rezo.entities.enums.CompanySize;
import com.rezo.entities.enums.OfferType;
import com.rezo.entities.enums.SwipeAction;
import com.rezo.entities.enums.UserRole;
import com.rezo.repositories.ChatSupportRepository;
import com.rezo.repositories.CompanyRepository;
import com.rezo.repositories.MessageRepository;
import com.rezo.repositories.OfferRepository;
import com.rezo.repositories.PackRepository;
import com.rezo.repositories.ProfileRepository;
import com.rezo.repositories.SwipeRepository;
import com.rezo.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class BackendApplicationTests {

	@Autowired
	private PackRepository packRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ProfileRepository profileRepository;

	@Autowired
	private CompanyRepository companyRepository;

	@Autowired
	private OfferRepository offerRepository;

	@Autowired
	private SwipeRepository swipeRepository;

	@Autowired
	private MessageRepository messageRepository;

	@Autowired
	private ChatSupportRepository chatSupportRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void shouldPerformCrudOnPack() {
		Pack pack = new Pack();
		pack.setNom("PACK_TEST");
		pack.setDescription("Pack pour test CRUD");
		pack.setPrix(new BigDecimal("19.90"));
		pack.setCible("ETUDIANT");
		pack.setFeatures("MATCHING,MESSAGE");

		Pack created = packRepository.save(pack);
		assertNotNull(created.getId());

		Optional<Pack> loaded = packRepository.findById(created.getId());
		assertTrue(loaded.isPresent());
		assertEquals("PACK_TEST", loaded.get().getNom());

		loaded.get().setPrix(new BigDecimal("29.90"));
		Pack updated = packRepository.save(loaded.get());
		assertEquals(new BigDecimal("29.90"), updated.getPrix());

		packRepository.deleteById(updated.getId());
		assertTrue(packRepository.findById(updated.getId()).isEmpty());
	}

	@Test
	void shouldPersistAndReadEntityRelations() {
		Pack pack = new Pack();
		pack.setNom("PACK_REL");
		pack.setDescription("Pack pour test relationnel");
		pack.setPrix(new BigDecimal("0.00"));
		pack.setCible("TOUS");
		pack.setFeatures("BASE");
		pack = packRepository.save(pack);

		User owner = new User();
		owner.setEmail("owner-" + UUID.randomUUID() + "@rezo.test");
		owner.setPasswordHash("hash-owner");
		owner.setPrenom("Owner");
		owner.setNom("Company");
		owner.setRole(UserRole.ENTREPRISE);
		owner.setPack(pack);
		owner = userRepository.save(owner);

		User candidate = new User();
		candidate.setEmail("candidate-" + UUID.randomUUID() + "@rezo.test");
		candidate.setPasswordHash("hash-candidate");
		candidate.setPrenom("Candidate");
		candidate.setNom("User");
		candidate.setRole(UserRole.ETUDIANT);
		candidate.setPack(pack);
		candidate = userRepository.save(candidate);

		Profile profile = new Profile();
		profile.setUser(candidate);
		profile.setNiveauEtude("Licence");
		profile.setDomaine("Informatique");
		profile.setCompetences(Set.of("Java", "Spring"));
		profile.setObjectif("Stage");
		profile = profileRepository.save(profile);

		Company company = new Company();
		company.setUser(owner);
		company.setRaisonSociale("Rezo Tech");
		company.setSecteurActivite("IT");
		company.setTaille(CompanySize.PME);
		company.setDescription("Entreprise de test");
		company = companyRepository.save(company);

		Offer offer = new Offer();
		offer.setTitre("Stage Backend Java");
		offer.setDescription("Offre de stage backend");
		offer.setType(OfferType.STAGE);
		offer.setDomaine("Informatique");
		offer.setLocation("Dakar");
		offer.setOwnerEntreprise(company);
		offer = offerRepository.save(offer);

		Swipe swipe = new Swipe();
		swipe.setUser(candidate);
		swipe.setOffer(offer);
		swipe.setAction(SwipeAction.LIKE);
		swipe = swipeRepository.save(swipe);

		Message message = new Message();
		message.setSender(candidate);
		message.setReceiver(owner);
		message.setOffer(offer);
		message.setContent("Bonjour, je suis interesse par l'offre.");
		message = messageRepository.save(message);

		ChatSupport chat = new ChatSupport();
		chat.setUser(candidate);
		chat.setUserMessage("Quel pack recommandes-tu ?");
		chat.setIaResponse("Le pack FREE pour commencer.");
		chat.setContext("PACK_HELP");
		chat.setSessionId(UUID.randomUUID());
		chat = chatSupportRepository.save(chat);

		assertTrue(userRepository.findByEmail(candidate.getEmail()).isPresent());
		assertTrue(profileRepository.findById(profile.getId()).isPresent());
		assertTrue(companyRepository.findById(company.getId()).isPresent());
		assertTrue(offerRepository.findById(offer.getId()).isPresent());
		assertTrue(swipeRepository.findById(swipe.getId()).isPresent());
		assertTrue(messageRepository.findById(message.getId()).isPresent());
		assertTrue(chatSupportRepository.findById(chat.getId()).isPresent());
	}

}
