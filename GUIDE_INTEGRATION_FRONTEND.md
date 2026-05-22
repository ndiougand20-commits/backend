# REZO Backend - Guide d'Integration Frontend Complet

> **Base URL** : `http://localhost:8082`
> **Authentification** : JWT Bearer Token (header `Authorization: Bearer <token>`)
> **Format** : JSON (`Content-Type: application/json`) sauf upload fichiers (`multipart/form-data`)
> **Swagger UI** : `http://localhost:8082/swagger-ui.html`

---

## Table des matieres

1. [Configuration & Securite](#1-configuration--securite)
2. [Enumerations & Constantes](#2-enumerations--constantes)
3. [Authentification](#3-authentification---apiauth)
4. [Profil Utilisateur](#4-profil-utilisateur---apiusers)
5. [Entreprises](#5-entreprises---apicompanies)
6. [Ecoles](#6-ecoles---apischools)
7. [Packs](#7-packs---apipacks)
8. [Offres](#8-offres---apioffers)
9. [Messagerie](#9-messagerie---apimessages)
10. [Matching / Recommandations](#10-matching--recommandations---apimatch)
11. [Upload Media (Photos & PDF)](#11-upload-media---apiusersmemedia)
12. [Controle d'acces aux fonctionnalites](#12-controle-dacces---apifeatures)
13. [Health Check](#13-health-check)
14. [Gestion des erreurs](#14-gestion-des-erreurs)
15. [Exemples Flutter/Dart complets](#15-exemples-flutterdart-complets)

---

## 1. Configuration & Securite

### 1.1 JWT Token

- **Algorithme** : HS256
- **Duree** : 24 heures (86 400 000 ms)
- **Header requis** : `Authorization: Bearer <token>`
- **Claims du token** :

```json
{
  "sub": "uuid-utilisateur",
  "email": "user@rezo.com",
  "role": "ETUDIANT",
  "packId": "uuid-pack",
  "iat": 1713000000,
  "exp": 1713086400
}
```

### 1.2 CORS

- **Origines autorisees** : `http://localhost:*`, `http://127.0.0.1:*`
- **Methodes** : GET, POST, PUT, PATCH, DELETE, OPTIONS
- **Credentials** : true

### 1.3 Endpoints publics (sans token)

| Methode | Path |
|---------|------|
| POST | `/api/auth/signup` |
| POST | `/api/auth/login` |
| GET | `/api/companies`, `/api/companies/{id}` |
| GET | `/api/schools`, `/api/schools/{id}` |
| GET | `/api/offers`, `/api/offers/{id}` |
| GET | `/api/packs`, `/api/packs/{id}` |
| GET | `/`, `/ping` |
| GET | `/swagger-ui/**`, `/api-docs/**` |

### 1.4 Endpoints proteges (token requis)

Tous les autres endpoints necessitent un JWT valide dans le header `Authorization`.

---

## 2. Enumerations & Constantes

### 2.1 Roles utilisateur (`UserRole`)

| Valeur | Description |
|--------|-------------|
| `ETUDIANT` | Etudiant cherchant un stage/emploi |
| `LYCEEN` | Lyceen en orientation |
| `EMPLOI` | Chercheur d'emploi |
| `ENTREPRISE` | Entreprise qui publie des offres |
| `ECOLE` | Etablissement scolaire |
| `ADMIN` | Administrateur plateforme |

### 2.2 Types d'offre (`OfferType`)

| Valeur | Description |
|--------|-------------|
| `STAGE` | Offre de stage |
| `EMPLOI` | Offre d'emploi |
| `FORMATION` | Formation |

### 2.3 Taille d'entreprise (`CompanySize`)

| Valeur | Description |
|--------|-------------|
| `MICRO` | Micro-entreprise |
| `PME` | Petite et moyenne entreprise |
| `ETI` | Entreprise de taille intermediaire |
| `GE` | Grande entreprise |

### 2.4 Statut ecole (`SchoolStatus`)

| Valeur | Description |
|--------|-------------|
| `PUBLIC` | Etablissement public |
| `PRIVE` | Etablissement prive |

### 2.5 Actions swipe (`SwipeAction`)

| Valeur | Description |
|--------|-------------|
| `LIKE` | Offre aimee |
| `DISLIKE` | Offre rejetee |

### 2.6 Features de pack

| Feature | Description |
|---------|-------------|
| `MATCHING_BASIC` | Acces basique au matching |
| `MATCHING_PREMIUM` | Matching avance |
| `MESSAGERIE_LIMITEE` | Messagerie avec limite |
| `MESSAGERIE_ILLIMITEE` | Messagerie sans limite |
| `MESSAGING` | Acces messagerie (alias) |
| `MESSAGE_ACCESS` | Acces messagerie (alias) |
| `OFFERS_PUBLISH` | Publication d'offres |
| `OFFERS_MANAGE` | Gestion d'offres |
| `BUSINESS_OPPORTUNITIES` | Opportunites business |
| `RECRUTEMENT` | Fonctionnalites recrutement |
| `AI_CHAT_ACCESS` | Acces chat IA |
| `CHAT_IA` | Chat IA (alias) |
| `IA_CHAT` | Chat IA (alias) |
| `AI_ASSISTANT` | Assistant IA (alias) |

---

## 3. Authentification - `/api/auth`

### 3.1 Inscription

```
POST /api/auth/signup
Content-Type: application/json
```

**Pas de token requis.**

#### Requete - Etudiant ou Chercheur d'emploi

```json
{
  "email": "jean.dupont@mail.com",
  "password": "MonMotDePasse123!",
  "role": "ETUDIANT",
  "prenom": "Jean",
  "nom": "Dupont",
  "telephone": "0600000000",
  "profil": {
    "niveauEtude": "Bac+3",
    "domaine": "Informatique",
    "competences": ["Java", "Spring Boot", "SQL", "Flutter"],
    "objectif": "Stage de fin d'etudes",
    "preferencesSecteur": ["FinTech", "SaaS", "Sante"],
    "preferencesLieu": ["Paris", "Lyon", "Remote"],
    "experiences": ["Stage L2 chez TechCorp", "Projet universitaire IA"]
  }
}
```

#### Requete - Lyceen

```json
{
  "email": "lyceen@mail.com",
  "password": "MotDePasse123!",
  "role": "LYCEEN",
  "prenom": "Marie",
  "nom": "Martin",
  "telephone": "0611111111",
  "profil": {
    "classeActuelle": "Terminale",
    "serieOrientation": "Generale - Mathematiques/NSI",
    "objectifPostbac": "Ecole d'ingenieur informatique",
    "centresInteret": ["Programmation", "Robotique", "Intelligence Artificielle"]
  }
}
```

#### Requete - Entreprise

```json
{
  "email": "contact@techcorp.com",
  "password": "MotDePasse123!",
  "role": "ENTREPRISE",
  "prenom": "Pierre",
  "nom": "Durand",
  "telephone": "0622222222",
  "profil": {
    "raisonSociale": "TechCorp SA",
    "secteurActivite": "Numerique / IT",
    "taille": "PME",
    "description": "Entreprise specialisee dans le developpement web et mobile",
    "adresse": "42 Rue de la Tech, 75001 Paris",
    "siteWeb": "https://techcorp.com",
    "logoUrl": "https://techcorp.com/logo.png"
  }
}
```

#### Requete - Ecole

```json
{
  "email": "admin@universite.fr",
  "password": "MotDePasse123!",
  "role": "ECOLE",
  "prenom": "Sophie",
  "nom": "Leblanc",
  "telephone": "0633333333",
  "profil": {
    "nomEtablissement": "Universite Paris Tech",
    "statut": "PUBLIC",
    "domaines": ["Informatique", "Mathematiques", "Physique"],
    "diplomesDelivres": ["Licence Info", "Master Data Science", "Master IA"],
    "description": "Universite de recherche et d'innovation",
    "adresse": "1 Avenue de la Science, 75005 Paris",
    "siteWeb": "https://universite-paris-tech.fr",
    "logoUrl": "https://universite-paris-tech.fr/logo.png"
  }
}
```

#### Reponse `201 Created`

```json
{
  "message": "Inscription reussie",
  "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "email": "jean.dupont@mail.com",
  "role": "ETUDIANT",
  "profileType": "PROFILE"
}
```

| Champ | Type | Description |
|-------|------|-------------|
| `message` | String | Confirmation |
| `userId` | UUID | Identifiant unique du compte cree |
| `email` | String | Email normalise (lowercase, trimmed) |
| `role` | String | Role assigne |
| `profileType` | String | `PROFILE` (etudiant/lyceen/emploi), `COMPANY` (entreprise), `SCHOOL` (ecole) |

#### Erreurs

| Code | Condition | Corps |
|------|-----------|-------|
| `400` | Champs obligatoires manquants | `{"message": "Email, mot de passe et role sont requis"}` |
| `400` | Role invalide | `{"message": "Role invalide: XYZ"}` |
| `400` | Champ profil manquant | `{"message": "Le champ '<champ>' est requis pour le role <ROLE>"}` |
| `409` | Email deja utilise | `{"message": "Email deja utilise"}` |

---

### 3.2 Connexion

```
POST /api/auth/login
Content-Type: application/json
```

**Pas de token requis.**

#### Requete

```json
{
  "email": "jean.dupont@mail.com",
  "password": "MonMotDePasse123!"
}
```

#### Reponse `200 OK`

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhMWIyYzNkNC1lNWY2LTc4OTAtYWJjZC1lZjEyMzQ1Njc4OTAiLCJlbWFpbCI6ImplYW4uZHVwb250QG1haWwuY29tIiwicm9sZSI6IkVUVURJQU5UIiwicGFja0lkIjoiMTExLTIyMi0zMzMiLCJpYXQiOjE3MTMwMDAwMDAsImV4cCI6MTcxMzA4NjQwMH0.XXXXX"
}
```

| Champ | Type | Description |
|-------|------|-------------|
| `token` | String | Token JWT valide 24h. A stocker cote client. |

**Decoder le token** (payload JWT base64) pour obtenir : `sub` (userId), `email`, `role`, `packId`.

#### Erreurs

| Code | Condition | Corps |
|------|-----------|-------|
| `400` | Email ou password vide | `{"message": "Email et mot de passe requis"}` |
| `401` | Email inconnu | `{"message": "Email ou mot de passe incorrect"}` |
| `401` | Mauvais mot de passe | `{"message": "Email ou mot de passe incorrect"}` |

---

### 3.3 Supprimer un utilisateur par email (dev/test)

```
DELETE /api/auth/users/by-email/{email}
```

#### Reponse `200 OK`

```json
{
  "message": "Utilisateur supprime"
}
```

#### Erreurs

| Code | Condition | Corps |
|------|-----------|-------|
| `404` | Email introuvable | `{"message": "Utilisateur introuvable"}` |

---

### 3.4 Supprimer TOUS les utilisateurs (dev/test)

```
DELETE /api/auth/users
```

#### Reponse `200 OK`

```json
{
  "message": "Tous les utilisateurs ont ete supprimes",
  "count": 42
}
```

---

## 4. Profil Utilisateur - `/api/users`

> **Tous les endpoints de cette section necessitent `Authorization: Bearer <token>`.**

### 4.1 Mon profil

```
GET /api/users/me
Authorization: Bearer <token>
```

#### Reponse `200 OK` - Etudiant/Emploi

```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "email": "jean.dupont@mail.com",
  "prenom": "Jean",
  "nom": "Dupont",
  "telephone": "0600000000",
  "role": "ETUDIANT",
  "avatarUrl": "/uploads/users/a1b2c3d4/photos/photo-abc.jpg",
  "packId": "pack-uuid-111",
  "packNom": "FREE",
  "packCible": "TOUS",
  "packFeatures": ["MATCHING_BASIC", "MESSAGERIE_LIMITEE"],
  "canManageOffers": false,
  "canUseMessaging": true,
  "canUseAiChat": false,
  "createdAt": "2026-04-13T19:37:07",
  "profil": {
    "niveauEtude": "Bac+3",
    "domaine": "Informatique",
    "competences": ["Java", "Spring Boot", "SQL"],
    "objectif": "Stage de fin d'etudes",
    "preferencesSecteur": ["FinTech", "SaaS"],
    "preferencesLieu": ["Paris", "Lyon"],
    "experiences": ["Stage L2 chez TechCorp"]
  }
}
```

#### Reponse `200 OK` - Lyceen

```json
{
  "id": "...",
  "email": "lyceen@mail.com",
  "prenom": "Marie",
  "nom": "Martin",
  "role": "LYCEEN",
  "profil": {
    "classeActuelle": "Terminale",
    "serieOrientation": "Generale - NSI",
    "objectifPostbac": "Ecole d'ingenieur",
    "centresInteret": ["Programmation", "Robotique"]
  }
}
```

#### Reponse `200 OK` - Entreprise

```json
{
  "id": "...",
  "email": "contact@techcorp.com",
  "role": "ENTREPRISE",
  "profil": {
    "raisonSociale": "TechCorp SA",
    "secteurActivite": "Numerique / IT",
    "taille": "PME",
    "description": "Entreprise specialisee...",
    "adresse": "42 Rue de la Tech, Paris",
    "siteWeb": "https://techcorp.com",
    "logoUrl": "https://techcorp.com/logo.png"
  }
}
```

#### Reponse `200 OK` - Ecole

```json
{
  "id": "...",
  "email": "admin@universite.fr",
  "role": "ECOLE",
  "profil": {
    "nomEtablissement": "Universite Paris Tech",
    "statut": "PUBLIC",
    "domaines": ["Informatique", "Mathematiques"],
    "diplomesDelivres": ["Licence Info", "Master IA"],
    "description": "Universite de recherche",
    "adresse": "1 Avenue de la Science, Paris",
    "siteWeb": "https://universite-paris-tech.fr",
    "logoUrl": "https://universite-paris-tech.fr/logo.png"
  }
}
```

#### Champs de la reponse UserMeResponse

| Champ | Type | Description |
|-------|------|-------------|
| `id` | UUID | Identifiant unique |
| `email` | String | Email du compte |
| `prenom` | String | Prenom |
| `nom` | String | Nom de famille |
| `telephone` | String | Numero de telephone |
| `role` | String | Role (voir enum `UserRole`) |
| `avatarUrl` | String \| null | Chemin relatif de la photo de profil |
| `packId` | UUID | ID du pack souscrit |
| `packNom` | String | Nom du pack (ex: `FREE`, `PREMIUM`) |
| `packCible` | String | Roles cibles du pack |
| `packFeatures` | String[] | Liste des features du pack |
| `canManageOffers` | Boolean | Peut publier/gerer des offres |
| `canUseMessaging` | Boolean | Peut envoyer des messages |
| `canUseAiChat` | Boolean | Peut utiliser le chat IA |
| `createdAt` | DateTime | Date de creation |
| `profil` | Object | Profil specifique au role (voir ci-dessus) |

#### Erreurs

| Code | Condition | Corps |
|------|-----------|-------|
| `401` | Token absent/invalide/expire | `{"message": "Non authentifie"}` |
| `404` | Utilisateur introuvable | `{"message": "Utilisateur introuvable"}` |

---

### 4.2 Modifier mon profil

```
PUT /api/users/me
Authorization: Bearer <token>
Content-Type: application/json
```

**Envoyer uniquement les champs a modifier.** Les champs absents ou `null` ne sont pas modifies.

#### Requete - Modifier infos de base

```json
{
  "prenom": "Jean-Pierre",
  "telephone": "0699999999"
}
```

#### Requete - Modifier photo de profil (apres upload)

```json
{
  "avatarUrl": "/uploads/users/a1b2c3d4/photos/new-photo.jpg"
}
```

#### Requete - Modifier profil etudiant

```json
{
  "profil": {
    "niveauEtude": "Bac+5",
    "domaine": "Data Science",
    "competences": ["Python", "TensorFlow", "SQL", "Docker"],
    "objectif": "CDI en data engineering"
  }
}
```

#### Requete - Modifier profil entreprise

```json
{
  "profil": {
    "description": "Nouvelle description de l'entreprise",
    "adresse": "Nouveau 42 Rue Tech, Paris"
  }
}
```

#### Reponse `200 OK`

Identique a `GET /api/users/me` avec les nouvelles valeurs.

#### Erreurs

| Code | Condition | Corps |
|------|-----------|-------|
| `401` | Token invalide | `{"message": "Non authentifie"}` |
| `404` | Utilisateur introuvable | `{"message": "Utilisateur introuvable"}` |
| `409` | Nouvel email deja pris | `{"message": "Email deja utilise"}` |

---

### 4.3 Changer de pack

```
PUT /api/users/me/pack
Authorization: Bearer <token>
Content-Type: application/json
```

#### Requete

```json
{
  "packId": "uuid-du-nouveau-pack"
}
```

#### Reponse `200 OK`

Identique a `GET /api/users/me` avec le nouveau pack.

#### Erreurs

| Code | Condition | Corps |
|------|-----------|-------|
| `400` | packId manquant | `{"message": "packId est requis"}` |
| `400` | Pack incompatible avec le role | `{"message": "Ce pack n'est pas compatible avec votre role"}` |
| `404` | Pack introuvable | `{"message": "Pack introuvable"}` |

---

### 4.4 Supprimer mon compte

```
DELETE /api/users/me
Authorization: Bearer <token>
```

#### Reponse `200 OK`

```json
{
  "message": "Compte supprime avec succes"
}
```

---

## 5. Entreprises - `/api/companies`

### 5.1 Lister les entreprises

```
GET /api/companies
```

**Pas de token requis.**

#### Reponse `200 OK`

```json
[
  {
    "id": "company-uuid-1",
    "ownerUserId": "user-uuid-1",
    "raisonSociale": "TechCorp SA",
    "secteurActivite": "Numerique / IT",
    "taille": "PME",
    "description": "Entreprise specialisee dans le dev web",
    "adresse": "42 Rue de la Tech, Paris",
    "siteWeb": "https://techcorp.com",
    "logoUrl": "https://techcorp.com/logo.png",
    "createdAt": "2026-04-13T10:00:00"
  }
]
```

#### Champs CompanyResponse

| Champ | Type | Description |
|-------|------|-------------|
| `id` | UUID | ID de la fiche entreprise |
| `ownerUserId` | UUID | ID de l'utilisateur proprietaire |
| `raisonSociale` | String | Raison sociale |
| `secteurActivite` | String | Secteur d'activite |
| `taille` | String | Enum : `MICRO`, `PME`, `ETI`, `GE` |
| `description` | String | Description de l'entreprise |
| `adresse` | String \| null | Adresse postale |
| `siteWeb` | String \| null | URL du site web |
| `logoUrl` | String \| null | URL du logo |
| `createdAt` | DateTime | Date de creation |

---

### 5.2 Detail d'une entreprise

```
GET /api/companies/{id}
```

**Pas de token requis.**

#### Reponse `200 OK`

Un seul objet `CompanyResponse` (meme structure que ci-dessus).

| Code | Condition | Corps |
|------|-----------|-------|
| `404` | ID introuvable | `{"message": "Entreprise introuvable"}` |

---

### 5.3 Creer une entreprise

```
POST /api/companies
Authorization: Bearer <token>
Content-Type: application/json
```

**Requiert le role `ENTREPRISE`.**

#### Requete

```json
{
  "raisonSociale": "TechCorp SA",
  "secteurActivite": "Numerique / IT",
  "taille": "PME",
  "description": "Entreprise specialisee dans le developpement web et mobile",
  "adresse": "42 Rue de la Tech, 75001 Paris",
  "siteWeb": "https://techcorp.com",
  "logoUrl": "https://techcorp.com/logo.png"
}
```

#### Champs CompanyRequest

| Champ | Type | Requis | Description |
|-------|------|--------|-------------|
| `raisonSociale` | String | Oui | 2-150 caracteres |
| `secteurActivite` | String | Oui | 2-120 caracteres |
| `taille` | String | Oui | `MICRO`, `PME`, `ETI`, `GE` |
| `description` | String | Oui | Min 2 caracteres |
| `adresse` | String | Non | Adresse postale |
| `siteWeb` | String | Non | URL http/https valide |
| `logoUrl` | String | Non | URL http/https valide |

#### Reponse `201 Created`

Objet `CompanyResponse` complet.

#### Erreurs

| Code | Condition | Corps |
|------|-----------|-------|
| `400` | Champs requis manquants | `{"message": "Le champ 'raisonSociale' est requis"}` |
| `403` | Role non ENTREPRISE | `{"message": "Seuls les utilisateurs ENTREPRISE peuvent creer une fiche"}` |
| `409` | Fiche deja existante | `{"message": "Une fiche entreprise existe deja pour cet utilisateur"}` |

---

### 5.4 Modifier une entreprise

```
PUT /api/companies/{id}
Authorization: Bearer <token>
Content-Type: application/json
```

**Seul le proprietaire peut modifier.**

#### Requete (champs optionnels en update)

```json
{
  "description": "Nouvelle description",
  "adresse": "Nouvelle adresse"
}
```

#### Reponse `200 OK`

Objet `CompanyResponse` mis a jour.

#### Erreurs

| Code | Condition | Corps |
|------|-----------|-------|
| `403` | Pas proprietaire | `{"message": "Vous n'etes pas le proprietaire de cette fiche"}` |
| `404` | ID introuvable | `{"message": "Entreprise introuvable"}` |

---

### 5.5 Supprimer une entreprise

```
DELETE /api/companies/{id}
Authorization: Bearer <token>
```

**Seul le proprietaire peut supprimer.**

#### Reponse `200 OK`

```json
{
  "message": "Entreprise supprimee avec succes"
}
```

---

## 6. Ecoles - `/api/schools`

### 6.1 Lister les ecoles

```
GET /api/schools
```

**Pas de token requis.**

#### Reponse `200 OK`

```json
[
  {
    "id": "school-uuid-1",
    "ownerUserId": "user-uuid-2",
    "nomEtablissement": "Universite Paris Tech",
    "statut": "PUBLIC",
    "domaines": ["Informatique", "Mathematiques", "Physique"],
    "diplomesDelivres": ["Licence Info", "Master Data Science"],
    "description": "Universite de recherche et d'innovation",
    "adresse": "1 Avenue de la Science, Paris",
    "siteWeb": "https://universite-paris-tech.fr",
    "logoUrl": "https://universite-paris-tech.fr/logo.png",
    "createdAt": "2026-04-13T10:00:00"
  }
]
```

#### Champs SchoolResponse

| Champ | Type | Description |
|-------|------|-------------|
| `id` | UUID | ID de la fiche ecole |
| `ownerUserId` | UUID | ID de l'utilisateur proprietaire |
| `nomEtablissement` | String | Nom de l'etablissement |
| `statut` | String | `PUBLIC` ou `PRIVE` |
| `domaines` | String[] | Domaines d'enseignement |
| `diplomesDelivres` | String[] | Diplomes delivres |
| `description` | String \| null | Description |
| `adresse` | String \| null | Adresse |
| `siteWeb` | String \| null | URL du site |
| `logoUrl` | String \| null | URL du logo |
| `createdAt` | DateTime | Date de creation |

---

### 6.2 Detail d'une ecole

```
GET /api/schools/{id}
```

**Pas de token requis.**

#### Reponse `200 OK`

Un seul objet `SchoolResponse`.

| Code | Condition | Corps |
|------|-----------|-------|
| `404` | ID introuvable | `{"message": "Ecole introuvable"}` |

---

### 6.3 Creer une ecole

```
POST /api/schools
Authorization: Bearer <token>
Content-Type: application/json
```

**Requiert le role `ECOLE`.**

#### Requete

```json
{
  "nomEtablissement": "Universite Paris Tech",
  "statut": "PUBLIC",
  "domaines": ["Informatique", "Mathematiques"],
  "diplomesDelivres": ["Licence Info", "Master IA"],
  "description": "Universite de recherche",
  "adresse": "1 Avenue de la Science, Paris",
  "siteWeb": "https://universite-paris-tech.fr",
  "logoUrl": "https://universite-paris-tech.fr/logo.png"
}
```

#### Champs SchoolRequest

| Champ | Type | Requis | Description |
|-------|------|--------|-------------|
| `nomEtablissement` | String | Oui | 2-150 caracteres |
| `statut` | String | Oui | `PUBLIC` ou `PRIVE` |
| `domaines` | String[] | Oui | Min 1 element |
| `diplomesDelivres` | String[] | Non | Liste de diplomes |
| `description` | String | Non | Description |
| `adresse` | String | Non | Adresse |
| `siteWeb` | String | Non | URL valide |
| `logoUrl` | String | Non | URL valide |

#### Reponse `201 Created`

Objet `SchoolResponse` complet.

#### Erreurs

| Code | Condition | Corps |
|------|-----------|-------|
| `403` | Role non ECOLE | `{"message": "Seuls les utilisateurs ECOLE peuvent creer une fiche"}` |
| `409` | Fiche deja existante | `{"message": "Une fiche ecole existe deja pour cet utilisateur"}` |

---

### 6.4 Modifier une ecole

```
PUT /api/schools/{id}
Authorization: Bearer <token>
Content-Type: application/json
```

**Seul le proprietaire peut modifier.** Meme structure que POST (champs optionnels en update).

#### Reponse `200 OK`

Objet `SchoolResponse` mis a jour.

---

### 6.5 Supprimer une ecole

```
DELETE /api/schools/{id}
Authorization: Bearer <token>
```

#### Reponse `200 OK`

```json
{
  "message": "Ecole supprimee avec succes"
}
```

---

## 7. Packs - `/api/packs`

### 7.1 Lister les packs

```
GET /api/packs
```

**Pas de token requis.**

#### Reponse `200 OK`

```json
[
  {
    "id": "pack-uuid-free",
    "nom": "FREE",
    "description": "Pack gratuit avec fonctionnalites de base",
    "prix": 0.00,
    "cible": "TOUS",
    "features": ["MATCHING_BASIC", "MESSAGERIE_LIMITEE"],
    "createdAt": "2026-01-01T00:00:00"
  },
  {
    "id": "pack-uuid-premium",
    "nom": "PREMIUM",
    "description": "Pack premium pour etudiants",
    "prix": 9.99,
    "cible": "ETUDIANT,EMPLOI,LYCEEN",
    "features": ["MATCHING_PREMIUM", "MESSAGERIE_ILLIMITEE", "AI_CHAT_ACCESS"],
    "createdAt": "2026-01-01T00:00:00"
  },
  {
    "id": "pack-uuid-business",
    "nom": "BUSINESS",
    "description": "Pack entreprise pour publier et recruter",
    "prix": 29.99,
    "cible": "ENTREPRISE,ECOLE",
    "features": ["OFFERS_PUBLISH", "OFFERS_MANAGE", "MESSAGERIE_ILLIMITEE", "RECRUTEMENT"],
    "createdAt": "2026-01-01T00:00:00"
  }
]
```

#### Champs PackResponse

| Champ | Type | Description |
|-------|------|-------------|
| `id` | UUID | ID du pack |
| `nom` | String | Nom du pack (uppercase) |
| `description` | String \| null | Description |
| `prix` | Number | Prix (BigDecimal, >= 0) |
| `cible` | String | Roles cibles separes par virgule (ex: `TOUS`, `ETUDIANT,EMPLOI`) |
| `features` | String[] | Liste des features activees |
| `createdAt` | DateTime | Date de creation |

---

### 7.2 Detail d'un pack

```
GET /api/packs/{id}
```

**Pas de token requis.**

---

### 7.3 Creer un pack (admin)

```
POST /api/packs
Authorization: Bearer <token>
Content-Type: application/json
```

**Requiert le role `ADMIN`.**

#### Requete

```json
{
  "nom": "STARTER",
  "description": "Pack decouverte pour etudiants",
  "prix": 4.99,
  "cible": "ETUDIANT,LYCEEN",
  "features": ["MATCHING_BASIC", "MESSAGERIE_LIMITEE"]
}
```

> **Note** : Le champ `cible` accepte aussi les alias JSON `cibles` ou `rolesCompatibles`.

#### Champs PackRequest

| Champ | Type | Requis | Description |
|-------|------|--------|-------------|
| `nom` | String | Oui | 2-100 caracteres, sera normalise en UPPERCASE |
| `description` | String | Non | Description du pack |
| `prix` | Number | Oui | >= 0 |
| `cible` | String | Oui | Roles cibles (ex: `TOUS` ou `ETUDIANT,EMPLOI`) |
| `features` | String[] | Oui | Min 1 feature |

#### Reponse `201 Created`

Objet `PackResponse` complet.

#### Erreurs

| Code | Condition | Corps |
|------|-----------|-------|
| `403` | Role non ADMIN | `{"message": "Seuls les administrateurs peuvent gerer les packs"}` |
| `409` | Nom deja utilise | `{"message": "Un pack avec ce nom existe deja"}` |

---

### 7.4 Modifier un pack (admin)

```
PUT /api/packs/{id}
Authorization: Bearer <token>
Content-Type: application/json
```

**Requiert `ADMIN`.** Meme structure que POST.

---

### 7.5 Supprimer un pack (admin)

```
DELETE /api/packs/{id}
Authorization: Bearer <token>
```

**Requiert `ADMIN`.** Echoue si des utilisateurs sont abonnes a ce pack.

#### Reponse `200 OK`

```json
{
  "message": "Pack supprime avec succes"
}
```

#### Erreurs

| Code | Condition | Corps |
|------|-----------|-------|
| `409` | Pack utilise par des users | `{"message": "Impossible de supprimer ce pack, des utilisateurs y sont abonnes"}` |

---

## 8. Offres - `/api/offers`

### 8.1 Lister les offres

```
GET /api/offers
```

**Pas de token requis.**

#### Reponse `200 OK`

```json
[
  {
    "id": "offer-uuid-1",
    "titre": "Stage Developpeur Java Backend",
    "description": "Rejoindre notre equipe backend pour developper...",
    "type": "STAGE",
    "domaine": "Informatique",
    "location": "Paris",
    "competencesRequises": ["Java", "Spring Boot", "SQL", "Git"],
    "datePublication": "2026-04-13T00:00:00",
    "dateDebut": "2026-06-01T00:00:00",
    "dateFin": "2026-08-31T00:00:00",
    "ownerType": "ENTREPRISE",
    "ownerUserId": "user-uuid-1",
    "ownerCompanyId": "company-uuid-1",
    "ownerSchoolId": null,
    "ownerDisplayName": "TechCorp SA",
    "createdAt": "2026-04-13T19:37:07"
  }
]
```

#### Champs OfferResponse

| Champ | Type | Description |
|-------|------|-------------|
| `id` | UUID | ID de l'offre |
| `titre` | String | Titre de l'offre |
| `description` | String | Description detaillee |
| `type` | String | `STAGE`, `EMPLOI`, ou `FORMATION` |
| `domaine` | String | Domaine d'activite |
| `location` | String | Lieu |
| `competencesRequises` | String[] | Competences requises |
| `datePublication` | DateTime | Date de publication |
| `dateDebut` | DateTime | Date de debut |
| `dateFin` | DateTime | Date de fin |
| `ownerType` | String | `ENTREPRISE` ou `ECOLE` |
| `ownerUserId` | UUID | ID de l'utilisateur proprietaire |
| `ownerCompanyId` | UUID \| null | ID de la fiche entreprise (si ENTREPRISE) |
| `ownerSchoolId` | UUID \| null | ID de la fiche ecole (si ECOLE) |
| `ownerDisplayName` | String | Nom affiche du proprietaire |
| `createdAt` | DateTime | Date de creation |

---

### 8.2 Detail d'une offre

```
GET /api/offers/{id}
```

**Pas de token requis.**

| Code | Condition | Corps |
|------|-----------|-------|
| `404` | Offre introuvable | `{"message": "Offre introuvable"}` |

---

### 8.3 Publier une offre

```
POST /api/offers
Authorization: Bearer <token>
Content-Type: application/json
```

**Requiert le role `ENTREPRISE` ou `ECOLE` avec un pack autorisant `OFFERS_PUBLISH`.**

#### Requete

```json
{
  "titre": "Stage Developpeur Java Backend",
  "description": "Rejoindre notre equipe backend pour developper des APIs REST avec Spring Boot",
  "type": "STAGE",
  "domaine": "Informatique",
  "location": "Paris",
  "competencesRequises": ["Java", "Spring Boot", "SQL", "Git"],
  "datePublication": "2026-04-13T00:00:00",
  "dateDebut": "2026-06-01T00:00:00",
  "dateFin": "2026-08-31T00:00:00"
}
```

> **Note** : `location` accepte aussi l'alias JSON `localisation`. `competencesRequises` accepte aussi `competences`.

#### Champs OfferRequest

| Champ | Type | Requis | Description |
|-------|------|--------|-------------|
| `titre` | String | Oui | 2-150 caracteres |
| `description` | String | Oui | Min 2 caracteres |
| `type` | String | Oui | `STAGE`, `EMPLOI`, `FORMATION` |
| `domaine` | String | Oui | 2-120 caracteres |
| `location` | String | Oui | 2-150 caracteres (alias: `localisation`) |
| `competencesRequises` | String[] | Oui | Min 1 (alias: `competences`) |
| `datePublication` | DateTime | Non | Defaut = maintenant |
| `dateDebut` | DateTime | Oui | Doit etre >= datePublication |
| `dateFin` | DateTime | Oui | Doit etre > dateDebut |

#### Reponse `201 Created`

Objet `OfferResponse` complet.

#### Erreurs

| Code | Condition | Corps |
|------|-----------|-------|
| `400` | Champs requis manquants | `{"message": "Le titre est requis"}` |
| `400` | Dates incoherentes | `{"message": "dateDebut doit etre apres datePublication"}` |
| `403` | Role insuffisant | `{"message": "Seuls ENTREPRISE ou ECOLE peuvent publier des offres"}` |
| `403` | Pack insuffisant | `{"message": "Votre pack ne permet pas de publier des offres"}` |

---

### 8.4 Modifier une offre

```
PUT /api/offers/{id}
Authorization: Bearer <token>
Content-Type: application/json
```

**Seul le proprietaire peut modifier.** Meme structure que POST (champs optionnels).

---

### 8.5 Supprimer une offre

```
DELETE /api/offers/{id}
Authorization: Bearer <token>
```

**Seul le proprietaire peut supprimer.**

#### Reponse `200 OK`

```json
{
  "message": "Offre supprimee avec succes"
}
```

---

## 9. Messagerie - `/api/messages`

> **Tous les endpoints de cette section necessitent `Authorization: Bearer <token>` et un pack autorisant la messagerie.**

### 9.1 Envoyer un message

```
POST /api/messages
Authorization: Bearer <token>
Content-Type: application/json
```

#### Requete

```json
{
  "receiverId": "uuid-du-destinataire",
  "content": "Bonjour, votre offre de stage m'interesse beaucoup !",
  "relatedOfferId": "uuid-offre-optionnel"
}
```

#### Champs MessageRequest

| Champ | Type | Requis | Description |
|-------|------|--------|-------------|
| `receiverId` | UUID | Oui | ID du destinataire (userId ou profileId) |
| `content` | String | Oui | Contenu du message (max 2000 caracteres) |
| `relatedOfferId` | UUID | Non | ID d'une offre liee au message |

#### Reponse `201 Created`

```json
{
  "id": "message-uuid-1",
  "senderId": "uuid-expediteur",
  "senderDisplayName": "Jean Dupont",
  "receiverId": "uuid-destinataire",
  "receiverDisplayName": "TechCorp SA",
  "content": "Bonjour, votre offre de stage m'interesse beaucoup !",
  "isRead": false,
  "relatedOfferId": "uuid-offre",
  "createdAt": "2026-04-13T20:15:30"
}
```

#### Champs MessageResponse

| Champ | Type | Description |
|-------|------|-------------|
| `id` | UUID | ID du message |
| `senderId` | UUID | ID de l'expediteur |
| `senderDisplayName` | String | Nom affiche de l'expediteur (`prenom nom` ou email) |
| `receiverId` | UUID | ID du destinataire |
| `receiverDisplayName` | String | Nom affiche du destinataire |
| `content` | String | Contenu du message |
| `isRead` | Boolean | Lu par le destinataire (cle JSON: `isRead`) |
| `relatedOfferId` | UUID \| null | Offre liee |
| `createdAt` | DateTime | Date d'envoi |

#### Erreurs

| Code | Condition | Corps |
|------|-----------|-------|
| `400` | Contenu vide ou trop long | `{"message": "Le contenu du message est requis (max 2000 caracteres)"}` |
| `400` | Auto-envoi | `{"message": "Impossible d'envoyer un message a soi-meme"}` |
| `403` | Pack sans messagerie | `{"message": "Votre pack ne permet pas d'utiliser la messagerie"}` |
| `404` | Destinataire introuvable | `{"message": "Destinataire introuvable"}` |

---

### 9.2 Recuperer une conversation

```
GET /api/messages/conversation/{userId}
Authorization: Bearer <token>
```

Retourne **tous les messages echanges** (envoyes + recus) entre l'utilisateur connecte et `{userId}`.

> **Note** : `{userId}` peut etre un userId ou un profileId.

#### Reponse `200 OK`

```json
[
  {
    "id": "msg-1",
    "senderId": "moi-uuid",
    "senderDisplayName": "Jean Dupont",
    "receiverId": "autre-uuid",
    "receiverDisplayName": "TechCorp SA",
    "content": "Bonjour !",
    "isRead": true,
    "relatedOfferId": null,
    "createdAt": "2026-04-13T20:15:30"
  },
  {
    "id": "msg-2",
    "senderId": "autre-uuid",
    "senderDisplayName": "TechCorp SA",
    "receiverId": "moi-uuid",
    "receiverDisplayName": "Jean Dupont",
    "content": "Bonjour Jean, merci pour votre interet !",
    "isRead": false,
    "relatedOfferId": null,
    "createdAt": "2026-04-13T20:20:00"
  }
]
```

---

### 9.3 Lister mes messages (pagine)

```
GET /api/messages
GET /api/messages?read=false&page=0&size=20&sort=desc
Authorization: Bearer <token>
```

#### Parametres de requete

| Parametre | Type | Defaut | Description |
|-----------|------|--------|-------------|
| `read` | Boolean | null | Filtre : `true` = lus, `false` = non lus, null = tous |
| `page` | int | `0` | Numero de page (0-indexed) |
| `size` | int | `20` | Taille de page (1-100) |
| `sort` | String | `desc` | Tri par date : `asc` ou `desc` |

#### Reponse `200 OK`

```json
{
  "items": [
    {
      "id": "msg-uuid",
      "senderId": "...",
      "senderDisplayName": "...",
      "receiverId": "...",
      "receiverDisplayName": "...",
      "content": "...",
      "isRead": false,
      "relatedOfferId": null,
      "createdAt": "2026-04-13T20:15:30"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3,
  "sort": "desc",
  "readFilter": false
}
```

---

### 9.4 Marquer un message comme lu

```
PUT /api/messages/{id}/read
Authorization: Bearer <token>
```

**Seul le destinataire du message peut le marquer comme lu.**

#### Reponse `200 OK`

Objet `MessageResponse` avec `isRead: true`.

#### Erreurs

| Code | Condition | Corps |
|------|-----------|-------|
| `403` | Pas destinataire | `{"message": "Seul le destinataire peut marquer ce message comme lu"}` |
| `404` | Message introuvable | `{"message": "Message introuvable"}` |

---

### 9.5 Supprimer un message

```
DELETE /api/messages/{id}
Authorization: Bearer <token>
```

**Seul l'expediteur du message peut le supprimer.**

#### Reponse `200 OK`

```json
{
  "message": "Message supprime avec succes"
}
```

#### Erreurs

| Code | Condition | Corps |
|------|-----------|-------|
| `403` | Pas expediteur | `{"message": "Seul l'expediteur peut supprimer ce message"}` |
| `404` | Message introuvable | `{"message": "Message introuvable"}` |

---

## 10. Matching / Recommandations - `/api/match`

### 10.1 Obtenir des recommandations

```
GET /api/match/recommendations
Authorization: Bearer <token>
```

Retourne les **top 10 offres** les plus pertinentes pour le profil de l'utilisateur, avec un score de 0 a 100.

#### Reponse `200 OK`

```json
{
  "recommendations": [
    {
      "offerId": "offer-uuid-1",
      "score": 87,
      "reasons": [
        "domaine compatible",
        "2 competence(s) commune(s)",
        "objectif coherent",
        "localisation preferee"
      ],
      "offer": {
        "id": "offer-uuid-1",
        "titre": "Stage Java Backend",
        "description": "Developper des APIs REST...",
        "type": "STAGE",
        "domaine": "Informatique",
        "location": "Paris",
        "competencesRequises": ["Java", "Spring Boot", "SQL"],
        "datePublication": "2026-04-13T00:00:00",
        "dateDebut": "2026-06-01T00:00:00",
        "dateFin": "2026-08-31T00:00:00",
        "ownerType": "ENTREPRISE",
        "ownerUserId": "user-uuid",
        "ownerCompanyId": "company-uuid",
        "ownerSchoolId": null,
        "ownerDisplayName": "TechCorp SA",
        "createdAt": "2026-04-13T19:37:07"
      }
    },
    {
      "offerId": "offer-uuid-2",
      "score": 65,
      "reasons": [
        "domaine compatible",
        "1 competence(s) commune(s)"
      ],
      "offer": { }
    }
  ],
  "suggestedPack": {
    "id": "pack-uuid-premium",
    "label": "PREMIUM",
    "reason": "Acces illimite a la messagerie pour contacter plus facilement les recruteurs"
  },
  "trace": {
    "userId": "user-uuid",
    "role": "ETUDIANT",
    "currentPack": "FREE",
    "excludedSwipeCount": 3,
    "evaluatedCount": 45,
    "returnedCount": 10,
    "timestamp": "2026-04-13T20:00:00"
  }
}
```

#### Champs MatchRecommendationsResponse

| Champ | Type | Description |
|-------|------|-------------|
| `recommendations` | Array | Top 10 offres scorees |
| `recommendations[].offerId` | UUID | ID de l'offre |
| `recommendations[].score` | int | Score 0-100 |
| `recommendations[].reasons` | String[] | Raisons du score |
| `recommendations[].offer` | Object | Objet OfferResponse complet |
| `suggestedPack` | Object \| null | Suggestion de pack upgrade |
| `suggestedPack.id` | UUID | ID du pack suggere |
| `suggestedPack.label` | String | Nom du pack |
| `suggestedPack.reason` | String | Raison de la suggestion |
| `trace` | Object | Debug : statistiques du calcul |

#### Algorithme de scoring

| Critere | Points | Description |
|---------|--------|-------------|
| Base | +10 | Score de depart |
| Domaine | +30 | Domaine de l'offre == domaine du profil |
| Competences | +12 chacune (max +30) | Competences en commun |
| Objectif | +15 | Coherence objectif/type d'offre |
| Localisation | +15 | Lieu de l'offre dans les preferences |
| Format (etudiant) | +10 | ETUDIANT/LYCEEN + offre STAGE |
| Format (emploi) | +12 | EMPLOI + offre EMPLOI |
| Format (business) | +8 | ENTREPRISE/ECOLE + offre EMPLOI |
| **Maximum** | **100** | Score plafonne |

#### Logique de suggestion de pack

| Condition | Pack suggere |
|-----------|-------------|
| Role business sans `OFFERS_PUBLISH` | Pack avec OFFERS_PUBLISH |
| Sans `MESSAGERIE_ILLIMITEE` | Pack avec messagerie illimitee |
| Sans `AI_CHAT_ACCESS` | Pack avec chat IA |
| < 3 recommandations sans `MATCHING_PREMIUM` | Pack avec matching premium |

---

## 11. Upload Media - `/api/users/me/media`

> **Tous les endpoints necessitent `Authorization: Bearer <token>`.**

### 11.1 Lister mes medias

```
GET /api/users/me/media
GET /api/users/me/media?category=PHOTO
GET /api/users/me/media?category=JUSTIFICATIF_PDF
Authorization: Bearer <token>
```

#### Parametres

| Parametre | Type | Requis | Description |
|-----------|------|--------|-------------|
| `category` | String | Non | Filtrer par `PHOTO` ou `JUSTIFICATIF_PDF` |

#### Reponse `200 OK`

```json
[
  {
    "id": "media-uuid-1",
    "category": "PHOTO",
    "originalFileName": "ma-photo.png",
    "contentType": "image/png",
    "fileUrl": "/uploads/users/user-uuid/photos/generated-uuid.png",
    "createdAt": "2026-04-13T19:37:07"
  },
  {
    "id": "media-uuid-2",
    "category": "JUSTIFICATIF_PDF",
    "originalFileName": "diplome-master.pdf",
    "contentType": "application/pdf",
    "fileUrl": "/uploads/users/user-uuid/justificatifs/generated-uuid.pdf",
    "createdAt": "2026-04-13T19:40:00"
  }
]
```

#### Champs UserMediaFileResponse

| Champ | Type | Description |
|-------|------|-------------|
| `id` | UUID | ID du fichier media |
| `category` | String | `PHOTO` ou `JUSTIFICATIF_PDF` |
| `originalFileName` | String | Nom original du fichier uploade |
| `contentType` | String | Type MIME (`image/png`, `application/pdf`, etc.) |
| `fileUrl` | String | Chemin relatif du fichier stocke |
| `createdAt` | DateTime | Date d'upload |

> **Pour afficher le fichier** : construire l'URL absolue = `baseUrl + fileUrl`
> Exemple : `http://localhost:8082/uploads/users/abc/photos/xyz.png`

---

### 11.2 Uploader une photo de profil

```
POST /api/users/me/media/photos
Authorization: Bearer <token>
Content-Type: multipart/form-data
```

#### Requete (multipart)

| Champ | Type | Requis | Description |
|-------|------|--------|-------------|
| `file` | File | Oui | Fichier image |

**Formats acceptes** : `image/jpeg`, `image/png`, `image/webp`, `image/gif`
**Taille max** : **8 MB**

#### Exemple cURL

```bash
curl -X POST http://localhost:8082/api/users/me/media/photos \
  -H "Authorization: Bearer <token>" \
  -F "file=@/path/to/photo.png"
```

#### Reponse `201 Created`

```json
{
  "id": "media-uuid",
  "category": "PHOTO",
  "originalFileName": "photo.png",
  "contentType": "image/png",
  "fileUrl": "/uploads/users/user-uuid/photos/generated-uuid.png",
  "createdAt": "2026-04-13T19:37:07"
}
```

#### Erreurs

| Code | Condition | Corps |
|------|-----------|-------|
| `400` | Pas de fichier | `{"message": "Aucun fichier recu"}` |
| `400` | Format invalide | `{"message": "Format image non supporte (jpeg, png, webp, gif)"}` |
| `400` | Taille depassee | `{"message": "L'image depasse la taille maximale autorisee (8MB)"}` |

---

### 11.3 Uploader un justificatif PDF

```
POST /api/users/me/media/justificatifs
Authorization: Bearer <token>
Content-Type: multipart/form-data
```

#### Requete (multipart)

| Champ | Type | Requis | Description |
|-------|------|--------|-------------|
| `file` | File | Oui | Fichier PDF |

**Format accepte** : `application/pdf` uniquement
**Taille max** : **12 MB**

#### Exemple cURL

```bash
curl -X POST http://localhost:8082/api/users/me/media/justificatifs \
  -H "Authorization: Bearer <token>" \
  -F "file=@/path/to/diplome.pdf"
```

#### Reponse `201 Created`

```json
{
  "id": "media-uuid",
  "category": "JUSTIFICATIF_PDF",
  "originalFileName": "diplome.pdf",
  "contentType": "application/pdf",
  "fileUrl": "/uploads/users/user-uuid/justificatifs/generated-uuid.pdf",
  "createdAt": "2026-04-13T19:37:07"
}
```

#### Erreurs

| Code | Condition | Corps |
|------|-----------|-------|
| `400` | Pas de fichier | `{"message": "Aucun fichier recu"}` |
| `400` | Format invalide | `{"message": "Seuls les fichiers PDF sont autorises pour les justificatifs"}` |
| `400` | Taille depassee | `{"message": "Le PDF depasse la taille maximale autorisee (12MB)"}` |

---

### 11.4 Supprimer un media

```
DELETE /api/users/me/media/{id}
Authorization: Bearer <token>
```

#### Reponse `200 OK`

```json
{
  "message": "Media supprime avec succes"
}
```

#### Erreurs

| Code | Condition | Corps |
|------|-----------|-------|
| `404` | Media introuvable ou pas proprietaire | `{"message": "Media introuvable"}` |

---

### 11.5 Workflow complet : changer sa photo de profil

```
1. POST /api/users/me/media/photos          → upload image → recup fileUrl
2. PUT  /api/users/me  {"avatarUrl": fileUrl} → met a jour le profil
3. GET  /api/users/me                         → rafraichir les donnees
```

### 11.6 Workflow complet : ajouter un justificatif

```
1. POST /api/users/me/media/justificatifs     → upload PDF
2. GET  /api/users/me/media?category=JUSTIFICATIF_PDF → rafraichir la liste
```

---

## 12. Controle d'acces - `/api/features`

> **Tous les endpoints necessitent `Authorization: Bearer <token>`.**

### 12.1 Verifier acces messagerie

```
GET /api/features/messaging/access
Authorization: Bearer <token>
```

#### Reponse `200 OK` (autorise)

```json
{
  "allowed": true,
  "packNom": "PREMIUM",
  "message": "Messagerie autorisee pour ce pack"
}
```

#### Reponse `403 Forbidden` (non autorise)

```json
{
  "allowed": false,
  "packNom": "FREE",
  "message": "Votre pack actuel ne permet pas d'utiliser la messagerie"
}
```

---

### 12.2 Verifier acces chat IA

```
GET /api/features/chat-ai/access
Authorization: Bearer <token>
```

#### Reponse `200 OK` (autorise)

```json
{
  "allowed": true,
  "packNom": "PREMIUM",
  "message": "Chat IA autorise pour ce pack"
}
```

#### Reponse `403 Forbidden` (non autorise)

```json
{
  "allowed": false,
  "packNom": "FREE",
  "message": "Votre pack actuel ne permet pas d'utiliser le chat IA"
}
```

---

### 12.3 Resume des droits

```
GET /api/features/access-summary
Authorization: Bearer <token>
```

#### Reponse `200 OK`

```json
{
  "packNom": "FREE",
  "canViewOpportunities": true,
  "canManageOffers": false,
  "canUseMessaging": true,
  "canUseAiChat": false
}
```

| Champ | Type | Description |
|-------|------|-------------|
| `packNom` | String | Nom du pack actuel |
| `canViewOpportunities` | Boolean | Peut voir les offres/matching |
| `canManageOffers` | Boolean | Peut publier/gerer les offres |
| `canUseMessaging` | Boolean | Peut envoyer des messages |
| `canUseAiChat` | Boolean | Peut utiliser le chat IA |

> **Utilisation cote frontend** : appeler cet endpoint au demarrage ou apres connexion pour conditionner l'affichage des fonctionnalites.

---

## 13. Health Check

### 13.1 Page d'accueil

```
GET /
```

**Reponse** : `"REZO BACKEND OK 🚀"` (text/plain)

### 13.2 Ping

```
GET /ping
```

**Reponse** : `"pong"` (text/plain)

---

## 14. Gestion des erreurs

### 14.1 Codes HTTP recurrents

| Code | Signification | Quand |
|------|---------------|-------|
| `200` | OK | Operation reussie |
| `201` | Created | Ressource creee (signup, create, upload) |
| `400` | Bad Request | Payload invalide, champs manquants, validation echouee |
| `401` | Unauthorized | Token absent, invalide ou expire |
| `403` | Forbidden | Role insuffisant, pack incompatible, pas proprietaire |
| `404` | Not Found | Ressource introuvable |
| `409` | Conflict | Doublon (email, nom pack), dependances existantes |

### 14.2 Format standard des erreurs

```json
{
  "message": "Description lisible de l'erreur"
}
```

### 14.3 Messages d'erreur frequents

| Message | Code | Endpoint |
|---------|------|----------|
| `Email, mot de passe et role sont requis` | 400 | /api/auth/signup |
| `Email deja utilise` | 409 | /api/auth/signup, /api/users/me |
| `Email ou mot de passe incorrect` | 401 | /api/auth/login |
| `Non authentifie` | 401 | Tous les endpoints proteges |
| `Utilisateur introuvable` | 404 | /api/users/me |
| `Ce pack n'est pas compatible avec votre role` | 400 | /api/users/me/pack |
| `Pack introuvable` | 404 | /api/users/me/pack |
| `Seuls les utilisateurs ENTREPRISE peuvent creer une fiche` | 403 | /api/companies |
| `Seuls les utilisateurs ECOLE peuvent creer une fiche` | 403 | /api/schools |
| `Seuls les administrateurs peuvent gerer les packs` | 403 | /api/packs |
| `Votre pack ne permet pas de publier des offres` | 403 | /api/offers |
| `Votre pack ne permet pas d'utiliser la messagerie` | 403 | /api/messages |
| `Impossible d'envoyer un message a soi-meme` | 400 | /api/messages |
| `Le contenu du message est requis (max 2000 caracteres)` | 400 | /api/messages |
| `Aucun fichier recu` | 400 | /api/users/me/media/* |
| `Format image non supporte (jpeg, png, webp, gif)` | 400 | /api/users/me/media/photos |
| `L'image depasse la taille maximale autorisee (8MB)` | 400 | /api/users/me/media/photos |
| `Seuls les fichiers PDF sont autorises pour les justificatifs` | 400 | /api/users/me/media/justificatifs |
| `Le PDF depasse la taille maximale autorisee (12MB)` | 400 | /api/users/me/media/justificatifs |

---

## 15. Exemples Flutter/Dart complets

### 15.1 Service API de base (Dio)

```dart
import 'package:dio/dio.dart';

class ApiService {
  static const String baseUrl = 'http://localhost:8082';
  final Dio _dio;
  String? _token;

  ApiService() : _dio = Dio(BaseOptions(
    baseUrl: baseUrl,
    connectTimeout: const Duration(seconds: 10),
    receiveTimeout: const Duration(seconds: 10),
    headers: {'Content-Type': 'application/json'},
  ));

  void setToken(String token) {
    _token = token;
    _dio.options.headers['Authorization'] = 'Bearer $token';
  }

  void clearToken() {
    _token = null;
    _dio.options.headers.remove('Authorization');
  }

  bool get isAuthenticated => _token != null;

  // --- AUTH ---

  Future<Map<String, dynamic>> signup({
    required String email,
    required String password,
    required String role,
    String? prenom,
    String? nom,
    String? telephone,
    Map<String, dynamic>? profil,
  }) async {
    final res = await _dio.post('/api/auth/signup', data: {
      'email': email,
      'password': password,
      'role': role,
      if (prenom != null) 'prenom': prenom,
      if (nom != null) 'nom': nom,
      if (telephone != null) 'telephone': telephone,
      if (profil != null) 'profil': profil,
    });
    return Map<String, dynamic>.from(res.data);
  }

  Future<String> login({
    required String email,
    required String password,
  }) async {
    final res = await _dio.post('/api/auth/login', data: {
      'email': email,
      'password': password,
    });
    final token = res.data['token'] as String;
    setToken(token);
    return token;
  }

  // --- USER PROFILE ---

  Future<Map<String, dynamic>> getMe() async {
    final res = await _dio.get('/api/users/me');
    return Map<String, dynamic>.from(res.data);
  }

  Future<Map<String, dynamic>> updateMe(Map<String, dynamic> data) async {
    final res = await _dio.put('/api/users/me', data: data);
    return Map<String, dynamic>.from(res.data);
  }

  Future<Map<String, dynamic>> updateMyPack(String packId) async {
    final res = await _dio.put('/api/users/me/pack', data: {
      'packId': packId,
    });
    return Map<String, dynamic>.from(res.data);
  }

  Future<void> deleteMe() async {
    await _dio.delete('/api/users/me');
  }

  // --- COMPANIES ---

  Future<List<dynamic>> getCompanies() async {
    final res = await _dio.get('/api/companies');
    return List<dynamic>.from(res.data);
  }

  Future<Map<String, dynamic>> getCompany(String id) async {
    final res = await _dio.get('/api/companies/$id');
    return Map<String, dynamic>.from(res.data);
  }

  Future<Map<String, dynamic>> createCompany(Map<String, dynamic> data) async {
    final res = await _dio.post('/api/companies', data: data);
    return Map<String, dynamic>.from(res.data);
  }

  Future<Map<String, dynamic>> updateCompany(String id, Map<String, dynamic> data) async {
    final res = await _dio.put('/api/companies/$id', data: data);
    return Map<String, dynamic>.from(res.data);
  }

  Future<void> deleteCompany(String id) async {
    await _dio.delete('/api/companies/$id');
  }

  // --- SCHOOLS ---

  Future<List<dynamic>> getSchools() async {
    final res = await _dio.get('/api/schools');
    return List<dynamic>.from(res.data);
  }

  Future<Map<String, dynamic>> getSchool(String id) async {
    final res = await _dio.get('/api/schools/$id');
    return Map<String, dynamic>.from(res.data);
  }

  Future<Map<String, dynamic>> createSchool(Map<String, dynamic> data) async {
    final res = await _dio.post('/api/schools', data: data);
    return Map<String, dynamic>.from(res.data);
  }

  Future<Map<String, dynamic>> updateSchool(String id, Map<String, dynamic> data) async {
    final res = await _dio.put('/api/schools/$id', data: data);
    return Map<String, dynamic>.from(res.data);
  }

  Future<void> deleteSchool(String id) async {
    await _dio.delete('/api/schools/$id');
  }

  // --- PACKS ---

  Future<List<dynamic>> getPacks() async {
    final res = await _dio.get('/api/packs');
    return List<dynamic>.from(res.data);
  }

  Future<Map<String, dynamic>> getPack(String id) async {
    final res = await _dio.get('/api/packs/$id');
    return Map<String, dynamic>.from(res.data);
  }

  // --- OFFERS ---

  Future<List<dynamic>> getOffers() async {
    final res = await _dio.get('/api/offers');
    return List<dynamic>.from(res.data);
  }

  Future<Map<String, dynamic>> getOffer(String id) async {
    final res = await _dio.get('/api/offers/$id');
    return Map<String, dynamic>.from(res.data);
  }

  Future<Map<String, dynamic>> createOffer(Map<String, dynamic> data) async {
    final res = await _dio.post('/api/offers', data: data);
    return Map<String, dynamic>.from(res.data);
  }

  Future<Map<String, dynamic>> updateOffer(String id, Map<String, dynamic> data) async {
    final res = await _dio.put('/api/offers/$id', data: data);
    return Map<String, dynamic>.from(res.data);
  }

  Future<void> deleteOffer(String id) async {
    await _dio.delete('/api/offers/$id');
  }

  // --- MESSAGES ---

  Future<Map<String, dynamic>> sendMessage({
    required String receiverId,
    required String content,
    String? relatedOfferId,
  }) async {
    final res = await _dio.post('/api/messages', data: {
      'receiverId': receiverId,
      'content': content,
      if (relatedOfferId != null) 'relatedOfferId': relatedOfferId,
    });
    return Map<String, dynamic>.from(res.data);
  }

  Future<List<dynamic>> getConversation(String userId) async {
    final res = await _dio.get('/api/messages/conversation/$userId');
    return List<dynamic>.from(res.data);
  }

  Future<Map<String, dynamic>> getMessages({
    bool? read,
    int page = 0,
    int size = 20,
    String sort = 'desc',
  }) async {
    final res = await _dio.get('/api/messages', queryParameters: {
      if (read != null) 'read': read,
      'page': page,
      'size': size,
      'sort': sort,
    });
    return Map<String, dynamic>.from(res.data);
  }

  Future<Map<String, dynamic>> markMessageRead(String id) async {
    final res = await _dio.put('/api/messages/$id/read');
    return Map<String, dynamic>.from(res.data);
  }

  Future<void> deleteMessage(String id) async {
    await _dio.delete('/api/messages/$id');
  }

  // --- MATCHING ---

  Future<Map<String, dynamic>> getRecommendations() async {
    final res = await _dio.get('/api/match/recommendations');
    return Map<String, dynamic>.from(res.data);
  }

  // --- MEDIA ---

  Future<List<dynamic>> getMyMedia({String? category}) async {
    final res = await _dio.get('/api/users/me/media', queryParameters: {
      if (category != null) 'category': category,
    });
    return List<dynamic>.from(res.data);
  }

  Future<Map<String, dynamic>> uploadPhoto(String filePath) async {
    final formData = FormData.fromMap({
      'file': await MultipartFile.fromFile(filePath),
    });
    final res = await _dio.post('/api/users/me/media/photos', data: formData);
    return Map<String, dynamic>.from(res.data);
  }

  Future<Map<String, dynamic>> uploadJustificatif(String filePath) async {
    final formData = FormData.fromMap({
      'file': await MultipartFile.fromFile(filePath),
    });
    final res = await _dio.post('/api/users/me/media/justificatifs', data: formData);
    return Map<String, dynamic>.from(res.data);
  }

  Future<void> deleteMedia(String id) async {
    await _dio.delete('/api/users/me/media/$id');
  }

  // --- FEATURES ---

  Future<Map<String, dynamic>> getAccessSummary() async {
    final res = await _dio.get('/api/features/access-summary');
    return Map<String, dynamic>.from(res.data);
  }

  Future<Map<String, dynamic>> checkMessagingAccess() async {
    final res = await _dio.get('/api/features/messaging/access');
    return Map<String, dynamic>.from(res.data);
  }

  Future<Map<String, dynamic>> checkAiChatAccess() async {
    final res = await _dio.get('/api/features/chat-ai/access');
    return Map<String, dynamic>.from(res.data);
  }

  // --- HEALTH ---

  Future<String> ping() async {
    final res = await _dio.get('/ping');
    return res.data.toString();
  }
}
```

---

### 15.2 Gestion d'erreurs Dio

```dart
import 'package:dio/dio.dart';

String handleApiError(DioException e) {
  if (e.response != null) {
    final data = e.response!.data;
    final statusCode = e.response!.statusCode;
    String message = 'Erreur inconnue';

    if (data is Map && data.containsKey('message')) {
      message = data['message'];
    }

    switch (statusCode) {
      case 400:
        return 'Donnees invalides : $message';
      case 401:
        return 'Session expiree. Veuillez vous reconnecter.';
      case 403:
        return 'Acces refuse : $message';
      case 404:
        return 'Ressource introuvable : $message';
      case 409:
        return 'Conflit : $message';
      default:
        return 'Erreur serveur ($statusCode) : $message';
    }
  }

  if (e.type == DioExceptionType.connectionTimeout ||
      e.type == DioExceptionType.receiveTimeout) {
    return 'Le serveur ne repond pas. Verifiez votre connexion.';
  }

  return 'Erreur de connexion au serveur.';
}
```

---

### 15.3 Stocker et gerer le JWT

```dart
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:convert';

class TokenStorage {
  static const _key = 'jwt_token';

  static Future<void> save(String token) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_key, token);
  }

  static Future<String?> get() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString(_key);
  }

  static Future<void> clear() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_key);
  }

  /// Decode le payload du JWT sans verification de signature
  static Map<String, dynamic>? decodePayload(String token) {
    try {
      final parts = token.split('.');
      if (parts.length != 3) return null;
      final payload = parts[1];
      final normalized = base64Url.normalize(payload);
      final decoded = utf8.decode(base64Url.decode(normalized));
      return json.decode(decoded);
    } catch (_) {
      return null;
    }
  }

  /// Verifie si le token est expire
  static bool isExpired(String token) {
    final payload = decodePayload(token);
    if (payload == null || !payload.containsKey('exp')) return true;
    final exp = payload['exp'] as int;
    return DateTime.now().millisecondsSinceEpoch / 1000 > exp;
  }
}
```

---

### 15.4 Workflow complet d'inscription + connexion

```dart
final api = ApiService();

// 1. Inscription etudiant
try {
  final signupResult = await api.signup(
    email: 'jean@mail.com',
    password: 'SecurePass123!',
    role: 'ETUDIANT',
    prenom: 'Jean',
    nom: 'Dupont',
    telephone: '0600000000',
    profil: {
      'niveauEtude': 'Bac+3',
      'domaine': 'Informatique',
      'competences': ['Java', 'Flutter', 'SQL'],
      'objectif': 'Stage',
      'preferencesSecteur': ['FinTech'],
      'preferencesLieu': ['Paris', 'Lyon'],
    },
  );
  print('Inscrit: ${signupResult['userId']}');
} on DioException catch (e) {
  print(handleApiError(e));
}

// 2. Connexion
try {
  final token = await api.login(
    email: 'jean@mail.com',
    password: 'SecurePass123!',
  );
  await TokenStorage.save(token);
  print('Connecte, token stocke');
} on DioException catch (e) {
  print(handleApiError(e));
}

// 3. Charger le profil
final me = await api.getMe();
print('Bonjour ${me['prenom']} ${me['nom']} (${me['role']})');
print('Pack: ${me['packNom']} - Features: ${me['packFeatures']}');
print('Peut envoyer des messages: ${me['canUseMessaging']}');

// 4. Charger les droits d'acces
final access = await api.getAccessSummary();
print('Matching: ${access['canViewOpportunities']}');
print('Offres: ${access['canManageOffers']}');
print('Messagerie: ${access['canUseMessaging']}');
print('Chat IA: ${access['canUseAiChat']}');

// 5. Recuperer les recommandations
final match = await api.getRecommendations();
for (final reco in match['recommendations']) {
  print('${reco['offer']['titre']} - Score: ${reco['score']}/100');
  print('  Raisons: ${reco['reasons'].join(', ')}');
}

// 6. Changer la photo de profil
final photoResult = await api.uploadPhoto('/path/to/photo.jpg');
await api.updateMe({'avatarUrl': photoResult['fileUrl']});

// 7. Envoyer un message
await api.sendMessage(
  receiverId: 'uuid-du-recruteur',
  content: 'Bonjour, votre offre m\'interesse !',
  relatedOfferId: 'uuid-offre',
);
```

---

### 15.5 URL absolue des fichiers media

```dart
String absoluteMediaUrl(String relativeFileUrl) {
  return '${ApiService.baseUrl}$relativeFileUrl';
}

// Usage dans un widget Image
Image.network(
  absoluteMediaUrl(user['avatarUrl']),
  fit: BoxFit.cover,
  errorBuilder: (_, __, ___) => const Icon(Icons.person, size: 80),
)
```

---

## Resume des routes (tableau recapitulatif)

| Methode | Path | Auth | Role | Description |
|---------|------|:----:|------|-------------|
| POST | `/api/auth/signup` | - | - | Inscription |
| POST | `/api/auth/login` | - | - | Connexion → JWT |
| DELETE | `/api/auth/users/by-email/{email}` | - | - | Supprimer user (dev) |
| DELETE | `/api/auth/users` | - | - | Supprimer tous (dev) |
| GET | `/api/users/me` | JWT | * | Mon profil |
| PUT | `/api/users/me` | JWT | * | Modifier profil |
| PUT | `/api/users/me/pack` | JWT | * | Changer pack |
| DELETE | `/api/users/me` | JWT | * | Supprimer compte |
| GET | `/api/users/me/media` | JWT | * | Mes medias |
| POST | `/api/users/me/media/photos` | JWT | * | Upload photo |
| POST | `/api/users/me/media/justificatifs` | JWT | * | Upload PDF |
| DELETE | `/api/users/me/media/{id}` | JWT | * | Supprimer media |
| GET | `/api/companies` | - | - | Liste entreprises |
| GET | `/api/companies/{id}` | - | - | Detail entreprise |
| POST | `/api/companies` | JWT | ENTREPRISE | Creer entreprise |
| PUT | `/api/companies/{id}` | JWT | owner | Modifier entreprise |
| DELETE | `/api/companies/{id}` | JWT | owner | Supprimer entreprise |
| GET | `/api/schools` | - | - | Liste ecoles |
| GET | `/api/schools/{id}` | - | - | Detail ecole |
| POST | `/api/schools` | JWT | ECOLE | Creer ecole |
| PUT | `/api/schools/{id}` | JWT | owner | Modifier ecole |
| DELETE | `/api/schools/{id}` | JWT | owner | Supprimer ecole |
| GET | `/api/packs` | - | - | Liste packs |
| GET | `/api/packs/{id}` | - | - | Detail pack |
| POST | `/api/packs` | JWT | ADMIN | Creer pack |
| PUT | `/api/packs/{id}` | JWT | ADMIN | Modifier pack |
| DELETE | `/api/packs/{id}` | JWT | ADMIN | Supprimer pack |
| GET | `/api/offers` | - | - | Liste offres |
| GET | `/api/offers/{id}` | - | - | Detail offre |
| POST | `/api/offers` | JWT | ENT/ECO + pack | Publier offre |
| PUT | `/api/offers/{id}` | JWT | owner + pack | Modifier offre |
| DELETE | `/api/offers/{id}` | JWT | owner + pack | Supprimer offre |
| POST | `/api/messages` | JWT | pack msg | Envoyer message |
| GET | `/api/messages` | JWT | pack msg | Mes messages (pagine) |
| GET | `/api/messages/conversation/{userId}` | JWT | pack msg | Conversation |
| PUT | `/api/messages/{id}/read` | JWT | receiver | Marquer lu |
| DELETE | `/api/messages/{id}` | JWT | sender | Supprimer message |
| GET | `/api/match/recommendations` | JWT | * | Recommandations |
| GET | `/api/features/messaging/access` | JWT | * | Check messagerie |
| GET | `/api/features/chat-ai/access` | JWT | * | Check chat IA |
| GET | `/api/features/access-summary` | JWT | * | Resume droits |
| GET | `/` | - | - | Health check |
| GET | `/ping` | - | - | Ping → pong |

---

## 16. Plan d'integration frontend (pas a pas, de A a Z)

Cette section te donne un ordre de mise en oeuvre recommande pour integrer le backend sans dette technique.

### 16.1 Sprint 1 - Fondations reseau et session

Objectif: etre capable de se connecter, persister la session, et securiser les routes privees.

1. Configurer un client HTTP unique (Dio / Axios / Fetch wrapper).
2. Ajouter un interceptor request pour injecter `Authorization: Bearer <token>`.
3. Ajouter un interceptor response pour traiter les `401`:
   - vider le token
   - vider le cache utilisateur
   - rediriger vers login
4. Ajouter un `SessionGuard` sur les routes privees.
5. Ajouter un ecran Splash au boot:
   - lire token
   - verifier expiration locale (`exp`) si decode possible
   - router vers `Login` ou `Home`

Livrable attendu:
- login/logout robustes
- pas d'acces prive sans token
- redirection automatique sur session invalide

### 16.2 Sprint 2 - Profil utilisateur complet

Objectif: afficher et modifier `/api/users/me` avec formulaire dynamique selon role.

1. Consommer `GET /api/users/me`.
2. Mapper la reponse vers un modele frontend unique.
3. Construire l'UI conditionnelle par role (`ETUDIANT`, `LYCEEN`, `ENTREPRISE`, etc.).
4. Implementer `PUT /api/users/me` avec payload partiel.
5. Integrer `PUT /api/users/me/pack`.
6. Mettre a jour l'etat local apres chaque mutation.

Livrable attendu:
- page profil dynamique
- edition persistante
- changement de pack fonctionnel

### 16.3 Sprint 3 - Domaines metier principaux

Objectif: publier/consommer contenu coeur produit.

1. Ecrans listing/detail pour entreprises, ecoles, offres.
2. CRUD prive pour roles autorises:
   - entreprise: fiche entreprise + offres
   - ecole: fiche ecole + offres
3. Ecran matching `GET /api/match/recommendations`.
4. Ecran messagerie:
   - liste
   - conversation
   - envoi
   - marquage lu

Livrable attendu:
- parcours metier principal complet

### 16.4 Sprint 4 - Media, droits, durcissement UX

Objectif: finaliser les details production-ready.

1. Upload photos/PDF (`multipart/form-data`).
2. Integration de `/api/features/access-summary` pour masquer/afficher des actions.
3. Gestion offline/timeout/retry.
4. Instrumentation analytics/logging (erreurs, taux de 401, latence).
5. Tests E2E des parcours critiques.

Livrable attendu:
- UX stable
- gestion d'erreurs coherente
- feature flags pack fully integrated

---

## 17. Architecture frontend recommandee

### 17.1 Couches

1. `core/network`: client HTTP, interceptors, parse erreurs.
2. `core/session`: stockage token, session manager, logout global.
3. `features/<module>/data`: datasource API + mapping DTO.
4. `features/<module>/domain`: modeles metier + cas d'usage.
5. `features/<module>/presentation`: pages, controllers/viewmodels.

### 17.2 Convention de nommage conseillee

- DTO entrant API: `XxxResponseDto`
- DTO sortant API: `XxxRequestDto`
- modele UI interne: `XxxModel`
- mapper: `XxxMapper`

### 17.3 Regle importante

Ne pas propager directement le JSON brut jusqu'a l'UI.

Pourquoi:
- evite les regressions si le backend evolue
- rend le typage et les tests plus fiables
- facilite le cache local

---

## 18. Mapping ecrans frontend -> endpoints backend

| Ecran frontend | Endpoint(s) principal(aux) | Auth | Notes integration |
|----------------|-----------------------------|------|-------------------|
| Splash | - | - | Check token local + expiration locale |
| Login | `POST /api/auth/login` | Public | Sauver token puis preload profil |
| Signup | `POST /api/auth/signup` | Public | Form different selon role |
| Home | `GET /api/features/access-summary` | JWT | Conditionner les cards/CTA |
| Mon Profil | `GET /api/users/me` | JWT | Source de verite utilisateur |
| Edit Profil | `PUT /api/users/me` | JWT | Payload partiel uniquement |
| Packs | `GET /api/packs`, `PUT /api/users/me/pack` | Mixte | Afficher compatibilite role |
| Entreprises | `GET /api/companies`, `POST/PUT/DELETE /api/companies` | Mixte | CRUD protege owner |
| Ecoles | `GET /api/schools`, `POST/PUT/DELETE /api/schools` | Mixte | CRUD protege owner |
| Offres | `GET /api/offers`, `POST/PUT/DELETE /api/offers` | Mixte | Controle role + pack |
| Matching | `GET /api/match/recommendations` | JWT | Afficher score + raisons + suggestedPack |
| Messages liste | `GET /api/messages` | JWT | Pagine + filtre read |
| Conversation | `GET /api/messages/conversation/{userId}`, `POST /api/messages` | JWT | Polling ou refresh manuel |
| Media | `GET/POST/DELETE /api/users/me/media*` | JWT | `fileUrl` relatif -> URL absolue |

---

## 19. Contrat d'erreurs frontend unifie

### 19.1 Structure a normaliser cote frontend

```json
{
  "status": 403,
  "title": "FORBIDDEN",
  "userMessage": "Votre pack actuel ne permet pas cette action",
  "technicalMessage": "Votre pack actuel ne permet pas de publier ou gerer des offres",
  "traceId": null
}
```

### 19.2 Strategie UX recommandee

1. `400`: erreurs de formulaire, afficher sous les champs.
2. `401`: session expiree, redirection login + snackbar.
3. `403`: action interdite, afficher CTA upgrade pack si pertinent.
4. `404`: ressource absente, fallback UI (empty/error state).
5. `409`: conflit (email, nom pack), proposer correction.
6. `5xx`: message generique + bouton retry.

### 19.3 Regle de retry

- Retry automatique seulement pour:
  - timeout reseau
  - DNS/transient network
- Pas de retry automatique pour:
  - `400`, `401`, `403`, `404`, `409`

---

## 20. Snippets TypeScript (React / Next / Vue)

### 20.1 Client API minimal avec gestion 401

```ts
type ApiError = {
  status: number;
  message: string;
};

const API_BASE_URL = "http://localhost:8082";

async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem("jwt_token");

  const headers: Record<string, string> = {
    ...(init.headers as Record<string, string> | undefined),
  };

  if (!headers["Content-Type"] && !(init.body instanceof FormData)) {
    headers["Content-Type"] = "application/json";
  }

  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers,
  });

  if (response.status === 401) {
    localStorage.removeItem("jwt_token");
    window.location.href = "/login";
    throw { status: 401, message: "Session expiree" } satisfies ApiError;
  }

  if (!response.ok) {
    let message = "Erreur inconnue";
    try {
      const data = await response.json();
      message = data?.message ?? message;
    } catch {
      // ignore parse error
    }
    throw { status: response.status, message } satisfies ApiError;
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}
```

### 20.2 Exemple d'appel: recuperer mon profil

```ts
type UserMeResponse = {
  id: string;
  email: string;
  prenom: string;
  nom: string;
  role: "ETUDIANT" | "LYCEEN" | "EMPLOI" | "ENTREPRISE" | "ECOLE" | "ADMIN";
  packNom: string;
  canUseMessaging: boolean;
  canManageOffers: boolean;
  canUseAiChat: boolean;
  profil: Record<string, unknown>;
};

async function getMe(): Promise<UserMeResponse> {
  return apiFetch<UserMeResponse>("/api/users/me");
}
```

---

## 21. Flux utilisateur cibles (par role)

### 21.1 ETUDIANT / EMPLOI

1. Signup role candidat.
2. Login.
3. Completer profil (`niveauEtude`, `domaine`, `competences`).
4. Consommer matching.
5. Contacter via messagerie si autorise.
6. Uploader justificatifs.

### 21.2 LYCEEN

1. Signup lyceen.
2. Renseigner orientation et centres d'interet.
3. Consulter offres/stages/formations.
4. Echanger avec ecoles/entreprises.

### 21.3 ENTREPRISE

1. Signup entreprise.
2. Creer/editer fiche entreprise.
3. Souscrire pack compatible si besoin.
4. Publier offres.
5. Traiter messages entrants.

### 21.4 ECOLE

1. Signup ecole.
2. Creer/editer fiche ecole.
3. Publier offres/formations.
4. Repondre aux demandes via messagerie.

---

## 22. Checklists QA frontend (pret recette)

### 22.1 Session et securite

- [ ] Le token est persiste localement de facon securisee.
- [ ] Toutes les routes privees sont bloquees sans token.
- [ ] Toute reponse `401` provoque un logout propre.
- [ ] Le header `Authorization` est envoye sur tous les endpoints proteges.

### 22.2 Donnees et formulaires

- [ ] Les enums sont bornees a la liste backend.
- [ ] Les erreurs `400` sont affichees champ par champ si possible.
- [ ] Les payloads d'update sont partiels (pas de champs inutiles).
- [ ] Les dates d'offres sont validees cote UI avant envoi.

### 22.3 Metier

- [ ] Les actions soumises a pack sont correctement masquees/desactivees.
- [ ] Le proprietaire uniquement peut voir les actions edit/delete sur ses ressources.
- [ ] Les conversations et messages sont bien tries et pagines.
- [ ] Les medias uploades sont affiches via URL absolue construite.

### 22.4 Resilience UX

- [ ] Timeout reseau gere avec retry manuel.
- [ ] Etats loading / empty / error disponibles sur les listes.
- [ ] Les boutons submit sont desactives pendant l'envoi.
- [ ] Les toasts/snackbars sont coherents avec les codes HTTP.

---

## 23. Conseils de performance et robustesse

1. Mettre en cache court terme `GET /api/packs`, `GET /api/companies`, `GET /api/schools`.
2. Debouncer la recherche/filtering frontend sur les listes d'offres.
3. Eviter les refetch complets inutiles apres mutation: patch local de l'etat quand possible.
4. Utiliser une cle stable par ressource (`id`) pour listes virtuelles.
5. Limiter la taille des images affichees dans l'UI (thumbnails).
6. Ajouter des logs front pour diagnostiquer les erreurs 401/403 recurrentes.

---

## 24. Demarrage rapide (copier-coller process)

1. Brancher `login` + stockage token.
2. Brancher interceptor auth + gestion `401`.
3. Brancher `GET /api/users/me` au demarrage.
4. Brancher `GET /api/features/access-summary` pour afficher/masquer les modules.
5. Ajouter profil edit (`PUT /api/users/me`).
6. Ajouter modules offres/messages/matching.
7. Ajouter media upload.
8. Executer la checklist QA section 22 avant release.

Avec cet ordre, tu integres le backend complet en minimisant les regressions et les effets de bord.
