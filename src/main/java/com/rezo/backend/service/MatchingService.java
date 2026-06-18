package com.rezo.backend.service;

import com.rezo.entities.Offer;
import com.rezo.entities.Profile;
import com.rezo.entities.School;
import com.rezo.entities.User;
import com.rezo.entities.enums.SwipeAction;
import com.rezo.entities.enums.UserRole;
import com.rezo.repositories.OfferRepository;
import com.rezo.repositories.ProfileRepository;
import com.rezo.repositories.ProfileSwipeRepository;
import com.rezo.repositories.SwipeRepository;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Service de matching centralisé.
 * Fournit la logique de scoring et d'explicabilité pour les recommandations.
 * Versioning: v1 - scoring simplifié (secteur, compétences, domaines)
 */
@Service
public class MatchingService {

    private final SwipeRepository swipeRepository;
    private final ProfileSwipeRepository profileSwipeRepository;
    private final ProfileRepository profileRepository;
    private final OfferRepository offerRepository;

    // Configuration de scoring (v1)
    private static final int SCORE_MIN = 5;
    private static final int SCORE_MAX = 100;
    private static final int DOMAIN_MATCH_WEIGHT = 25;
    private static final int OBJECTIVE_MATCH_WEIGHT = 30;
    private static final int SECTOR_MATCH_WEIGHT = 15;
    private static final int KEYWORD_MATCH_WEIGHT = 14;
    private static final int ROLE_BONUS_WEIGHT = 8;

    public MatchingService(SwipeRepository swipeRepository,
                          ProfileSwipeRepository profileSwipeRepository,
                          ProfileRepository profileRepository,
                          OfferRepository offerRepository) {
        this.swipeRepository = swipeRepository;
        this.profileSwipeRepository = profileSwipeRepository;
        this.profileRepository = profileRepository;
        this.offerRepository = offerRepository;
    }

    /**
     * Calcule un score pour une offre selon le profil de l'utilisateur.
     * Explicabilité fournie via raisons.
     */
    public MatchScore scoreOffer(User user, Offer offer) {
        if (user == null || offer == null) {
            return new MatchScore(SCORE_MIN, List.of());
        }

        Optional<Profile> userProfile = profileRepository.findByUserId(user.getId());
        int score = SCORE_MIN;
        List<String> reasons = new ArrayList<>();

        if (userProfile.isEmpty()) {
            reasons.add("profil incomplet - matching limité");
            return new MatchScore(score, reasons);
        }

        Profile profile = userProfile.get();
        Set<String> userDomains = profile.getCompetences();
        Set<String> userSectors = profile.getPreferencesSecteur();
        String userLevel = profile.getNiveauEtude();

        // Match domaines/compétences vs offre
        if (userDomains != null && !userDomains.isEmpty() && offer.getDescription() != null) {
            int matches = countTextMatchesInString(userDomains, offer.getDescription());
            if (matches > 0) {
                score += Math.min(50, matches * DOMAIN_MATCH_WEIGHT);
                reasons.add(matches + " competence(s) correspondent");
            }
        }

        // Match secteur préféré vs description offre
        if (userSectors != null && !userSectors.isEmpty() && offer.getDescription() != null) {
            int sectorMatches = countTextMatchesInString(userSectors, offer.getDescription());
            if (sectorMatches > 0) {
                score += Math.min(20, sectorMatches * SECTOR_MATCH_WEIGHT);
                reasons.add("secteur d'activité correspond");
            }
        }

        // Localisation si disponible
        if (offer.getLocation() != null && !offer.getLocation().isBlank()) {
            reasons.add("offre basée à " + offer.getLocation());
        }

        score = Math.min(SCORE_MAX, score);
        if (reasons.isEmpty()) {
            reasons.add("offre disponible sur la plateforme");
        }

        return new MatchScore(score, reasons);
    }

    /**
     * Calcule un score pour une école selon le profil d'un lycéen.
     */
    public MatchScore scoreSchool(User lyceenUser, School school) {
        if (lyceenUser == null || school == null) {
            return new MatchScore(SCORE_MIN, List.of());
        }

        Optional<Profile> userProfile = profileRepository.findByUserId(lyceenUser.getId());
        int score = SCORE_MIN;
        List<String> reasons = new ArrayList<>();

        if (userProfile.isEmpty()) {
            reasons.add("profil incomplet - matching limité");
            return new MatchScore(score, reasons);
        }

        Profile profile = userProfile.get();
        Set<String> centresInteret = profile.getCentresInteret();
        String objectifPostbac = profile.getObjectifPostbac();
        String serieOrientation = profile.getSerieOrientation();

        // Match domaines école vs intérêts lycéen
        if (school.getDomaines() != null && !centresInteret.isEmpty()) {
            int domainMatches = countTextMatches(centresInteret, school.getDomaines());
            if (domainMatches > 0) {
                score += Math.min(50, domainMatches * DOMAIN_MATCH_WEIGHT);
                reasons.add("domaine(s) compatible(s) avec vos intérêts");
            }
        }

        // Match objectif post-bac vs diplômes
        if (objectifPostbac != null && !objectifPostbac.isBlank() && school.getDiplomesDelivres() != null) {
            int diplomeMatches = countTextMatches(Set.of(objectifPostbac), school.getDiplomesDelivres());
            if (diplomeMatches > 0) {
                score += OBJECTIVE_MATCH_WEIGHT;
                reasons.add("diplôme correspondant à votre objectif post-bac");
            }
        }

        // Match série d'orientation vs domaines
        if (serieOrientation != null && !serieOrientation.isBlank() && school.getDomaines() != null) {
            int serieMatches = countTextMatches(Set.of(serieOrientation), school.getDomaines());
            if (serieMatches > 0) {
                score += SECTOR_MATCH_WEIGHT;
                reasons.add("série d'orientation compatible");
            }
        }

        score = Math.min(SCORE_MAX, score);
        if (reasons.isEmpty()) {
            reasons.add("école disponible sur la plateforme");
        }

        return new MatchScore(score, reasons);
    }

    /**
     * Calcule un score pour un profil candidat selon le profil recruteur.
     */
    public MatchScore scoreProfile(User recruiter, User candidate) {
        if (recruiter == null || candidate == null) {
            return new MatchScore(SCORE_MIN, List.of());
        }

        Optional<Profile> candidateProfile = profileRepository.findByUserId(candidate.getId());
        int score = SCORE_MIN;
        List<String> reasons = new ArrayList<>();

        if (candidateProfile.isEmpty()) {
            reasons.add("profil candidat incomplet");
            return new MatchScore(score, reasons);
        }

        // Bonus rôle
        if (candidate.getRole() == UserRole.ETUDIANT) {
            score += ROLE_BONUS_WEIGHT;
            reasons.add("profil étudiant");
        } else if (candidate.getRole() == UserRole.LYCEEN && recruiter.getRole() == UserRole.ECOLE) {
            score += ROLE_BONUS_WEIGHT;
            reasons.add("profil lycéen adapté à la formation");
        }

        // Match mots-clés secteur/compétences
        Profile candidateProf = candidateProfile.get();
        Set<String> candidateKeywords = extractKeywords(candidateProf);
        Set<String> recruiterKeywords = extractRecruiterKeywords(recruiter);

        int keywordMatches = countKeywordMatches(recruiterKeywords, candidateKeywords);
        if (keywordMatches > 0) {
            score += Math.min(70, keywordMatches * KEYWORD_MATCH_WEIGHT);
            reasons.add(keywordMatches + " correspondance(s) sur secteur/compétences");
        }

        score = Math.min(SCORE_MAX, score);
        if (reasons.isEmpty()) {
            reasons.add("profil disponible sur la plateforme");
        }

        return new MatchScore(score, reasons);
    }

    /**
     * Extrait les mots-clés du profil candidat.
     */
    private Set<String> extractKeywords(Profile profile) {
        Set<String> keywords = new java.util.HashSet<>();
        if (profile.getDomaine() != null) keywords.add(profile.getDomaine().toLowerCase());
        if (profile.getNiveauEtude() != null) keywords.add(profile.getNiveauEtude().toLowerCase());
        if (profile.getCompetences() != null) {
            profile.getCompetences().forEach(c -> keywords.add(c.toLowerCase()));
        }
        if (profile.getPreferencesSecteur() != null) {
            profile.getPreferencesSecteur().forEach(s -> keywords.add(s.toLowerCase()));
        }
        if (profile.getCentresInteret() != null) {
            profile.getCentresInteret().forEach(i -> keywords.add(i.toLowerCase()));
        }
        return keywords;
    }

    /**
     * Extrait les mots-clés du profil recruteur.
     */
    private Set<String> extractRecruiterKeywords(User recruiter) {
        Set<String> keywords = new java.util.HashSet<>();
        // Simplification: utilise la description si disponible
        if (recruiter.getRole() == UserRole.ENTREPRISE) {
            // TODO: charger company et extraire raisonSociale, secteurActivite
        } else if (recruiter.getRole() == UserRole.ECOLE) {
            // TODO: charger school et extraire nomEtablissement, domaines
        }
        return keywords;
    }

    /**
     * Compte les correspondances de texte entre deux ensembles.
     */
    private int countTextMatches(Set<String> source, Set<String> target) {
        if (source == null || target == null) return 0;
        return (int) source.stream()
                .filter(s -> target.stream().anyMatch(t -> t.toLowerCase().contains(s.toLowerCase())))
                .count();
    }

    /**
     * Compte les correspondances de texte entre un ensemble et une liste.
     */
    private int countTextMatches(Set<String> source, List<String> target) {
        if (source == null || target == null) return 0;
        return (int) source.stream()
                .filter(s -> target.stream().anyMatch(t -> t != null && t.toLowerCase().contains(s.toLowerCase())))
                .count();
    }

    /**
     * Compte les correspondances dans une chaîne de texte.
     */
    private int countTextMatchesInString(Set<String> source, String targetText) {
        if (source == null || targetText == null) return 0;
        String lowerTarget = targetText.toLowerCase();
        return (int) source.stream()
                .filter(s -> lowerTarget.contains(s.toLowerCase()))
                .count();
    }

    /**
     * Compte les correspondances de mots-clés.
     */
    private int countKeywordMatches(Set<String> recruiterKeywords, Set<String> candidateKeywords) {
        if (recruiterKeywords == null || candidateKeywords == null) return 0;
        return (int) recruiterKeywords.stream()
                .filter(rk -> candidateKeywords.stream().anyMatch(ck -> ck.contains(rk) || rk.contains(ck)))
                .count();
    }

    /**
     * Résultat de scoring avec explicabilité.
     */
    public static class MatchScore {
        private final int score;
        private final List<String> reasons;

        public MatchScore(int score, List<String> reasons) {
            this.score = Math.min(SCORE_MAX, Math.max(SCORE_MIN, score));
            this.reasons = reasons != null ? reasons : List.of("aucune raison fournie");
        }

        public int getScore() {
            return score;
        }

        public List<String> getReasons() {
            return reasons;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("score", score);
            map.put("reasons", reasons);
            return map;
        }
    }
}
