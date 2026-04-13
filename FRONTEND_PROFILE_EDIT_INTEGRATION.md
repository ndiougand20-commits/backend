# Frontend Flutter - Ticket Page edition profil

## Objectif du ticket
Construire une page profil complete (infos utilisateur, image, pack, historique) + edition robuste avec validation, sauvegarde API, feedback UX et affichage instantane des changements.

---

## 1) Ce que le backend expose deja (a consommer)

## Profil courant
- GET /api/users/me
- Auth: Bearer token requis
- Retourne:
  - infos user: id, email, prenom, nom, telephone, avatarUrl, role
  - infos pack: packId, packNom, packCible, packFeatures, canManageOffers, canUseMessaging, canUseAiChat
  - profil role-specific dans profil {}

## Edition profil
- PUT /api/users/me
- Auth: Bearer token requis
- Body partiel accepte (tu envoies seulement ce qui change)
- Champs user updatables:
  - email
  - prenom
  - nom
  - telephone
  - avatarUrl
- Champs role-specific via profil {}

## Changement pack
- PUT/PATCH /api/users/me/pack
- Auth: Bearer token requis
- Body:
```json
{ "packId": "uuid" }
```

## Liste packs
- GET /api/packs (public)
- utile pour proposer un select de pack

## Historique (sources disponibles)
- GET /api/messages (auth): historique conversationnel
- GET /api/offers (public): tu peux filtrer cote front sur ownerUserId == user.id pour afficher les offres publiees par l'utilisateur (entreprise/ecole)

Important:
- pas d'endpoint upload binaire image natif ici
- avatarUrl est une URL string

---

## 2) Champs profil par role (pour le formulaire dynamique)

## ETUDIANT / EMPLOI
Dans profil:
- niveauEtude (obligatoire si present dans payload)
- domaine (obligatoire si present)
- competences: List<String>
- objectif
- preferencesSecteur: List<String>
- preferencesLieu: List<String>
- experiences: List<String>

## LYCEEN
Dans profil:
- classeActuelle (obligatoire si present)
- serieOrientation (obligatoire si present)
- objectifPostbac
- centresInteret: List<String>

## ENTREPRISE
Dans profil:
- raisonSociale (obligatoire si present)
- secteurActivite (obligatoire si present)
- taille (MICRO, PME, ETI, GE)
- description (obligatoire si present)
- adresse
- siteWeb
- logoUrl

## ECOLE
Dans profil:
- nomEtablissement (obligatoire si present)
- statut (PUBLIC, PRIVE)
- domaines: List<String>
- diplomesDelivres: List<String>
- description
- adresse
- siteWeb
- logoUrl

---

## 3) Architecture Flutter recommandee

## Packages
```yaml
dependencies:
  dio: ^5.7.0
  flutter_secure_storage: ^9.2.2
  flutter_form_builder: ^9.2.1
  form_builder_validators: ^11.1.2
  image_picker: ^1.1.2
```

## Couches minimales
- ProfileApi (appels HTTP)
- ProfileRepository (mapping + orchestration)
- ProfileController/ViewModel (etat ecran)
- ProfilePage + EditProfilePage

## Modeles a creer
- UserMeModel
- RoleProfileModel (union/simple class selon role)
- PackModel
- UpdateProfileRequestModel

---

## 4) Flux UX complet

1. Ouvrir ProfilePage
2. Charger GET /api/users/me
3. Afficher:
   - header user (avatar, prenom/nom, email)
   - cartes pack (nom + features)
   - sections profil selon role
   - bloc historique (messages / offres)
4. Bouton Modifier -> EditProfilePage
5. Form pre-rempli avec valeurs actuelles
6. Validation locale instantanee
7. Submit -> PUT /api/users/me
8. Snackbar succes/erreur
9. Recharger GET /api/users/me
10. Retour profile avec modif visible immediatement

---

## 5) Contrats API utiles au frontend

## GET /api/users/me
Header:
- Authorization: Bearer token

Success 200:
```json
{
  "id": "uuid",
  "email": "user@rezo.com",
  "prenom": "Awa",
  "nom": "Fall",
  "telephone": "+221...",
  "role": "ETUDIANT",
  "avatarUrl": "https://...",
  "packId": "uuid",
  "packNom": "FREE",
  "packCible": "TOUS",
  "packFeatures": ["MATCHING_BASIC", "MESSAGERIE_LIMITEE"],
  "canManageOffers": false,
  "canUseMessaging": true,
  "canUseAiChat": false,
  "createdAt": "2026-...",
  "profil": {
    "niveauEtude": "M1",
    "domaine": "Informatique",
    "competences": ["Java"]
  }
}
```

## PUT /api/users/me
Body exemple ETUDIANT:
```json
{
  "prenom": "Alicia",
  "nom": "Martin",
  "telephone": "+33612345678",
  "avatarUrl": "https://cdn.app/avatar.png",
  "profil": {
    "niveauEtude": "M2",
    "domaine": "Data Science",
    "competences": ["Java", "SQL"],
    "objectif": "CDI",
    "preferencesLieu": ["Paris", "Remote"]
  }
}
```

Erreurs frequentes:
- 400: email/prenom/nom vides, valeur enum invalide, champs profil invalides
- 401: token absent/invalide
- 409: email deja utilise

## PUT /api/users/me/pack
```json
{ "packId": "uuid" }
```

Erreurs:
- 400: pack incompatible role
- 404: user/pack introuvable

---

## 6) Validation front a implementer

## Base user
- email format valide
- prenom non vide si modifie
- nom non vide si modifie
- telephone: regex souple (ou optional strict selon pays)
- avatarUrl: URL valide si renseigne

## Role-specific
- ETUDIANT: niveauEtude et domaine non vides si presents
- ENTREPRISE: raisonSociale, secteurActivite, taille, description coherents
- ECOLE: nomEtablissement et statut valides

## Bonne pratique
- valider localement avant submit
- afficher message champ par champ
- desactiver bouton Sauvegarder pendant chargement

---

## 7) Gestion image/avatar

Comme le backend attend une URL:

Option A (rapide)
- champ texte URL avatar dans le formulaire

Option B (UX meilleure)
- image_picker pour choisir photo
- upload vers Cloudinary/Firebase Storage/S3
- recuperer URL publique
- envoyer cette URL dans avatarUrl

---

## 8) Changement pack (UX)

1. Charger GET /api/packs
2. Afficher bottom sheet/dialog de packs
3. Afficher details: nom, prix, features
4. Selection -> PUT /api/users/me/pack
5. Si 200, recharger /api/users/me
6. Mettre a jour les badges d'acces (canUseMessaging, canUseAiChat, etc.)

---

## 9) Historique dans la page profil

## Messages
- source: GET /api/messages
- afficher derniers items (content, sender/receiver, isRead, createdAt)

## Offres publiees (si ENTREPRISE/ECOLE)
- source: GET /api/offers
- filtrer localement ownerUserId == currentUser.id
- afficher titre, type, datePublication

Note:
- si tu veux un historique utilisateur plus fin (ex: timeline globale), il faudrait un endpoint backend dedie.

---

## 10) Extrait service Flutter (Dio)

```dart
class ProfileApi {
  final Dio dio;
  ProfileApi(this.dio);

  Future<Map<String, dynamic>> getMe() async {
    final r = await dio.get('/api/users/me');
    return Map<String, dynamic>.from(r.data);
  }

  Future<Map<String, dynamic>> updateMe(Map<String, dynamic> payload) async {
    final r = await dio.put('/api/users/me', data: payload);
    return Map<String, dynamic>.from(r.data);
  }

  Future<Map<String, dynamic>> updatePack(String packId) async {
    final r = await dio.put('/api/users/me/pack', data: { 'packId': packId });
    return Map<String, dynamic>.from(r.data);
  }

  Future<List<dynamic>> getPacks() async {
    final r = await dio.get('/api/packs');
    return (r.data as List<dynamic>);
  }

  Future<Map<String, dynamic>> getMessages({bool? read}) async {
    final r = await dio.get('/api/messages', queryParameters: {
      if (read != null) 'read': read,
      'page': 0,
      'size': 20,
      'sort': 'desc',
    });
    return Map<String, dynamic>.from(r.data);
  }
}
```

---

## 11) Feedback utilisateur (toast/snackbar)

## Success
- Profil mis a jour avec succes
- Pack mis a jour

## Error mapping recommande
- 400 -> Donnees invalides, verifie les champs
- 401 -> Session expiree, reconnectez-vous
- 403 -> Action non autorisee
- 409 -> Cet email est deja utilise

---

## 12) Scenarios de validation ticket

## Edition + sauvegarde
- modifier prenom/nom/telephone + champ profil
- sauvegarder
- verifier retour 200
- verifier affichage immediat des nouvelles valeurs

## Validation invalide
- envoyer nom vide
- attendu 400 + feedback front

## Changement pack
- selectionner pack compatible
- verifier mise a jour packNom et droits derives

## Route protegee
- supprimer token local
- ouvrir page profil
- attendu: redirection login

## Rechargement ecran
- revenir sur page profil apres update
- les nouvelles valeurs doivent rester visibles (pas de stale state)

---

## 13) Checklist finale

- [ ] Page profil affiche infos user + pack + role profile
- [ ] Formulaire edition dynamique selon role
- [ ] Validations champs en local
- [ ] PUT /api/users/me integre
- [ ] Snackbar succes/erreur integre
- [ ] Changement pack via /api/users/me/pack
- [ ] Historique (messages/offres) affiche
- [ ] Modif visible immediatement apres sauvegarde
- [ ] Route profil inaccessible sans token

---

## 14) Plan d'implementation rapide (ordre)

1. Construire ProfilePage (lecture /api/users/me)
2. Ajouter EditProfilePage + form dynamique role
3. Brancher PUT /api/users/me
4. Ajouter section Packs + PUT /api/users/me/pack
5. Ajouter bloc Historique (messages puis offres filtrees)
6. Finaliser validation UX + snackbars
7. Tester cas valides/invalides + session
