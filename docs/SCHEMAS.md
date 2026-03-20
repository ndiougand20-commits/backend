# Schéma de la base de données – REZO

---

## 1. Entités principales

### User
| Champ           | Type    | Contraintes          | Commentaire                  |
|-----------------|---------|----------------------|------------------------------|
| id              | uuid    | PK                   |                              |
| email           | string  | UNIQUE, NOT NULL     | Login principal              |
| password_hash   | string  | NOT NULL             | Stocké hashé                 |
| prenom          | string  | NOT NULL             |                              |
| nom             | string  | NOT NULL             |                              |
| telephone       | string  |                      | Optionnel                    |
| role            | enum    | NOT NULL             | ETUDIANT, LYCEEN, EMPLOI, ENTREPRISE, ECOLE, ADMIN |
| pack_id         | uuid    | NOT NULL, FK         | Vers Pack                    |
| avatar_url      | string  |                      | Optionnel                    |
| date_inscription| datetime|                      |                              |

### Profile_Etudiant / Chercheur d’Emploi
| Champ                | Type       | Contraintes                 | Commentaire                 |
|----------------------|------------|-----------------------------|-----------------------------|
| id                   | uuid       | PK                          |                             |
| user_id              | uuid       | UNIQUE, NOT NULL, FK        | Vers User                   |
| niveau_etude         | string     | NOT NULL                    | Bac, Licence, etc.          |
| domaine              | string     | NOT NULL                    | Informatique, Droit, etc.   |
| competences          | text/array | NOT NULL                    | Tag ou liste                |
| objectif             | string     | NOT NULL                    | Stage, Emploi, etc.         |
| preferences_secteur  | text/array |                             | Optionnel                   |
| preferences_lieu     | text/array |                             | Optionnel                   |
| experiences          | text/array |                             | Stage/Emploi passés         |
| cv_url               | string     |                             | Lien vers PDF/Doc optionnel |
| --- Documents ---    |            |                             |                             |
| diplome_ids          | array FK   |                             | Vers Diplome                |
| releve_note_ids      | array FK   |                             | Vers ReleveNote             |
| accreditation_ids    | array FK   |                             | Vers Accreditation          |
| linea_ids            | array FK   |                             | Vers Linea                  |

### Profile_Lyceen
| Champ                | Type       | Contraintes                 | Commentaire                 |
|----------------------|------------|-----------------------------|-----------------------------|
| id                   | uuid       | PK                          |                             |
| user_id              | uuid       | UNIQUE, NOT NULL, FK        | Vers User                   |
| classe_actuelle      | string     | NOT NULL                    | Première, Terminale, etc.   |
| serie_orientation    | string     | NOT NULL                    | S, ES, Pro, etc.            |
| objectif_postbac     | string     | NOT NULL                    | Cursus visé                 |
| centres_interet      | text/array |                             | Optionnel                   |
| --- Documents ---    |            |                             |                             |
| bulletins_ids        | array FK   |                             | Vers ReleveNote             |
| diplome_ids          | array FK   |                             | Vers Diplome                |
| accreditation_ids    | array FK   |                             | Vers Accreditation          |
| linea_ids            | array FK   |                             | Vers Linea                  |

### Profile_Entreprise
| Champ                    | Type       | Contraintes                 | Commentaire              |
|--------------------------|------------|-----------------------------|--------------------------|
| id                       | uuid       | PK                          |                          |
| user_id                  | uuid       | UNIQUE, NOT NULL, FK        | Vers User                |
| raison_sociale           | string     | NOT NULL                    |                          |
| secteur_activite         | string     | NOT NULL                    |                          |
| taille                   | enum       | NOT NULL                    | MICRO, PME, ETI, GE      |
| description              | text       | NOT NULL                    |                          |
| adresse                  | string     |                             | Optionnel                |
| site_web                 | string     |                             |                          |
| logo_url                 | string     |                             |                          |
| --- Documents ---        |            |                             |                          |
| accreditation_ids        | array FK   |                             | Vers Accreditation       |
| linea_ids                | array FK   |                             | Vers Linea               |
| document_societaire_ids  | array FK   |                             | Vers DocumentSocietaire  |

### Profile_Ecole
| Champ                    | Type       | Contraintes                 | Commentaire                   |
|--------------------------|------------|-----------------------------|-------------------------------|
| id                       | uuid       | PK                          |                               |
| user_id                  | uuid       | UNIQUE, NOT NULL, FK        | Vers User                     |
| nom_etablissement        | string     | NOT NULL                    |                               |
| statut                   | enum       | NOT NULL                    | PUBLIC, PRIVE                 |
| domaines                 | text/array | NOT NULL                    |                               |
| diplomes_delivres        | text/array | NOT NULL                    |                               |
| description              | string     |                             |                               |
| adresse                  | string     |                             | Optionnel                     |
| site_web                 | string     |                             |                               |
| logo_url                 | string     |                             |                               |
| --- Documents ---        |            |                             |                               |
| accreditation_ids        | array FK   |                             | Vers Accreditation            |
| linea_ids                | array FK   |                             | Vers Linea                    |
| document_officiel_ids    | array FK   |                             | Vers DocumentOfficiel         |

---

### Pack
- id, nom, description, prix, cible, features

### Message
- id, sender_id, receiver_id, content, timestamp, is_read, offer_id

### ChatSupport
- id, user_id, user_message, ia_response, context, timestamp, session_id

### Swipe
- id, user_id, offer_id, action (LIKE, DISLIKE), date_swipe

### Offer
- id, titre, description, type (STAGE, EMPLOI, FORMATION), date_publication,
- owner_entreprise_id (Profile_Entreprise FK), owner_ecole_id (Profile_Ecole FK)
- domaine, location, date_debut, date_fin

---

### --- Tables Documents / Références annexes ---

#### Diplome
| Champ            | Type   |             | Commentaire                        |
|------------------|--------|-------------|-------------------------------------|
| id               | uuid   | PK          |                                    |
| profile_id       | uuid   | FK          | Vers Profile_Etudiant/Profile_Lyceen|
| intitule         | string | NOT NULL    | Nom du diplôme                     |
| type             | string | NOT NULL    | Bac, licence, Master, etc.         |
| file_url         | string | NOT NULL    | Lien vers PDF/document             |
| date_obtention   | date   |             |                                    |
| statut           | string |             | Validé, en cours, annulé…          |

#### ReleveNote (Bulletin)
| Champ            | Type   |             | Commentaire               |
|------------------|--------|-------------|---------------------------|
| id               | uuid   | PK          |                           |
| profile_id       | uuid   | FK          | Vers Profile_Etudiant/Profile_Lyceen|
| file_url         | string | NOT NULL    | Bulletins scannés         |
| année            | int    |             |                           |
| niveau           | string |             | Ex : 1ère, Terminale      |

#### Accreditation
| Champ            | Type   |             | Commentaire                        |
|------------------|--------|-------------|-------------------------------------|
| id               | uuid   | PK          |                                    |
| profile_id       | uuid   | FK          | Vers tous profils                  |
| label            | string | NOT NULL    | Label/Certif (ex : TOEFL, CTI…)    |
| organisme        | string |             |                                    |
| date_obtention   | date   |             |                                    |
| file_url         | string |             | Preuve si dispo                    |

#### Linea (Liens pro/web)
| Champ        | Type   |             | Commentaire                    |
|--------------|--------|-------------|--------------------------------|
| id           | uuid   | PK          |                                |
| profile_id   | uuid   | FK          | Vers tous profils              |
| type         | string | NOT NULL    | LinkedIn, GitHub, Site, etc.   |
| url          | string | NOT NULL    |                                |


#### DocumentSocietaire/DocumentOfficiel (Pour Entreprise/École)
| Champ        | Type   |             | Commentaire                    |
|--------------|--------|-------------|--------------------------------|
| id           | uuid   | PK          |                                |
| profile_id   | uuid   | FK          | Vers Profile_Entreprise/Ecole  |
| type         | string | NOT NULL    | Statuts, convention, etc.      |
| file_url     | string | NOT NULL    | Lien PDF/Scan                  |
| nom          | string |             | Titre du document              |

---

## 2. Relations et cardinalités principales

- **User** `1---1` **Profile_X** (clé unique user_id)
- **Profile_X** `1---N` **Diplome** / **ReleveNote** / **Accreditation** / **Linea** / **DocumentSocietaire/Officiel**
- **User** `M---1` **Pack**
- **Offer** `M---1` **Profile_Ecole** ou **Profile_Entreprise** (au moins une FK non nulle)
- **Message**, **Swipe**, **ChatSupport** reliés à User
- **Cascade Delete** : delete User = delete Profile, et tous documents annexes

---

## 3. Contraintes Métier et Sécurité

- email unique dans User
- Un seul profil par user (clé user_id unique dans chaque Profile\*)
- Un Document annexé ne doit être accessible qu'à son propriétaire (ou admin)
- Pour chaque offre, only one owner (soit owner_entreprise_id, soit owner_ecole_id)
- Un Swipe unique par (user_id, offer_id)
- Enum sur roles, tailles d’entreprise, statuts diplôme, type de document, etc.
- Accès admin sur tous documents pour supervision/facturation

---

## 4. Schéma dbdiagram.io (structuration, à copier)

```dbml
Table User {
  id uuid [pk]
  email varchar [unique, not null]
  password_hash varchar [not null]
  prenom varchar [not null]
  nom varchar [not null]
  telephone varchar
  role varchar [not null]
  pack_id uuid [not null, ref: > Pack.id]
  avatar_url varchar
  date_inscription timestamp
}

Table Profile_Etudiant {
  id uuid [pk]
  user_id uuid [unique, not null, ref: > User.id]
  niveau_etude varchar
  domaine varchar
  competences text
  objectif varchar
  preferences_secteur text
  preferences_lieu text
  experiences text
  cv_url varchar
}

Table Profile_Lyceen {
  id uuid [pk]
  user_id uuid [unique, not null, ref: > User.id]
  classe_actuelle varchar
  serie_orientation varchar
  objectif_postbac varchar
  centres_interet text
}

Table Profile_Entreprise {
  id uuid [pk]
  user_id uuid [unique, not null, ref: > User.id]
  raison_sociale varchar
  secteur_activite varchar
  taille varchar
  description text
  adresse varchar
  site_web varchar
  logo_url varchar
}

Table Profile_Ecole {
  id uuid [pk]
  user_id uuid [unique, not null, ref: > User.id]
  nom_etablissement varchar
  statut varchar
  domaines text
  diplomes_delivres text
  description text
  adresse varchar
  site_web varchar
  logo_url varchar
}

Table Pack {
  id uuid [pk]
  nom varchar
  description text
  prix decimal
  cible varchar
  features text
}

Table Message {
  id uuid [pk]
  sender_id uuid [not null, ref: > User.id]
  receiver_id uuid [not null, ref: > User.id]
  content text
  timestamp timestamp
  is_read bool
  offer_id uuid [ref: > Offer.id]
}

Table ChatSupport {
  id uuid [pk]
  user_id uuid [not null, ref: > User.id]
  user_message text
  ia_response text
  context varchar
  timestamp timestamp
  session_id uuid
}

Table Swipe {
  id uuid [pk]
  user_id uuid [not null, ref: > User.id]
  offer_id uuid [not null, ref: > Offer.id]
  action varchar
  date_swipe timestamp
}

Table Offer {
  id uuid [pk]
  titre varchar
  description text
  type varchar
  date_publication timestamp
  owner_entreprise_id uuid [ref: > Profile_Entreprise.id]
  owner_ecole_id uuid [ref: > Profile_Ecole.id]
  domaine varchar
  location varchar
  date_debut timestamp
  date_fin timestamp
}

Table Diplome {
  id uuid [pk]
  profile_id uuid [not null, ref: > Profile_Etudiant.id, ref: > Profile_Lyceen.id]
  intitule varchar
  type varchar
  file_url varchar
  date_obtention date
  statut varchar
}

Table ReleveNote {
  id uuid [pk]
  profile_id uuid [not null, ref: > Profile_Etudiant.id, ref: > Profile_Lyceen.id]
  file_url varchar
  année int
  niveau varchar
}

Table Accreditation {
  id uuid [pk]
  profile_id uuid [not null, ref: > Profile_Etudiant.id, ref: > Profile_Lyceen.id, ref: > Profile_Ecole.id, ref: > Profile_Entreprise.id]
  label varchar
  organisme varchar
  date_obtention date
  file_url varchar
}

Table Linea {
  id uuid [pk]
  profile_id uuid [not null, ref: > Profile_Etudiant.id, ref: > Profile_Lyceen.id, ref: > Profile_Ecole.id, ref: > Profile_Entreprise.id]
  type varchar
  url varchar
}

Table DocumentSocietaire {
  id uuid [pk]
  profile_id uuid [not null, ref: > Profile_Entreprise.id]
  type varchar
  file_url varchar
  nom varchar
}

Table DocumentOfficiel {
  id uuid [pk]
  profile_id uuid [not null, ref: > Profile_Ecole.id]
  type varchar
  file_url varchar
  nom varchar
}
```

---

