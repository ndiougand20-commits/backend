# 🎯 Algorithme de Matching REZO — Documentation Complète

## 1. Vue d'ensemble

Le matching REZO utilise une **approche pondérée multi-critères** (v1) pour recommander des opportunités aux candidats (ETUDIANT, LYCEEN) et réciproquement des profils aux recruteurs (ECOLE, ENTREPRISE).

**Objectif principal**: Maximiser la **pertinence perçue** des recommandations et le **taux de conversion match → conversation**.

---

## 2. Formule Mathématique (v1)

### 2.1 Scoring Offer → Candidate (Etudiant/Lyceen)

```
SCORE_OFFRE(user, offer) = 
    w_domain × similarity(user.domaines ∩ offer.domaines) +
    w_sector × similarity(user.secteur ⊆ offer.secteur) +
    w_objective × similarity(user.objectif ≈ offer.niveau_poste) +
    w_keywords × keyword_match(user.profile_text, offer.description) +
    w_role × role_bonus(user.role, offer.type)
```

### 2.2 Poids Actuels (v1)

| Critère | Poids | Justification |
|---------|-------|---------------|
| **Domain Match** (DOMAIN_MATCH_WEIGHT) | 25 | Compétences/domaines = critère principal (~40% de l'évaluation HR) |
| **Objective Match** (OBJECTIVE_MATCH_WEIGHT) | 30 | Alignement niveau d'étude/objectif carrière = priorité majeure |
| **Sector Match** (SECTOR_MATCH_WEIGHT) | 15 | Secteur d'activité = contrainte secondaire mais importante |
| **Keyword Match** (KEYWORD_MATCH_WEIGHT) | 14 | Mots-clés libres = signalisation fine (CV, compétences implicites) |
| **Role Bonus** (ROLE_BONUS_WEIGHT) | 8 | Bonus rôle ETUDIANT (intérêt pour alternance) vs LYCEEN (formation) |
| **TOTAL POIDS** | **92** | Score normalisé ensuite sur [0, 100] |

### 2.3 Formule Normalisée

```
SCORE_FINAL = min(100, max(5, (SCORE_BRUT / 92) × 100))
```

- **Min 5** : Évite de rejeter des candidatures avec score très bas (peut y avoir du bruit)
- **Max 100** : Seuil plafond pour cohérence UX
- **Résultat** : Score dans l'intervalle [5, 100]

---

## 3. Détail de Chaque Critère

### 3.1 Domain Match (25 pts)

**Calcul**: Intersection entre domaines du profil user et domaines requis par l'offre.

```
domain_match = count(user.domaines ∩ offer.domaines)
similarity = min(1.0, domain_match / max(|user.domaines|, |offer.domaines|))
contribution = similarity × 25
```

**Exemple**:
- User: `["Python", "Java", "SQL", "React"]` (4 domaines)
- Offer: `["Java", "Spring", "SQL", "PostgreSQL"]` (4 domaines)
- Intersection: `["Java", "SQL"]` (2 domaines)
- Similarité: `2 / 4 = 0.5` (50%)
- Contribution: `0.5 × 25 = 12.5 pts`

**Raison**: Les HR évaluent ~40% des candidatures sur les compétences techniques.

---

### 3.2 Objective Match (30 pts)

**Calcul**: Alignement entre l'objectif d'études/carrière du candidat et le niveau/poste proposé.

```
if user.objectif_postbac matches offer.niveau_etude:
    contribution = 30 pts (100% match)
else if related(user.objectif, offer.niveau):
    contribution = 15 pts (50% match)
else:
    contribution = 0 pts (mismatch)
```

**Exemple**:
- LYCEEN.objectif_postbac = "Licence Informatique" 
- OFFER.niveau_etude = "BAC+1/+2 (License)" ✓ Match complet → **30 pts**

- ETUDIANT.objectif = "Stage Data Science"
- OFFER.type = "Poste CDI" ✗ Mismatch → **0 pts**

**Raison**: L'alignement carrière est le facteur **#1 de satisfaction** post-hire.

---

### 3.3 Sector Match (15 pts)

**Calcul**: Présence du secteur d'activité de l'offre dans les préférences sectorielles du candidat.

```
sector_match = 1 if offer.secteur ∈ user.secteurs_preferences else 0
contribution = sector_match × 15
```

**Exemple**:
- User: `["Tech", "Finance", "Santé"]`
- Offer.secteur: `"Tech"` ✓ Match → **15 pts**
- Offer.secteur: `"Retail"` ✗ Pas dans préférences → **0 pts**

**Raison**: Filtrage rapide pour exclure secteurs hors-intérêt.

---

### 3.4 Keyword Match (14 pts)

**Calcul**: Présence de mots-clés du profile du candidat dans la description de l'offre (et vice-versa).

```
keywords_found = count(user_keywords ∩ offer_keywords)
match_ratio = min(1.0, keywords_found / 5) // Normalisation sur 5 mots-clés typiques
contribution = match_ratio × 14
```

**Exemple**:
- User profile keywords (CV): `["machine learning", "python", "big data", "aws", "agile"]`
- Offer description keywords: `["machine learning", "python", "mlops", "aws"]`
- Keywords trouvés: `4 / 5 → 0.8` match
- Contribution: `0.8 × 14 = 11.2 pts`

**Raison**: Capture les signaleurs implicites (technos, méthodologies) au-delà des "domaines" structurés.

---

### 3.5 Role Bonus (8 pts)

**Calcul**: Bonus appliqué selon le rôle du candidat et le type d'offre.

```
if user.role == ETUDIANT && offer.type ∈ [ALTERNANCE, STAGE]:
    contribution = 8 pts (rôle aligné)
else if user.role == LYCEEN && offer.type == FORMATION:
    contribution = 8 pts (lycéen = formation)
else:
    contribution = 4 pts (bonus partiel pour autres alignements)
```

**Raison**: Encourage les matchs rôle-à-rôle (LYCEEN → formation école, ETUDIANT → stage/alternance).

---

## 4. Matching Réciproque: Recruiter → Candidate Profile

### 4.1 Cas d'usage

- **ENTREPRISE** parcourt les profils ETUDIANT pour recruter
- **ECOLE** parcourt les profils LYCEEN pour les admettre

### 4.2 Formule Symétrique

```
SCORE_PROFILE(recruiter, candidate) =
    w_domain × similarity(recruiter.domaines_requis ∩ candidate.domaines) +
    w_objective × similarity(candidate.objectif ⊆ recruiter.postes_proposes) +
    w_keywords × keyword_match(candidate.cv, recruiter.description_role) +
    w_role × role_bonus(recruiter.role, candidate.role)
```

**Poids identiques** (v1) — à affiner après collecte KPI.

---

## 5. Explicabilité & Raisons

Chaque score retourne une liste de **raisons courtes** pour aider le candidat à comprendre la pertinence:

```json
{
  "score": 78,
  "reasons": [
    "✓ 3/4 compétences requises (Python, SQL, React)",
    "✓ Objectif carrière aligné (Stage Data Science)",
    "⚠ Secteur non dans vos préférences (Retail vs Tech/Finance)",
    "✓ Mots-clés pertinents trouvés (AWS, agile)"
  ]
}
```

**Bénéfice**: Augmente la **confiance** dans l'algorithme et facilite le **refinement** des profils.

---

## 6. Comparaison Baseline vs Pondéré

### 6.1 Baseline (Non-pondéré): Score Brut

```
BASELINE_SCORE(user, offer) = 
    (count(user.domaines ∩ offer.domaines)) × 20 +
    (user.secteur == offer.secteur ? 20 : 0) +
    (user.objectif == offer.niveau ? 20 : 0) +
    (random_bonus ∈ [0, 40])
```

**Résultat**: Ordre aléatoire au-delà des 3 critères, 40% de variance.

### 6.2 Pondéré (v1): Notre approche

- **Pondération** : Répartition intelligente des poids selon l'importance HR
- **Keyword matching** : Capture des signaleurs implicites
- **Role bonus** : Encouragement alignement rôle
- **Normalization** : Score cohérent [5, 100]

### 6.3 Résultats Attendus

| Métrique | Baseline | Pondéré (v1) |
|----------|----------|--------------|
| Taux Like moyen | ~30% | **~50%+** (hypothèse) |
| Score avg (liked) | 40 | **70+** |
| Score avg (disliked) | 42 | **35-45** (discrimination) |
| Temps réponse | ~2 min | **~1 min** (plus décisif) |

---

## 7. Limitations de v1

### 7.1 Connues

1. **Poids fixes**: Pas d'apprentissage dynamique → à adapter après KPI
2. **Pas de ML**: Pas d'embeddings, NLP, ou prédiction
3. **Pas de contexte tempore**l: Ne tient pas compte de "offre publiée il y a 6 mois"
4. **Pas de feedback loop**: Score ne se met pas à jour selon actions user
5. **Pas d'explicabilité avancée**: Raisons courtes, pas d'analyse causale
6. **Peu de signaleurs sociaux**: Pas de "recruiter X aime Y candidats"

### 7.2 Stratégies de Mitigation (v1)

- ✅ Threshold minimal (5 pts) pour éviter "trop mauvaises" suggestions
- ✅ Top-10 tri par score pour meilleures propositions visibles
- ✅ Exclusion automatique offres swipées (sauf override)
- ✅ Raisons détaillées pour aider utilisateur à refiner son profil

---

## 8. Perspectives: Vers v2 et v3

### 8.1 v2 (Court terme: 1-2 mois)

```
Ajouts proposés:
- ML embeddings (doc2vec) pour matching texte fin
- Feedback loop: L'algorithme ajuste poids selon taux LIKE/DISLIKE
- A/B testing: Comparer v1 vs v2 sur cohort
- Temporal decay: Offres "anciennes" moins bien rankées
```

### 8.2 v3+ (Long terme)

```
- GraphML: Social graph (recruiter A recrute souvent Y domaine)
- Causal ML: Identifier vrais drivers de match vs corrélations
- Explainable AI: Utiliser LIME/SHAP pour raisons détaillées
- Multi-modal: Intégrer vidéos CV, tone of voice, etc.
- Cold start: Recommandations pour profiles/offres neuves
```

---

## 9. Instrumentation & Mesure

### 9.1 Événements Capturés

```
MatchingEvent = {
  eventId: UUID,
  userId: UUID,
  userRole: "ETUDIANT" | "LYCEEN" | "ECOLE" | "ENTREPRISE",
  targetId: UUID,
  targetType: "OFFER" | "SCHOOL" | "PROFILE",
  score: 5-100,
  reasons: ["✓ Domain match", ...],
  action: "VIEWED" | "LIKED" | "DISLIKED" | "SKIPPED",
  createdAt: LocalDateTime,
  actionAt: LocalDateTime,
  responseTimeMs: Long
}
```

### 9.2 KPI Principaux

| KPI | Calcul | Cible |
|-----|--------|-------|
| **Like Rate** | `count(action=LIKED) / count(recommendations)` | >40% |
| **Action Rate** | `(count(LIKED) + count(DISLIKED)) / count(recommendations)` | >60% |
| **Avg Score Liked** | `mean(score | action=LIKED)` | >70 |
| **Avg Score Disliked** | `mean(score | action=DISLIKED)` | <45 |
| **Response Time** | `mean(actionAt - createdAt)` | <2 min |
| **Match Rate** | `count(mutual_swipes) / count(likes)` | >30% |
| **Msg Rate** | `count(conversations) / count(matches)` | >80% |

### 9.3 Queries Export

**CSV**: Via endpoint `GET /api/kpi/matching/events-csv`

```sql
SELECT 
  eventId, userId, userRole, targetId, targetType, 
  score, action, createdAt, actionAt, responseTimeMs
FROM matching_events
WHERE createdAt >= '2026-06-01'
ORDER BY createdAt DESC
```

**Analyse Notebook** (Python):
```python
import pandas as pd
df = pd.read_csv('matching-events.csv')
print(f"Like Rate: {(df['action']=='LIKED').sum() / len(df) * 100:.1f}%")
print(f"Avg Score (LIKED): {df[df['action']=='LIKED']['score'].mean():.1f}")
print(f"Discrimination: {df[df['action']=='DISLIKED']['score'].mean():.1f}")
```

---

## 10. Conclusion

Le matching REZO v1 est une **approche pondérée simple mais efficace**, conçue pour:
- ✅ Maximiser la pertinence perçue (raisons explicitantes)
- ✅ Faciliter l'évolution (poids paramétrables)
- ✅ Permettre la mesure et l'itération (instrumentation)
- ✅ Supporter les 4 rôles (ETUDIANT, LYCEEN, ECOLE, ENTREPRISE)

**Mesure sera le juge**: Les KPI collectés en production déterminera v2.
