# Guide Frontend - Edition de Profil avec Photo et Justificatifs

Ce guide explique comment integrer la partie edition de profil cote frontend (Flutter, mobile ou web) avec:
- photo de profil (image)
- justificatifs de diplome (PDF)

Base API:
- `http://localhost:8082` (a adapter selon ton environnement)

Auth:
- Tous les endpoints ci-dessous demandent `Authorization: Bearer <JWT>`.

---

## 1) Vue d'ensemble du flow

1. Charger le profil utilisateur (`GET /api/users/me`).
2. L'utilisateur choisit une image de profil et/ou un ou plusieurs PDF justificatifs.
3. Uploader les fichiers via endpoints media.
4. Pour la photo de profil, enregistrer l'URL retournee dans le profil (`PUT /api/users/me` avec `avatarUrl`).
5. Afficher la liste des medias (`GET /api/users/me/media`).
6. Supprimer un media si besoin (`DELETE /api/users/me/media/{id}`).

---

## 2) Endpoints edition de profil

### 2.1 Lire le profil courant

`GET /api/users/me`

Reponse (extrait):

```json
{
  "id": "...",
  "email": "user@rezo.com",
  "prenom": "Jane",
  "nom": "Doe",
  "telephone": "0600000000",
  "role": "ETUDIANT",
  "avatarUrl": "/uploads/users/<userId>/photos/<file>.jpg",
  "profil": {
    "niveauEtude": "Bac+3",
    "domaine": "Informatique"
  }
}
```

### 2.2 Mettre a jour le profil

`PUT /api/users/me`

Tu peux envoyer seulement les champs a modifier. Pour la photo, c'est `avatarUrl` qui doit etre mis a jour apres upload.

Payload minimal (exemple):

```json
{
  "avatarUrl": "/uploads/users/<userId>/photos/abc.jpg"
}
```

---

## 3) Endpoints media (photo + justificatifs)

### 3.1 Uploader une photo de profil

`POST /api/users/me/media/photos`

- Content-Type: `multipart/form-data`
- Champ obligatoire: `file`
- Formats acceptes: `jpeg`, `png`, `webp`, `gif`
- Taille max: `8 MB`

Reponse `201`:

```json
{
  "id": "uuid",
  "category": "PHOTO",
  "originalFileName": "photo.png",
  "contentType": "image/png",
  "fileUrl": "/uploads/users/<userId>/photos/<generated>.png",
  "createdAt": "2026-04-13T19:37:07"
}
```

### 3.2 Uploader un justificatif diplome

`POST /api/users/me/media/justificatifs`

- Content-Type: `multipart/form-data`
- Champ obligatoire: `file`
- Type accepte: PDF uniquement
- Taille max: `12 MB`

Reponse `201`:

```json
{
  "id": "uuid",
  "category": "JUSTIFICATIF_PDF",
  "originalFileName": "diplome.pdf",
  "contentType": "application/pdf",
  "fileUrl": "/uploads/users/<userId>/justificatifs/<generated>.pdf",
  "createdAt": "2026-04-13T19:37:07"
}
```

### 3.3 Lister les medias

`GET /api/users/me/media`

Option filtre:
- `GET /api/users/me/media?category=PHOTO`
- `GET /api/users/me/media?category=JUSTIFICATIF_PDF`

Reponse `200`: tableau de `UserMediaFileResponse`.

### 3.4 Supprimer un media

`DELETE /api/users/me/media/{id}`

Reponse `200`:

```json
{
  "message": "Media supprime avec succes"
}
```

Reponse `404` si le media n'existe pas ou n'appartient pas a l'utilisateur.

---

## 4) Ordre recommande cote frontend

### Cas A - Changement photo de profil

1. Upload image vers `POST /api/users/me/media/photos`.
2. Recuperer `fileUrl` de la reponse.
3. Appeler `PUT /api/users/me` avec `{ "avatarUrl": "<fileUrl>" }`.
4. Rafraichir `GET /api/users/me`.

### Cas B - Ajout justificatif diplome

1. Upload PDF vers `POST /api/users/me/media/justificatifs`.
2. Rafraichir la liste `GET /api/users/me/media?category=JUSTIFICATIF_PDF`.
3. Afficher nom original + date + action supprimer.

---

## 5) Exemple Flutter (Dio)

```dart
import 'package:dio/dio.dart';

class ProfileApi {
  final Dio dio;
  final String baseUrl;

  ProfileApi(this.dio, {this.baseUrl = 'http://localhost:8082'});

  Options _auth(String token) => Options(
        headers: {'Authorization': 'Bearer $token'},
      );

  Future<Map<String, dynamic>> getMe(String token) async {
    final res = await dio.get('$baseUrl/api/users/me', options: _auth(token));
    return Map<String, dynamic>.from(res.data);
  }

  Future<Map<String, dynamic>> uploadPhoto(String token, String filePath) async {
    final formData = FormData.fromMap({
      'file': await MultipartFile.fromFile(filePath),
    });
    final res = await dio.post(
      '$baseUrl/api/users/me/media/photos',
      data: formData,
      options: _auth(token),
    );
    return Map<String, dynamic>.from(res.data);
  }

  Future<Map<String, dynamic>> uploadJustificatifPdf(String token, String filePath) async {
    final formData = FormData.fromMap({
      'file': await MultipartFile.fromFile(filePath),
    });
    final res = await dio.post(
      '$baseUrl/api/users/me/media/justificatifs',
      data: formData,
      options: _auth(token),
    );
    return Map<String, dynamic>.from(res.data);
  }

  Future<List<dynamic>> listMyMedia(String token, {String? category}) async {
    final res = await dio.get(
      '$baseUrl/api/users/me/media',
      queryParameters: category == null ? null : {'category': category},
      options: _auth(token),
    );
    return List<dynamic>.from(res.data);
  }

  Future<void> updateAvatarUrl(String token, String avatarUrl) async {
    await dio.put(
      '$baseUrl/api/users/me',
      data: {'avatarUrl': avatarUrl},
      options: _auth(token),
    );
  }

  Future<void> deleteMedia(String token, String mediaId) async {
    await dio.delete('$baseUrl/api/users/me/media/$mediaId', options: _auth(token));
  }
}
```

---

## 6) Gestion d'erreurs a implementer dans l'UI

Codes frequents:
- `400`: format invalide, taille depassee, payload incorrect
- `401`: token absent/invalide/expire
- `404`: utilisateur ou media introuvable
- `409`: email deja utilise (sur update profil)

Messages backend utiles:
- `Aucun fichier recu`
- `Format image non supporte (jpeg, png, webp, gif)`
- `L'image depasse la taille maximale autorisee (8MB)`
- `Seuls les fichiers PDF sont autorises pour les justificatifs`
- `Le PDF depasse la taille maximale autorisee (12MB)`

---

## 7) Rendu image/PDF dans le frontend

Le backend retourne `fileUrl` en chemin relatif (ex: `/uploads/users/...`).

Construis l'URL absolue cote frontend:
- `absoluteUrl = baseUrl + fileUrl`

Exemple:
- `baseUrl = http://localhost:8082`
- `fileUrl = /uploads/users/123/photos/a.png`
- URL finale: `http://localhost:8082/uploads/users/123/photos/a.png`

---

## 8) Bonnes pratiques UX

- Valider localement type + taille avant upload.
- Afficher une barre de progression upload.
- Sur photo: afficher preview avant confirmation.
- Sur justificatifs: afficher statut (upload en cours, succes, erreur).
- Autoriser suppression d'un justificatif avec confirmation.

---

## 9) Checklist dev frontend

- Ecran edition profil relie a `GET /api/users/me` et `PUT /api/users/me`
- Upload photo via `POST /api/users/me/media/photos`
- Mise a jour `avatarUrl` apres upload photo
- Upload justificatifs via `POST /api/users/me/media/justificatifs`
- Liste des justificatifs via `GET /api/users/me/media?category=JUSTIFICATIF_PDF`
- Suppression media via `DELETE /api/users/me/media/{id}`
- Gestion des erreurs `400/401/404/409`
