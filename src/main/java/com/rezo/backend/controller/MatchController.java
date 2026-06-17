package com.rezo.backend.controller;

import com.rezo.backend.dto.match.MatchRecommendationItem;
import com.rezo.backend.dto.match.MatchRecommendationsResponse;
import com.rezo.backend.dto.match.ProfileSwipeRequest;
import com.rezo.backend.dto.match.SuggestedPackResponse;
import com.rezo.backend.dto.match.SwipeRequest;
import com.rezo.backend.dto.offer.OfferResponse;
import com.rezo.backend.dto.school.SchoolResponse;
import com.rezo.backend.service.PackRules;
import com.rezo.entities.Company;
import com.rezo.entities.Offer;
import com.rezo.entities.Pack;
import com.rezo.entities.Profile;
import com.rezo.entities.ProfileSwipe;
import com.rezo.entities.School;
import com.rezo.entities.Swipe;
import com.rezo.entities.User;
import com.rezo.entities.UserMediaFile;
import com.rezo.entities.enums.OfferType;
import com.rezo.entities.enums.SwipeAction;
import com.rezo.entities.enums.UserRole;
import com.rezo.repositories.CompanyRepository;
import com.rezo.repositories.OfferRepository;
import com.rezo.repositories.PackRepository;
import com.rezo.repositories.ProfileSwipeRepository;
import com.rezo.repositories.ProfileRepository;
import com.rezo.repositories.SchoolRepository;
import com.rezo.repositories.SwipeRepository;
import com.rezo.repositories.UserMediaFileRepository;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final ProfileSwipeRepository profileSwipeRepository;
    private final UserMediaFileRepository userMediaFileRepository;

    public MatchController(UserRepository userRepository,
                           ProfileRepository profileRepository,
                           OfferRepository offerRepository,
                           SwipeRepository swipeRepository,
                           PackRepository packRepository,
                           CompanyRepository companyRepository,
                           SchoolRepository schoolRepository,
                           ProfileSwipeRepository profileSwipeRepository,
                           UserMediaFileRepository userMediaFileRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.offerRepository = offerRepository;
        this.swipeRepository = swipeRepository;
        this.packRepository = packRepository;
        this.companyRepository = companyRepository;
        this.schoolRepository = schoolRepository;
        this.profileSwipeRepository = profileSwipeRepository;
        this.userMediaFileRepository = userMediaFileRepository;
    }

    @Operation(summary = "Recommendations intelligentes", description = "Retourne des opportunites scorees selon le profil du user, exclut les offres deja swipees et suggere un pack adapte")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recommendations calculees"),
            @ApiResponse(responseCode = "401", description = "Non authentifie")
    })
    @GetMapping("/recommendations")
    @Transactional
        public ResponseEntity<?> getRecommendations(
            Principal principal,
            @RequestParam(name = "includeSwiped", defaultValue = "false") boolean includeSwiped
        ) {
        try {
            User user = resolveAuthenticatedUser(principal);
            Set<UUID> swipedOfferIds = swipeRepository.findOfferIdsByUserId(user.getId());
            MatchProfileSnapshot snapshot = buildSnapshot(user);
            boolean hasOpportunityAccess = PackRules.canViewOpportunities(user);
            List<Offer> allOffers = hasOpportunityAccess ? offerRepository.findAllWithOwners() : List.of();

            List<MatchRecommendationItem> recommendations = new ArrayList<>();
            int evaluatedCount = 0;

            if (!hasOpportunityAccess) {
                LOGGER.info("Matching blocked userId={} pack={} reason=no-opportunity-access", user.getId(), currentPackName(user));
            } else {
                for (Offer offer : allOffers) {
                    if (offer.getId() == null) {
                        continue;
                    }
                    if (!includeSwiped && swipedOfferIds.contains(offer.getId())) {
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
            response.setTrace(buildTrace(
                    user,
                    swipedOfferIds.size(),
                    evaluatedCount,
                    recommendations.size(),
                    hasOpportunityAccess,
                    allOffers.size()
            ));
            return ResponseEntity.ok(response);
        } catch (UnauthorizedException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", exception.getMessage()));
        }
    }

    @Operation(summary = "Enregistrer un swipe", description = "Enregistre un LIKE ou DISLIKE sur une offre. Si un swipe existe deja pour la meme paire user/offre, l'action est mise a jour.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Swipe enregistre ou mis a jour"),
            @ApiResponse(responseCode = "400", description = "Payload invalide"),
            @ApiResponse(responseCode = "401", description = "Non authentifie"),
            @ApiResponse(responseCode = "404", description = "Offre introuvable")
    })
    @PostMapping("/swipe")
    @Transactional
    public ResponseEntity<?> recordSwipe(@RequestBody SwipeRequest request, Principal principal) {
        try {
            User user = resolveAuthenticatedUser(principal);

            if (request == null || request.getOfferId() == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Le champ offerId est obligatoire"));
            }
            if (request.getAction() == null || request.getAction().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Le champ action est obligatoire (LIKE ou DISLIKE)"));
            }

            SwipeAction action;
            try {
                action = SwipeAction.valueOf(request.getAction().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("message", "Action invalide : utilisez LIKE ou DISLIKE"));
            }

            Optional<Offer> offerOpt = offerRepository.findById(request.getOfferId());
            if (offerOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Offre introuvable"));
            }

            Optional<Swipe> existing = swipeRepository.findByUserIdAndOfferId(user.getId(), request.getOfferId());
            Swipe swipe;
            if (existing.isPresent()) {
                swipe = existing.get();
                swipe.setAction(action);
            } else {
                swipe = new Swipe();
                swipe.setUser(user);
                swipe.setOffer(offerOpt.get());
                swipe.setAction(action);
            }
            swipe = swipeRepository.save(swipe);

            LOGGER.info("Swipe enregistre userId={} offerId={} action={}", user.getId(), request.getOfferId(), action);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("swipeId", swipe.getId());
            body.put("offerId", request.getOfferId());
            body.put("action", action.name());
            body.put("createdAt", swipe.getCreatedAt());
            return ResponseEntity.status(HttpStatus.CREATED).body(body);
        } catch (UnauthorizedException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", exception.getMessage()));
        }
    }

    @Operation(summary = "Recommandations d'ecoles (lyceens)", description = "Retourne des ecoles scorees selon le profil du lyceen (centresInteret, objectifPostbac, serieOrientation). Requiert le role LYCEEN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recommandations calculees"),
            @ApiResponse(responseCode = "401", description = "Non authentifie"),
            @ApiResponse(responseCode = "403", description = "Acces reserve aux lyceens")
    })
    @GetMapping("/school-recommendations")
    @Transactional
    public ResponseEntity<?> getSchoolRecommendations(
            Principal principal,
            @RequestParam(name = "includeSwiped", defaultValue = "false") boolean includeSwiped,
            @RequestParam(name = "secteur", required = false) String secteur
    ) {
        try {
            User user = resolveAuthenticatedUser(principal);

            if (user.getRole() != UserRole.LYCEEN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Les recommandations d'ecoles sont reservees aux lyceens"));
            }

            Set<String> centresInteret = new LinkedHashSet<>();
            String objectifPostbac = null;
            String serieOrientation = null;

            Optional<Profile> optProfile = profileRepository.findByUserId(user.getId());
            if (optProfile.isPresent()) {
                Profile profile = optProfile.get();
                addAllText(centresInteret, profile.getCentresInteret());
                addText(centresInteret, profile.getDomaine());
                objectifPostbac = profile.getObjectifPostbac();
                serieOrientation = profile.getSerieOrientation();
            }

            String secteurNormalized = secteur != null && !secteur.isBlank()
                    ? secteur.trim().toLowerCase(Locale.ROOT)
                    : null;

            List<School> allSchools = schoolRepository.findAllWithUser();

            List<Map<String, Object>> scored = new ArrayList<>();
            int filteredOutBySectorCount = 0;
            for (School school : allSchools) {
                if (secteurNormalized != null && !matches(Set.of(secteurNormalized),
                        school.getDescription(),
                        school.getNomEtablissement(),
                        school.getAdresse(),
                        school.getSiteWeb(),
                        school.getDomaines() != null ? String.join(" ", school.getDomaines()) : null)) {
                    filteredOutBySectorCount++;
                    continue;
                }

                int score = 5;
                List<String> reasons = new ArrayList<>();

                // Intérêts / domaines
                int domainMatchCount = 0;
                if (school.getDomaines() != null && !centresInteret.isEmpty()) {
                    for (String domaine : school.getDomaines()) {
                        if (domaine == null) continue;
                        String domaineNorm = domaine.toLowerCase(Locale.ROOT);
                        for (String interet : centresInteret) {
                            if (!interet.isBlank() && domaineNorm.contains(interet)) {
                                domainMatchCount++;
                                break;
                            }
                        }
                    }
                }
                if (domainMatchCount > 0) {
                    score += Math.min(50, domainMatchCount * 25);
                    reasons.add("domaine(s) compatible(s) avec vos interets");
                }

                // Objectif post-bac vs diplômes délivrés
                if (objectifPostbac != null && !objectifPostbac.isBlank() && school.getDiplomesDelivres() != null) {
                    String obj = objectifPostbac.toLowerCase(Locale.ROOT);
                    for (String diplome : school.getDiplomesDelivres()) {
                        if (diplome != null && diplome.toLowerCase(Locale.ROOT).contains(obj)) {
                            score += 30;
                            reasons.add("diplome correspondant a votre objectif post-bac");
                            break;
                        }
                    }
                }

                // Série d'orientation vs domaines / description
                if (serieOrientation != null && !serieOrientation.isBlank()) {
                    String serie = serieOrientation.toLowerCase(Locale.ROOT);
                    boolean serieMatch = false;
                    if (school.getDomaines() != null) {
                        for (String domaine : school.getDomaines()) {
                            if (domaine != null && domaine.toLowerCase(Locale.ROOT).contains(serie)) {
                                serieMatch = true;
                                break;
                            }
                        }
                    }
                    if (!serieMatch && school.getDescription() != null && school.getDescription().toLowerCase(Locale.ROOT).contains(serie)) {
                        serieMatch = true;
                    }
                    if (serieMatch) {
                        score += 15;
                        reasons.add("serie d'orientation compatible");
                    }
                }

                if (reasons.isEmpty()) {
                    reasons.add("ecole disponible sur la plateforme");
                }

                score = Math.min(100, score);
                LOGGER.info("School scoring userId={} schoolId={} score={} reasons={}", user.getId(), school.getId(), score, reasons);

                Map<String, Object> item = new LinkedHashMap<>();
                item.put("score", score);
                item.put("reasons", reasons);
                item.put("school", toSchoolResponse(school));
                scored.add(item);
            }

            scored.sort((a, b) -> Integer.compare((int) b.get("score"), (int) a.get("score")));
            if (scored.size() > 10) {
                scored = scored.subList(0, 10);
            }

            Map<String, Object> trace = new LinkedHashMap<>();
            trace.put("userId", user.getId());
            trace.put("role", user.getRole().name());
            trace.put("evaluatedSchoolCount", allSchools.size());
            trace.put("filteredOutBySectorCount", filteredOutBySectorCount);
            trace.put("returnedCount", scored.size());
            trace.put("includeSwiped", includeSwiped);
            trace.put("secteur", secteur);
            trace.put("timestamp", LocalDateTime.now());

            if (includeSwiped) {
                LOGGER.info("School recommendations includeSwiped=true requested by userId={} (non applicable in school mode)", user.getId());
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("recommendations", scored);
            response.put("trace", trace);
            return ResponseEntity.ok(response);
        } catch (UnauthorizedException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", exception.getMessage()));
        }
    }

    @Operation(summary = "Recommendations de profils (ecole/entreprise)", description = "Retourne des profils de candidats scores pour les roles ECOLE et ENTREPRISE")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recommandations calculees"),
            @ApiResponse(responseCode = "401", description = "Non authentifie"),
            @ApiResponse(responseCode = "403", description = "Acces reserve aux ecoles et entreprises")
    })
    @GetMapping("/profile-recommendations")
    @Transactional
    public ResponseEntity<?> getProfileRecommendations(
            Principal principal,
            @RequestParam(name = "includeSwiped", defaultValue = "false") boolean includeSwiped
    ) {
        try {
            User user = resolveAuthenticatedUser(principal);
            if (user.getRole() != UserRole.ECOLE && user.getRole() != UserRole.ENTREPRISE) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Les recommandations de profils sont reservees aux ecoles et entreprises"));
            }

            Set<UUID> alreadySwipedTargetIds = new LinkedHashSet<>(
                    profileSwipeRepository.findTargetUserIdsBySwiperId(user.getId())
            );

            Set<String> recruiterKeywords = new LinkedHashSet<>();
            if (user.getRole() == UserRole.ENTREPRISE) {
                companyRepository.findByUserId(user.getId()).ifPresent(company -> {
                    addText(recruiterKeywords, company.getSecteurActivite());
                    addText(recruiterKeywords, company.getDescription());
                    addText(recruiterKeywords, company.getRaisonSociale());
                });
            } else {
                schoolRepository.findByUserId(user.getId()).ifPresent(school -> {
                    addAllText(recruiterKeywords, school.getDomaines());
                    addText(recruiterKeywords, school.getDescription());
                    addText(recruiterKeywords, school.getNomEtablissement());
                });
            }

            List<User> users = userRepository.findByRoleIn(List.of(UserRole.ETUDIANT, UserRole.LYCEEN));
            List<Map<String, Object>> recommendations = new ArrayList<>();
            int evaluatedCount = 0;

            for (User target : users) {
                if (target.getId() == null || target.getId().equals(user.getId())) {
                    continue;
                }
                if (!isProfileRecommendationTargetAllowed(user.getRole(), target.getRole())) {
                    continue;
                }
                if (!includeSwiped && alreadySwipedTargetIds.contains(target.getId())) {
                    continue;
                }

                evaluatedCount++;
                Optional<Profile> targetProfile = profileRepository.findByUserId(target.getId());
                Set<String> targetKeywords = new LinkedHashSet<>();
                targetProfile.ifPresent(profile -> {
                    addText(targetKeywords, profile.getDomaine());
                    addText(targetKeywords, profile.getNiveauEtude());
                    addText(targetKeywords, profile.getClasseActuelle());
                    addText(targetKeywords, profile.getSerieOrientation());
                    addText(targetKeywords, profile.getObjectif());
                    addText(targetKeywords, profile.getObjectifPostbac());
                    addAllText(targetKeywords, profile.getCompetences());
                    addAllText(targetKeywords, profile.getPreferencesSecteur());
                    addAllText(targetKeywords, profile.getCentresInteret());
                });

                int score = 10;
                List<String> reasons = new ArrayList<>();
                int keywordMatches = countKeywordMatches(recruiterKeywords, targetKeywords);
                if (keywordMatches > 0) {
                    score += Math.min(70, keywordMatches * 14);
                    reasons.add(keywordMatches + " correspondance(s) sur secteur/competences");
                }
                if (target.getRole() == UserRole.ETUDIANT) {
                    score += 8;
                    reasons.add("profil etudiant");
                }
                if (target.getRole() == UserRole.LYCEEN && user.getRole() == UserRole.ECOLE) {
                    score += 8;
                    reasons.add("profil lyceen adapte a la formation");
                }
                if (reasons.isEmpty()) {
                    reasons.add("profil disponible sur la plateforme");
                }
                score = Math.min(100, score);

                List<Map<String, Object>> media = userMediaFileRepository
                        .findByUserIdOrderByCreatedAtDesc(target.getId())
                        .stream()
                        .limit(5)
                        .map(this::toMediaMap)
                        .toList();

                Map<String, Object> item = new LinkedHashMap<>();
                item.put("userId", target.getId());
                item.put("prenom", target.getPrenom());
                item.put("nom", target.getNom());
                item.put("avatarUrl", target.getAvatarUrl());
                item.put("role", target.getRole() != null ? target.getRole().name() : null);
                item.put("niveauEtude", targetProfile.map(Profile::getNiveauEtude).orElse(null));
                item.put("domaine", targetProfile.map(Profile::getDomaine).orElse(null));
                item.put("classeActuelle", targetProfile.map(Profile::getClasseActuelle).orElse(null));
                item.put("serieOrientation", targetProfile.map(Profile::getSerieOrientation).orElse(null));
                item.put("competences", targetProfile.map(Profile::getCompetences).orElse(Set.of()));
                item.put("score", score);
                item.put("reasons", reasons);
                item.put("mediaFiles", media);
                recommendations.add(item);
            }

            recommendations.sort((a, b) -> Integer.compare((int) b.get("score"), (int) a.get("score")));
            if (recommendations.size() > 10) {
                recommendations = new ArrayList<>(recommendations.subList(0, 10));
            }

            Map<String, Object> trace = new LinkedHashMap<>();
            trace.put("userId", user.getId());
            trace.put("role", user.getRole().name());
            trace.put("includeSwiped", includeSwiped);
            trace.put("excludedSwipeCount", alreadySwipedTargetIds.size());
            trace.put("evaluatedCount", evaluatedCount);
            trace.put("returnedCount", recommendations.size());
            trace.put("timestamp", LocalDateTime.now());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("recommendations", recommendations);
            response.put("trace", trace);
            return ResponseEntity.ok(response);
        } catch (UnauthorizedException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", exception.getMessage()));
        }
    }

    @Operation(summary = "Enregistrer un profile swipe", description = "Enregistre un LIKE ou DISLIKE sur un profil cible (etudiant/lyceen)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Profile swipe enregistre"),
            @ApiResponse(responseCode = "400", description = "Payload invalide"),
            @ApiResponse(responseCode = "401", description = "Non authentifie"),
            @ApiResponse(responseCode = "403", description = "Role insuffisant"),
            @ApiResponse(responseCode = "404", description = "Profil cible introuvable")
    })
    @PostMapping("/profile-swipe")
    @Transactional
    public ResponseEntity<?> recordProfileSwipe(@RequestBody ProfileSwipeRequest request, Principal principal) {
        try {
            User user = resolveAuthenticatedUser(principal);
            if (user.getRole() != UserRole.ECOLE && user.getRole() != UserRole.ENTREPRISE) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Seuls les roles ECOLE et ENTREPRISE peuvent swiper des profils"));
            }

            if (request == null || request.getTargetUserId() == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Le champ targetUserId est obligatoire"));
            }
            if (request.getAction() == null || request.getAction().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Le champ action est obligatoire (LIKE ou DISLIKE)"));
            }

            SwipeAction action;
            try {
                action = SwipeAction.valueOf(request.getAction().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("message", "Action invalide : utilisez LIKE ou DISLIKE"));
            }

            Optional<User> targetOpt = userRepository.findById(request.getTargetUserId());
            if (targetOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Profil cible introuvable"));
            }

            User target = targetOpt.get();
            if (target.getId().equals(user.getId())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Vous ne pouvez pas swiper votre propre profil"));
            }
            if (!isProfileRecommendationTargetAllowed(user.getRole(), target.getRole())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Le role cible n'est pas compatible avec ce type de swipe"));
            }

            Optional<ProfileSwipe> existing = profileSwipeRepository.findBySwiperIdAndTargetUserId(user.getId(), target.getId());
            ProfileSwipe profileSwipe;
            if (existing.isPresent()) {
                profileSwipe = existing.get();
                profileSwipe.setAction(action);
            } else {
                profileSwipe = new ProfileSwipe();
                profileSwipe.setSwiper(user);
                profileSwipe.setTargetUser(target);
                profileSwipe.setAction(action);
            }
            profileSwipe = profileSwipeRepository.save(profileSwipe);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("profileSwipeId", profileSwipe.getId());
            body.put("swiperUserId", user.getId());
            body.put("targetUserId", target.getId());
            body.put("action", profileSwipe.getAction().name());
            body.put("createdAt", profileSwipe.getCreatedAt());
            return ResponseEntity.status(HttpStatus.CREATED).body(body);
        } catch (UnauthorizedException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", exception.getMessage()));
        }
    }

    @Operation(summary = "Matches mutuels", description = "Retourne les matches mutuels de l'utilisateur connecte")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matches retournes"),
            @ApiResponse(responseCode = "401", description = "Non authentifie")
    })
    @GetMapping("/mutual")
    @Transactional
    public ResponseEntity<?> getMutualMatches(Principal principal) {
        try {
            User user = resolveAuthenticatedUser(principal);
            List<Map<String, Object>> mutualMatches = new ArrayList<>();

            if (user.getRole() == UserRole.ETUDIANT || user.getRole() == UserRole.LYCEEN) {
                List<Swipe> likes = swipeRepository.findByUserIdAndActionWithOffer(user.getId(), SwipeAction.LIKE);
                for (Swipe like : likes) {
                    Offer offer = like.getOffer();
                    if (offer == null || offer.getId() == null) {
                        continue;
                    }
                    UUID ownerUserId = offerRepository.findOwnerUserIdById(offer.getId()).orElse(null);
                    if (ownerUserId == null) {
                        continue;
                    }
                    boolean recruiterLikedBack = profileSwipeRepository.existsBySwiperIdAndTargetUserIdAndAction(
                            ownerUserId,
                            user.getId(),
                            SwipeAction.LIKE
                    );
                    if (!recruiterLikedBack) {
                        continue;
                    }

                    Optional<User> recruiter = userRepository.findById(ownerUserId);
                    if (recruiter.isEmpty()) {
                        continue;
                    }

                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("matchedUserId", recruiter.get().getId());
                        item.put("matchedUserName", resolveDisplayName(recruiter.get()));
                    item.put("avatarUrl", recruiter.get().getAvatarUrl());
                    item.put("offerTitle", offer.getTitre());
                    item.put("matchedAt", like.getCreatedAt());
                    mutualMatches.add(item);
                }
            } else if (user.getRole() == UserRole.ECOLE || user.getRole() == UserRole.ENTREPRISE) {
                List<ProfileSwipe> profileLikes = profileSwipeRepository.findBySwiperIdAndAction(user.getId(), SwipeAction.LIKE);
                for (ProfileSwipe profileLike : profileLikes) {
                    User candidate = profileLike.getTargetUser();
                    if (candidate == null || candidate.getId() == null) {
                        continue;
                    }

                    boolean candidateLikedOffer = swipeRepository.existsCandidateLikeOnOwnerOffers(
                            candidate.getId(),
                            user.getId(),
                            SwipeAction.LIKE
                    );
                    if (!candidateLikedOffer) {
                        continue;
                    }

                    String offerTitle = swipeRepository
                            .findCandidateLikedOfferTitlesForOwner(candidate.getId(), user.getId(), SwipeAction.LIKE)
                            .stream()
                            .findFirst()
                            .orElse(null);

                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("matchedUserId", candidate.getId());
                        item.put("matchedUserName", resolveDisplayName(candidate));
                    item.put("avatarUrl", candidate.getAvatarUrl());
                    item.put("offerTitle", offerTitle);
                    item.put("matchedAt", profileLike.getCreatedAt());
                    mutualMatches.add(item);
                }
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("mutualMatches", mutualMatches);
            response.put("count", mutualMatches.size());
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

    private String resolveDisplayName(User user) {
        String fullName = ((user.getPrenom() != null ? user.getPrenom() : "") + " "
                + (user.getNom() != null ? user.getNom() : "")).trim();
        if (!fullName.isEmpty()) {
            return fullName;
        }

        if (user.getRole() == UserRole.ENTREPRISE) {
            Optional<Company> company = companyRepository.findByUserId(user.getId());
            if (company.isPresent() && company.get().getRaisonSociale() != null
                    && !company.get().getRaisonSociale().isBlank()) {
                return company.get().getRaisonSociale().trim();
            }
        }

        if (user.getRole() == UserRole.ECOLE) {
            Optional<School> school = schoolRepository.findByUserId(user.getId());
            if (school.isPresent() && school.get().getNomEtablissement() != null
                    && !school.get().getNomEtablissement().isBlank()) {
                return school.get().getNomEtablissement().trim();
            }
        }

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            return user.getEmail().trim();
        }

        return "Contact";
    }

    private MatchProfileSnapshot buildSnapshot(User user) {
        Set<String> domains = new LinkedHashSet<>();
        Set<String> locations = new LinkedHashSet<>();
        Set<String> skills = new LinkedHashSet<>();
        Set<String> goals = new LinkedHashSet<>();

        switch (user.getRole()) {
            case ETUDIANT -> {
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

    private Map<String, Object> buildTrace(
            User user,
            int excludedSwipeCount,
            int evaluatedCount,
            int returnedCount,
            boolean hasOpportunityAccess,
            int availableOfferCount
    ) {
        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("userId", user.getId());
        trace.put("role", user.getRole().name());
        trace.put("currentPack", currentPackName(user));
        trace.put("hasOpportunityAccess", hasOpportunityAccess);
        trace.put("availableOfferCount", availableOfferCount);
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
            response.setOwnerLogoUrl(offer.getOwnerEntreprise().getLogoUrl());
        } else if (offer.getOwnerEcole() != null) {
            response.setOwnerType("ECOLE");
            response.setOwnerSchoolId(offer.getOwnerEcole().getId());
            response.setOwnerUserId(offer.getOwnerEcole().getUser() != null ? offer.getOwnerEcole().getUser().getId() : null);
            response.setOwnerDisplayName(offer.getOwnerEcole().getNomEtablissement());
            response.setOwnerLogoUrl(offer.getOwnerEcole().getLogoUrl());
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

    private boolean isProfileRecommendationTargetAllowed(UserRole swiperRole, UserRole targetRole) {
        if (swiperRole == UserRole.ENTREPRISE) {
            return targetRole == UserRole.ETUDIANT;
        }
        if (swiperRole == UserRole.ECOLE) {
            return targetRole == UserRole.ETUDIANT || targetRole == UserRole.LYCEEN;
        }
        return false;
    }

    private int countKeywordMatches(Set<String> recruiterKeywords, Set<String> targetKeywords) {
        if (recruiterKeywords == null || recruiterKeywords.isEmpty() || targetKeywords == null || targetKeywords.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String recruiterKeyword : recruiterKeywords) {
            if (recruiterKeyword == null || recruiterKeyword.isBlank()) {
                continue;
            }
            if (targetKeywords.contains(recruiterKeyword)) {
                count++;
            }
        }
        return count;
    }

    private Map<String, Object> toMediaMap(UserMediaFile mediaFile) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", mediaFile.getId());
        map.put("category", mediaFile.getCategory());
        map.put("contentType", mediaFile.getContentType());
        map.put("fileUrl", mediaFile.getFileUrl());
        map.put("createdAt", mediaFile.getCreatedAt());
        return map;
    }

    private SchoolResponse toSchoolResponse(School school) {
        SchoolResponse response = new SchoolResponse();
        response.setId(school.getId());
        response.setOwnerUserId(school.getUser() != null ? school.getUser().getId() : null);
        response.setNomEtablissement(school.getNomEtablissement());
        response.setStatut(school.getStatut() != null ? school.getStatut().name() : null);
        response.setDomaines(school.getDomaines() != null ? new LinkedHashSet<>(school.getDomaines()) : new LinkedHashSet<>());
        response.setDiplomesDelivres(school.getDiplomesDelivres() != null ? new LinkedHashSet<>(school.getDiplomesDelivres()) : new LinkedHashSet<>());
        response.setDescription(school.getDescription());
        response.setAdresse(school.getAdresse());
        response.setSiteWeb(school.getSiteWeb());
        response.setLogoUrl(school.getLogoUrl());
        response.setCreatedAt(school.getCreatedAt());
        return response;
    }

    private record MatchProfileSnapshot(Set<String> domains, Set<String> skills, Set<String> goals, Set<String> locations) {
    }

    private static class UnauthorizedException extends RuntimeException {
        private UnauthorizedException(String message) {
            super(message);
        }
    }
}
