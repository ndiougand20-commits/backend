package com.rezo.backend.controller;

import com.rezo.backend.dto.user.UserMediaFileResponse;
import com.rezo.backend.service.UserMediaStorageService;
import com.rezo.entities.User;
import com.rezo.entities.UserMediaFile;
import com.rezo.entities.enums.UserRole;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/me/media")
@SecurityRequirement(name = "bearer-jwt")
public class UserMediaController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserMediaController.class);
    private static final String LEGACY_DEFAULT_JUSTIFICATIF = "JUSTIFICATIF_PDF";
    private static final Set<String> VALID_PDF_CATEGORIES = Set.of(
            LEGACY_DEFAULT_JUSTIFICATIF,
            "CV",
            "LM",
            "DIPLOME",
            "BULLETIN",
            "JUSTIFICATIF_RECONN",
            "JUSTIFICATIF_ENTREPRISE",
            "OFFER_BROCHURE"
    );

    private final UserRepository userRepository;
    private final UserMediaFileRepository userMediaFileRepository;
    private final UserMediaStorageService userMediaStorageService;

    public UserMediaController(
            UserRepository userRepository,
            UserMediaFileRepository userMediaFileRepository,
            UserMediaStorageService userMediaStorageService
    ) {
        this.userRepository = userRepository;
        this.userMediaFileRepository = userMediaFileRepository;
        this.userMediaStorageService = userMediaStorageService;
    }

    @Operation(summary = "Lister mes medias", description = "Retourne les photos et justificatifs de l'utilisateur connecte")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste retournee"),
            @ApiResponse(responseCode = "401", description = "Non authentifie")
    })
    @GetMapping
    public ResponseEntity<?> listMyMedia(@RequestParam(required = false) String category, Principal principal) {
        try {
            User user = resolveAuthenticatedUser(principal);
            UUID userId = user.getId();
            List<UserMediaFile> files;
            if (category == null || category.isBlank()) {
                files = userMediaFileRepository.findByUserIdOrderByCreatedAtDesc(userId);
            } else {
                String normalizedCategory = category.trim().toUpperCase(Locale.ROOT);
                files = userMediaFileRepository.findByUserIdAndCategoryOrderByCreatedAtDesc(userId, normalizedCategory);
            }
            return ResponseEntity.ok(files.stream().map(UserMediaFileResponse::from).toList());
        } catch (UnauthorizedException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", exception.getMessage()));
        }
    }

    @Operation(summary = "Uploader une photo", description = "Accepte uniquement des fichiers image pour le profil")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Photo uploadee"),
            @ApiResponse(responseCode = "400", description = "Fichier invalide"),
            @ApiResponse(responseCode = "401", description = "Non authentifie")
    })
    @PostMapping("/photos")
    @Transactional
    public ResponseEntity<?> uploadPhoto(@RequestParam("file") MultipartFile file, Principal principal) {
        return upload(file, principal, UploadKind.PHOTO, null);
    }

    @Operation(summary = "Uploader un justificatif PDF", description = "Accepte uniquement des fichiers PDF")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Justificatif uploade"),
            @ApiResponse(responseCode = "400", description = "Fichier invalide"),
            @ApiResponse(responseCode = "401", description = "Non authentifie")
    })
    @PostMapping("/justificatifs")
    @Transactional
    public ResponseEntity<?> uploadJustificatif(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "category", required = false) String category,
            Principal principal
    ) {
        return upload(file, principal, UploadKind.JUSTIFICATIF, category);
    }

    @Operation(summary = "Supprimer un media", description = "Supprime un fichier media appartenant a l'utilisateur connecte")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Media supprime"),
            @ApiResponse(responseCode = "401", description = "Non authentifie"),
            @ApiResponse(responseCode = "404", description = "Media introuvable")
    })
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteMedia(@PathVariable UUID id, Principal principal) {
        try {
            User user = resolveAuthenticatedUser(principal);
            UUID userId = user.getId();
            int deleted = userMediaFileRepository.deleteByIdAndUserId(id, userId);
            if (deleted == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Media introuvable"));
            }
            return ResponseEntity.ok(Map.of("message", "Media supprime avec succes"));
        } catch (UnauthorizedException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", exception.getMessage()));
        }
    }

    private ResponseEntity<?> upload(MultipartFile file, Principal principal, UploadKind kind, String requestedCategory) {
        try {
            User user = resolveAuthenticatedUser(principal);
            UUID userId = user.getId();

            String category = null;
            if (kind == UploadKind.JUSTIFICATIF) {
                category = normalizeJustificatifCategory(requestedCategory);
                ensureCategoryAllowedForRole(user, category);
            }

            UserMediaStorageService.StoredMedia stored = kind == UploadKind.PHOTO
                    ? userMediaStorageService.storePhoto(userId, file)
                    : userMediaStorageService.storeJustificatifPdf(userId, file, category);

            UserMediaFile entity = new UserMediaFile();
            entity.setUserId(userId);
            entity.setCategory(stored.category());
            entity.setOriginalFileName(stored.originalFileName());
            entity.setContentType(stored.contentType());
            entity.setFileUrl(stored.fileUrl());
            entity = userMediaFileRepository.save(entity);

            LOGGER.info("Media upload userId={} category={} id={}", userId, entity.getCategory(), entity.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(UserMediaFileResponse.from(entity));
        } catch (UnauthorizedException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", exception.getMessage()));
        } catch (ForbiddenException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", exception.getMessage()));
        } catch (UserMediaStorageService.MediaValidationException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        } catch (UserMediaStorageService.MediaStorageException exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors du stockage du fichier"));
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

    private String normalizeJustificatifCategory(String requestedCategory) {
        String normalized = (requestedCategory == null || requestedCategory.isBlank())
                ? LEGACY_DEFAULT_JUSTIFICATIF
                : requestedCategory.trim().toUpperCase(Locale.ROOT);
        if (!VALID_PDF_CATEGORIES.contains(normalized)) {
            throw new UserMediaStorageService.MediaValidationException("Categorie de justificatif invalide");
        }
        return normalized;
    }

    private void ensureCategoryAllowedForRole(User user, String category) {
        if (user == null || user.getRole() == null || category == null) {
            throw new ForbiddenException("Categorie non autorisee pour ce profil");
        }

        if (user.getRole() == UserRole.ADMIN) {
            return;
        }

        Set<String> allowedCategories = switch (user.getRole()) {
            case ETUDIANT -> Set.of(LEGACY_DEFAULT_JUSTIFICATIF, "CV", "LM", "DIPLOME");
            case LYCEEN -> Set.of(LEGACY_DEFAULT_JUSTIFICATIF, "BULLETIN", "JUSTIFICATIF_RECONN");
            case ECOLE -> Set.of(LEGACY_DEFAULT_JUSTIFICATIF, "JUSTIFICATIF_RECONN", "OFFER_BROCHURE");
            case ENTREPRISE -> Set.of(LEGACY_DEFAULT_JUSTIFICATIF, "JUSTIFICATIF_ENTREPRISE", "OFFER_BROCHURE");
            default -> Set.of();
        };

        if (!allowedCategories.contains(category)) {
            throw new ForbiddenException("Categorie non autorisee pour ce role");
        }
    }

    private enum UploadKind {
        PHOTO,
        JUSTIFICATIF
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
}
