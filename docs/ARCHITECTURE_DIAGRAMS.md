# Architecture et Diagrammes REZO Matching

## 1. Diagramme d'Authentification et Sécurité

```mermaid
graph TD
    A["Client\n(Flutter/Web)"] -->|POST /api/auth/signup| B["AuthController"]
    B -->|Validate role| C["AuthService"]
    C -->|Create User| D["UserRepository"]
    D -->|Save with Pack| E["Database"]
    
    F["Client"] -->|POST /api/auth/login| B
    B -->|Verify password| C
    C -->|Generate JWT| G["JwtService"]
    G -->|Access + Refresh Token| F
    
    H["Client"] -->|GET /api/users/me| I["UserController"]
    I -->|Extract from JWT| J["SecurityFilterChain"]
    J -->|validate token| G
    G -->|Principal| I
    I -->|OK 200| H
    
    K["Unauthenticated"] -->|GET /api/users/me| J
    J -->|Principal = null| I
    I -->|UNAUTHORIZED 401| K
```

## 2. Diagramme Workflow Swipe → Match → Message

```mermaid
graph LR
    A["ETUDIANT/LYCEEN"] -->|1. GET /api/match/recommendations| B["MatchController"]
    B -->|Score avec\nMatchingService| C["MatchingService"]
    C -->|Calcule Score\n+ Reasons| D["MatchRecommendationsResponse"]
    D -->|Affiche top-10| A
    
    A -->|2. POST /api/match/swipe| B
    B -->|LIKE/DISLIKE| E["SwipeRepository"]
    E -->|Save| F["Database"]
    
    G["ECOLE/ENTREPRISE"] -->|3. GET /api/match/profile-recommendations| B
    B -->|Score profils| C
    C -->|Profile Matching| D
    D -->|Affiche top-10| G
    
    G -->|4. POST /api/match/profile-swipe| B
    B -->|LIKE/DISLIKE| H["ProfileSwipeRepository"]
    H -->|Save| F
    
    I["Détecte Match Mutuel"] -->|Swipe + ProfileSwipe| J["isMutualMatch()"]
    J -->|TRUE| K["MessageController"]
    K -->|Autorise POST /api/messages| L["MessageRepository"]
    L -->|Save| F
    
    J -->|FALSE| M["403 Forbidden"]
    
    L -->|Crée Conversation| N["Frontend"]
    N -->|Affiche Messages| A
```

## 3. Diagramme de Scoring Matching (Détails)

```mermaid
graph TD
    A["Candidate User\n+ Profile"] -->|Extract| B["Competencies\nSectors\nObjective"]
    C["Offer\n+ Owner"] -->|Extract| D["Domain\nSector\nLevel"]
    
    B -->|25%| E["Domain Match"]
    D -->|Compare| E
    E -->|0-25 pts| F["SCORE_BRUT"]
    
    B -->|30%| G["Objective Match"]
    D -->|Compare| G
    G -->|0-30 pts| F
    
    B -->|15%| H["Sector Match"]
    D -->|Compare| H
    H -->|0-15 pts| F
    
    B -->|14%| I["Keyword Match"]
    D -->|Compare| I
    I -->|0-14 pts| F
    
    B -->|8%| J["Role Bonus"]
    J -->|0-8 pts| F
    
    F -->|Sum = 0-92| K["Normalize\nto 0-100"]
    K -->|min 5, max 100| L["SCORE_FINAL"]
    
    L -->|Generate| M["Reasons\nList"]
    M -->|MatchRecommendationItem| N["API Response"]
    
    N -->|Record Event| O["MatchingInstrumentationService"]
    O -->|Store Event| P["In-Memory\nList"]
```

## 4. Diagramme des Services

```mermaid
graph TD
    A["MatchController"] -->|Uses| B["MatchingService"]
    A -->|Record Event| C["MatchingInstrumentationService"]
    A -->|Query| D["UserRepository"]
    A -->|Query| E["OfferRepository"]
    A -->|Query| F["SwipeRepository"]
    A -->|Query| G["ProfileSwipeRepository"]
    
    B -->|Extract| H["MatchProfileSnapshot"]
    B -->|Score| I["scoreOffer"]
    B -->|Score| J["scoreSchool"]
    B -->|Score| K["scoreProfile"]
    
    C -->|Collect| L["MatchingEvent"]
    C -->|Export| M["CSV"]
    C -->|Calculate| N["KPI"]
    
    O["UserController"] -->|Check Auth| P["SecurityFilterChain"]
    P -->|Extract from JWT| Q["JwtService"]
    Q -->|Validate Token| R["RefreshTokenService"]
    
    S["KPIController"] -->|Get KPIs| C
    S -->|Export CSV| C
```

## 5. Diagramme des Cas d'Usage (4 Rôles)

```mermaid
graph TD
    A["ETUDIANT"] -->|GET /recommendations| B["Match Offres"]
    B -->|Score par\ndomaine+objectif| C["Stage/Alternance"]
    A -->|POST /swipe| D["LIKE/DISLIKE"]
    D -->|Détect Match| E["ECOLE/ENTREPRISE"]
    E -->|POST /profile-swipe| F["LIKE/DISLIKE"]
    F -->|Mutual?| G["Match!"]
    G -->|POST /messages| H["Conversation"]
    
    I["LYCEEN"] -->|GET /school-recommendations| J["Match Formations"]
    J -->|Score par\ncentresInteret+objectif| K["Écoles"]
    I -->|POST /profile-swipe| L["LIKE/DISLIKE"]
    L -->|Détect Match| M["ECOLE"]
    M -->|POST /swipe| N["LIKE/DISLIKE"]
    N -->|Mutual?| O["Match!"]
    O -->|Conversation| I
    
    P["ECOLE"] -->|GET /profile-recommendations| Q["Match Candidats"]
    Q -->|Score par\ndomaines+objectif| R["ETUDIANT/LYCEEN"]
    P -->|POST /profile-swipe| S["LIKE/DISLIKE"]
    S -->|Détect Match| T["Candidat"]
    T -->|POST /swipe| U["LIKE/DISLIKE"]
    U -->|Mutual?| V["Match!"]
    V -->|Messaging| P
    
    W["ENTREPRISE"] -->|GET /profile-recommendations| X["Match Candidats"]
    X -->|Score par\ncompétences| Y["ETUDIANT"]
    W -->|POST /profile-swipe| Z["LIKE/DISLIKE"]
    Z -->|Détect Match| AA["Candidat"]
    AA -->|POST /swipe| AB["LIKE/DISLIKE"]
    AB -->|Mutual?| AC["Match!"]
    AC -->|Job Discussion| W
```

## 6. Diagramme de Séquence: Swipe → Match → Message

```mermaid
sequenceDiagram
    participant E as ETUDIANT
    participant MC as MatchController
    participant MS as MatchingService
    participant MIS as MatchingInstrumentationService
    participant DB as Database
    participant EV as ECOLE
    participant MC2 as MessageController

    E->>MC: 1. GET /recommendations
    MC->>MS: scoreOffer(user, offer)
    MS->>MS: Domain + Objective + Sector + Keywords + Role
    MS-->>MC: MatchScore + reasons
    MC->>MIS: recordRecommendation(userId, score, reasons)
    MIS->>MIS: Store event
    MC-->>E: [ { score: 78, reasons: [...] } ]
    
    E->>MC: 2. POST /swipe {offerId, "LIKE"}
    MC->>DB: Save Swipe(user, offer, LIKE)
    DB-->>MC: OK
    MC->>MIS: recordAction(event, "LIKED")
    MIS->>MIS: Update event.action + responseTime
    MC-->>E: 201 Created
    
    EV->>MC: 3. GET /profile-recommendations
    MC->>MS: scoreProfile(ecole, etudiant)
    MS-->>MC: Score + reasons
    MC-->>EV: Top 10 candidats
    
    EV->>MC: 4. POST /profile-swipe {userId, "LIKE"}
    MC->>DB: Save ProfileSwipe(ecole, etudiant, LIKE)
    DB-->>MC: OK
    MC->>MIS: recordAction(event, "LIKED")
    MC-->>EV: 201 Created
    
    E->>MC: 5. Détecte Match Mutuel
    MC->>DB: Check Swipe + ProfileSwipe bidirectionnels
    DB-->>MC: TRUE
    
    E->>MC2: 6. POST /messages {targetUserId, text}
    MC2->>MC2: isMutualMatch(userId, targetUserId)
    MC2-->>MC2: OK → Autorisé
    MC2->>DB: Save Message
    DB-->>MC2: 201 Created
    MC2-->>E: Message envoyé
```

## 7. Diagramme: Comparaison Baseline vs Pondéré

```mermaid
graph TD
    A["100 Offres"] -->|Baseline| B["Tri simple"]
    B -->|Domain+Sector+Level\nuniquement| C["Ordre 1-100"]
    C -->|40% aléatoire| D["User reçoit"]
    
    A -->|Pondéré v1| E["Scoring pondéré"]
    E -->|25% Domain\n+30% Objective\n+15% Sector\n+14% Keywords\n+8% Role| F["Normalisé 5-100"]
    F -->|Discrimination nette| G["Top 10"]
    
    D -->|Qualité prédictions| H["Like Rate ~30%\nScore moy liked: 40\nVariance: 40%"]
    G -->|Qualité prédictions| I["Like Rate >50%\nScore moy liked: 70+\nVariance: <20%"]
    
    H -->|KPI| J["Baseline: Faible\npertinence"]
    I -->|KPI| K["v1: Forte\npertinence"]
```
