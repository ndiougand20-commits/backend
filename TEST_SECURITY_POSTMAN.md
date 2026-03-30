# Test Sécurité JWT (scrum-16) - Postman Guide

## Prerequisites
- Application running: `.\mvnw.cmd spring-boot:run` on port 8080
- Postman installed
- Un utilisateur créé via signup (voir Step 0)

---

## Step 0: Créer un utilisateur + récupérer le token

### Signup
**Method:** `POST`  
**URL:** `http://localhost:8080/api/auth/signup`

**Body (JSON raw):**
```json
{
  "email": "test-security@rezo.com",
  "password": "SecurePass123!",
  "role": "ETUDIANT",
  "profil": {
    "niveau_etude": "M1",
    "domaine": "Informatique"
  }
}
```

### Login (récupérer le token)
**Method:** `POST`  
**URL:** `http://localhost:8080/api/auth/login`

**Body (JSON raw):**
```json
{
  "email": "test-security@rezo.com",
  "password": "SecurePass123!"
}
```

**Réponse (200):**
```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiI..."
}
```

> **Copie ce token**, tu en auras besoin pour tous les tests ci-dessous.

---

## Test 1: Route publique SANS token → 200 OK

Ces routes doivent rester accessibles sans authentification.

### Ping
**Method:** `GET`  
**URL:** `http://localhost:8080/ping`  
**Headers:** aucun  
**Expected:** `200 OK` — `pong`

### Signup
**Method:** `POST`  
**URL:** `http://localhost:8080/api/auth/signup`  
**Headers:** `Content-Type: application/json`  
**Body:** (n'importe quel payload valide)  
**Expected:** `201` ou `409` (si email déjà pris) — mais PAS `401`

### Login
**Method:** `POST`  
**URL:** `http://localhost:8080/api/auth/login`  
**Headers:** `Content-Type: application/json`  
**Body:** (credentials valides)  
**Expected:** `200 OK` + token — PAS `401`

### Swagger UI
**Method:** `GET`  
**URL:** `http://localhost:8080/swagger-ui.html`  
**Headers:** aucun  
**Expected:** `200 OK` (redirect vers Swagger UI)

---

## Test 2: Route protégée SANS token → 401

### Supprimer tous les users (route protégée)
**Method:** `DELETE`  
**URL:** `http://localhost:8080/api/auth/users`  
**Headers:** aucun (pas de Authorization)

**Expected (401 Unauthorized):**
```json
{
  "message": "Non authentifie: token JWT manquant ou invalide"
}
```

### Supprimer un user par email (route protégée)
**Method:** `DELETE`  
**URL:** `http://localhost:8080/api/auth/users/by-email/test@rezo.com`  
**Headers:** aucun

**Expected (401 Unauthorized):**
```json
{
  "message": "Non authentifie: token JWT manquant ou invalide"
}
```

---

## Test 3: Route protégée avec token INVALIDE → 401

**Method:** `DELETE`  
**URL:** `http://localhost:8080/api/auth/users`

**Headers:**
```
Authorization: Bearer ceci.nest.pas.un.vrai.token
```

**Expected (401 Unauthorized):**
```json
{
  "message": "Token JWT invalide ou expire"
}
```

---

## Test 4: Route protégée avec token VALIDE → 200 OK

**Method:** `DELETE`  
**URL:** `http://localhost:8080/api/auth/users/by-email/test-security@rezo.com`

**Headers:**
```
Authorization: Bearer <COLLE_TON_TOKEN_ICI>
```

**Expected (200 OK):**
```json
{
  "message": "Utilisateur supprime"
}
```

---

## Test 5: Route restreinte par rôle avec MAUVAIS rôle → 403

> L'utilisateur créé ci-dessus a le rôle `ETUDIANT`.
> La route `/api/admin/**` est réservée au rôle `ADMIN`.

**Method:** `GET`  
**URL:** `http://localhost:8080/api/admin/anything`

**Headers:**
```
Authorization: Bearer <TOKEN_ETUDIANT>
```

**Expected (403 Forbidden):**
```json
{
  "message": "Acces refuse: role insuffisant"
}
```

---

## Test 6: Route restreinte par rôle avec BON rôle → 200

Pour ce test, il faut créer un utilisateur avec le rôle `ENTREPRISE` :

### Signup entreprise
**Method:** `POST`  
**URL:** `http://localhost:8080/api/auth/signup`

**Body:**
```json
{
  "email": "entreprise@rezo.com",
  "password": "EntreprisePass123!",
  "role": "ENTREPRISE",
  "profil": {
    "raison_sociale": "TechCorp",
    "secteur": "IT",
    "taille": "PME",
    "description": "Entreprise tech"
  }
}
```

### Login entreprise
**Method:** `POST`  
**URL:** `http://localhost:8080/api/auth/login`

**Body:**
```json
{
  "email": "entreprise@rezo.com",
  "password": "EntreprisePass123!"
}
```

### Accéder à /api/entreprise/** avec le token ENTREPRISE
**Method:** `GET`  
**URL:** `http://localhost:8080/api/entreprise/anything`

**Headers:**
```
Authorization: Bearer <TOKEN_ENTREPRISE>
```

**Expected:** `404 Not Found` (endpoint n'existe pas encore, mais PAS `401` ni `403`)

> Si tu obtiens `404`, c'est que le token et le rôle sont bien acceptés.
> `401` = token manquant/invalide. `403` = mauvais rôle.

---

## Résumé des résultats attendus

| Test | Route | Token | Rôle | Résultat |
|------|-------|-------|------|----------|
| 1 | `/ping` | ❌ Aucun | — | **200** |
| 1 | `/api/auth/login` | ❌ Aucun | — | **200** |
| 1 | `/swagger-ui.html` | ❌ Aucun | — | **200** |
| 2 | `/api/auth/users` | ❌ Aucun | — | **401** |
| 3 | `/api/auth/users` | ❌ Invalide | — | **401** |
| 4 | `/api/auth/users/by-email/...` | ✅ Valide | ETUDIANT | **200** |
| 5 | `/api/admin/anything` | ✅ Valide | ETUDIANT | **403** |
| 6 | `/api/entreprise/anything` | ✅ Valide | ENTREPRISE | **404** (pas 401/403) |
