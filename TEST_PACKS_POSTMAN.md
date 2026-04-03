# Test CRUD Packs & Droits par Pack (scrum-20) - Postman Guide

## Prérequis
- Application démarrée : `./mvnw.cmd spring-boot:run`
- Base PostgreSQL disponible
- Un compte utilisateur classique avec JWT valide
- Un **compte admin** avec JWT valide pour le CRUD packs

## 0. Créer un admin si tu n'en as pas

### Étape A — créer un compte classique
**Method:** `POST`  
**URL:** `http://localhost:8080/api/auth/signup`

**Headers:**
```http
Content-Type: application/json
```

**Body:**
```json
{
  "email": "admin@rezo.com",
  "password": "SecurePass123!",
  "role": "ETUDIANT",
  "prenom": "Super",
  "nom": "Admin",
  "profil": {
    "niveauEtude": "M1",
    "domaine": "Informatique",
    "competences": ["Gestion", "Pilotage"]
  }
}
```

**Expected:** `201 Created`

### Étape B — le promouvoir en `ADMIN` en base
Dans PostgreSQL :

```sql
UPDATE users
SET role = 'ADMIN'
WHERE email = 'admin@rezo.com';
```

### Étape C — se connecter pour récupérer le JWT admin
**Method:** `POST`  
**URL:** `http://localhost:8080/api/auth/login`

**Body:**
```json
{
  "email": "admin@rezo.com",
  "password": "SecurePass123!"
}
```

**Expected:** `200 OK`

Stocke le token retourné dans la variable Postman `admin_token`.

> Si le compte existe déjà, tu peux juste rejouer l'`UPDATE` SQL puis refaire le login.

---

## 1. Lister les packs (public)

### GET `/api/packs`
**URL:** `http://localhost:8080/api/packs`

**Headers:** aucun

**Expected:** `200 OK`

**Vérifie :**
- `nom`
- `prix`
- `cible`
- `features`

---

## 2. Créer un pack business (admin only)

### POST `/api/packs`
**Headers:**
```http
Content-Type: application/json
Authorization: Bearer {{admin_token}}
```

**Body:**
```json
{
  "nom": "BUSINESS_PLUS",
  "description": "Pack premium pour entreprises et écoles",
  "prix": 29.99,
  "cible": "ENTREPRISE,ECOLE",
  "features": [
    "OFFERS_PUBLISH",
    "MESSAGERIE_ILLIMITEE",
    "AI_CHAT_ACCESS"
  ]
}
```

**Expected:** `201 Created`

Stocke l'id retourné dans `business_pack_id`.

---

## 3. Créer un pack candidat (admin only)

### POST `/api/packs`
**Headers:**
```http
Content-Type: application/json
Authorization: Bearer {{admin_token}}
```

**Body:**
```json
{
  "nom": "PREMIUM_CANDIDAT",
  "description": "Pack premium pour étudiants et chercheurs d'emploi",
  "prix": 9.99,
  "cible": "ETUDIANT,LYCEEN,EMPLOI",
  "features": [
    "MATCHING_PREMIUM",
    "MESSAGERIE_ILLIMITEE",
    "AI_CHAT_ACCESS"
  ]
}
```

**Expected:** `201 Created`

Stocke l'id retourné dans `candidate_pack_id`.

---

## 3.b Créer un pack jetable pour tester le DELETE

### POST `/api/packs`
**Headers:**
```http
Content-Type: application/json
Authorization: Bearer {{admin_token}}
```

**Body:**
```json
{
  "nom": "DELETE_TEST_PACK",
  "description": "Pack temporaire pour tester la suppression",
  "prix": 1.99,
  "cible": "TOUS",
  "features": ["MATCHING_BASIC"]
}
```

**Expected:** `201 Created`

Stocke l'id retourné dans `delete_pack_id`.

> ⚠️ N'utilise pas `candidate_pack_id` ou `business_pack_id` pour le delete si ce pack est déjà attribué à un utilisateur, sinon l'API retournera `409 Conflict`.

---

## 4. Tester la sécurité admin (doit échouer en 403)

### POST `/api/packs` avec un user non admin
**Headers:**
```http
Content-Type: application/json
Authorization: Bearer {{user_token}}
```

**Expected:** `403 Forbidden`

**Body attendu :**
```json
{
  "message": "Seul un administrateur peut gerer les packs"
}
```

---

## 5. Modifier un pack (admin only)

### PUT `/api/packs/{id}`
**URL:** `http://localhost:8080/api/packs/{{business_pack_id}}`

**Headers:**
```http
Content-Type: application/json
Authorization: Bearer {{admin_token}}
```

**Body:**
```json
{
  "description": "Pack business mis à jour",
  "prix": 39.99,
  "cible": "ENTREPRISE,ECOLE",
  "features": [
    "OFFERS_PUBLISH",
    "MESSAGERIE_ILLIMITEE",
    "AI_CHAT_ACCESS"
  ]
}
```

**Expected:** `200 OK`

---

## 6. Changer le pack de l'utilisateur connecté

### PUT `/api/users/me/pack`
**URL:** `http://localhost:8080/api/users/me/pack`

**Headers:**
```http
Content-Type: application/json
Authorization: Bearer {{user_token}}
```

**Body:**
```json
{
  "packId": "{{candidate_pack_id}}"
}
```

**Expected:** `200 OK`

**Vérifie dans la réponse :**
- `packId`
- `packNom`
- `packCible`
- `packFeatures`
- `canManageOffers`
- `canUseMessaging`
- `canUseAiChat`

---

## 7. Tester l'incompatibilité de pack

Exemple : un `ETUDIANT` tente de prendre le pack `BUSINESS_PLUS`.

### PUT `/api/users/me/pack`
**Body:**
```json
{
  "packId": "{{business_pack_id}}"
}
```

**Expected:** `400 Bad Request`

**Body attendu :**
```json
{
  "message": "Ce pack n'est pas compatible avec votre role"
}
```

---

## 8. Vérifier les droits front/API après changement de pack

### GET `/api/users/me`
**Headers:**
```http
Authorization: Bearer {{user_token}}
```

**Expected:** `200 OK`

**Vérifie :**
- `packNom`
- `canUseMessaging`
- `canUseAiChat`
- `canManageOffers`

---

## 9. Vérifier le résumé des accès métier

### GET `/api/features/access-summary`
**Headers:**
```http
Authorization: Bearer {{user_token}}
```

**Expected:** `200 OK`

**Exemple de réponse :**
```json
{
  "packNom": "PREMIUM_CANDIDAT",
  "canViewOpportunities": true,
  "canManageOffers": false,
  "canUseMessaging": true,
  "canUseAiChat": true
}
```

---

## 10. Tester la messagerie selon le pack

### GET `/api/features/messaging/access`
**Headers:**
```http
Authorization: Bearer {{user_token}}
```

**Expected si autorisé:** `200 OK`

**Expected si bloqué:** `403 Forbidden`

**Body possible :**
```json
{
  "allowed": false,
  "packNom": "FREE",
  "message": "Votre pack actuel ne permet pas d'utiliser la messagerie"
}
```

---

## 11. Tester le chat IA selon le pack

### GET `/api/features/chat-ai/access`
**Headers:**
```http
Authorization: Bearer {{user_token}}
```

**Expected si autorisé:** `200 OK`

**Expected si bloqué:** `403 Forbidden`

**Body possible :**
```json
{
  "allowed": false,
  "packNom": "FREE",
  "message": "Votre pack actuel ne permet pas d'utiliser le chat IA"
}
```

---

## 12. Vérifier la restriction sur la publication d'offres

Avec un compte `ENTREPRISE` ou `ECOLE` en pack `FREE`, tente :

### POST `/api/offers`
**Headers:**
```http
Content-Type: application/json
Authorization: Bearer {{company_token}}
```

**Body:**
```json
{
  "titre": "Offre bloquée pack FREE",
  "description": "Test restriction pack",
  "type": "STAGE",
  "domaine": "Informatique",
  "location": "Paris",
  "competencesRequises": ["Java"],
  "datePublication": "2026-04-03T10:00:00",
  "dateDebut": "2026-06-01T09:00:00",
  "dateFin": "2026-08-31T18:00:00"
}
```

**Expected:** `403 Forbidden`

**Body attendu :**
```json
{
  "message": "Votre pack actuel ne permet pas de publier ou gerer des offres"
}
```

> Après passage sur un pack business compatible, la même requête doit retourner `201 Created`.

---

## 13. Supprimer un pack non utilisé (admin only)

### DELETE `/api/packs/{id}`
**URL:** `http://localhost:8080/api/packs/{{delete_pack_id}}`

**Headers:**
```http
Authorization: Bearer {{admin_token}}
```

**Expected:** `200 OK`

**Body attendu :**
```json
{
  "message": "Pack supprime avec succes"
}
```

> Si tu obtiens `409 Conflict`, cela veut dire que le pack est encore utilisé par au moins un utilisateur. Dans ce cas, remets d'abord l'utilisateur sur un autre pack (par exemple `FREE`) avec `PUT /api/users/me/pack`, puis refais le `DELETE`.

---

## Résultats attendus

| Cas | Endpoint | Résultat attendu |
|-----|----------|------------------|
| Liste des packs | `GET /api/packs` | `200` |
| Création pack par admin | `POST /api/packs` | `201` |
| Création pack par non-admin | `POST /api/packs` | `403` |
| Changement de pack utilisateur | `PUT /api/users/me/pack` | `200` |
| Changement vers pack incompatible | `PUT /api/users/me/pack` | `400` |
| Résumé des droits | `GET /api/features/access-summary` | `200` |
| Messagerie bloquée selon pack | `GET /api/features/messaging/access` | `403` |
| Chat IA bloqué selon pack | `GET /api/features/chat-ai/access` | `403` |
| Publication d'offre bloquée par pack | `POST /api/offers` | `403` |
| Suppression pack non utilisé par admin | `DELETE /api/packs/{id}` | `200` |
