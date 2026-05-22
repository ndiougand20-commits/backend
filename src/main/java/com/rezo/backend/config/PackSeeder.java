package com.rezo.backend.config;

import com.rezo.entities.Pack;
import com.rezo.repositories.PackRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Insère les packs de base au démarrage si la table est vide.
 * Idempotent : ne fait rien si les packs existent déjà.
 */
@Component
public class PackSeeder {

    private static final Logger LOGGER = LoggerFactory.getLogger(PackSeeder.class);

    private final PackRepository packRepository;

    public PackSeeder(PackRepository packRepository) {
        this.packRepository = packRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        if (packRepository.count() > 0) {
            LOGGER.info("PackSeeder: packs deja presents, skip.");
            return;
        }

        List<Pack> packs = List.of(
            // ── Pack gratuit par défaut (assigné à l'inscription) ──────────────
            buildPack(
                "FREE",
                "Pack gratuit offert a tous les nouveaux utilisateurs. Acces de base au matching.",
                BigDecimal.ZERO,
                "TOUS",
                "MATCHING_BASIC"
            ),

            // ── Lycéens ───────────────────────────────────────────────────────
            buildPack(
                "Vision",
                "Orientation personnalisee post-bac : suggestion d'ecoles adaptees a ton profil et aide pour decider de ton avenir.",
                new BigDecimal("5000"),
                "LYCEEN",
                "MATCHING_BASIC,MATCHING_PREMIUM"
            ),

            // ── Étudiants & Chercheurs d'emploi ──────────────────────────────
            buildPack(
                "Essentiel",
                "Orientation personnalisee et visibilite aupres des entreprises. Acces aux opportunites ciblees.",
                new BigDecimal("5000"),
                "ETUDIANT",
                "MATCHING_BASIC"
            ),
            buildPack(
                "Connexion",
                "Orientation, visibilite amelioree et possibilite d'interagir avec certaines entreprises via la messagerie.",
                new BigDecimal("10000"),
                "ETUDIANT",
                "MATCHING_BASIC,MESSAGERIE_LIMITEE"
            ),
            buildPack(
                "REZO",
                "Pack premium : orientation avancee, visibilite maximale, contacts illimites avec les entreprises et acces au chat IA.",
                new BigDecimal("20000"),
                "ETUDIANT",
                "MATCHING_BASIC,MATCHING_PREMIUM,MESSAGERIE_ILLIMITEE,AI_CHAT_ACCESS"
            ),

            // ── Écoles privées ────────────────────────────────────────────────
            buildPack(
                "Visibilite",
                "Presence sur la plateforme, visibilite premium et publication d'offres pour attirer des etudiants qualifies.",
                new BigDecimal("25000"),
                "ECOLE",
                "MATCHING_BASIC,OFFERS_PUBLISH,OFFERS_MANAGE"
            ),
            buildPack(
                "Marketing",
                "Promotion des programmes, campagnes internes, acquisition d'etudiants et messagerie illimitee.",
                new BigDecimal("50000"),
                "ECOLE",
                "MATCHING_BASIC,MATCHING_PREMIUM,OFFERS_PUBLISH,OFFERS_MANAGE,MESSAGERIE_ILLIMITEE"
            ),

            // ── Entreprises ───────────────────────────────────────────────────
            buildPack(
                "Recrutement",
                "Publication d'offres, acces au matching de base et messagerie limitee pour entrer en contact avec les candidats.",
                new BigDecimal("35000"),
                "ENTREPRISE",
                "MATCHING_BASIC,OFFERS_PUBLISH,OFFERS_MANAGE,MESSAGERIE_LIMITEE"
            ),
            buildPack(
                "Recrutement Pro",
                "Matching avance, publication illimitee d'offres, messagerie illimitee et chat IA pour un recrutement optimal.",
                new BigDecimal("75000"),
                "ENTREPRISE",
                "MATCHING_BASIC,MATCHING_PREMIUM,OFFERS_PUBLISH,OFFERS_MANAGE,MESSAGERIE_ILLIMITEE,AI_CHAT_ACCESS"
            )
        );

        packRepository.saveAll(packs);
        LOGGER.info("PackSeeder: {} packs inseres avec succes.", packs.size());
    }

    private Pack buildPack(String nom, String description, BigDecimal prix, String cible, String features) {
        Pack pack = new Pack();
        pack.setNom(nom);
        pack.setDescription(description);
        pack.setPrix(prix);
        pack.setCible(cible);
        pack.setFeatures(features);
        return pack;
    }
}
