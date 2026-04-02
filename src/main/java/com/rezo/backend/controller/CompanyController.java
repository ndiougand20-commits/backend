package com.rezo.backend.controller;

import com.rezo.backend.dto.company.CompanyRequest;
import com.rezo.backend.dto.company.CompanyResponse;
import com.rezo.entities.Company;
import com.rezo.entities.User;
import com.rezo.entities.enums.CompanySize;
import com.rezo.entities.enums.UserRole;
import com.rezo.repositories.CompanyRepository;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompanyController.class);

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    public CompanyController(CompanyRepository companyRepository, UserRepository userRepository) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Lister les entreprises", description = "Retourne la liste publique des entreprises")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Liste retournee")})
    @GetMapping
    @Transactional
    public ResponseEntity<List<CompanyResponse>> listCompanies() {
        List<CompanyResponse> companies = companyRepository.findAllWithUser().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(companies);
    }

    @Operation(summary = "Detail d'une entreprise", description = "Retourne une entreprise par son identifiant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entreprise retournee"),
            @ApiResponse(responseCode = "404", description = "Entreprise introuvable")
    })
    @GetMapping("/{id}")
    @Transactional
    public ResponseEntity<?> getCompanyById(@PathVariable UUID id) {
        return companyRepository.findByIdWithUser(id)
                .<ResponseEntity<?>>map(company -> ResponseEntity.ok(toResponse(company)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Entreprise introuvable")));
    }

    @Operation(summary = "Creer une entreprise", description = "Creation d'une fiche entreprise par un utilisateur role ENTREPRISE")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Entreprise creee"),
            @ApiResponse(responseCode = "400", description = "Payload invalide"),
            @ApiResponse(responseCode = "401", description = "Non authentifie"),
            @ApiResponse(responseCode = "403", description = "Role insuffisant"),
            @ApiResponse(responseCode = "409", description = "Fiche deja existante")
    })
    @SecurityRequirement(name = "bearer-jwt")
    @PostMapping
    @Transactional
    public ResponseEntity<?> createCompany(@RequestBody CompanyRequest request, Principal principal) {
        try {
            User user = resolveAuthenticatedUser(principal);
            if (user.getRole() != UserRole.ENTREPRISE) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Seuls les utilisateurs ENTREPRISE peuvent creer une fiche entreprise"));
            }
            if (companyRepository.findByUserId(user.getId()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "Une fiche entreprise existe deja pour ce compte"));
            }

            Company company = new Company();
            company.setUser(user);
            applyRequest(company, request, true);
            company = companyRepository.save(company);

            LOGGER.info("Entreprise creee id={} userId={}", company.getId(), user.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(company));
        } catch (UnauthorizedException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", exception.getMessage()));
        } catch (BadRequestException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @Operation(summary = "Modifier une entreprise", description = "Modification d'une fiche entreprise par son proprietaire uniquement")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entreprise mise a jour"),
            @ApiResponse(responseCode = "400", description = "Payload invalide"),
            @ApiResponse(responseCode = "401", description = "Non authentifie"),
            @ApiResponse(responseCode = "403", description = "Ownership refuse"),
            @ApiResponse(responseCode = "404", description = "Entreprise introuvable")
    })
    @SecurityRequirement(name = "bearer-jwt")
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateCompany(@PathVariable UUID id, @RequestBody CompanyRequest request, Principal principal) {
        try {
            User user = resolveAuthenticatedUser(principal);
            Optional<Company> optionalCompany = companyRepository.findByIdWithUser(id);
            if (optionalCompany.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Entreprise introuvable"));
            }
            Company company = optionalCompany.get();
            if (!company.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Vous ne pouvez modifier que votre propre fiche entreprise"));
            }
            applyRequest(company, request, false);
            company = companyRepository.save(company);
            LOGGER.info("Entreprise mise a jour id={} userId={}", company.getId(), user.getId());
            return ResponseEntity.ok(toResponse(company));
        } catch (UnauthorizedException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", exception.getMessage()));
        } catch (BadRequestException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @Operation(summary = "Supprimer une entreprise", description = "Suppression d'une fiche entreprise par son proprietaire uniquement")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entreprise supprimee"),
            @ApiResponse(responseCode = "401", description = "Non authentifie"),
            @ApiResponse(responseCode = "403", description = "Ownership refuse"),
            @ApiResponse(responseCode = "404", description = "Entreprise introuvable")
    })
    @SecurityRequirement(name = "bearer-jwt")
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteCompany(@PathVariable UUID id, Principal principal) {
        try {
            User user = resolveAuthenticatedUser(principal);
            Optional<UUID> optionalOwnerUserId = companyRepository.findOwnerUserIdById(id);
            if (optionalOwnerUserId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Entreprise introuvable"));
            }
            if (!optionalOwnerUserId.get().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Vous ne pouvez supprimer que votre propre fiche entreprise"));
            }
            int deleted = companyRepository.deleteByIdDirect(id);
            if (deleted == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Entreprise introuvable"));
            }
            LOGGER.info("Entreprise supprimee id={} userId={}", id, user.getId());
            return ResponseEntity.ok(Map.of("message", "Entreprise supprimee avec succes"));
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

    private void applyRequest(Company company, CompanyRequest request, boolean creation) {
        if (request == null) {
            throw new BadRequestException("Payload manquant");
        }

        if (creation || request.getRaisonSociale() != null) {
            String value = trim(request.getRaisonSociale());
            if (value == null || value.length() < 2 || value.length() > 150) {
                throw new BadRequestException("raisonSociale doit contenir entre 2 et 150 caracteres");
            }
            company.setRaisonSociale(value);
        }
        if (creation || request.getSecteurActivite() != null) {
            String value = trim(request.getSecteurActivite());
            if (value == null || value.length() < 2 || value.length() > 120) {
                throw new BadRequestException("secteurActivite doit contenir entre 2 et 120 caracteres");
            }
            company.setSecteurActivite(value);
        }
        if (creation || request.getTaille() != null) {
            String value = trim(request.getTaille());
            if (value == null) {
                throw new BadRequestException("Le champ taille est obligatoire");
            }
            try {
                company.setTaille(CompanySize.valueOf(value.toUpperCase()));
            } catch (IllegalArgumentException exception) {
                throw new BadRequestException("Valeur invalide pour taille: " + value);
            }
        }
        if (creation || request.getDescription() != null) {
            String value = trim(request.getDescription());
            if (value == null || value.length() < 2) {
                throw new BadRequestException("description doit contenir au moins 2 caracteres");
            }
            company.setDescription(value);
        }
        if (request.getAdresse() != null) {
            company.setAdresse(trim(request.getAdresse()));
        }
        if (request.getSiteWeb() != null) {
            String siteWeb = trim(request.getSiteWeb());
            validateOptionalUrl(siteWeb, "siteWeb");
            company.setSiteWeb(siteWeb);
        }
        if (request.getLogoUrl() != null) {
            String logoUrl = trim(request.getLogoUrl());
            validateOptionalUrl(logoUrl, "logoUrl");
            company.setLogoUrl(logoUrl);
        }
    }

    private CompanyResponse toResponse(Company company) {
        CompanyResponse response = new CompanyResponse();
        response.setId(company.getId());
        response.setOwnerUserId(company.getUser() != null ? company.getUser().getId() : null);
        response.setRaisonSociale(company.getRaisonSociale());
        response.setSecteurActivite(company.getSecteurActivite());
        response.setTaille(company.getTaille() != null ? company.getTaille().name() : null);
        response.setDescription(company.getDescription());
        response.setAdresse(company.getAdresse());
        response.setSiteWeb(company.getSiteWeb());
        response.setLogoUrl(company.getLogoUrl());
        response.setCreatedAt(company.getCreatedAt());
        return response;
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
