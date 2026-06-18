package com.rezo.backend.controller;

import com.rezo.backend.dto.common.ApiErrorResponse;
import com.rezo.backend.dto.auth.LoginRequest;
import com.rezo.backend.dto.auth.LoginResponse;
import com.rezo.backend.dto.auth.LogoutRequest;
import com.rezo.backend.dto.auth.RefreshRequest;
import com.rezo.backend.dto.auth.RefreshResponse;
import com.rezo.backend.dto.auth.SignupRequest;
import com.rezo.backend.dto.auth.SignupResponse;
import com.rezo.backend.service.JwtService;
import com.rezo.backend.service.RefreshTokenService;
import com.rezo.entities.Company;
import com.rezo.entities.Pack;
import com.rezo.entities.Profile;
import com.rezo.entities.School;
import com.rezo.entities.User;
import com.rezo.entities.enums.CompanySize;
import com.rezo.entities.enums.SchoolStatus;
import com.rezo.entities.enums.UserRole;
import com.rezo.repositories.CompanyRepository;
import com.rezo.repositories.PackRepository;
import com.rezo.repositories.ProfileRepository;
import com.rezo.repositories.SchoolRepository;
import com.rezo.repositories.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.security.Principal;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository userRepository;
    private final PackRepository packRepository;
    private final ProfileRepository profileRepository;
    private final CompanyRepository companyRepository;
    private final SchoolRepository schoolRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final Environment environment;

    public AuthController(
            UserRepository userRepository,
            PackRepository packRepository,
            ProfileRepository profileRepository,
            CompanyRepository companyRepository,
            SchoolRepository schoolRepository,
            JwtService jwtService,
                RefreshTokenService refreshTokenService,
            Environment environment
    ) {
        this.userRepository = userRepository;
        this.packRepository = packRepository;
        this.profileRepository = profileRepository;
        this.companyRepository = companyRepository;
        this.schoolRepository = schoolRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.environment = environment;
    }

    @Operation(summary = "Inscription multi-profils", description = "Cree un compte User et le profil associe selon le role")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Inscription reussie"),
            @ApiResponse(responseCode = "400", description = "Payload invalide"),
            @ApiResponse(responseCode = "409", description = "Email deja utilise")
    })
    @PostMapping("/signup")
    @Transactional
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
        try {
            validateBasePayload(request);
            UserRole role = parseRole(request.getRole());

            if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase(Locale.ROOT))) {
                LOGGER.warn("Signup refuse: email deja utilise ({})", request.getEmail());
                return error(HttpStatus.CONFLICT, "CONFLICT", "Email deja utilise");
            }

            Map<String, Object> profil = Optional.ofNullable(request.getProfil()).orElse(Collections.emptyMap());
            validateRoleSpecificPayload(role, profil);

            User user = new User();
            user.setEmail(request.getEmail().trim().toLowerCase(Locale.ROOT));
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            user.setRole(role);
            user.setPrenom(defaultIfBlank(request.getPrenom(), "Utilisateur"));
            user.setNom(defaultIfBlank(request.getNom(), "Rezo"));
            user.setTelephone(request.getTelephone());
            user.setPack(resolveDefaultPack());

            user = userRepository.save(user);
            String profileType = createAndLinkComplementaryProfile(role, profil, user);

            LOGGER.info("Signup reussi pour email={} role={}", user.getEmail(), role);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new SignupResponse(
                            "Inscription reussie",
                            user.getId(),
                            user.getEmail(),
                            user.getRole().name(),
                            profileType
                    ));
        } catch (BadRequestException exception) {
            LOGGER.warn("Signup invalide: {}", exception.getMessage());
            return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", exception.getMessage());
        }
    }

    @Operation(summary = "Login", description = "Authentification et generation du token JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login reussi, token JWT retourne"),
            @ApiResponse(responseCode = "400", description = "Payload invalide"),
            @ApiResponse(responseCode = "401", description = "Email ou mot de passe incorrect")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request == null || isBlank(request.getEmail()) || isBlank(request.getPassword())) {
            return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Email et mot de passe obligatoires");
        }
        Optional<User> optUser = userRepository.findByEmail(request.getEmail().trim().toLowerCase(Locale.ROOT));
        if (optUser.isEmpty() || !passwordEncoder.matches(request.getPassword(), optUser.get().getPasswordHash())) {
            LOGGER.warn("Login echoue pour email={}", request.getEmail());
            return error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Email ou mot de passe incorrect");
        }
        String token = jwtService.generateToken(optUser.get());
        String refreshToken = refreshTokenService.issueRefreshToken(optUser.get());
        LOGGER.info("Login reussi pour email={}", optUser.get().getEmail());
        return ResponseEntity.ok(new LoginResponse(token, refreshToken));
    }

    @Operation(summary = "Refresh token", description = "Renouvelle le token d'acces via un refresh token valide")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token renouvele"),
            @ApiResponse(responseCode = "400", description = "Payload invalide"),
            @ApiResponse(responseCode = "401", description = "Refresh token invalide ou expire")
    })
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {
        if (request == null || isBlank(request.getRefreshToken())) {
            return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Refresh token obligatoire");
        }

        try {
            RefreshTokenService.TokenPair tokenPair =
                    refreshTokenService.rotateRefreshToken(request.getRefreshToken().trim());
            return ResponseEntity.ok(new RefreshResponse(tokenPair.accessToken(), tokenPair.refreshToken()));
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("Refresh refuse: {}", exception.getMessage());
            return error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", exception.getMessage());
        }
    }

    @Operation(summary = "Logout", description = "Invalide les refresh tokens de la session courante")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logout reussi"),
            @ApiResponse(responseCode = "401", description = "Non authentifie")
    })
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody(required = false) LogoutRequest request, Principal principal) {
        if (principal == null || isBlank(principal.getName())) {
            return error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Non authentifie");
        }

        UUID userId;
        try {
            userId = UUID.fromString(principal.getName());
        } catch (IllegalArgumentException exception) {
            return error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Session invalide");
        }

        if (request != null && !isBlank(request.getRefreshToken())) {
            refreshTokenService.revokeByToken(request.getRefreshToken().trim());
        }
        refreshTokenService.revokeAllForUser(userId);

        return ResponseEntity.ok(Map.of("message", "Logout reussi"));
    }

    @Operation(summary = "Suppression tous les utilisateurs (dev/test)", description = "⚠️ DANGER: Supprime TOUS les comptes Users et leurs profils. Utiliser UNIQUEMENT en dev/test.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tous les utilisateurs ont ete supprimes")
    })
    @org.springframework.context.annotation.Profile("dev")
    @DeleteMapping("/users")
    @Transactional
    public ResponseEntity<?> deleteAllUsers() {
        if (!isDevProfileActive()) {
            return error(HttpStatus.FORBIDDEN, "FORBIDDEN", "Endpoint disponible uniquement en environnement dev");
        }
        try {
            profileRepository.deleteAll();
            companyRepository.deleteAll();
            schoolRepository.deleteAll();
            long deletedCount = userRepository.count();
            userRepository.deleteAll();
            LOGGER.warn("DANGER: Tous les utilisateurs ont ete supprimes (count={})", deletedCount);
            return ResponseEntity.ok(Map.of("message", "Tous les utilisateurs ont ete supprimes", "count", deletedCount));
        } catch (Exception exception) {
            LOGGER.error("Erreur lors de la suppression en masse: {}", exception.getMessage());
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "Erreur lors de la suppression de masse");
        }
    }

    @Operation(summary = "Suppression utilisateur", description = "Supprime un compte User et son profil associe via email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Suppression reussie"),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable"),
            @ApiResponse(responseCode = "409", description = "Suppression impossible a cause de dependances")
    })
    @org.springframework.context.annotation.Profile("dev")
    @DeleteMapping("/users/by-email/{email}")
    @Transactional
    public ResponseEntity<?> deleteUserByEmail(@PathVariable("email") String email) {
        if (!isDevProfileActive()) {
            return error(HttpStatus.FORBIDDEN, "FORBIDDEN", "Endpoint disponible uniquement en environnement dev");
        }
        String normalizedEmail = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        if (isBlank(normalizedEmail)) {
            return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Le parametre email est obligatoire");
        }

        Optional<UUID> optionalUserId = userRepository.findIdByEmail(normalizedEmail);
        if (optionalUserId.isEmpty()) {
            LOGGER.warn("Suppression impossible: email introuvable ({})", normalizedEmail);
            return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "Utilisateur introuvable");
        }

        try {
            UUID userId = optionalUserId.get();

            profileRepository.deleteAllByUserId(userId);
            companyRepository.deleteAllByUserId(userId);
            schoolRepository.deleteAllByUserId(userId);
            int deletedUsers = userRepository.deleteByIdDirect(userId);
            if (deletedUsers == 0) {
                return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "Utilisateur introuvable");
            }

            LOGGER.info("Suppression reussie pour email={}", normalizedEmail);
            return ResponseEntity.ok(Map.of("message", "Utilisateur supprime"));
        } catch (DataIntegrityViolationException exception) {
            LOGGER.warn("Suppression refusee pour email={} cause={}", normalizedEmail, exception.getMessage());
            return error(HttpStatus.CONFLICT, "CONFLICT", "Suppression impossible: dependances existantes");
        }
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity
                .status(status)
                .body(ApiErrorResponse.of(status.value(), code, message, "/api/auth"));
    }

    private void validateBasePayload(SignupRequest request) {
        if (request == null) {
            throw new BadRequestException("Payload manquant");
        }
        if (isBlank(request.getEmail())) {
            throw new BadRequestException("Le champ email est obligatoire");
        }
        if (isBlank(request.getPassword())) {
            throw new BadRequestException("Le champ password est obligatoire");
        }
        if (isBlank(request.getRole())) {
            throw new BadRequestException("Le champ role est obligatoire");
        }
    }

    private UserRole parseRole(String rawRole) {
        try {
            return UserRole.valueOf(rawRole.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Role invalide: " + rawRole);
        }
    }

    private void validateRoleSpecificPayload(UserRole role, Map<String, Object> profil) {
        switch (role) {
            case ETUDIANT -> {
                requiredString(profil, "niveauEtude", "niveau_etude");
                requiredString(profil, "domaine");
            }
            case LYCEEN -> {
                requiredString(profil, "classeActuelle", "classe");
                requiredString(profil, "serieOrientation", "serie");
            }
            case ENTREPRISE -> {
                requiredString(profil, "raisonSociale", "raison_sociale");
                requiredString(profil, "secteurActivite", "secteur");
                requiredString(profil, "taille");
                requiredString(profil, "description");
            }
            case ECOLE -> {
                requiredString(profil, "nomEtablissement", "nom_etablissement");
                requiredString(profil, "statut");
            }
            default -> throw new BadRequestException("Inscription non supportee pour le role " + role);
        }
    }

    private Pack resolveDefaultPack() {
        return packRepository.findByNomIgnoreCase("FREE")
                .or(() -> packRepository.findAll().stream().findFirst())
                .orElseThrow(() -> new BadRequestException("Aucun pack disponible pour creer le compte"));
    }

    private String createAndLinkComplementaryProfile(UserRole role, Map<String, Object> profil, User user) {
        return switch (role) {
            case ETUDIANT -> {
                Profile profile = new Profile();
                profile.setUser(user);
                profile.setNiveauEtude(requiredString(profil, "niveauEtude", "niveau_etude"));
                profile.setDomaine(requiredString(profil, "domaine"));
                profile.setCompetences(stringSet(profil, "competences"));
                profile.setObjectif(readOptionalString(profil, "objectif"));
                profile.setPreferencesSecteur(stringSet(profil, "preferencesSecteur", "preferences_secteur"));
                profile.setPreferencesLieu(stringSet(profil, "preferencesLieu", "preferences_lieu"));
                profile.setExperiences(stringSet(profil, "experiences"));
                profileRepository.save(profile);
                yield "PROFILE";
            }
            case LYCEEN -> {
                Profile profile = new Profile();
                profile.setUser(user);
                profile.setClasseActuelle(requiredString(profil, "classeActuelle", "classe"));
                profile.setSerieOrientation(requiredString(profil, "serieOrientation", "serie"));
                profile.setObjectifPostbac(readOptionalString(profil, "objectifPostbac", "objectif_postbac"));
                profile.setCentresInteret(stringSet(profil, "centresInteret", "centres_interet"));
                profileRepository.save(profile);
                yield "PROFILE";
            }
            case ENTREPRISE -> {
                Company company = new Company();
                company.setUser(user);
                company.setRaisonSociale(requiredString(profil, "raisonSociale", "raison_sociale"));
                company.setSecteurActivite(requiredString(profil, "secteurActivite", "secteur"));
                company.setTaille(parseEnum(requiredString(profil, "taille"), CompanySize.class, "taille"));
                company.setDescription(requiredString(profil, "description"));
                company.setAdresse(readOptionalString(profil, "adresse"));
                company.setSiteWeb(readOptionalString(profil, "siteWeb", "site_web"));
                company.setLogoUrl(readOptionalString(profil, "logoUrl", "logo_url"));
                companyRepository.save(company);
                yield "COMPANY";
            }
            case ECOLE -> {
                School school = new School();
                school.setUser(user);
                school.setNomEtablissement(requiredString(profil, "nomEtablissement", "nom_etablissement"));
                school.setStatut(parseEnum(requiredString(profil, "statut"), SchoolStatus.class, "statut"));
                school.setDomaines(stringSet(profil, "domaines"));
                school.setDiplomesDelivres(stringSet(profil, "diplomesDelivres", "diplomes_delivres"));
                school.setDescription(readOptionalString(profil, "description"));
                school.setAdresse(readOptionalString(profil, "adresse"));
                school.setSiteWeb(readOptionalString(profil, "siteWeb", "site_web"));
                school.setLogoUrl(readOptionalString(profil, "logoUrl", "logo_url"));
                schoolRepository.save(school);
                yield "SCHOOL";
            }
            default -> throw new BadRequestException("Inscription non supportee pour le role " + role);
        };
    }

    private <E extends Enum<E>> E parseEnum(String raw, Class<E> enumClass, String fieldName) {
        try {
            return Enum.valueOf(enumClass, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Valeur invalide pour " + fieldName + ": " + raw);
        }
    }

    private String readOptionalString(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value instanceof String text && !text.isBlank()) {
                return text.trim();
            }
        }
        return null;
    }

    private String requiredString(Map<String, Object> source, String... keys) {
        String value = readOptionalString(source, keys);
        if (value == null) {
            throw new BadRequestException("Champ profil obligatoire manquant: " + String.join("/", keys));
        }
        return value;
    }

    private Set<String> stringSet(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value instanceof List<?> list) {
                Set<String> values = new HashSet<>();
                for (Object item : list) {
                    if (item instanceof String text && !text.isBlank()) {
                        values.add(text.trim());
                    }
                }
                return values;
            }
        }
        return new HashSet<>();
    }

    private String defaultIfBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isDevProfileActive() {
        return environment != null && environment.matchesProfiles("dev");
    }

    private static class BadRequestException extends RuntimeException {
        private BadRequestException(String message) {
            super(message);
        }
    }
}
