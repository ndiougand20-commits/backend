# Test Matching Intelligent & Pack Suggéré (scrum-21) - Postman Guide

## Prérequis
- Application démarrée : `./mvnw.cmd spring-boot:run`
- Base PostgreSQL disponible
- Les endpoints du sprint packs sont disponibles
- Un ou plusieurs packs existent (`FREE`, `PREMIUM_CANDIDAT`, `BUSINESS_PLUS`)

---

## 1. Préparer des offres à matcher

Crée un compte `ENTREPRISE` puis publie 2 ou 3 offres via `POST /api/offers`.

### Exemple offre 1
```json
{
  "titre": "Stage Backend Java",
  "description": "Stage backend Spring Boot orienté API REST",
  "type": "STAGE",
  "domaine": "Informatique",
  "location": "Paris",
  "competencesRequises": ["Java", "Spring", "SQL"],
  "datePublication": "2026-04-07T10:00:00",
  "dateDebut": "2026-06-01T09:00:00",
  "dateFin": "2026-08-31T18:00:00"
}
```

### Exemple offre 2
```json
{
  "titre": "Stage Marketing Digital",
  "description": "Stage orienté SEO et réseaux sociaux",
  "type": "STAGE",
  "domaine": "Marketing",
  "location": "Lyon",
  "competencesRequises": ["SEO", "Communication"],
  "datePublication": "2026-04-07T10:00:00",
  "dateDebut": "2026-06-01T09:00:00",
  "dateFin": "2026-08-31T18:00:00"
}
```

---

## 2. Tester avec un profil `ETUDIANT`

### Signup étudiant
**POST** `http://localhost:8080/api/auth/signup`

```json
{
  "email": "match-etudiant@rezo.com",
  "password": "SecurePass123!",
  "role": "ETUDIANT",
  "prenom": "Alice",
  "nom": "Matcher",
  "profil": {
    "niveauEtude": "M1",
    "domaine": "Informatique",
    "competences": ["Java", "Spring", "SQL"],
    "objectif": "Trouver un stage backend",
    "preferencesLieu": ["Paris"]
  }
}
```

### Login étudiant
**POST** `http://localhost:8080/api/auth/login`

```json
{
  "email": "match-etudiant@rezo.com",
  "password": "SecurePass123!"
}
```

Stocke le token dans `student_token`.

### Appeler le matching
**GET** `http://localhost:8080/api/match/recommendations`

**Headers:**
```http
Authorization: Bearer {{student_token}}
```

**Expected:** `200 OK`

**À vérifier :**
- `recommendations` contient des offres triées par score décroissant
- l'offre `Stage Backend Java` remonte avant l'offre marketing
- `suggestedPack` est visible avec une `reason`
- `trace.excludedSwipeCount`, `trace.evaluatedCount`, `trace.returnedCount` sont présents

---

## 3. Tester avec un profil `LYCEEN`

### Signup lycéen
```json
{
  "email": "match-lyceen@rezo.com",
  "password": "SecurePass123!",
  "role": "LYCEEN",
  "prenom": "Leo",
  "nom": "Lyceen",
  "profil": {
    "classeActuelle": "Terminale",
    "serieOrientation": "NSI",
    "objectifPostbac": "Trouver une voie en informatique",
    "centresInteret": ["Informatique", "Développement"]
  }
}
```

Puis login et appel de :

**GET** `http://localhost:8080/api/match/recommendations`

**Résultat attendu :**
- réponse différente de l'étudiant
- scoring adapté à son profil
- pack suggéré potentiellement différent

---

## 4. Tester avec un profil `ENTREPRISE`

Avec un user `ENTREPRISE` en pack `FREE` :

**GET** `http://localhost:8080/api/match/recommendations`

**Résultat attendu :**
- `suggestedPack.label` peut recommander un pack business
- `suggestedPack.reason` mentionne la publication/gestion des offres si le pack est insuffisant

---

## 5. Vérifier l'exclusion des offres déjà swipées

Comme il n'y a pas encore d'endpoint public de création de swipe dans ce sprint, tu peux simuler le cas en base PostgreSQL.

### SQL de test
```sql
INSERT INTO swipes (id, user_id, offer_id, action, created_at)
VALUES (
  gen_random_uuid(),
  'UUID_DU_USER',
  'UUID_DE_L_OFFRE',
  'DISLIKE',
  now()
);
```

Ensuite rappelle :

**GET** `http://localhost:8080/api/match/recommendations`

**Résultat attendu :**
- l'offre swipée n'apparaît plus dans `recommendations`
- `trace.excludedSwipeCount` augmente

---

## 6. Exemple de réponse attendue

```json
{
  "recommendations": [
    {
      "offerId": "123e4567-e89b-12d3-a456-426614174000",
      "score": 92,
      "reasons": [
        "domaine compatible",
        "2 competence(s) commune(s)",
        "localisation preferee"
      ],
      "offer": {
        "titre": "Stage Backend Java",
        "type": "STAGE",
        "domaine": "Informatique",
        "location": "Paris"
      }
    }
  ],
  "suggestedPack": {
    "id": "pack-uuid",
    "label": "PREMIUM_CANDIDAT",
    "reason": "Acces illimite a la messagerie pour contacter plus facilement les recruteurs"
  },
  "trace": {
    "excludedSwipeCount": 1,
    "evaluatedCount": 2,
    "returnedCount": 1
  }
}
```

---

## 7. Vérifier les logs backend

Dans la console Spring Boot, tu dois voir des logs de ce type :

```text
Matching snapshot userId=... role=ETUDIANT domains=[informatique] skills=[java, spring]
Matching scoring userId=... offerId=... score=100 reasons=[domaine compatible, 2 competence(s) commune(s)]
Matching trace userId=... offerId=... excluded=already_swiped
Pack suggestion userId=... currentPack=FREE suggestedPack=PREMIUM_CANDIDAT reason=Acces illimite a la messagerie...
```

---

## Résultats attendus

| Cas | Endpoint | Résultat attendu |
|-----|----------|------------------|
| Matching étudiant | `GET /api/match/recommendations` | `200` + recommandations triées |
| Matching lycéen | `GET /api/match/recommendations` | `200` + réponse différente |
| Offre déjà swipée | `GET /api/match/recommendations` | offre exclue |
| Pack suggéré | `GET /api/match/recommendations` | `suggestedPack` visible |
| Logs scoring | console backend | traces `snapshot`, `score`, `excluded`, `suggestion` |
