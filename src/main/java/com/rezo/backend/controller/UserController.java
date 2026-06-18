package com.rezo.backend.controller;

import com.rezo.backend.dto.user.UpdateUserRequest;
import com.rezo.backend.dto.user.UserMeResponse;
import com.rezo.backend.dto.user.UserPackUpdateRequest;
import com.rezo.backend.service.PackRules;
import com.rezo.entities.Company;
import com.rezo.entities.Pack;
import com.rezo.entities.ProfileSwipe;
import com.rezo.entities.Profile;
import com.rezo.entities.School;
import com.rezo.entities.Swipe;
import com.rezo.entities.User;
import com.rezo.entities.enums.SwipeAction;
import com.rezo.entities.enums.CompanySize;
import com.rezo.entities.enums.SchoolStatus;
import com.rezo.entities.enums.UserRole;
import com.rezo.repositories.CompanyRepository;
import com.rezo.repositories.MessageRepository;
import com.rezo.repositories.OfferRepository;
import com.rezo.repositories.PackRepository;
import com.rezo.repositories.ProfileSwipeRepository;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@SecurityRequirement(name = "bearer-jwt")
public class UserController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final CompanyRepository companyRepository;
    private final SchoolRepository schoolRepository;
    private final PackRepository packRepository;
    private final SwipeRepository swipeRepository;
    private final ProfileSwipeRepository profileSwipeRepository;
    private final OfferRepository offerRepository;
    private final MessageRepository messageRepository;

    public UserController(UserRepository userRepository,
                          ProfileRepository profileRepository,
                          CompanyRepository companyRepository,
                          SchoolRepository schoolRepository,
                          PackRepository packRepository,
                          SwipeRepository swipeRepository,
                          ProfileSwipeRepository profileSwipeRepository,
                          OfferRepository offerRepository,
                          MessageRepository messageRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.companyRepository = companyRepository;
        this.schoolRepository = schoolRepository;
        this.packRepository = packRepository;
        this.swipeRepository = swipeRepository;
        this.profileSwipeRepository = profileSwipeRepository;
        this.offerRepository = offerRepository;
        this.messageRepository = messageRepository;
    }

    // ─── GET /api/users/me ───────────────────────────────────────────────

    @Operation(summary = "Mon profil", description = "Retourne les infos de l'utilisateur connecte et son profil associe")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profil retourne"),
            @ApiResponse(responseCode = "401", description = "Non authentifie"),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
    })
    @GetMapping("/me")
    @Transactional
    public ResponseEntity<?> getMe(Principal principal) {
        try {
            UUID userId = extractUserId(principal);
            Optional<User> optUser = userRepository.findByIdWithPack(userId);
            if (optUser.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Utilisateur introuvable"));
            }
            User user = optUser.get();
            return ResponseEntity.ok(toMeResponse(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Non authentifie"));
        }
    }

    @Operation(summary = "Mes statistiques", description = "Retourne les stats principales du dashboard")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stats retournees"),
            @ApiResponse(responseCode = "401", description = "Non authentifie"),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
    })
    @GetMapping("/me/stats")
    @Transactional
    public ResponseEntity<?> getMyStats(Principal principal) {
        try {
            UUID userId = extractUserId(principal);
            Optional<User> optUser = userRepository.findByIdWithPack(userId);
            if (optUser.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Utilisateur introuvable"));
            }
            User user = optUser.get();
            return ResponseEntity.ok(buildStatsResponse(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Non authentifie"));
        }
    }

    // ─── PUT /api/users/me ───────────────────────────────────────────────

    @Operation(summary = "Modifier mon profil", description = "Met a jour les infos du user connecte et de son profil selon le role")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profil mis a jour"),
            @ApiResponse(responseCode = "400", description = "Payload invalide"),
            @ApiResponse(responseCode = "401", description = "Non authentifie"),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable"),
            @ApiResponse(responseCode = "409", description = "Email deja utilise")
    })
    @PutMapping("/me")
    @Transactional
    public ResponseEntity<?> updateMe(@RequestBody UpdateUserRequest request, Principal principal) {
        try {
            UUID userId = extractUserId(principal);
            Optional<User> optUser = userRepository.findByIdWithPack(userId);
            if (optUser.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Utilisateur introuvable"));
            }
            User user = optUser.get();

            // --- Update user fields (only non-null fields) ---
            if (request.getEmail() != null) {
                String newEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
                if (newEmail.isBlank()) {
                    return ResponseEntity.badRequest().body(Map.of("message", "L'email ne peut pas etre vide"));
                }
                if (!newEmail.equals(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(Map.of("message", "Email deja utilise"));
                }
                user.setEmail(newEmail);
            }
            if (request.getPrenom() != null) {
                if (request.getPrenom().isBlank()) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Le prenom ne peut pas etre vide"));
                }
                user.setPrenom(request.getPrenom().trim());
            }
            if (request.getNom() != null) {
                if (request.getNom().isBlank()) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Le nom ne peut pas etre vide"));
                }
                user.setNom(request.getNom().trim());
            }
            if (request.getTelephone() != null) {
                user.setTelephone(request.getTelephone().trim());
            }
            if (request.getAvatarUrl() != null) {
                user.setAvatarUrl(request.getAvatarUrl().trim());
            }
            userRepository.save(user);

            // --- Update role-specific profile ---
            Map<String, Object> profilData = request.getProfil() != null ? request.getProfil() : Collections.emptyMap();
            if (!profilData.isEmpty()) {
                try {
                    updateRoleProfile(user, profilData);
                } catch (BadRequestException e) {
                    return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
                }
            }

            LOGGER.info("Profil mis a jour pour userId={}", userId);
            return ResponseEntity.ok(toMeResponse(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Non authentifie"));
        }
    }

    // ─── PUT /api/users/me/pack ──────────────────────────────────────────

    @Operation(summary = "Changer mon pack", description = "Permet a l'utilisateur connecte de souscrire ou basculer vers un pack compatible avec son role")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pack mis a jour"),
            @ApiResponse(responseCode = "400", description = "Pack incompatible ou payload invalide"),
            @ApiResponse(responseCode = "401", description = "Non authentifie"),
            @ApiResponse(responseCode = "404", description = "Utilisateur ou pack introuvable")
    })
    @org.springframework.web.bind.annotation.RequestMapping(value = "/me/pack", method = {org.springframework.web.bind.annotation.RequestMethod.PUT, org.springframework.web.bind.annotation.RequestMethod.PATCH})
    @Transactional
    public ResponseEntity<?> updateMyPack(@RequestBody UserPackUpdateRequest request, Principal principal) {
        try {
            UUID userId = extractUserId(principal);
            if (request == null || request.getPackId() == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Le champ packId est obligatoire"));
            }

            Optional<User> optUser = userRepository.findByIdWithPack(userId);
            if (optUser.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Utilisateur introuvable"));
            }

            Optional<Pack> optPack = packRepository.findById(request.getPackId());
            if (optPack.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Pack introuvable"));
            }

            User user = optUser.get();
            Pack pack = optPack.get();
            if (!PackRules.isPackCompatible(pack, user.getRole())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Ce pack n'est pas compatible avec votre role"));
            }

            user.setPack(pack);
            userRepository.save(user);
            LOGGER.info("Pack mis a jour pour userId={} pack={}", userId, pack.getNom());
            return ResponseEntity.ok(toMeResponse(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Non authentifie"));
        }
    }

    // ─── DELETE /api/users/me ────────────────────────────────────────────

    @Operation(summary = "Supprimer mon compte", description = "Supprime le user connecte et son profil associe")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compte supprime"),
            @ApiResponse(responseCode = "401", description = "Non authentifie"),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
    })
    @DeleteMapping("/me")
    @Transactional
    public ResponseEntity<?> deleteMe(Principal principal) {
        try {
            UUID userId = extractUserId(principal);

            if (!userRepository.existsById(userId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Utilisateur introuvable"));
            }

            profileRepository.deleteAllByUserId(userId);
            companyRepository.deleteAllByUserId(userId);
            schoolRepository.deleteAllByUserId(userId);
            userRepository.deleteByIdDirect(userId);

            LOGGER.info("Compte supprime pour userId={}", userId);
            return ResponseEntity.ok(Map.of("message", "Compte supprime avec succes"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Non authentifie"));
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private UUID extractUserId(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new IllegalArgumentException("Non authentifie");
        }
        return UUID.fromString(principal.getName());
    }

    private UserMeResponse toMeResponse(User user) {
        UserMeResponse dto = new UserMeResponse();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setPrenom(user.getPrenom());
        dto.setNom(user.getNom());
        dto.setTelephone(user.getTelephone());
        dto.setRole(user.getRole().name());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setPackId(user.getPack() != null ? user.getPack().getId() : null);
        dto.setPackNom(user.getPack() != null ? user.getPack().getNom() : null);
        dto.setPackCible(user.getPack() != null ? user.getPack().getCible() : null);
        dto.setPackFeatures(user.getPack() != null ? PackRules.parseCsv(user.getPack().getFeatures()) : new HashSet<>());
        dto.setCanManageOffers(PackRules.canManageOffers(user));
        dto.setCanUseMessaging(PackRules.canUseMessaging(user));
        dto.setCanUseAiChat(PackRules.canUseAiChat(user));
        dto.setMatchCount(calculateMatchCount(user));
        dto.setCreatedAt(user.getCreatedAt());
        dto.setProfil(buildProfilMap(user));
        return dto;
    }

    private long calculateMatchCount(User user) {
        if (user == null || user.getId() == null || user.getRole() == null) {
            return 0;
        }

        Set<UUID> matchedUserIds = new HashSet<>();

        if (user.getRole() == UserRole.ETUDIANT || user.getRole() == UserRole.LYCEEN) {
            List<Swipe> likes = swipeRepository.findByUserIdAndActionWithOffer(user.getId(), SwipeAction.LIKE);
            for (Swipe like : likes) {
                if (like.getOffer() == null || like.getOffer().getId() == null) {
                    continue;
                }
                UUID ownerUserId = offerRepository.findOwnerUserIdById(like.getOffer().getId()).orElse(null);
                if (ownerUserId == null) {
                    continue;
                }
                boolean recruiterLikedBack = profileSwipeRepository.existsBySwiperIdAndTargetUserIdAndAction(
                        ownerUserId,
                        user.getId(),
                        SwipeAction.LIKE
                );
                if (recruiterLikedBack) {
                    matchedUserIds.add(ownerUserId);
                }
            }
        } else if (user.getRole() == UserRole.ECOLE || user.getRole() == UserRole.ENTREPRISE) {
            List<ProfileSwipe> profileLikes = profileSwipeRepository.findBySwiperIdAndAction(user.getId(), SwipeAction.LIKE);
            for (ProfileSwipe profileLike : profileLikes) {
                if (profileLike.getTargetUser() == null || profileLike.getTargetUser().getId() == null) {
                    continue;
                }
                UUID candidateId = profileLike.getTargetUser().getId();
                boolean candidateLikedOffer = swipeRepository.existsCandidateLikeOnOwnerOffers(
                        candidateId,
                        user.getId(),
                        SwipeAction.LIKE
                );
                if (candidateLikedOffer) {
                    matchedUserIds.add(candidateId);
                }
            }
        }

        return matchedUserIds.size();
    }

    private Map<String, Object> buildStatsResponse(User user) {
        long matchCount = calculateMatchCount(user);
        long likesSent = isRecruiterRole(user.getRole())
                ? profileSwipeRepository.countBySwiperIdAndAction(user.getId(), SwipeAction.LIKE)
                : swipeRepository.countByUserIdAndAction(user.getId(), SwipeAction.LIKE);
        long likesReceived = isRecruiterRole(user.getRole())
                ? swipeRepository.countLikesOnOwnerOffers(user.getId(), SwipeAction.LIKE)
                : profileSwipeRepository.countByTargetUserIdAndAction(user.getId(), SwipeAction.LIKE);
        long offerCount = isRecruiterRole(user.getRole())
                ? offerRepository.countByOwnerUserId(user.getId())
                : 0L;
        long unreadMessages = messageRepository.countUnreadByReceiverId(user.getId());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("matchCount", matchCount);
        body.put("likesSent", likesSent);
        body.put("likesReceived", likesReceived);
        body.put("offerCount", offerCount);
        body.put("unreadMessages", unreadMessages);
        return body;
    }

    private boolean isRecruiterRole(UserRole role) {
        return role == UserRole.ECOLE || role == UserRole.ENTREPRISE;
    }

    private Map<String, Object> buildProfilMap(User user) {
        UserRole role = user.getRole();
        Map<String, Object> map = new LinkedHashMap<>();

        switch (role) {
            case ETUDIANT, LYCEEN -> {
                Optional<Profile> opt = profileRepository.findByUserId(user.getId());
                if (opt.isPresent()) {
                    Profile p = opt.get();
                    if (role == UserRole.LYCEEN) {
                        map.put("classeActuelle", p.getClasseActuelle());
                        map.put("serieOrientation", p.getSerieOrientation());
                        map.put("objectifPostbac", p.getObjectifPostbac());
                        map.put("centresInteret", copySet(p.getCentresInteret()));
                    } else {
                        map.put("niveauEtude", p.getNiveauEtude());
                        map.put("domaine", p.getDomaine());
                        map.put("competences", copySet(p.getCompetences()));
                        map.put("objectif", p.getObjectif());
                        map.put("preferencesSecteur", copySet(p.getPreferencesSecteur()));
                        map.put("preferencesLieu", copySet(p.getPreferencesLieu()));
                        map.put("experiences", copySet(p.getExperiences()));
                    }
                }
            }
            case ENTREPRISE -> {
                Optional<Company> opt = companyRepository.findByUserId(user.getId());
                if (opt.isPresent()) {
                    Company c = opt.get();
                    map.put("raisonSociale", c.getRaisonSociale());
                    map.put("secteurActivite", c.getSecteurActivite());
                    map.put("taille", c.getTaille().name());
                    map.put("description", c.getDescription());
                    map.put("adresse", c.getAdresse());
                    map.put("siteWeb", c.getSiteWeb());
                    map.put("logoUrl", c.getLogoUrl());
                }
            }
            case ECOLE -> {
                Optional<School> opt = schoolRepository.findByUserId(user.getId());
                if (opt.isPresent()) {
                    School s = opt.get();
                    map.put("nomEtablissement", s.getNomEtablissement());
                    map.put("statut", s.getStatut().name());
                    map.put("domaines", copySet(s.getDomaines()));
                    map.put("diplomesDelivres", copySet(s.getDiplomesDelivres()));
                    map.put("description", s.getDescription());
                    map.put("adresse", s.getAdresse());
                    map.put("siteWeb", s.getSiteWeb());
                    map.put("logoUrl", s.getLogoUrl());
                }
            }
            default -> { /* ADMIN — no profile */ }
        }
        return map;
    }

    private void updateRoleProfile(User user, Map<String, Object> data) {
        switch (user.getRole()) {
            case ETUDIANT -> updateEtudiantProfile(user, data);
            case LYCEEN -> updateLyceenProfile(user, data);
            case ENTREPRISE -> updateCompanyProfile(user, data);
            case ECOLE -> updateSchoolProfile(user, data);
            default -> { /* ADMIN — nothing to update */ }
        }
    }

    private void updateEtudiantProfile(User user, Map<String, Object> data) {
        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException("Profil introuvable pour cet utilisateur"));
        if (data.containsKey("niveauEtude")) {
            String val = stringValue(data, "niveauEtude");
            if (val == null || val.isBlank()) throw new BadRequestException("niveauEtude ne peut pas etre vide");
            profile.setNiveauEtude(val);
        }
        if (data.containsKey("domaine")) {
            String val = stringValue(data, "domaine");
            if (val == null || val.isBlank()) throw new BadRequestException("domaine ne peut pas etre vide");
            profile.setDomaine(val);
        }
        if (data.containsKey("competences")) profile.setCompetences(stringSet(data, "competences"));
        if (data.containsKey("objectif")) profile.setObjectif(stringValue(data, "objectif"));
        if (data.containsKey("preferencesSecteur")) profile.setPreferencesSecteur(stringSet(data, "preferencesSecteur"));
        if (data.containsKey("preferencesLieu")) profile.setPreferencesLieu(stringSet(data, "preferencesLieu"));
        if (data.containsKey("experiences")) profile.setExperiences(stringSet(data, "experiences"));
        profileRepository.save(profile);
    }

    private void updateLyceenProfile(User user, Map<String, Object> data) {
        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException("Profil introuvable pour cet utilisateur"));
        if (data.containsKey("classeActuelle")) {
            String val = stringValue(data, "classeActuelle");
            if (val == null || val.isBlank()) throw new BadRequestException("classeActuelle ne peut pas etre vide");
            profile.setClasseActuelle(val);
        }
        if (data.containsKey("serieOrientation")) {
            String val = stringValue(data, "serieOrientation");
            if (val == null || val.isBlank()) throw new BadRequestException("serieOrientation ne peut pas etre vide");
            profile.setSerieOrientation(val);
        }
        if (data.containsKey("objectifPostbac")) profile.setObjectifPostbac(stringValue(data, "objectifPostbac"));
        if (data.containsKey("centresInteret")) profile.setCentresInteret(stringSet(data, "centresInteret"));
        profileRepository.save(profile);
    }

    private void updateCompanyProfile(User user, Map<String, Object> data) {
        Company company = companyRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException("Profil entreprise introuvable"));
        if (data.containsKey("raisonSociale")) {
            String val = stringValue(data, "raisonSociale");
            if (val == null || val.isBlank()) throw new BadRequestException("raisonSociale ne peut pas etre vide");
            company.setRaisonSociale(val);
        }
        if (data.containsKey("secteurActivite")) {
            String val = stringValue(data, "secteurActivite");
            if (val == null || val.isBlank()) throw new BadRequestException("secteurActivite ne peut pas etre vide");
            company.setSecteurActivite(val);
        }
        if (data.containsKey("taille")) {
            String val = stringValue(data, "taille");
            try {
                company.setTaille(CompanySize.valueOf(val.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Valeur invalide pour taille: " + val);
            }
        }
        if (data.containsKey("description")) {
            String val = stringValue(data, "description");
            if (val == null || val.isBlank()) throw new BadRequestException("description ne peut pas etre vide");
            company.setDescription(val);
        }
        if (data.containsKey("adresse")) company.setAdresse(stringValue(data, "adresse"));
        if (data.containsKey("siteWeb")) company.setSiteWeb(stringValue(data, "siteWeb"));
        if (data.containsKey("logoUrl")) company.setLogoUrl(stringValue(data, "logoUrl"));
        companyRepository.save(company);
    }

    private void updateSchoolProfile(User user, Map<String, Object> data) {
        School school = schoolRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException("Profil ecole introuvable"));
        if (data.containsKey("nomEtablissement")) {
            String val = stringValue(data, "nomEtablissement");
            if (val == null || val.isBlank()) throw new BadRequestException("nomEtablissement ne peut pas etre vide");
            school.setNomEtablissement(val);
        }
        if (data.containsKey("statut")) {
            String val = stringValue(data, "statut");
            try {
                school.setStatut(SchoolStatus.valueOf(val.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Valeur invalide pour statut: " + val);
            }
        }
        if (data.containsKey("domaines")) school.setDomaines(stringSet(data, "domaines"));
        if (data.containsKey("diplomesDelivres")) school.setDiplomesDelivres(stringSet(data, "diplomesDelivres"));
        if (data.containsKey("description")) school.setDescription(stringValue(data, "description"));
        if (data.containsKey("adresse")) school.setAdresse(stringValue(data, "adresse"));
        if (data.containsKey("siteWeb")) school.setSiteWeb(stringValue(data, "siteWeb"));
        if (data.containsKey("logoUrl")) school.setLogoUrl(stringValue(data, "logoUrl"));
        schoolRepository.save(school);
    }

    private String stringValue(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val instanceof String s ? s.trim() : (val != null ? val.toString().trim() : null);
    }

    private Set<String> stringSet(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof List<?> list) {
            Set<String> set = new HashSet<>();
            for (Object item : list) {
                if (item instanceof String s && !s.isBlank()) set.add(s.trim());
            }
            return set;
        }
        return new HashSet<>();
    }

    private Set<String> copySet(Set<String> source) {
        return source == null ? new HashSet<>() : new HashSet<>(source);
    }

    private static class BadRequestException extends RuntimeException {
        private BadRequestException(String message) {
            super(message);
        }
    }
}
