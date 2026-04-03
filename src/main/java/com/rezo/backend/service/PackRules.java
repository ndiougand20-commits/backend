package com.rezo.backend.service;

import com.rezo.entities.Pack;
import com.rezo.entities.User;
import com.rezo.entities.enums.UserRole;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class PackRules {

    private static final Set<String> OFFER_FEATURES = Set.of(
            "OFFERS_PUBLISH", "OFFERS_MANAGE", "BUSINESS_OPPORTUNITIES", "RECRUTEMENT"
    );
    private static final Set<String> MESSAGING_FEATURES = Set.of(
            "MESSAGERIE_LIMITEE", "MESSAGERIE_ILLIMITEE", "MESSAGING", "MESSAGE_ACCESS"
    );
    private static final Set<String> AI_CHAT_FEATURES = Set.of(
            "AI_CHAT_ACCESS", "CHAT_IA", "IA_CHAT", "AI_ASSISTANT"
    );

    private PackRules() {
    }

    public static Set<String> parseCsv(String rawValue) {
        Set<String> values = new LinkedHashSet<>();
        if (rawValue == null || rawValue.isBlank()) {
            return values;
        }
        for (String value : rawValue.split(",")) {
            String normalized = normalize(value);
            if (normalized != null) {
                values.add(normalized);
            }
        }
        return values;
    }

    public static Set<String> parseValues(Collection<String> rawValues) {
        Set<String> values = new LinkedHashSet<>();
        if (rawValues == null) {
            return values;
        }
        for (String value : rawValues) {
            String normalized = normalize(value);
            if (normalized != null) {
                values.add(normalized);
            }
        }
        return values;
    }

    public static boolean isPackCompatible(Pack pack, UserRole role) {
        if (pack == null || role == null) {
            return false;
        }
        Set<String> targets = parseCsv(pack.getCible());
        if (targets.isEmpty() || targets.contains("TOUS") || targets.contains("ALL")) {
            return true;
        }
        if (targets.contains(role.name())) {
            return true;
        }
        return isBusinessRole(role) && (targets.contains("BUSINESS") || targets.contains("B2B") || targets.contains("PRO"))
                || isCandidateRole(role) && (targets.contains("CANDIDAT") || targets.contains("CANDIDATS") || targets.contains("STUDENT") || targets.contains("ACADEMIC"));
    }

    public static boolean canManageOffers(User user) {
        return isAdmin(user)
                || isBusinessRole(user != null ? user.getRole() : null) && hasAnyFeature(user, OFFER_FEATURES);
    }

    public static boolean canUseMessaging(User user) {
        return isAdmin(user) || hasAnyFeature(user, MESSAGING_FEATURES);
    }

    public static boolean canUseAiChat(User user) {
        return isAdmin(user) || hasAnyFeature(user, AI_CHAT_FEATURES);
    }

    public static boolean canViewOpportunities(User user) {
        return isAdmin(user)
                || hasAnyFeature(user, Set.of("MATCHING_BASIC", "MATCHING_PREMIUM"))
                || canManageOffers(user);
    }

    private static boolean hasAnyFeature(User user, Set<String> requiredFeatures) {
        if (user == null || user.getPack() == null) {
            return false;
        }
        Set<String> userFeatures = parseCsv(user.getPack().getFeatures());
        for (String required : requiredFeatures) {
            if (userFeatures.contains(required)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAdmin(User user) {
        return user != null && user.getRole() == UserRole.ADMIN;
    }

    private static boolean isBusinessRole(UserRole role) {
        return role == UserRole.ENTREPRISE || role == UserRole.ECOLE;
    }

    private static boolean isCandidateRole(UserRole role) {
        return role == UserRole.ETUDIANT || role == UserRole.LYCEEN || role == UserRole.EMPLOI;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            return null;
        }
        return trimmed.toUpperCase(Locale.ROOT).replace(' ', '_');
    }
}
