# Test CRUD Utilisateur / Profil (scrum-17) - Postman Guide

## Prerequisites
- Application running: `.\mvnw.cmd spring-boot:run`
- Postman installed
- Base PostgreSQL disponible

---

## Step 0: Créer un utilisateur de test

### Signup
**Method:** `POST`  
**URL:** `http://localhost:8080/api/auth/signup`

**Headers:**
```http
Content-Type: application/json
```

**Body (raw JSON):**
```json
{
  "email": "crud-check@rezo.com",
  "password": "SecurePass123!",
  "role": "ETUDIANT",
  "prenom": "Alice",
  "nom": "Dupont",
  "profil": {
    "niveau_etude": "M1",
    "domaine": "Informatique",
    "competences": ["Java", "SQL"],
    "objectif": "Stage"
  }
}
```

**Expected:** `201 Created`

---

## Step 1: Récupérer un JWT

### Login
**Method:** `POST`  
**URL:** `http://localhost:8080/api/auth/login`

**Headers:**
```http
Content-Type: application/json
```

**Body (raw JSON):**
```json
{
  "email": "crud-check@rezo.com",
  "password": "SecurePass123!"
}
```

**Expected:** `200 OK`

**Response example:**
```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9..."
}
```

Copie ce token dans une variable Postman `jwt_token` ou colle-le ensuite dans les headers.

---

## Step 2: Vérifier GET /api/users/me

### Mon profil
**Method:** `GET`  
**URL:** `http://localhost:8080/api/users/me`

**Headers:**
```http
Authorization: Bearer {{jwt_token}}
```

**Expected:** `200 OK`

**Réponse attendue, exemple :**
```json
{
  "id": "...",
  "email": "crud-check@rezo.com",
  "prenom": "Alice",
  "nom": "Dupont",
  "telephone": null,
  "role": "ETUDIANT",
  "avatarUrl": null,
  "packNom": "FREE",
  "createdAt": "2026-03-31T...",
  "profil": {
    "niveauEtude": "M1",
    "domaine": "Informatique",
    "competences": ["Java", "SQL"],
    "objectif": "Stage",
    "preferencesSecteur": [],
    "preferencesLieu": [],
    "experiences": []
  }
}
```

Points à vérifier :
- `email` correct
- `role` correct
- `profil.niveauEtude` correct
- `profil.domaine` correct

---

## Step 3: Vérifier PUT /api/users/me

### Modifier infos user + profil
**Method:** `PUT`  
**URL:** `http://localhost:8080/api/users/me`

**Headers:**
```http
Content-Type: application/json
Authorization: Bearer {{jwt_token}}
```

**Body (raw JSON):**
```json
{
  "prenom": "Alicia",
  "nom": "Martin",
  "telephone": "+33612345678",
  "avatarUrl": "https://cdn.rezo.com/avatar/alicia.png",
  "profil": {
    "niveauEtude": "M2",
    "domaine": "Data Science",
    "competences": ["Java", "Spring Boot", "PostgreSQL"],
    "objectif": "CDI",
    "preferencesSecteur": ["Tech", "Finance"],
    "preferencesLieu": ["Paris", "Remote"],
    "experiences": ["Stage backend"]
  }
}
```

**Expected:** `200 OK`

**Points à vérifier dans la réponse :**
- `prenom = Alicia`
- `nom = Martin`
- `telephone = +33612345678`
- `profil.niveauEtude = M2`
- `profil.domaine = Data Science`
- `profil.competences` contient `Spring Boot`

---

## Step 4: Refaire GET /api/users/me

Refais exactement la requête GET précédente.

**Expected:** `200 OK`

But : vérifier que les modifications sont persistées en base et non seulement renvoyées par le PUT.

---

## Step 5: Vérifier 401 sans JWT

### GET sans token
**Method:** `GET`  
**URL:** `http://localhost:8080/api/users/me`

**Headers:** aucun

**Expected:** `401 Unauthorized`

**Body attendu :**
```json
{
  "message": "Non authentifie: token JWT manquant ou invalide"
}
```

### PUT sans token
**Method:** `PUT`  
**URL:** `http://localhost:8080/api/users/me`

**Headers:**
```http
Content-Type: application/json
```

**Body:**
```json
{
  "prenom": "Hack"
}
```

**Expected:** `401 Unauthorized`

### DELETE sans token
**Method:** `DELETE`  
**URL:** `http://localhost:8080/api/users/me`

**Headers:** aucun

**Expected:** `401 Unauthorized`

---

## Step 6: Vérifier 401 avec JWT invalide

### GET avec faux token
**Method:** `GET`  
**URL:** `http://localhost:8080/api/users/me`

**Headers:**
```http
Authorization: Bearer faux.token.invalide
```

**Expected:** `401 Unauthorized`

**Body attendu :**
```json
{
  "message": "Token JWT invalide ou expire"
}
```

---

## Step 7: Vérifier les validations PUT

### Email vide
**Method:** `PUT`  
**URL:** `http://localhost:8080/api/users/me`

**Headers:**
```http
Content-Type: application/json
Authorization: Bearer {{jwt_token}}
```

**Body:**
```json
{
  "email": "   "
}
```

**Expected:** `400 Bad Request`

**Body attendu :**
```json
{
  "message": "L'email ne peut pas etre vide"
}
```

### Prénom vide
**Body:**
```json
{
  "prenom": "   "
}
```

**Expected:** `400 Bad Request`

**Body attendu :**
```json
{
  "message": "Le prenom ne peut pas etre vide"
}
```

### Nom vide
**Body:**
```json
{
  "nom": "   "
}
```

**Expected:** `400 Bad Request`

**Body attendu :**
```json
{
  "message": "Le nom ne peut pas etre vide"
}
```

### Champ profil invalide étudiant
**Body:**
```json
{
  "profil": {
    "niveauEtude": "   "
  }
}
```

**Expected:** `400 Bad Request`

**Body attendu :**
```json
{
  "message": "niveauEtude ne peut pas etre vide"
}
```

---

## Step 8: Vérifier l’unicité email

Crée d’abord un second utilisateur :

### Signup second user
**Method:** `POST`  
**URL:** `http://localhost:8080/api/auth/signup`

**Body:**
```json
{
  "email": "second-user@rezo.com",
  "password": "SecurePass123!",
  "role": "ETUDIANT",
  "prenom": "Bob",
  "nom": "Test",
  "profil": {
    "niveau_etude": "L3",
    "domaine": "Maths"
  }
}
```

Ensuite, avec le token du premier user, tente :

### PUT email déjà pris
**Method:** `PUT`  
**URL:** `http://localhost:8080/api/users/me`

**Headers:**
```http
Content-Type: application/json
Authorization: Bearer {{jwt_token}}
```

**Body:**
```json
{
  "email": "second-user@rezo.com"
}
```

**Expected:** `409 Conflict`

**Body attendu :**
```json
{
  "message": "Email deja utilise"
}
```

---

## Step 9: Vérifier DELETE /api/users/me

### Supprimer mon compte
**Method:** `DELETE`  
**URL:** `http://localhost:8080/api/users/me`

**Headers:**
```http
Authorization: Bearer {{jwt_token}}
```

**Expected:** `200 OK`

**Body attendu :**
```json
{
  "message": "Compte supprime avec succes"
}
```

---

## Step 10: Vérifier qu’il n’existe plus

### Refaire GET avec l’ancien token
**Method:** `GET`  
**URL:** `http://localhost:8080/api/users/me`

**Headers:**
```http
Authorization: Bearer {{jwt_token}}
```

**Expected:** `404 Not Found`

**Body attendu :**
```json
{
  "message": "Utilisateur introuvable"
}
```

---

## Ce que ce ticket valide réellement

- `GET /api/users/me` fonctionne avec JWT valide
- `PUT /api/users/me` modifie uniquement l’utilisateur du token
- `DELETE /api/users/me` supprime uniquement l’utilisateur du token
- aucune route ne permet de cibler un autre utilisateur par id
- sans JWT : `401`
- JWT invalide : `401`
- payload invalide : `400`
- email déjà utilisé : `409`

---

## Résumé rapide des statuts attendus

| Cas | Endpoint | Résultat attendu |
|-----|----------|------------------|
| Lecture profil avec token valide | `GET /api/users/me` | `200` |
| Modification profil avec token valide | `PUT /api/users/me` | `200` |
| Suppression compte avec token valide | `DELETE /api/users/me` | `200` |
| Lecture sans token | `GET /api/users/me` | `401` |
| Lecture avec faux token | `GET /api/users/me` | `401` |
| PUT avec champ vide | `PUT /api/users/me` | `400` |
| PUT avec email déjà pris | `PUT /api/users/me` | `409` |
| GET après suppression | `GET /api/users/me` | `404` |
