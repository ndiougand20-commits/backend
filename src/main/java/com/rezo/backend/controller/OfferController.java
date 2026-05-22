package com.rezo.backend.controller;

import com.rezo.backend.dto.offer.OfferRequest;
import com.rezo.backend.dto.offer.OfferResponse;
import com.rezo.backend.service.PackRules;
import com.rezo.backend.service.UserMediaStorageService;
import com.rezo.entities.Company;
import com.rezo.entities.Offer;
import com.rezo.entities.School;
import com.rezo.entities.User;
import com.rezo.entities.enums.OfferType;
import com.rezo.entities.enums.SwipeAction;
import com.rezo.entities.enums.UserRole;
import com.rezo.repositories.CompanyRepository;
import com.rezo.repositories.OfferRepository;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/offers")
public class OfferController {

    private static final Logger LOGGER = LoggerFactory.getLogger(OfferController.class);

    private final OfferRepository offerRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final SchoolRepository schoolRepository;
    private final SwipeRepository swipeRepository;
    private final UserMediaStorageService userMediaStorageService;

    public OfferController(
            OfferRepository offerRepository,
            UserRepository userRepository,
            CompanyRepository companyRepository,
            SchoolRepository schoolRepository,
            SwipeRepository swipeRepository,
            UserMediaStorageService userMediaStorageService
    ) {
        this.offerRepository = offerRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.schoolRepository = schoolRepository;
        this.swipeRepository = swipeRepository;
        this.userMediaStorageService = userMediaStorageService;
    }

    @Operation(summary = "Lister les offres", description = "Retourne la liste publique des offres stages/emplois avec leur proprietaire")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Liste retournee")})
    @GetMapping
    @Transactional
    public ResponseEntity<List<OfferResponse>> listOffers() {
        List<OfferResponse> offers = offerRepository.findAllWithOwners().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(offers);
    }

    @Operation(summary = "Detail d'une offre", description = "Retourne une offre par son identifiant avec les informations d'affiliation")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offre retournee"),
            @ApiResponse(responseCode = "404", description = "Offre introuvable")
    })
    @GetMapping("/{id}")
    @Transactional
    public ResponseEntity<?> getOfferById(@PathVariable UUID id) {
        return offerRepository.findByIdWithOwners(id)
                .<ResponseEntity<?>>map(offer -> ResponseEntity.ok(toResponse(offer)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Offre introuvable")));
    }

    @Operation(summary = "Utilisateurs ayant aime une offre", description = "Retourne la liste des utilisateurs qui ont swipe LIKE sur cette offre. Accessible uniquement par le proprietaire de l'offre.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste retournee"),
            @ApiResponse(responseCode = "401", description = "Non authentifie"),
            @ApiResponse(responseCode = "403", description = "Acces refuse"),
            @ApiResponse(responseCode = "404", description = "Offre introuvable")
    })
    @SecurityRequirement(name = "bearer-jwt")
    @GetMapping("/{id}/liked-by")
    @Transactional
    public ResponseEntity<?> getLikedBy(@PathVariable UUID id, Principal principal) {
        try {
            User user = resolveAuthenticatedUser(principal);
            Optional<UUID> optionalOwnerUserId = offerRepository.findOwnerUserIdById(id);
            if (optionalOwnerUserId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Offre introuvable"));
            }
            if (!optionalOwnerUserId.get().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Acces reserve au proprietaire de l'offre"));
            }
            List<User> likers = swipeRepository.findLikersByOfferId(id, SwipeAction.LIKE);
            List<Map<String, Object>> result = likers.stream()
                    .map(liker -> {
                        Map<String, Object> entry = new LinkedHashMap<>();
                        entry.put("userId", liker.getId());
                        entry.put("prenom", liker.getPrenom());
                        entry.put("nom", liker.getNom());
                        entry.put("role", liker.getRole() != null ? liker.getRole().name() : null);
                        entry.put("avatarUrl", liker.getAvatarUrl());
                        return entry;
                    })
                    .toList();
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("offerId", id);
            body.put("count", result.size());
            body.put("likers", result);
            LOGGER.info("liked-by offerId={} ownerId={} count={}", id, user.getId(), result.size());
            return ResponseEntity.ok(body);
        } catch (UnauthorizedException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", exception.getMessage()));
        } catch (ForbiddenException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", exception.getMessage()));
        }
    }

    @Operation(summary = "Publier une offre", description = "Publication d'une offre par un utilisateur legitime ECOLE ou ENTREPRISE")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Offre creee"),
            @ApiResponse(responseCode = "400", description = "Payload invalide"),
            @ApiResponse(responseCode = "401", description = "Non authentifie"),
            @ApiResponse(responseCode = "403", description = "Role insuffisant"),
            @ApiResponse(responseCode = "409", description = "Profil proprietaire manquant")
    })
    @SecurityRequirement(name = "bearer-jwt")
    @PostMapping
    @Transactional
    public ResponseEntity<?> createOffer(@RequestBody OfferRequest request, Principal principal) {
        try {
            User user = resolveAuthenticatedUser(principal);
            Offer offer = new Offer();
            attachOwner(offer, user);
            ensurePackAllowsOfferManagement(user);
            applyRequest(offer, request, true);
            offer = offerRepository.save(offer);

            LOGGER.info("Offre creee id={} userId={} type={}", offer.getId(), user.getId(), offer.getType());
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(offer));
        } catch (UnauthorizedException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", exception.getMessage()));
        } catch (ForbiddenException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", exception.getMessage()));
        } catch (ConflictException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", exception.getMessage()));
        } catch (BadRequestException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @Operation(summary = "Modifier une offre", description = "Modification d'une offre par son proprietaire uniquement")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offre mise a jour"),
            @ApiResponse(responseCode = "400", description = "Payload invalide"),
            @ApiResponse(responseCode = "401", description = "Non authentifie"),
            @ApiResponse(responseCode = "403", description = "Ownership refuse"),
            @ApiResponse(responseCode = "404", description = "Offre introuvable")
    })
    @SecurityRequirement(name = "bearer-jwt")
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateOffer(@PathVariable UUID id, @RequestBody OfferRequest request, Principal principal) {
        try {
            User user = resolveAuthenticatedUser(principal);
            Optional<Offer> optionalOffer = offerRepository.findByIdWithOwners(id);
            if (optionalOffer.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Offre introuvable"));
            }
            Offer offer = optionalOffer.get();
            if (!resolveOwnerUserId(offer).equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Vous ne pouvez modifier que votre propre offre"));
            }
            ensurePackAllowsOfferManagement(user);
            applyRequest(offer, request, false);
            offer = offerRepository.save(offer);
            LOGGER.info("Offre mise a jour id={} userId={}", offer.getId(), user.getId());
            return ResponseEntity.ok(toResponse(offer));
        } catch (UnauthorizedException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", exception.getMessage()));
        } catch (ForbiddenException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", exception.getMessage()));
        } catch (BadRequestException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @Operation(summary = "Supprimer une offre", description = "Suppression d'une offre par son proprietaire uniquement")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offre supprimee"),
            @ApiResponse(responseCode = "401", description = "Non authentifie"),
            @ApiResponse(responseCode = "403", description = "Ownership refuse"),
            @ApiResponse(responseCode = "404", description = "Offre introuvable")
    })
    @SecurityRequirement(name = "bearer-jwt")
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteOffer(@PathVariable UUID id, Principal principal) {
        try {
            User user = resolveAuthenticatedUser(principal);
            Optional<UUID> optionalOwnerUserId = offerRepository.findOwnerUserIdById(id);
            if (optionalOwnerUserId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Offre introuvable"));
            }
            if (!optionalOwnerUserId.get().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Vous ne pouvez supprimer que votre propre offre"));
            }
            ensurePackAllowsOfferManagement(user);
            int deleted = offerRepository.deleteByIdDirect(id);
            if (deleted == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Offre introuvable"));
            }
            LOGGER.info("Offre supprimee id={} userId={}", id, user.getId());
            return ResponseEntity.ok(Map.of("message", "Offre supprimee avec succes"));
        } catch (UnauthorizedException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", exception.getMessage()));
        } catch (ForbiddenException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", exception.getMessage()));
        }
    }

    @Operation(summary = "Uploader un PDF d'offre", description = "Permet au proprietaire d'une offre de joindre une brochure/fiche PDF")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF d'offre enregistre"),
            @ApiResponse(responseCode = "400", description = "Fichier invalide"),
            @ApiResponse(responseCode = "401", description = "Non authentifie"),
            @ApiResponse(responseCode = "403", description = "Ownership refuse"),
            @ApiResponse(responseCode = "404", description = "Offre introuvable")
    })
    @SecurityRequirement(name = "bearer-jwt")
    @PostMapping("/{id}/media")
    @Transactional
    public ResponseEntity<?> uploadOfferMedia(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            Principal principal
    ) {
        try {
            User user = resolveAuthenticatedUser(principal);
            Optional<Offer> optionalOffer = offerRepository.findByIdWithOwners(id);
            if (optionalOffer.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Offre introuvable"));
            }

            Offer offer = optionalOffer.get();
            if (!resolveOwnerUserId(offer).equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Vous ne pouvez joindre un media qu'a votre propre offre"));
            }
            ensurePackAllowsOfferManagement(user);

            UserMediaStorageService.StoredMedia stored = userMediaStorageService.storeOfferPdf(user.getId(), file);
            offer.setPdfUrl(stored.fileUrl());
            Offer saved = offerRepository.save(offer);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("offerId", saved.getId());
            body.put("pdfUrl", saved.getPdfUrl());
            body.put("message", "PDF d'offre enregistre avec succes");
            return ResponseEntity.ok(body);
        } catch (UnauthorizedException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", exception.getMessage()));
        } catch (ForbiddenException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", exception.getMessage()));
        } catch (UserMediaStorageService.MediaValidationException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    private User resolveAuthenticatedUser(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new UnauthorizedException("Non authentifie");
        }
        UUID userId = UUID.fromString(principal.getName());
        return userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Utilisateur authentifie introuvable"));
    }

    private void attachOwner(Offer offer, User user) {
        if (user.getRole() == UserRole.ENTREPRISE) {
            Company company = companyRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ConflictException("Vous devez d'abord creer votre fiche entreprise avant de publier une offre"));
            offer.setOwnerEntreprise(company);
            offer.setOwnerEcole(null);
            return;
        }
        if (user.getRole() == UserRole.ECOLE) {
            School school = schoolRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ConflictException("Vous devez d'abord creer votre fiche ecole avant de publier une offre"));
            offer.setOwnerEcole(school);
            offer.setOwnerEntreprise(null);
            return;
        }
        throw new ForbiddenException("Seuls les utilisateurs ECOLE ou ENTREPRISE peuvent publier une offre");
    }

    private void ensurePackAllowsOfferManagement(User user) {
        if (!PackRules.canManageOffers(user)) {
            throw new ForbiddenException("Votre pack actuel ne permet pas de publier ou gerer des offres");
        }
    }

    private UUID resolveOwnerUserId(Offer offer) {
        if (offer.getOwnerEntreprise() != null && offer.getOwnerEntreprise().getUser() != null) {
            return offer.getOwnerEntreprise().getUser().getId();
        }
        if (offer.getOwnerEcole() != null && offer.getOwnerEcole().getUser() != null) {
            return offer.getOwnerEcole().getUser().getId();
        }
        throw new BadRequestException("Owner de l'offre introuvable");
    }

    private void applyRequest(Offer offer, OfferRequest request, boolean creation) {
        if (request == null) {
            throw new BadRequestException("Payload manquant");
        }

        if (creation || request.getTitre() != null) {
            String value = trim(request.getTitre());
            if (value == null || value.length() < 2 || value.length() > 150) {
                throw new BadRequestException("titre doit contenir entre 2 et 150 caracteres");
            }
            offer.setTitre(value);
        }

        if (creation || request.getDescription() != null) {
            String value = trim(request.getDescription());
            if (value == null || value.length() < 2) {
                throw new BadRequestException("description doit contenir au moins 2 caracteres");
            }
            offer.setDescription(value);
        }

        if (creation || request.getType() != null) {
            String value = trim(request.getType());
            if (value == null) {
                throw new BadRequestException("Le champ type est obligatoire");
            }
            OfferType parsedType;
            try {
                parsedType = OfferType.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException exception) {
                throw new BadRequestException("type doit etre STAGE ou EMPLOI");
            }
            if (parsedType != OfferType.STAGE && parsedType != OfferType.EMPLOI) {
                throw new BadRequestException("type doit etre STAGE ou EMPLOI");
            }
            offer.setType(parsedType);
        }

        if (creation || request.getDomaine() != null) {
            String value = trim(request.getDomaine());
            if (value == null || value.length() < 2 || value.length() > 120) {
                throw new BadRequestException("domaine doit contenir entre 2 et 120 caracteres");
            }
            offer.setDomaine(value);
        }

        if (creation || request.getLocation() != null) {
            String value = trim(request.getLocation());
            if (value == null || value.length() < 2 || value.length() > 150) {
                throw new BadRequestException("location doit contenir entre 2 et 150 caracteres");
            }
            offer.setLocation(value);
        }

        if (creation || request.getPdfUrl() != null) {
            String value = trim(request.getPdfUrl());
            offer.setPdfUrl(value == null || value.isBlank() ? null : value);
        }

        if (creation || request.getCompetencesRequises() != null) {
            offer.setCompetencesRequises(normalizeNonEmptySet(request.getCompetencesRequises(), "competencesRequises"));
        }

        if (creation) {
            LocalDateTime datePublication = request.getDatePublication() != null ? request.getDatePublication() : LocalDateTime.now();
            offer.setDatePublication(datePublication);
        } else if (request.getDatePublication() != null) {
            offer.setDatePublication(request.getDatePublication());
        }

        if (creation || request.getDateDebut() != null) {
            if (request.getDateDebut() == null) {
                throw new BadRequestException("Le champ dateDebut est obligatoire");
            }
            offer.setDateDebut(request.getDateDebut());
        }

        if (creation || request.getDateFin() != null) {
            if (request.getDateFin() == null) {
                throw new BadRequestException("Le champ dateFin est obligatoire");
            }
            offer.setDateFin(request.getDateFin());
        }

        validateDates(offer.getDatePublication(), offer.getDateDebut(), offer.getDateFin());
    }

    private void validateDates(LocalDateTime datePublication, LocalDateTime dateDebut, LocalDateTime dateFin) {
        LocalDateTime now = LocalDateTime.now().plusMinutes(1);
        if (datePublication != null && datePublication.isAfter(now)) {
            throw new BadRequestException("datePublication ne peut pas etre dans le futur");
        }
        if (dateDebut != null && dateFin != null && !dateDebut.isBefore(dateFin)) {
            throw new BadRequestException("dateDebut doit etre strictement avant dateFin");
        }
        if (datePublication != null && dateDebut != null && dateDebut.isBefore(datePublication)) {
            throw new BadRequestException("dateDebut doit etre posterieure ou egale a datePublication");
        }
    }

    private OfferResponse toResponse(Offer offer) {
        OfferResponse response = new OfferResponse();
        response.setId(offer.getId());
        response.setTitre(offer.getTitre());
        response.setDescription(offer.getDescription());
        response.setType(offer.getType() != null ? offer.getType().name() : null);
        response.setDomaine(offer.getDomaine());
        response.setLocation(offer.getLocation());
        response.setPdfUrl(offer.getPdfUrl());
        response.setCompetencesRequises(copySet(offer.getCompetencesRequises()));
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

    private Set<String> normalizeNonEmptySet(List<String> values, String fieldName) {
        Set<String> set = new HashSet<>();
        if (values != null) {
            for (String value : values) {
                String trimmed = trim(value);
                if (trimmed != null && !trimmed.isBlank()) {
                    set.add(trimmed);
                }
            }
        }
        if (set.isEmpty()) {
            throw new BadRequestException(fieldName + " ne peut pas etre vide");
        }
        return set;
    }

    private Set<String> copySet(Set<String> values) {
        return values == null ? new HashSet<>() : new HashSet<>(values);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static class BadRequestException extends RuntimeException {
        private BadRequestException(String message) {
            super(message);
        }
    }

    private static class UnauthorizedException extends RuntimeException {
        private UnauthorizedException(String message) {
            super(message);
        }
    }

    private static class ForbiddenException extends RuntimeException {
        private ForbiddenException(String message) {
            super(message);
        }
    }

    private static class ConflictException extends RuntimeException {
        private ConflictException(String message) {
            super(message);
        }
    }
}
