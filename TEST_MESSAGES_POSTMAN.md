# TEST MESSAGES POSTMAN

Ce guide permet de verifier manuellement le ticket **messagerie utilisateur/profil**.

---

## 0. Pre-requis

- Backend lance sur `http://localhost:8080` (ou adapte le port)
- Deux utilisateurs existants avec JWT valides
- Les deux packs doivent autoriser la messagerie (`MESSAGERIE_LIMITEE` ou `MESSAGERIE_ILLIMITEE`)

---

## 1. Creer deux comptes de test

### 1.1 Expediteur

**POST** `http://localhost:8080/api/auth/signup`

```json
{
  "email": "msg.sender@rezo.com",
  "password": "SecurePass123!",
  "role": "ETUDIANT",
  "prenom": "Nina",
  "nom": "Sender",
  "profil": {
    "niveauEtude": "M1",
    "domaine": "Informatique",
    "competences": ["Java", "Spring"],
    "objectif": "Trouver un stage",
    "preferencesLieu": ["Paris"]
  }
}
```

### 1.2 Destinataire

**POST** `http://localhost:8080/api/auth/signup`

```json
{
  "email": "msg.receiver@rezo.com",
  "password": "SecurePass123!",
  "role": "ENTREPRISE",
  "prenom": "Marc",
  "nom": "Receiver",
  "profil": {
    "raison_sociale": "MessageCorp",
    "secteur": "IT",
    "taille": "PME",
    "description": "Entreprise de test"
  }
}
```

---

## 2. Se connecter et recuperer les tokens

### 2.1 Login expediteur

**POST** `http://localhost:8080/api/auth/login`

```json
{
  "email": "msg.sender@rezo.com",
  "password": "SecurePass123!"
}
```

### 2.2 Login destinataire

**POST** `http://localhost:8080/api/auth/login`

```json
{
  "email": "msg.receiver@rezo.com",
  "password": "SecurePass123!"
}
```

> Conserver les deux `token` JWT et le `userId` du destinataire.

---

## 3. Envoyer un message

**POST** `http://localhost:8080/api/messages`

Headers:
- `Authorization: Bearer <TOKEN_EXPEDITEUR>`
- `Content-Type: application/json`

```json
{
  "receiverId": "<USER_ID_DESTINATAIRE>",
  "content": "Bonjour, je vous contacte au sujet de votre profil.",
  "relatedOfferId": null
}
```

### Attendu
- **201 Created**
- retour avec `id`, `senderId`, `receiverId`, `content`, `isRead=false`

---

## 4. Recuperer la conversation

**GET** `http://localhost:8080/api/messages/conversation/<USER_ID_DESTINATAIRE>`

Headers:
- `Authorization: Bearer <TOKEN_EXPEDITEUR>`

### Attendu
- **200 OK**
- liste des messages echanges entre les deux comptes uniquement

---

## 5. Lister mes messages avec filtre

**GET** `http://localhost:8080/api/messages?read=false&page=0&size=10&sort=asc`

Headers:
- `Authorization: Bearer <TOKEN_DESTINATAIRE>`

### Attendu
- **200 OK**
- objet avec :
  - `items`
  - `totalElements`
  - `totalPages`
  - `readFilter`

---

## 6. Marquer un message comme lu

**PUT** `http://localhost:8080/api/messages/<MESSAGE_ID>/read`

Headers:
- `Authorization: Bearer <TOKEN_DESTINATAIRE>`

### Attendu
- **200 OK**
- `isRead = true`

---

## 7. Supprimer un message

**DELETE** `http://localhost:8080/api/messages/<MESSAGE_ID>`

Headers:
- `Authorization: Bearer <TOKEN_EXPEDITEUR>`

### Attendu
- **200 OK**

```json
{
  "message": "Message supprime avec succes"
}
```

---

## 8. Cas d'erreur a verifier

### 8.1 Sans JWT
- appel sur `/api/messages`
- attendu : **401 Unauthorized**

### 8.2 Auto-message interdit

```json
{
  "receiverId": "<MON_USER_ID>",
  "content": "Bonjour moi-meme"
}
```

Attendu : **400**

```json
{
  "message": "Vous ne pouvez pas vous envoyer un message a vous-meme"
}
```

### 8.3 Suppression par quelqu'un d'autre
- tenter `DELETE /api/messages/{id}` avec le token du destinataire
- attendu : **403**

```json
{
  "message": "Vous ne pouvez supprimer que vos propres messages"
}
```

### 8.4 Message vide

```json
{
  "receiverId": "<USER_ID_DESTINATAIRE>",
  "content": "   "
}
```

Attendu : **400**

```json
{
  "message": "Le contenu du message est obligatoire"
}
```
