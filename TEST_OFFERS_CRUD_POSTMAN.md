# Test CRUD Offres Stages / Emplois (scrum-19) - Postman Guide

## Prérequis
- Application démarrée : `./mvnw.cmd spring-boot:run`
- JWT valides pour un compte `ENTREPRISE` ou `ECOLE`
- Une fiche entreprise ou école déjà créée pour le compte utilisé

> Tu peux réutiliser `company_token`, `school_token`, `company_id` et `school_id` des guides précédents.

---

## 1. Publier une offre avec un compte `ENTREPRISE`

### POST `/api/offers`
**URL:** `http://localhost:8080/api/offers`

**Headers:**
```http
Content-Type: application/json
Authorization: Bearer {{company_token}}
```

**Body:**
```json
{
  "titre": "Stage Backend Spring Boot",
  "description": "Nous cherchons un stagiaire pour renforcer l'équipe backend Java.",
  "type": "STAGE",
  "domaine": "Informatique",
  "location": "Paris",
  "competencesRequises": ["Java", "Spring Boot", "PostgreSQL"],
  "datePublication": "2026-04-02T10:00:00",
  "dateDebut": "2026-06-01T09:00:00",
  "dateFin": "2026-08-31T18:00:00"
}
```

**Expected:** `201 Created`

**Points à vérifier dans la réponse :**
- `ownerType = "ENTREPRISE"`
- `ownerUserId` correspond au user connecté
- `ownerCompanyId` est renseigné
- `ownerDisplayName` contient la raison sociale

Copie l'`id` retourné dans une variable Postman : `offer_id`.

---

## 2. Publier une offre avec un compte `ECOLE`

### POST `/api/offers`
**Headers:**
```http
Content-Type: application/json
Authorization: Bearer {{school_token}}
```

**Body:**
```json
{
  "titre": "Offre d'emploi - Formateur Java",
  "description": "Notre école recrute un formateur Java pour la rentrée.",
  "type": "EMPLOI",
  "domaine": "Education",
  "location": "Lyon",
  "competencesRequises": ["Java", "Pédagogie"],
  "datePublication": "2026-04-02T11:00:00",
  "dateDebut": "2026-09-01T09:00:00",
  "dateFin": "2027-06-30T18:00:00"
}
```

**Expected:** `201 Created`

**Vérifie :**
- `ownerType = "ECOLE"`
- `ownerSchoolId` est renseigné
- `ownerDisplayName` contient le nom de l'établissement

---

## 3. Lister toutes les offres (public)

### GET `/api/offers`
**URL:** `http://localhost:8080/api/offers`

**Headers:** aucun

**Expected:** `200 OK`

**Vérifie dans la liste :**
- chaque offre a bien un `ownerType`
- les champs `ownerCompanyId` ou `ownerSchoolId` sont cohérents
- `ownerDisplayName` correspond bien au profil propriétaire

---

## 4. Récupérer une offre précise (public)

### GET `/api/offers/{id}`
**URL:** `http://localhost:8080/api/offers/{{offer_id}}`

**Expected:** `200 OK`

---

## 5. Modifier sa propre offre

### PUT `/api/offers/{id}`
**URL:** `http://localhost:8080/api/offers/{{offer_id}}`

**Headers:**
```http
Content-Type: application/json
Authorization: Bearer {{company_token}}
```

**Body:**
```json
{
  "titre": "Stage Backend Spring Boot - Updated",
  "location": "Paris La Défense",
  "competencesRequises": ["Java", "Spring Boot", "Docker"],
  "dateFin": "2026-09-15T18:00:00"
}
```

**Expected:** `200 OK`

---

## 6. Tester l'ownership (doit échouer en 403)

Connecte un **autre** compte `ENTREPRISE` ou `ECOLE`, récupère `other_token`, puis tente :

### DELETE `/api/offers/{id}`
**Headers:**
```http
Authorization: Bearer {{other_token}}
```

**Expected:** `403 Forbidden`

**Body attendu :**
```json
{
  "message": "Vous ne pouvez supprimer que votre propre offre"
}
```

---

## 7. Tester le type invalide

### POST `/api/offers`
**Headers:**
```http
Content-Type: application/json
Authorization: Bearer {{company_token}}
```

**Body:**
```json
{
  "titre": "Offre invalide",
  "description": "Test type invalide",
  "type": "FORMATION",
  "domaine": "Informatique",
  "location": "Paris",
  "competencesRequises": ["Java"],
  "datePublication": "2026-04-02T10:00:00",
  "dateDebut": "2026-06-01T09:00:00",
  "dateFin": "2026-08-31T18:00:00"
}
```

**Expected:** `400 Bad Request`

**Body attendu :**
```json
{
  "message": "type doit etre STAGE ou EMPLOI"
}
```

---

## 8. Tester des dates incohérentes

### POST `/api/offers`
**Headers:**
```http
Content-Type: application/json
Authorization: Bearer {{school_token}}
```

**Body:**
```json
{
  "titre": "Offre dates invalides",
  "description": "Test de validation métier sur les dates",
  "type": "STAGE",
  "domaine": "Data",
  "location": "Remote",
  "competencesRequises": ["Python"],
  "datePublication": "2026-04-02T10:00:00",
  "dateDebut": "2026-08-10T09:00:00",
  "dateFin": "2026-06-01T18:00:00"
}
```

**Expected:** `400 Bad Request`

**Body attendu :**
```json
{
  "message": "dateDebut doit etre strictement avant dateFin"
}
```

---

## 9. Tester compétences requises vides

### POST `/api/offers`
**Headers:**
```http
Content-Type: application/json
Authorization: Bearer {{company_token}}
```

**Body:**
```json
{
  "titre": "Offre sans compétences",
  "description": "Test validation compétences",
  "type": "EMPLOI",
  "domaine": "Cloud",
  "location": "Remote",
  "competencesRequises": [],
  "datePublication": "2026-04-02T10:00:00",
  "dateDebut": "2026-06-01T09:00:00",
  "dateFin": "2026-08-31T18:00:00"
}
```

**Expected:** `400 Bad Request`

**Body attendu :**
```json
{
  "message": "competencesRequises ne peut pas etre vide"
}
```

---

## 10. Supprimer sa propre offre

### DELETE `/api/offers/{id}`
**URL:** `http://localhost:8080/api/offers/{{offer_id}}`

**Headers:**
```http
Authorization: Bearer {{company_token}}
```

**Expected:** `200 OK`

**Body attendu :**
```json
{
  "message": "Offre supprimee avec succes"
}
```

---

## Résultats attendus

| Cas | Endpoint | Résultat attendu |
|-----|----------|------------------|
| Liste publique des offres | `GET /api/offers` | `200` |
| Détail d'une offre | `GET /api/offers/{id}` | `200` |
| Création par owner `ENTREPRISE` | `POST /api/offers` | `201` |
| Création par owner `ECOLE` | `POST /api/offers` | `201` |
| Modification par le propriétaire | `PUT /api/offers/{id}` | `200` |
| Modification/suppression par un autre user | `PUT/DELETE /api/offers/{id}` | `403` |
| Type invalide | `POST /api/offers` | `400` |
| Dates incohérentes | `POST /api/offers` | `400` |
| Compétences vides | `POST /api/offers` | `400` |
| Suppression par le propriétaire | `DELETE /api/offers/{id}` | `200` |
