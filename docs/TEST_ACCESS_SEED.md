# Acces de test frontend (seed SQL)

## 1) Prerequis
- Le backend doit pointer vers la DB locale:
  - host: localhost
  - port: 5432
  - database: rezo_db
  - user: rezo_user
  - password: rezo987_password
- Executer le script:

```bash
psql -U rezo_user -d rezo_db -f docs/seed_front_test_data.sql
```

## 2) Comptes de test (mot de passe unique)
- Mot de passe pour tous les comptes: password

| Role | Email |
|---|---|
| ADMIN | admin@rezo.test |
| ETUDIANT | etudiant1@rezo.test |
| ETUDIANT | etudiant2@rezo.test |
| LYCEEN | lyceen1@rezo.test |
| ENTREPRISE | entreprise1@rezo.test |
| ENTREPRISE | entreprise2@rezo.test |
| ECOLE | ecole1@rezo.test |
| ECOLE | ecole2@rezo.test |

## 3) Jeux de donnees inclus
- Packs: 9 packs complets
- Users: 8 comptes multi-roles
- Profiles: 3 profils (etudiant, lyceen)
- Companies: 2
- Schools: 2
- Offers: 4 (stage, emploi, formations)
- Swipes: 4 (LIKE/DISLIKE)
- Profile swipes: 2 (pour matches mutuels)
- Messages: 3
- User media files: 5
- Chat support: 2

## 4) Scenarios front a tester rapidement
- Login par role avec JWT
- /api/users/me et edition de profil
- /api/match/recommendations et /api/match/swipe
- /api/match/profile-recommendations, /api/match/profile-swipe, /api/match/mutual
- /api/offers et /api/offers/{id}/liked-by
- /api/messages + conversation + statut lu/non lu
- /api/features/access-summary
- /api/users/me/media

## 5) Notes
- Le script fait un TRUNCATE complet des tables applicatives avant insertion.
- Les IDs sont fixes pour faciliter les tests deterministes frontend.
- Le seeder Java des packs n interviendra pas ici car les packs sont deja inseres.
