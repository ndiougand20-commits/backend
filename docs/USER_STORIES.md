# Projet REZO – User Stories détaillées & Design du Profil Utilisateur

---

## Table des matières

1. [User Stories détaillées (Agile)](#user-stories-detaillées-agile)
   - Étudiant
   - Lycéen
   - Chercheur d’emploi
   - Entreprise
   - École / Université
   - Commun / Transverse
   - Admin
2. [Infos Utilisateur – Design strict](#infos-utilisateur--design-strict)
3. [Légende](#légende)

---

## USER STORIES DÉTAILLÉES (AGILE)

---

### 🎓 Étudiant

#### 1. Inscription et authentification
- **Story** :  
  En tant qu’étudiant, je veux pouvoir m’inscrire, choisir mon type de profil et m’authentifier, afin d’accéder aux fonctionnalités de REZO.
- **Critères d’acceptation :**
  - L’application me propose le choix du type d’utilisateur « Étudiant »
  - Je peux saisir : nom, prénom, email, mot de passe, niveau d’étude
  - Validation de la création, connexion/déconnexion sans bug
  - Accès dashboard étudiant après login

#### 2. Gestion du profil étudiant
- **Story** :  
  En tant qu’étudiant, je veux renseigner et éditer compétences, objectifs, niveaux afin d’obtenir un matching pertinent.
- **Critères d’acceptation :**
  - Profil modifiable : niveau, filière, compétences clés, objectifs, préférences (géographie, secteur...)
  - Données répercutées sur les suggestions en matching

#### 3. Découverte d’opportunités & Matching
- **Story** :  
  En tant qu’étudiant, je veux voir une liste personnalisée de stages/emplois/formations pertinents.
- **Critères d’acceptation :**
  - Page "Découvrir" avec suggestions personnalisées
  - Tri selon mon profil et mes filtres, exclut les refusés

#### 4. Parcours swipe & matching intelligent
- **Story** :  
  En tant qu’étudiant, je veux swiper (like/dislike) des opportunités, afin de valider/refuser, avec historique.
- **Critères d’acceptation :**
  - Swipe supprime la carte, back notifié
  - Historique complet accessible
  - Packs proposés si besoin

#### 5. Messagerie
- **Story** :  
  En tant qu’étudiant, je veux contacter une entreprise ou université via messagerie interne.
- **Critères d’acceptation :**
  - Bouton contact disponible sur offres/candidatures
  - Liste des conversations, notification nouveaux messages

#### 6. Support IA et packs
- **Story** :  
  En tant qu’étudiant, je souhaite demander à un chat IA : “Quel pack me convient ?” et recevoir de l’aide personnalisée.
- **Critères d’acceptation :**
  - Chat IA comprend et répond
  - Recommandation pack automatique selon mon profil/historique
  - FAQ proposée si hors contexte

---

### 🏫 Lycéen

#### 7. Orientation scolaire par IA
- **Story** :  
  En tant que lycéen, je veux un chat IA qui me recommande des cursus et écoles adaptées.
- **Critères d’acceptation :**
  - Chat IA pose questions/m’aiguille
  - Liste d’écoles/formations selon réponses/profil

#### 8. Messagerie lycée <-> école
- **Story** :  
  En tant que lycéen, je souhaite pouvoir échanger par message avec un conseiller établissement.
- **Critères d’acceptation :**
  - Module messagerie entre lycéen/école
  - Notifications sur nouvelle réponse

---

### 👔 Chercheur d’emploi

#### 9. Recherche d’emploi via matching
- **Story** :  
  En tant que chercheur d’emploi, je veux voir des offres ciblées, exclure secteurs non souhaités.
- **Critères d’acceptation :**
  - Matching sophistiqué (profil, exclu, filtres)
  - Mise à jour suggestions si préférence modifiée

#### 10. Support IA pour packs
- **Story** :  
  En tant que chercheur d’emploi, je veux que l’IA me propose le meilleur pack selon mes besoins.
- **Critères d’acceptation :**
  - Question/IA/réponse orientée packs, proposition d’upgrade

#### 11. Messagerie candidat <-> employeur
- **Story** :  
  En tant que chercheur d’emploi, je veux pouvoir converser avec une entreprise après match.
- **Critères d’acceptation :**
  - Accès messagerie post-match, archives, notif messages

---

### 🏢 Entreprise

#### 12. Inscription et gestion profil
- **Story** :  
  En tant qu’entreprise, je veux créer un profil complet et visible.
- **Critères d’acceptation :**
  - Informations : nom, secteur, taille, logo, site
  - Edition profil, voir stats vues/candidats

#### 13. Publication et gestion d’offres
- **Story** :  
  En tant qu’entreprise, je veux publier (CRUD) des offres de stages/emplois
- **Critères d’acceptation :**
  - Lien offres <-> profil entreprise
  - Offres listées, éditables, supprimables

#### 14. Matching & présélection
- **Story** :  
  En tant qu’entreprise, je veux utiliser le swipe pour présélectionner des candidats pertinents.
- **Critères d’acceptation :**
  - Workflow swipe, logs likes/dislikes/candidats shortlist
  - IA propose packs adaptés si limite recrutement atteinte

#### 15. Messagerie Entreprise—Candidat
- **Story** :  
  En tant qu’entreprise, je veux contacter un étudiant/candidat qui a matché avec une de mes offres.
- **Critères d’acceptation :**
  - Messagerie dispo post-match
  - Historique par offre/profil, notif messages reçus

---

### 🎓 École / Université

#### 16. Inscription, publication programmes
- **Story** :  
  En tant qu’école, je veux créer un profil, publier programmes/événements.
- **Critères d’acceptation :**
  - CRUD des formations, description/diplômes, logo

#### 17. Matching formations—candidatures
- **Story** :  
  En tant qu’école, je veux matcher avec étudiants ciblés et engager le dialogue.
- **Critères d’acceptation :**
  - Suggestions intelligentes de profils
  - Messagerie post suggestion

#### 18. Support IA pour marketing/packs
- **Story** :  
  En tant qu’école, je veux demander à l’IA comment améliorer ma visibilité ou choisir un pack.
- **Critères d’acceptation :**
  - Chat IA accessible via tableau de bord
  - Suggestion pack école adaptée

---

### 🌐 Tous profils / Fonctions transverses

#### 19. FAQ & fallback IA
- **Story** :  
  En tant qu’utilisateur, je veux qu’en cas de question incomprise, le chat me propose automatiquement des FAQ pertinentes.
- **Critères d’acceptation :**
  - Fallback FAQ IA automatique, menu aide dédié

#### 20. Notifications intelligentes
- **Story** :  
  En tant qu’utilisateur, je veux être notifié des nouveaux messages, offres, matches ou packs disponibles.
- **Critères d’acceptation :**
  - Notification Android/iOS/Flutter à chaque évènement clé

---

### 👨‍💼 Admin

#### 21. Supervision globale
- **Story** :  
  En tant qu’admin, je veux pouvoir superviser les utilisateurs, offres, packs, messages et conversations IA/bot.
- **Critères d’acceptation :**
  - Espace backoffice sécurisé, logs consultables
  - Gestion manuelle ban/activation/upgrade user

---

## INFOS UTILISATEUR – DESIGN STRICT

---

### Informations de base (obligatoires pour tous)
| Champ                | Type      | Obligatoire | Détail/Remarque                 |
|----------------------|-----------|-------------|----------------------------------|
| ID utilisateur       | UUID      | Oui         | Auto généré                     |
| Email                | Email     | Oui         | Unique, login                   |
| Mot de passe (hash)  | Texte     | Oui         | Jamais stocké en clair          |
| Nom                  | Texte     | Oui         |                                 |
| Prénom               | Texte     | Oui         |                                 |
| Numéro de téléphone  | Texte     | Non         | Optionnel, SMS éventuel         |
| Rôle utilisateur     | Enum      | Oui         | étudiant/lycéen/emploi/ent./école  |
| Pack actuel          | Réf/ID    | Oui         |                                  |
| Avatar/photo         | Fichier   | Non (MVP)   |                                  |
| Date inscription     | Date      | Oui         | Auto générée                    |

---

#### Étudiant / Chercheur d’Emploi

| Champ                  | Type        | Obligatoire | Remarque                          |
|------------------------|-------------|-------------|-----------------------------------|
| Niveau d’études        | Enum        | Oui         | (Bac, Bac+1, L, M, ...)          |
| Filière/Domaine        | Texte       | Oui         |                                  |
| Compétences clés       | Liste       | Oui         | Min 3 sugg. (tag/autocomplete)   |
| Objectif/recherche     | Texte       | Oui         | Stage, CDD, CDI, alternance, ... |
| Préférences (secteur)  | Liste       | Non         | Industrie, IT, région, pays...   |
| Expérience(s)          | Liste       | Non         | Poste, entreprise, durée …       |
| CV (PDF/doc)           | Fichier     | Non         | Bonus V2                         |

---

#### Lycéen

| Champ                  | Type   | Obligatoire | Remarque                 |
|------------------------|--------|-------------|--------------------------|
| Classe actuelle        | Enum   | Oui         | 1ère, Terminale, etc.    |
| Série/Orientation      | Texte  | Oui         | S, L, ES, Pro, ...       |
| Objectif post-bac      | Texte  | Oui         | Cursus visé              |
| Centres d'intérêt      | Liste  | Non         |                          |

---

#### Entreprise

| Champ                  | Type     | Obligatoire | Remarque                      |
|------------------------|----------|-------------|-------------------------------|
| Raison sociale         | Texte    | Oui         |                              |
| Secteur d’activité     | Texte    | Oui         |                              |
| Taille/salariés        | Enum     | Oui         | Micro, PME, GE …              |
| Description rapide     | Texte    | Oui         |                              |
| Adresse siège          | Texte    | Non         |                              |
| Site Web               | URL      | Non         |                              |
| Logo                   | Fichier  | Non (MVP)   |                              |
| Offres publiées        | Liste    | Auto        | Lié au CRUD offres           |

---

#### École / Université

| Champ                     | Type    | Obligatoire | Remarque                  |
|---------------------------|---------|-------------|---------------------------|
| Nom établissement         | Texte   | Oui         |                           |
| Statut (public/privé)     | Enum    | Oui         |                           |
| Diplômes délivrés         | Liste   | Oui         | (Licence, Master, …)      |
| Domaines principaux       | Liste   | Oui         |                           |
| Description courte        | Texte   | Oui         |                           |
| Adresse siège             | Texte   | Non         |                           |
| Site Web                  | URL     | Non         |                           |
| Logo                      | Fichier | Non (MVP)   |                           |
| Programmes ouverts        | Liste   | Auto        | Via CRUD offres           |

---

## Sécurité & Technique

- Email unique, validé à l’inscription (MVP : pas de mail de confirmation obligatoire)
- Mot de passe hashé uniquement, jamais stocké en clair
- Contrôle de tous Enum/listes par code (pas d’injection côté front)
- Les profis sont adaptés à chaque rôle côté UI/UX et base de données
- Avatar/logo/fichier : upload optionnel mais prévu en structure

---

## Légende

- **MVP** : Obligatoire pour la première version livrable.
- **Critères d’acceptation** : Ce qui doit fonctionner pour valider la user story.
- **Bonus** : Fonctionnalité pour V2 ou extension.

---
