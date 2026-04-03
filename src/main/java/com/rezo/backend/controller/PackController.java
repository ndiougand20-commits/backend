package com.rezo.backend.controller;

import com.rezo.backend.dto.pack.PackRequest;
import com.rezo.backend.dto.pack.PackResponse;
import com.rezo.backend.service.PackRules;
import com.rezo.entities.Pack;
import com.rezo.entities.User;
import com.rezo.entities.enums.UserRole;
import com.rezo.repositories.PackRepository;
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

import java.math.BigDecimal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/packs")
public class PackController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PackController.class);

    private final PackRepository packRepository;
    private final UserRepository userRepository;

    public PackController(PackRepository packRepository, UserRepository userRepository) {
        this.packRepository = packRepository;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Lister les packs", description = "Retourne la liste des packs disponibles pour affichage ou souscription")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Liste retournee")})
    @GetMapping
    public ResponseEntity<List<PackResponse>> listPacks() {
        return ResponseEntity.ok(packRepository.findAll().stream().map(this::toResponse).toList());
    }

    @Operation(summary = "Detail d'un pack", description = "Retourne le detail d'un pack par son identifiant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pack retourne"),
            @ApiResponse(responseCode = "404", description = "Pack introuvable")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getPackById(@PathVariable UUID id) {
        return packRepository.findById(id)
                .<ResponseEntity<?>>map(pack -> ResponseEntity.ok(toResponse(pack)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Pack introuvable")));
    }

    @Operation(summary = "Creer un pack", description = "Creation d'un nouveau pack. Reserve a l'administrateur")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pack cree"),
            @ApiResponse(responseCode = "400", description = "Payload invalide"),
            @ApiResponse(responseCode = "401", description = "Non authentifie"),
            @ApiResponse(responseCode = "403", description = "Reserve admin"),
            @ApiResponse(responseCode = "409", description = "Nom deja utilise")
    })
    @SecurityRequirement(name = "bearer-jwt")
    @PostMapping
    @Transactional
    public ResponseEntity<?> createPack(@RequestBody PackRequest request, Principal principal) {
        try {
            resolveAdmin(principal);
            Pack pack = new Pack();
            applyRequest(pack, request, true);
            pack = packRepository.save(pack);
            LOGGER.info("Pack cree id={} nom={}", pack.getId(), pack.getNom());
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(pack));
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

    @Operation(summary = "Modifier un pack", description = "Modification d'un pack existant. Reserve a l'administrateur")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pack mis a jour"),
            @ApiResponse(responseCode = "400", description = "Payload invalide"),
            @ApiResponse(responseCode = "401", description = "Non authentifie"),
            @ApiResponse(responseCode = "403", description = "Reserve admin"),
            @ApiResponse(responseCode = "404", description = "Pack introuvable"),
            @ApiResponse(responseCode = "409", description = "Nom deja utilise")
    })
    @SecurityRequirement(name = "bearer-jwt")
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updatePack(@PathVariable UUID id, @RequestBody PackRequest request, Principal principal) {
        try {
            resolveAdmin(principal);
            Optional<Pack> optionalPack = packRepository.findById(id);
            if (optionalPack.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Pack introuvable"));
            }
            Pack pack = optionalPack.get();
            applyRequest(pack, request, false);
            pack = packRepository.save(pack);
            LOGGER.info("Pack mis a jour id={} nom={}", pack.getId(), pack.getNom());
            return ResponseEntity.ok(toResponse(pack));
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

    @Operation(summary = "Supprimer un pack", description = "Suppression d'un pack non attribue a des utilisateurs. Reserve a l'administrateur")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pack supprime"),
            @ApiResponse(responseCode = "401", description = "Non authentifie"),
            @ApiResponse(responseCode = "403", description = "Reserve admin"),
            @ApiResponse(responseCode = "404", description = "Pack introuvable"),
            @ApiResponse(responseCode = "409", description = "Pack deja attribue")
    })
    @SecurityRequirement(name = "bearer-jwt")
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deletePack(@PathVariable UUID id, Principal principal) {
        try {
            resolveAdmin(principal);
            Optional<Pack> optionalPack = packRepository.findById(id);
            if (optionalPack.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Pack introuvable"));
            }
            if (packRepository.countUsersByPackId(id) > 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "Suppression impossible: ce pack est actuellement attribue a des utilisateurs"));
            }
            packRepository.delete(optionalPack.get());
            LOGGER.info("Pack supprime id={}", id);
            return ResponseEntity.ok(Map.of("message", "Pack supprime avec succes"));
        } catch (UnauthorizedException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", exception.getMessage()));
        } catch (ForbiddenException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", exception.getMessage()));
        }
    }

    private User resolveAdmin(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new UnauthorizedException("Non authentifie");
        }
        UUID userId = UUID.fromString(principal.getName());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Utilisateur authentifie introuvable"));
        if (user.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Seul un administrateur peut gerer les packs");
        }
        return user;
    }

    private void applyRequest(Pack pack, PackRequest request, boolean creation) {
        if (request == null) {
            throw new BadRequestException("Payload manquant");
        }

        if (creation || request.getNom() != null) {
            String nom = normalize(request.getNom());
            if (nom == null || nom.length() < 2 || nom.length() > 100) {
                throw new BadRequestException("nom doit contenir entre 2 et 100 caracteres");
            }
            Optional<Pack> existing = packRepository.findByNomIgnoreCase(nom);
            if (existing.isPresent() && (pack.getId() == null || !existing.get().getId().equals(pack.getId()))) {
                throw new ConflictException("Un pack avec ce nom existe deja");
            }
            pack.setNom(nom);
        }

        if (request.getDescription() != null || creation) {
            pack.setDescription(request != null ? trim(request.getDescription()) : null);
        }

        if (creation || request.getPrix() != null) {
            BigDecimal prix = request.getPrix();
            if (prix == null || prix.compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("prix doit etre superieur ou egal a 0");
            }
            pack.setPrix(prix);
        }

        if (creation || request.getCible() != null) {
            Set<String> targets = PackRules.parseCsv(request.getCible());
            if (targets.isEmpty()) {
                throw new BadRequestException("cible est obligatoire");
            }
            pack.setCible(String.join(",", targets));
        }

        if (creation || request.getFeatures() != null) {
            Set<String> features = PackRules.parseValues(request.getFeatures());
            if (features.isEmpty()) {
                throw new BadRequestException("features ne peut pas etre vide");
            }
            pack.setFeatures(String.join(",", features));
        }
    }

    private PackResponse toResponse(Pack pack) {
        PackResponse response = new PackResponse();
        response.setId(pack.getId());
        response.setNom(pack.getNom());
        response.setDescription(pack.getDescription());
        response.setPrix(pack.getPrix());
        response.setCible(pack.getCible());
        response.setFeatures(new ArrayList<>(PackRules.parseCsv(pack.getFeatures())));
        response.setCreatedAt(pack.getCreatedAt());
        return response;
    }

    private String normalize(String value) {
        String trimmed = trim(value);
        return trimmed == null ? null : trimmed.toUpperCase();
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
