# Test CRUD Écoles / Entreprises (scrum-18) - Postman Guide

## Prérequis
- Application démarrée : `.\mvnw.cmd spring-boot:run`
- Postman installé
- Base PostgreSQL disponible

---

## 1. Créer un utilisateur `ECOLE`

### Signup école
**Method:** `POST`  
**URL:** `http://localhost:8080/api/auth/signup`

**Headers:**
```http
Content-Type: application/json
```

**Body:**
```json
{
  "email": "ecole-owner@rezo.com",
  "password": "SecurePass123!",
  "role": "ECOLE",
  "prenom": "Admin",
  "nom": "School",
  "profil": {
    "nom_etablissement": "Mon Ecole",
    "statut": "PUBLIC",
    "domaines": ["Informatique"],
    "diplomes_delivres": ["Bachelor"],
    "description": "Ecole tech"
  }
}
```

**Expected:** `201 Created`

---

## 2. Login école et récupérer le JWT

### Login école
**Method:** `POST`  
**URL:** `http://localhost:8080/api/auth/login`

**Body:**
```json
{
  "email": "ecole-owner@rezo.com",
  "password": "SecurePass123!"
}
```

**Expected:** `200 OK`

**Response:**
```json
{
  "token": "eyJhbGciOi..."
}
```

Stocke ce token dans une variable Postman `school_token`.

---

## 3. Créer une fiche école

### POST /api/schools
**Method:** `POST`  
**URL:** `http://localhost:8080/api/schools`

**Headers:**
```http
Content-Type: application/json
Authorization: Bearer {{school_token}}
```

**Body:**
```json
{
  "nomEtablissement": "ESGI",
  "statut": "PUBLIC",
  "domaines": ["Informatique", "Data"],
  "diplomesDelivres": ["Bachelor", "Master"],
  "description": "Grande école informatique",
  "adresse": "Paris",
  "siteWeb": "https://www.esgi.fr",
  "logoUrl": "https://www.esgi.fr/logo.png"
}
```

**Expected:** `201 Created`

**À noter :** copie l'`id` retourné dans une variable `school_id`.

---

## 4. Lire la liste des écoles (public)

### GET /api/schools
**Method:** `GET`  
**URL:** `http://localhost:8080/api/schools`

**Headers:** aucun

**Expected:** `200 OK`

---

## 5. Lire une école précise (public)

### GET /api/schools/{id}
**Method:** `GET`  
**URL:** `http://localhost:8080/api/schools/{{school_id}}`

**Expected:** `200 OK`

---

## 6. Modifier sa propre école

### PUT /api/schools/{id}
**Method:** `PUT`  
**URL:** `http://localhost:8080/api/schools/{{school_id}}`

**Headers:**
```http
Content-Type: application/json
Authorization: Bearer {{school_token}}
```

**Body:**
```json
{
  "nomEtablissement": "ESGI Updated",
  "statut": "PRIVE",
  "domaines": ["Informatique", "Cyber"],
  "siteWeb": "https://www.esgi-updated.fr"
}
```

**Expected:** `200 OK`

---

## 7. Tester l'ownership école (doit échouer en 403)

Crée un **second** utilisateur `ECOLE`, connecte-le, récupère `other_school_token`, puis tente :

### PUT d'une école qui ne lui appartient pas
**Method:** `PUT`  
**URL:** `http://localhost:8080/api/schools/{{school_id}}`

**Headers:**
```http
Content-Type: application/json
Authorization: Bearer {{other_school_token}}
```

**Body:**
```json
{
  "nomEtablissement": "Hack School"
}
```

**Expected:** `403 Forbidden`

**Body attendu :**
```json
{
  "message": "Vous ne pouvez modifier que votre propre fiche ecole"
}
```

---

## 8. Tester validation école

### URL invalide
**Method:** `POST`  
**URL:** `http://localhost:8080/api/schools`

**Headers:**
```http
Content-Type: application/json
Authorization: Bearer {{school_token}}
```

**Body:**
```json
{
  "nomEtablissement": "Bad School",
  "statut": "PUBLIC",
  "domaines": ["Informatique"],
  "siteWeb": "notaurl"
}
```

**Expected:** `400 Bad Request`

**Body attendu :**
```json
{
  "message": "siteWeb doit etre une URL valide (http/https)"
}
```

---

## 9. Supprimer sa propre école

### DELETE /api/schools/{id}
**Method:** `DELETE`  
**URL:** `http://localhost:8080/api/schools/{{school_id}}`

**Headers:**
```http
Authorization: Bearer {{school_token}}
```

**Expected:** `200 OK`

**Body attendu :**
```json
{
  "message": "Ecole supprimee avec succes"
}
```

---

# Partie Entreprises

## 10. Créer un utilisateur `ENTREPRISE`

### Signup entreprise
**Method:** `POST`  
**URL:** `http://localhost:8080/api/auth/signup`

**Body:**
```json
{
  "email": "entreprise-owner@rezo.com",
  "password": "SecurePass123!",
  "role": "ENTREPRISE",
  "prenom": "Admin",
  "nom": "Company",
  "profil": {
    "raison_sociale": "TechCorp",
    "secteur": "IT",
    "taille": "PME",
    "description": "Entreprise tech"
  }
}
```

**Expected:** `201 Created`

---

## 11. Login entreprise

### POST /api/auth/login
**Body:**
```json
{
  "email": "entreprise-owner@rezo.com",
  "password": "SecurePass123!"
}
```

**Expected:** `200 OK`

Stocke le token dans `company_token`.

---

## 12. Créer une fiche entreprise

### POST /api/companies
**Method:** `POST`  
**URL:** `http://localhost:8080/api/companies`

**Headers:**
```http
Content-Type: application/json
Authorization: Bearer {{company_token}}
```

**Body:**
```json
{
  "raisonSociale": "TechCorp",
  "secteurActivite": "IT",
  "taille": "PME",
  "description": "Entreprise innovante",
  "adresse": "Lyon",
  "siteWeb": "https://techcorp.com",
  "logoUrl": "https://techcorp.com/logo.png"
}
```

**Expected:** `201 Created`

Copie l'id dans `company_id`.

---

## 13. Lire la liste des entreprises (public)

### GET /api/companies
**Method:** `GET`  
**URL:** `http://localhost:8080/api/companies`

**Expected:** `200 OK`

---

## 14. Modifier sa propre entreprise

### PUT /api/companies/{id}
**Method:** `PUT`  
**URL:** `http://localhost:8080/api/companies/{{company_id}}`

**Headers:**
```http
Content-Type: application/json
Authorization: Bearer {{company_token}}
```

**Body:**
```json
{
  "raisonSociale": "TechCorp Updated",
  "description": "Entreprise encore plus innovante",
  "siteWeb": "https://updated-techcorp.com"
}
```

**Expected:** `200 OK`

---

## 15. Tester l'ownership entreprise (403)

Crée un autre utilisateur `ENTREPRISE`, récupère `other_company_token`, puis tente :

### DELETE d'une entreprise qui ne lui appartient pas
**Method:** `DELETE`  
**URL:** `http://localhost:8080/api/companies/{{company_id}}`

**Headers:**
```http
Authorization: Bearer {{other_company_token}}
```

**Expected:** `403 Forbidden`

**Body attendu :**
```json
{
  "message": "Vous ne pouvez supprimer que votre propre fiche entreprise"
}
```

---

## 16. Tester validation entreprise

### URL invalide
**Method:** `POST`  
**URL:** `http://localhost:8080/api/companies`

**Headers:**
```http
Content-Type: application/json
Authorization: Bearer {{company_token}}
```

**Body:**
```json
{
  "raisonSociale": "Bad Corp",
  "secteurActivite": "IT",
  "taille": "PME",
  "description": "Test",
  "siteWeb": "notaurl"
}
```

**Expected:** `400 Bad Request`

**Body attendu :**
```json
{
  "message": "siteWeb doit etre une URL valide (http/https)"
}
```

---

## 17. Supprimer sa propre entreprise

### DELETE /api/companies/{id}
**Method:** `DELETE`  
**URL:** `http://localhost:8080/api/companies/{{company_id}}`

**Headers:**
```http
Authorization: Bearer {{company_token}}
```

**Expected:** `200 OK`

**Body attendu :**
```json
{
  "message": "Entreprise supprimee avec succes"
}
```

---

## Résultats attendus

| Cas | Endpoint | Résultat attendu |
|-----|----------|------------------|
| Liste publique écoles | `GET /api/schools` | `200` |
| Détail école | `GET /api/schools/{id}` | `200` |
| Création école par user ECOLE | `POST /api/schools` | `201` |
| Modification école par owner | `PUT /api/schools/{id}` | `200` |
| Modification école par autre user | `PUT /api/schools/{id}` | `403` |
| Validation site école invalide | `POST /api/schools` | `400` |
| Suppression école par owner | `DELETE /api/schools/{id}` | `200` |
| Liste publique entreprises | `GET /api/companies` | `200` |
| Création entreprise par user ENTREPRISE | `POST /api/companies` | `201` |
| Suppression entreprise par autre user | `DELETE /api/companies/{id}` | `403` |
| Validation site entreprise invalide | `POST /api/companies` | `400` |
| Suppression entreprise par owner | `DELETE /api/companies/{id}` | `200` |
