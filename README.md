# REZO Backend

> **🎊 STATUS: SOUTENANCE READY** ✅  
> **Tests**: 66/66 PASSING  
> **Documentation**: Complete (Algorithm + Architecture + KPI Analysis)

---

## 📖 Quick Links (For Thesis Presentation)

### Must Read First
- **[QUICK_START.md](QUICK_START.md)** - 60-second overview (5 min read)
- **[SOUTENANCE_SUMMARY.md](SOUTENANCE_SUMMARY.md)** - Executive summary (10 min read)

### Detailed Documentation
- **[docs/MATCHING_ALGORITHM.md](docs/MATCHING_ALGORITHM.md)** - Algorithm formula + weights + comparisons (15 min read)
- **[docs/ARCHITECTURE_DIAGRAMS.md](docs/ARCHITECTURE_DIAGRAMS.md)** - 7 system architecture diagrams (10 min read)

### Tools & Scripts
- **[scripts/analyze_kpi.py](scripts/analyze_kpi.py)** - Python KPI analysis
- **[scripts/verify_soutenance_ready.sh](scripts/verify_soutenance_ready.sh)** - Verification script

---
---

## Description

REZO Backend est l’API RESTful qui alimente l’application mobile REZO : une plateforme de matching intelligent et messagerie entre étudiants, lycéens, chercheurs d’emploi, écoles et entreprises.

---

## Stack Technique

- **Langage** : Java 17+
- **Framework** : Spring Boot
- **Base de données** : PostgreSQL
- **Gestion des dépendances** : Maven (pom.xml)
- **Outils de documentation** : Swagger/OpenAPI

---

## Structure du projet

```plaintext
rezo-backend/
├── src/
│   ├── main/
│   │   ├── java/             # Code source Java
│   │   │   └── com/rezo/...  # Dossiers métier (controller, entity, service, repository)
│   │   └── resources/        # Configuration (application.properties), scripts, migrations
├── docs/                     # Documentation technique, schémas, user stories
├── config/                   # Fichiers de configuration avancée (option)
├── pom.xml                   # Dépendances Maven
├── README.md
```

---

## Installation rapide

1. **Pré-requis :**
   - Java JDK 17+
   - PostgreSQL (base de données créée, ex : `rezo_db`)
   - Maven (`mvn` ou `./mvnw`)
   - (Optionnel) pgAdmin pour visualiser la base

2. **Setup PostgreSQL** :
    - Créer la base :
        ```sql
        CREATE DATABASE rezo_db;
        ```

3. **Configuration (src/main/resources/application.properties) :**
    ```
    spring.datasource.url=jdbc:postgresql://localhost:5432/rezo_db
    spring.datasource.username=<votre_user_pg>
    spring.datasource.password=<votre_mdp_pg>
    spring.jpa.hibernate.ddl-auto=update
    ```

4. **Lancer l’application :**
    ```bash
    ./mvnw spring-boot:run
    ```
    Ou
    ```bash
    mvn spring-boot:run
    ```

5. **Documentation API :**
    - Une fois lancé, accéder à : [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) pour tester les endpoints.

---

## Modules/MVP prévus

- Authentification multi-profils (étudiant, lycée, école, entreprise)
- Gestion CRUD utilisateurs, profils, packs, écoles, entreprises, offres
- Matching intelligent (recommandations, swipe, suggestion pack)
- Messagerie inter-profils
- Chat IA support (proposition packs, FAQ, orientation)
- Restrictions et logique par packs

---

## Liens utiles

- [Sprint backlog et user stories](./docs/USER_STORIES.md)
- [Schéma UML/relationnel](./docs/SCHEMA.md)
- [Guide API Swagger](http://localhost:8080/swagger-ui.html)
- [Flutter Frontend](https://github.com/<ton-user>/rezo-frontend)

---

## Roadmap MVP

1. Initialisation API Spring Boot
2. Écriture des entités principales
3. Mise en place de l’auth sécurisée (JWT)
4. Premier CRUD utilisateurs/écoles/entreprises/offres
5. Endpoints de matching
6. Messagerie et chat support IA
7. Documentation et tests automatisés

---

## Contribution

- Toute nouvelle feature ou correction : créer une branche (`feature/xxx`, `bugfix/yyy`)
- Push via pull request pour revue
- Les docs et schémas doivent être mis à jour dans `/docs`

---

## Contact

Pour toute question, suggestion ou bug :
- Ndiouga NDIAYE : ndiougand20@gmail.com

