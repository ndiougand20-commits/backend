package com.rezo.backend.controller;

import com.rezo.backend.dto.match.MatchRecommendationItem;
import com.rezo.backend.dto.match.MatchRecommendationsResponse;
import com.rezo.backend.dto.match.SuggestedPackResponse;
import com.rezo.backend.dto.offer.OfferResponse;
import com.rezo.backend.service.PackRules;
import com.rezo.entities.Company;
import com.rezo.entities.Offer;
import com.rezo.entities.Pack;
import com.rezo.entities.Profile;
import com.rezo.entities.School;
import com.rezo.entities.User;
import com.rezo.entities.enums.OfferType;
import com.rezo.entities.enums.UserRole;
import com.rezo.repositories.CompanyRepository;
import com.rezo.repositories.OfferRepository;
import com.rezo.repositories.PackRepository;
import com.rezo.repositories.ProfileRepository;
import com.rezo.repositories.SchoolRepository;
import com.rezo.repositories.SwipeRepository;
import com.rezo.repositories.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/match")
@SecurityRequirement(name = "bearer-jwt")
public class MatchController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MatchController.class);

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final OfferRepository offerRepository;
    private final SwipeRepository swipeRepository;
    private final PackRepository packRepository;
    private final CompanyRepository companyRepository;
    private final SchoolRepository schoolRepository;

    public MatchController(UserRepository userRepository,
                           ProfileRepository profileRepository,
                           OfferRepository offerRepository,
                           SwipeRepository swipeRepository,
                           PackRepository packRepository,
                           CompanyRepository companyRepository,
                           SchoolRepository schoolRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.offerRepository = offerRepository;
        this.swipeRepository = swipeRepository;
        this.packRepository = packRepository;
        this.companyRepository = companyRepository;
        this.schoolRepository = schoolRepository;
    }

    @Operation(summary = "Recommendations intelligentes", description = "Retourne des opportunites scorees selon le profil du user, exclut les offres deja swipees et suggere un pack adapte")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recommendations calculees"),
            @ApiResponse(responseCode = "401", description = "Non authentifie")
    })
    @GetMapping("/recommendations")
    @Transactional
    public ResponseEntity<?> getRecommendations(Principal principal) {
        try {
            User user = resolveAuthenticatedUser(principal);
            Set<UUID> swipedOfferIds = swipeRepository.findOfferIdsByUserId(user.getId());
            MatchProfileSnapshot snapshot = buildSnapshot(user);

            List<MatchRecommendationItem> recommendations = new ArrayList<>();
            int evaluatedCount = 0;

            if (!PackRules.canViewOpportunities(user)) {
                LOGGER.info("Matching blocked userId={} pack={} reason=no-opportunity-access", user.getId(), currentPackName(user));
            } else {
                for (Offer offer : offerRepository.findAllWithOwners()) {
                    if (offer.getId() == null) {
                        continue;
                    }
                    if (swipedOfferIds.contains(offer.getId())) {
                        LOGGER.info("Matching trace userId={} offerId={} excluded=already_swiped", user.getId(), offer.getId());
                        continue;
                    }
                    evaluatedCount++;
                    MatchRecommendationItem item = scoreOffer(user, snapshot, offer);
                    recommendations.add(item);
                }
            }

            recommendations.sort(Comparator.comparingInt(MatchRecommendationItem::getScore).reversed());
            if (recommendations.size() > 10) {
                recommendations = new ArrayList<>(recommendations.subList(0, 10));
            }

            SuggestedPackResponse suggestedPack = suggestPack(user, recommendations.size());

            MatchRecommendationsResponse response = new MatchRecommendationsResponse();
            response.setRecommendations(recommendations);
            response.setSuggestedPack(suggestedPack);
            response.setTrace(buildTrace(user, swipedOfferIds.size(), evaluatedCount, recommendations.size()));
            return ResponseEntity.ok(response);
        } catch (UnauthorizedException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", exception.getMessage()));
        }
    }

    private User resolveAuthenticatedUser(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new UnauthorizedException("Non authentifie");
        }
        UUID userId = UUID.fromString(principal.getName());
        return userRepository.findByIdWithPack(userId)
                .orElseThrow(() -> new UnauthorizedException("Utilisateur authentifie introuvable"));
    }

    private MatchProfileSnapshot buildSnapshot(User user) {
        Set<String> domains = new LinkedHashSet<>();
        Set<String> locations = new LinkedHashSet<>();
        Set<String> skills = new LinkedHashSet<>();
        Set<String> goals = new LinkedHashSet<>();

        switch (user.getRole()) {
            case ETUDIANT, EMPLOI -> {
                Optional<Profile> optionalProfile = profileRepository.findByUserId(user.getId());
                optionalProfile.ifPresent(profile -> {
                    addText(domains, profile.getDomaine());
                    addAllText(domains, profile.getPreferencesSecteur());
                    addText(goals, profile.getObjectif());
                    addAllText(locations, profile.getPreferencesLieu());
                    addAllText(skills, profile.getCompetences());
                    addAllText(goals, profile.getExperiences());
                });
                if (user.getRole() == UserRole.ETUDIANT) {
                    goals.add("stage");
                }
                if (user.getRole() == UserRole.EMPLOI) {
                    goals.add("emploi");
                    goals.add("job");
                }
            }
            case LYCEEN -> {
                Optional<Profile> optionalProfile = profileRepository.findByUserId(user.getId());
                optionalProfile.ifPresent(profile -> {
                    addText(goals, profile.getObjectifPostbac());
                    addAllText(domains, profile.getCentresInteret());
                    addText(goals, profile.getSerieOrientation());
                });
                goals.add("stage");
                goals.add("orientation");
            }
            case ENTREPRISE -> companyRepository.findByUserId(user.getId()).ifPresent(company -> {
                addText(domains, company.getSecteurActivite());
                addText(locations, company.getAdresse());
                addText(goals, company.getDescription());
                addText(goals, company.getRaisonSociale());
            });
            case ECOLE -> schoolRepository.findByUserId(user.getId()).ifPresent(school -> {
                addAllText(domains, school.getDomaines());
                addText(locations, school.getAdresse());
                addText(goals, school.getDescription());
                addText(goals, school.getNomEtablissement());
            });
            default -> goals.add("matching");
        }

        LOGGER.info("Matching snapshot userId={} role={} domains={} skills={} goals={} locations={}",
                user.getId(), user.getRole(), domains, skills, goals, locations);
        return new MatchProfileSnapshot(domains, skills, goals, locations);
    }

    private MatchRecommendationItem scoreOffer(User user, MatchProfileSnapshot snapshot, Offer offer) {
        int score = 10;
        List<String> reasons = new ArrayList<>();

        if (matches(snapshot.domains(), offer.getDomaine(), offer.getTitre(), offer.getDescription())) {
            score += 30;
            reasons.add("domaine compatible");
        }

        int skillMatches = countMatches(snapshot.skills(), offer.getCompetencesRequises());
        if (skillMatches > 0) {
            score += Math.min(30, skillMatches * 12);
            reasons.add(skillMatches + " competence(s) commune(s)");
        }

        if (matches(snapshot.goals(), offer.getTitre(), offer.getDescription(), offer.getType() != null ? offer.getType().name() : null)) {
            score += 15;
            reasons.add("objectif coherent");
        }

        if (matches(snapshot.locations(), offer.getLocation())) {
            score += 15;
            reasons.add("localisation preferee");
        }

        if ((user.getRole() == UserRole.ETUDIANT || user.getRole() == UserRole.LYCEEN) && offer.getType() == OfferType.STAGE) {
            score += 10;
            reasons.add("format stage adapte");
        }
        if (user.getRole() == UserRole.EMPLOI && offer.getType() == OfferType.EMPLOI) {
            score += 12;
            reasons.add("format emploi adapte");
        }
        if ((user.getRole() == UserRole.ENTREPRISE || user.getRole() == UserRole.ECOLE) && offer.getType() == OfferType.EMPLOI) {
            score += 8;
            reasons.add("veille recrutement pertinente");
        }

        score = Math.min(100, score);
        LOGGER.info("Matching scoring userId={} offerId={} score={} reasons={}", user.getId(), offer.getId(), score, reasons);

        MatchRecommendationItem item = new MatchRecommendationItem();
        item.setOfferId(offer.getId());
        item.setScore(score);
        item.setReasons(reasons);
        item.setOffer(toOfferResponse(offer));
        return item;
    }

    private SuggestedPackResponse suggestPack(User user, int recommendationCount) {
        Set<String> currentFeatures = user.getPack() != null ? PackRules.parseCsv(user.getPack().getFeatures()) : Set.of();
        String reason;
        String preferredFeature;

        if ((user.getRole() == UserRole.ENTREPRISE || user.getRole() == UserRole.ECOLE) && !PackRules.canManageOffers(user)) {
            preferredFeature = "OFFERS_PUBLISH";
            reason = "Publication et gestion des offres limitees sur votre pack actuel";
        } else if (!currentFeatures.contains("MESSAGERIE_ILLIMITEE")) {
            preferredFeature = "MESSAGERIE_ILLIMITEE";
            reason = "Acces illimite a la messagerie pour contacter plus facilement les recruteurs";
        } else if (!PackRules.canUseAiChat(user)) {
            preferredFeature = "AI_CHAT_ACCESS";
            reason = "Acces au chat IA pour booster votre accompagnement";
        } else if (recommendationCount >= 3 && !currentFeatures.contains("MATCHING_PREMIUM")) {
            preferredFeature = "MATCHING_PREMIUM";
            reason = "Matching avance pour afficher davantage d'opportunites pertinentes";
        } else {
            preferredFeature = null;
            reason = "Votre pack actuel couvre deja bien vos besoins";
        }

        Pack chosenPack = chooseSuggestedPack(user, preferredFeature).orElse(user.getPack());

        SuggestedPackResponse suggestion = new SuggestedPackResponse();
        suggestion.setId(chosenPack != null ? chosenPack.getId() : null);
        suggestion.setLabel(chosenPack != null ? chosenPack.getNom() : currentPackName(user));
        suggestion.setReason(reason);

        LOGGER.info("Pack suggestion userId={} currentPack={} suggestedPack={} reason={}",
                user.getId(), currentPackName(user), suggestion.getLabel(), reason);
        return suggestion;
    }

    private Optional<Pack> chooseSuggestedPack(User user, String preferredFeature) {
        return packRepository.findAll().stream()
                .filter(pack -> PackRules.isPackCompatible(pack, user.getRole()))
                .filter(pack -> user.getPack() == null || !pack.getId().equals(user.getPack().getId()))
                .sorted((left, right) -> {
                    int leftScore = rankPack(left, preferredFeature);
                    int rightScore = rankPack(right, preferredFeature);
                    return Integer.compare(rightScore, leftScore);
                })
                .findFirst();
    }

    private int rankPack(Pack pack, String preferredFeature) {
        int score = PackRules.parseCsv(pack.getFeatures()).size();
        if (preferredFeature != null && PackRules.parseCsv(pack.getFeatures()).contains(preferredFeature)) {
            score += 100;
        }
        return score;
    }

    private Map<String, Object> buildTrace(User user, int excludedSwipeCount, int evaluatedCount, int returnedCount) {
        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("userId", user.getId());
        trace.put("role", user.getRole().name());
        trace.put("currentPack", currentPackName(user));
        trace.put("excludedSwipeCount", excludedSwipeCount);
        trace.put("evaluatedCount", evaluatedCount);
        trace.put("returnedCount", returnedCount);
        trace.put("timestamp", LocalDateTime.now());
        return trace;
    }

    private OfferResponse toOfferResponse(Offer offer) {
        OfferResponse response = new OfferResponse();
        response.setId(offer.getId());
        response.setTitre(offer.getTitre());
        response.setDescription(offer.getDescription());
        response.setType(offer.getType() != null ? offer.getType().name() : null);
        response.setDomaine(offer.getDomaine());
        response.setLocation(offer.getLocation());
        response.setCompetencesRequises(offer.getCompetencesRequises() != null ? new LinkedHashSet<>(offer.getCompetencesRequises()) : new LinkedHashSet<>());
        response.setDatePublication(offer.getDatePublication());
        response.setDateDebut(offer.getDateDebut());
        response.setDateFin(offer.getDateFin());
        response.setCreatedAt(offer.getCreatedAt());
        if (offer.getOwnerEntreprise() != null) {
            response.setOwnerType("ENTREPRISE");
            response.setOwnerCompanyId(offer.getOwnerEntreprise().getId());
            response.setOwnerUserId(offer.getOwnerEntreprise().getUser() != null ? offer.getOwnerEntreprise().getUser().getId() : null);
            response.setOwnerDisplayName(offer.getOwnerEntreprise().getRaisonSociale());
        } else if (offer.getOwnerEcole() != null) {
            response.setOwnerType("ECOLE");
            response.setOwnerSchoolId(offer.getOwnerEcole().getId());
            response.setOwnerUserId(offer.getOwnerEcole().getUser() != null ? offer.getOwnerEcole().getUser().getId() : null);
            response.setOwnerDisplayName(offer.getOwnerEcole().getNomEtablissement());
        }
        return response;
    }

    private void addText(Set<String> target, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        target.add(normalized);
        for (String token : normalized.split("[^a-z0-9à-ÿ]+")) {
            if (!token.isBlank()) {
                target.add(token);
            }
        }
    }

    private void addAllText(Set<String> target, Set<String> values) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            addText(target, value);
        }
    }

    private boolean matches(Set<String> keywords, String... haystacks) {
        if (keywords == null || keywords.isEmpty()) {
            return false;
        }
        for (String haystack : haystacks) {
            if (haystack == null || haystack.isBlank()) {
                continue;
            }
            String normalizedHaystack = haystack.toLowerCase(Locale.ROOT);
            for (String keyword : keywords) {
                if (!keyword.isBlank() && normalizedHaystack.contains(keyword)) {
                    return true;
                }
            }
        }
        return false;
    }

    private int countMatches(Set<String> userSkills, Set<String> offerSkills) {
        if (userSkills == null || userSkills.isEmpty() || offerSkills == null || offerSkills.isEmpty()) {
            return 0;
        }
        int count = 0;
        Set<String> normalizedOfferSkills = new LinkedHashSet<>();
        for (String skill : offerSkills) {
            if (skill != null && !skill.isBlank()) {
                normalizedOfferSkills.add(skill.trim().toLowerCase(Locale.ROOT));
            }
        }
        for (String skill : userSkills) {
            if (skill != null && normalizedOfferSkills.contains(skill.trim().toLowerCase(Locale.ROOT))) {
                count++;
            }
        }
        return count;
    }

    private String currentPackName(User user) {
        return user != null && user.getPack() != null ? user.getPack().getNom() : "AUCUN";
    }

    private record MatchProfileSnapshot(Set<String> domains, Set<String> skills, Set<String> goals, Set<String> locations) {
    }

    private static class UnauthorizedException extends RuntimeException {
        private UnauthorizedException(String message) {
            super(message);
        }
    }
}
