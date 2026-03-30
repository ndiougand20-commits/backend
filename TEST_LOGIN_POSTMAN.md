# Test Login API - Postman Guide

## Prerequisites
- Application running: `.\mvnw.cmd spring-boot:run` on port 8080
- Postman installed

---

## Step 1: SIGNUP — Create a User

### Request
**Method:** `POST`  
**URL:** `http://localhost:8080/api/auth/signup`

### Headers
```
Content-Type: application/json
```

### Body (JSON raw)
```json
{
  "email": "alice@rezo.com",
  "password": "MonMotDePasse123!",
  "role": "ETUDIANT",
  "profil": {
    "niveau_etude": "M1",
    "domaine": "Informatique"
  }
}
```

### Expected Response (201 Created)
```json
{
  "message": "Inscription reussie",
  "userId": "2117fc63-74f0-413c-99d0-d21c303ae3df",
  "email": "alice@rezo.com",
  "role": "ETUDIANT",
  "profileType": "PROFILE"
}
```

---

## Step 2: LOGIN — Generate JWT Token

### Request
**Method:** `POST`  
**URL:** `http://localhost:8080/api/auth/login`

### Headers
```
Content-Type: application/json
```

### Body (JSON raw)
```json
{
  "email": "alice@rezo.com",
  "password": "MonMotDePasse123!"
}
```

### Expected Response (200 OK)
```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiIyMTE3ZmM2My03NGYwLTQxM2MtOTlkMC1kMjFjMzAzYWUzZGYiLCJlbWFpbCI6ImFsaWNlQHJlem8uY29tIiwicm9sZSI6IkVUVURJQU5UIiwicGFja0lkIjoiYWY5YjQ0NDYtNTlmOS00MWZmLWExMWYtZWZjY2U5NWQ3Yjk4IiwiaWF0IjoxNzc0ODAzMTA1LCJleHAiOjE3NzQ4ODk1MDV9.wR-vCoDEYdBji2-k9C-EL64zk-mqWbkTmb-xMZF7w5dRwRcFqNcbhnN-dm030pno"
}
```

---

## Error Tests

### Test 1: Wrong Password
**Body:**
```json
{
  "email": "alice@rezo.com",
  "password": "WrongPassword"
}
```

**Expected Response (401 Unauthorized):**
```json
{
  "message": "Email ou mot de passe incorrect"
}
```

### Test 2: Unknown Email
**Body:**
```json
{
  "email": "unknown@rezo.com",
  "password": "AnyPassword123!"
}
```

**Expected Response (401 Unauthorized):**
```json
{
  "message": "Email ou mot de passe incorrect"
}
```

### Test 3: Missing Email
**Body:**
```json
{
  "password": "MonMotDePasse123!"
}
```

**Expected Response (400 Bad Request):**
```json
{
  "message": "Email et mot de passe obligatoires"
}
```

### Test 4: Missing Password
**Body:**
```json
{
  "email": "alice@rezo.com"
}
```

**Expected Response (400 Bad Request):**
```json
{
  "message": "Email et mot de passe obligatoires"
}
```

---

## Using the Token

Once you have the token from the login response, you can use it in protected endpoints:

**Header:**
```
Authorization: Bearer <your_token_here>
```

Example:
```
Authorization: Bearer eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiIyMTE3ZmM2My03NGYwLTQxM2MtOTlkMC1kMjFjMzAzYWUzZGYiLCJlbWFpbCI6ImFsaWNlQHJlem8uY29tIiwicm9sZSI6IkVUVURJQU5UIiwicGFja0lkIjoiYWY5YjQ0NDYtNTlmOS00MWZmLWExMWYtZWZjY2U5NWQ3Yjk4IiwiaWF0IjoxNzc0ODAzMTA1LCJleHAiOjE3NzQ4ODk1MDV9.wR-vCoDEYdBji2-k9C-EL64zk-mqWbkTmb-xMZF7w5dRwRcFqNcbhnN-dm030pno
```

---

## Notes

- Change `alice@rezo.com` and password to test with different values
- JWT token has a 24-hour expiration (86400000 ms)
- Token payload includes: `sub` (userId), `email`, `role`, `packId`, `iat`, `exp`
- Each signup creates a unique user; reuse the same credentials for multiple login tests
