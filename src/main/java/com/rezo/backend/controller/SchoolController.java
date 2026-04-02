package com.rezo.backend.controller;

import com.rezo.backend.dto.school.SchoolRequest;
import com.rezo.backend.dto.school.SchoolResponse;
import com.rezo.entities.School;
import com.rezo.entities.User;
import com.rezo.entities.enums.SchoolStatus;
import com.rezo.entities.enums.UserRole;
import com.rezo.repositories.SchoolRepository;
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

import java.net.URI;
import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools")
public class SchoolController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchoolController.class);

    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;

    public SchoolController(SchoolRepository schoolRepository, UserRepository userRepository) {
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Lister les ecoles", description = "Retourne la liste publique des ecoles")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Liste retournee")})
    @GetMapping
    @Transactional
    public ResponseEntity<List<SchoolResponse>> listSchools() {
        List<SchoolResponse> schools = schoolRepository.findAllWithUser().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(schools);
    }

    @Operation(summary = "Detail d'une ecole", description = "Retourne une ecole par son identifiant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ecole retournee"),
            @ApiResponse(responseCode = "404", description = "Ecole introuvable")
    })
    @GetMapping("/{id}")
    @Transactional
    public ResponseEntity<?> getSchoolById(@PathVariable UUID id) {
        return schoolRepository.findByIdWithUser(id)
                .<ResponseEntity<?>>map(school -> ResponseEntity.ok(toResponse(school)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Ecole introuvable")));
    }

    @Operation(summary = "Creer une ecole", description = "Creation d'une fiche ecole par un utilisateur role ECOLE")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ecole creee"),
            @ApiResponse(responseCode = "400", description = "Payload invalide"),
            @ApiResponse(responseCode = "401", description = "Non authentifie"),
            @ApiResponse(responseCode = "403", description = "Role insuffisant"),
            @ApiResponse(responseCode = "409", description = "Fiche deja existante")
    })
    @SecurityRequirement(name = "bearer-jwt")
    @PostMapping
    @Transactional
    public ResponseEntity<?> createSchool(@RequestBody SchoolRequest request, Principal principal) {
        try {
            User user = resolveAuthenticatedUser(principal);
            if (user.getRole() != UserRole.ECOLE) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Seuls les utilisateurs ECOLE peuvent creer une fiche ecole"));
            }
            if (schoolRepository.findByUserId(user.getId()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "Une fiche ecole existe deja pour ce compte"));
            }

            School school = new School();
            school.setUser(user);
            applyRequest(school, request, true);
            school = schoolRepository.save(school);

            LOGGER.info("Ecole creee id={} userId={}", school.getId(), user.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(school));
        } catch (UnauthorizedException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", exception.getMessage()));
        } catch (BadRequestException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @Operation(summary = "Modifier une ecole", description = "Modification d'une fiche ecole par son proprietaire uniquement")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ecole mise a jour"),
            @ApiResponse(responseCode = "400", description = "Payload invalide"),
            @ApiResponse(responseCode = "401", description = "Non authentifie"),
            @ApiResponse(responseCode = "403", description = "Ownership refuse"),
            @ApiResponse(responseCode = "404", description = "Ecole introuvable")
    })
    @SecurityRequirement(name = "bearer-jwt")
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateSchool(@PathVariable UUID id, @RequestBody SchoolRequest request, Principal principal) {
        try {
            User user = resolveAuthenticatedUser(principal);
            Optional<School> optionalSchool = schoolRepository.findByIdWithUser(id);
            if (optionalSchool.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Ecole introuvable"));
            }
            School school = optionalSchool.get();
            if (!school.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Vous ne pouvez modifier que votre propre fiche ecole"));
            }
            applyRequest(school, request, false);
            school = schoolRepository.save(school);
            LOGGER.info("Ecole mise a jour id={} userId={}", school.getId(), user.getId());
            return ResponseEntity.ok(toResponse(school));
        } catch (UnauthorizedException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", exception.getMessage()));
        } catch (BadRequestException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @Operation(summary = "Supprimer une ecole", description = "Suppression d'une fiche ecole par son proprietaire uniquement")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ecole supprimee"),
            @ApiResponse(responseCode = "401", description = "Non authentifie"),
            @ApiResponse(responseCode = "403", description = "Ownership refuse"),
            @ApiResponse(responseCode = "404", description = "Ecole introuvable")
    })
    @SecurityRequirement(name = "bearer-jwt")
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteSchool(@PathVariable UUID id, Principal principal) {
        try {
            User user = resolveAuthenticatedUser(principal);
            Optional<UUID> optionalOwnerUserId = schoolRepository.findOwnerUserIdById(id);
            if (optionalOwnerUserId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Ecole introuvable"));
            }
            if (!optionalOwnerUserId.get().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Vous ne pouvez supprimer que votre propre fiche ecole"));
            }
            int deleted = schoolRepository.deleteByIdDirect(id);
            if (deleted == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Ecole introuvable"));
            }
            LOGGER.info("Ecole supprimee id={} userId={}", id, user.getId());
            return ResponseEntity.ok(Map.of("message", "Ecole supprimee avec succes"));
        } catch (UnauthorizedException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", exception.getMessage()));
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

    private void applyRequest(School school, SchoolRequest request, boolean creation) {
        if (request == null) {
            throw new BadRequestException("Payload manquant");
        }

        if (creation || request.getNomEtablissement() != null) {
            String value = trim(request.getNomEtablissement());
            if (value == null || value.length() < 2 || value.length() > 150) {
                throw new BadRequestException("nomEtablissement doit contenir entre 2 et 150 caracteres");
            }
            school.setNomEtablissement(value);
        }

        if (creation || request.getStatut() != null) {
            String value = trim(request.getStatut());
            if (value == null) {
                throw new BadRequestException("Le champ statut est obligatoire");
            }
            try {
                school.setStatut(SchoolStatus.valueOf(value.toUpperCase()));
            } catch (IllegalArgumentException exception) {
                throw new BadRequestException("Valeur invalide pour statut: " + value);
            }
        }

        if (creation || request.getDomaines() != null) {
            Set<String> domaines = normalizeNonEmptySet(request.getDomaines(), "domaines");
            school.setDomaines(domaines);
        }

        if (request.getDiplomesDelivres() != null) {
            school.setDiplomesDelivres(normalizeSet(request.getDiplomesDelivres()));
        }
        if (request.getDescription() != null) {
            school.setDescription(trim(request.getDescription()));
        }
        if (request.getAdresse() != null) {
            school.setAdresse(trim(request.getAdresse()));
        }
        if (request.getSiteWeb() != null) {
            String siteWeb = trim(request.getSiteWeb());
            validateOptionalUrl(siteWeb, "siteWeb");
            school.setSiteWeb(siteWeb);
        }
        if (request.getLogoUrl() != null) {
            String logoUrl = trim(request.getLogoUrl());
            validateOptionalUrl(logoUrl, "logoUrl");
            school.setLogoUrl(logoUrl);
        }
    }

    private SchoolResponse toResponse(School school) {
        SchoolResponse response = new SchoolResponse();
        response.setId(school.getId());
        response.setOwnerUserId(school.getUser() != null ? school.getUser().getId() : null);
        response.setNomEtablissement(school.getNomEtablissement());
        response.setStatut(school.getStatut() != null ? school.getStatut().name() : null);
        response.setDomaines(copySet(school.getDomaines()));
        response.setDiplomesDelivres(copySet(school.getDiplomesDelivres()));
        response.setDescription(school.getDescription());
        response.setAdresse(school.getAdresse());
        response.setSiteWeb(school.getSiteWeb());
        response.setLogoUrl(school.getLogoUrl());
        response.setCreatedAt(school.getCreatedAt());
        return response;
    }

    private Set<String> normalizeNonEmptySet(List<String> values, String fieldName) {
        Set<String> set = normalizeSet(values);
        if (set.isEmpty()) {
            throw new BadRequestException(fieldName + " ne peut pas etre vide");
        }
        return set;
    }

    private Set<String> normalizeSet(List<String> values) {
        Set<String> set = new HashSet<>();
        if (values == null) {
            return set;
        }
        for (String value : values) {
            String trimmed = trim(value);
            if (trimmed != null && !trimmed.isBlank()) {
                set.add(trimmed);
            }
        }
        return set;
    }

    private Set<String> copySet(Set<String> values) {
        return values == null ? new HashSet<>() : new HashSet<>(values);
    }

    private void validateOptionalUrl(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return;
        }
        try {
            URI uri = URI.create(value);
            if (uri.getScheme() == null || !(uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https"))) {
                throw new IllegalArgumentException();
            }
        } catch (Exception exception) {
            throw new BadRequestException(fieldName + " doit etre une URL valide (http/https)");
        }
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
}
