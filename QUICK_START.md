# 🚀 REZO Backend — Quick Start for Soutenance

## ⏱️ 60 Seconds Overview

**What?** REZO backend with intelligent matching algorithm v1 + KPI instrumentation.  
**Status?** ✅ 66/66 tests passing, production-ready.  
**For What?** Thesis presentation on matching algorithm for student-job matching platform.

---

## 📦 What You Get

### 1. **Matching Algorithm v1** (pondéré)
- Scores opportunities using 5-criteria formula
- Returns explicability reasons for each match
- Supports 4 roles: ETUDIANT, LYCEEN, ECOLE, ENTREPRISE

### 2. **KPI Instrumentation**
- Captures all recommendation events
- Tracks user actions (LIKE/DISLIKE)
- Exports data for analysis

### 3. **Complete Documentation**
- Algorithm math formula + weights
- Architecture diagrams (7 Mermaid diagrams)
- Baseline comparison
- Python analysis script

### 4. **Security**
- JWT authentication + Refresh Token
- Role-based access control (5 roles)
- Ownership validation on all sensitive operations
- 401/403 status codes validated

---

## 🏃 Quick Run

### Clone & Setup
```bash
cd C:\dev\backend

# Compile
.\mvnw.cmd clean compile

# Run all 66 tests (35 sec)
.\mvnw.cmd clean test

# Expected: BUILD SUCCESS, Tests run: 66, Failures: 0
```

### Run Specific Tests
```bash
# KPI tests only
.\mvnw.cmd "-Dtest=MatchingInstrumentationServiceTest" test

# Integration tests only
.\mvnw.cmd "-Dtest=*ApiIntegrationTest" test

# Auth tests only
.\mvnw.cmd "-Dtest=*AuthController*" test
```

### Start Backend (Dev)
```bash
.\mvnw.cmd spring-boot:run
# API available at http://localhost:8080
```

---

## 📊 KPI Analysis

### Export Events
```bash
# Get real-time KPI metrics
curl http://localhost:8080/api/kpi/matching

# Export CSV file
curl http://localhost:8080/api/kpi/matching/events-csv > events.csv
```

### Analyze with Python
```bash
# Demo mode (simulated data)
python3 scripts/analyze_kpi.py --demo

# Real data from CSV
python3 scripts/analyze_kpi.py --csv events.csv --html report.html
```

---

## 📚 Documentation

### Must Read
1. **[SOUTENANCE_SUMMARY.md](SOUTENANCE_SUMMARY.md)** ← Start here! (Executive summary)
2. **[MATCHING_ALGORITHM.md](docs/MATCHING_ALGORITHM.md)** ← Detailed algorithm + formulas
3. **[ARCHITECTURE_DIAGRAMS.md](docs/ARCHITECTURE_DIAGRAMS.md)** ← System architecture

### Optional
- [TODO.md](../TODO.md) - Full project status
- [analyze_kpi.py](scripts/analyze_kpi.py) - Metric analysis script
- [verify_soutenance_ready.sh](scripts/verify_soutenance_ready.sh) - Verification script

---

## 🧪 Test Suites

### Core Security (40 unit tests)
```bash
AuthControllerLoginTest             (8 tests)  - Login/logout workflows
AuthControllerSignupTest            (5 tests)  - Signup validation
JwtSecurityTest                     (5 tests)  - JWT token validation
JwtServiceTest                      (3 tests)  - Token generation/refresh
MessageControllerOwnershipTest      (3 tests)  - Message ownership 403
OfferControllerOwnershipTest        (8 tests)  - Offer ownership 403
OfferControllerTest                 (8 tests)  - Offer CRUD
```

### API Integration (20 tests)
```bash
AuthApiIntegrationTest              (1 test)   - Full auth flow + token rotation
OfferApiIntegrationTest             (2 tests)  - 401 + 403
MessageApiIntegrationTest           (4 tests)  - 401 + 403 + mutual match
SecurityApiIntegrationTest          (1 test)   - Role-based 403
CompanyApiIntegrationTest           (2 tests)  - 401 + 403
SchoolApiIntegrationTest            (2 tests)  - 401 + 403
PackApiIntegrationTest              (2 tests)  - 401 + 403 admin-only
UserMediaApiIntegrationTest         (2 tests)  - 401 + 403 category
UserApiIntegrationTest              (1 test)   - 401 on /api/users/me
ChatApiIntegrationTest              (1 test)   - 401 on chat
FeatureAccessApiIntegrationTest     (1 test)   - 401 on features
MatchApiIntegrationTest             (1 test)   - 401 on match endpoints
```

### KPI & Instrumentation (6 tests)
```bash
MatchingInstrumentationServiceTest  (6 tests)  - KPI events, export, metrics
```

---

## 🎯 Matching Algorithm Quick Facts

### Formula (v1)
```
Score = min(100, max(5, (
  0.25 × domain_similarity +
  0.30 × objective_match +
  0.15 × sector_match +
  0.14 × keyword_match +
  0.08 × role_bonus
) × 100 / 0.92))
```

### Expected Results vs Baseline
| Metric | Baseline | v1 | Gain |
|--------|----------|----|----|
| Like Rate | 30% | 50%+ | **+67%** |
| Score (LIKED) | 40 | 70+ | **+75%** |
| Discrimination | -2 | 46+ | **+2400%** |

### Explicability Example
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

---

## 🔑 Key Endpoints

### Authentication
- `POST /api/auth/signup` - Create account (ETUDIANT, LYCEEN, ECOLE, ENTREPRISE)
- `POST /api/auth/login` - Get JWT tokens
- `POST /api/auth/refresh` - Refresh access token
- `POST /api/auth/logout` - Invalidate refresh token

### Matching
- `GET /api/match/recommendations` - Get offer recommendations (ETUDIANT/LYCEEN)
- `GET /api/match/school-recommendations` - Get school recommendations (LYCEEN)
- `GET /api/match/profile-recommendations` - Get candidate profiles (ECOLE/ENTREPRISE)
- `POST /api/match/swipe` - Like/dislike offer
- `POST /api/match/profile-swipe` - Like/dislike candidate
- `GET /api/match/mutual` - Get mutual matches

### Messaging
- `POST /api/messages` - Send message (401 if not auth, 403 if no match)
- `GET /api/messages` - List messages (paginé)
- `GET /api/messages/conversation/{userId}` - Get conversation

### KPI
- `GET /api/kpi/matching` - Real-time KPI metrics
- `GET /api/kpi/matching/events-csv` - Export CSV

---

## ⚠️ Important Notes

### Security Rules (Validated by Tests)
- ✅ All authenticated endpoints return **401 Unauthorized** when no JWT
- ✅ All ownership-sensitive endpoints return **403 Forbidden** on non-owner
- ✅ Message endpoint returns **403** if no mutual match
- ✅ Admin endpoints return **403** for non-ADMIN roles

### Test Data
- Created via direct repository (not signup) to bypass email validation
- Fixtures include users of all 4 roles
- Profiles with competencies/sectors/objectives predefined

### Limitations (v1)
- KPI events are in-memory (10K max)
- Weights fixed (no ML adjustment)
- Matching detected ad-hoc (no persistence)
- No social signals integration

---

## 🎓 For Presentation

### Show These First
1. Run tests: `.\mvnw.cmd clean test` → **66/66 PASSING**
2. Explain algorithm from **MATCHING_ALGORITHM.md** (formulas + examples)
3. Show diagrams from **ARCHITECTURE_DIAGRAMS.md** (7 visualizations)

### Then Demo
1. Start backend: `.\mvnw.cmd spring-boot:run`
2. Signup user via Postman/curl
3. Fetch recommendations: `GET /api/match/recommendations`
4. Show explicability reasons in response

### Prove It Works
1. Export KPI CSV: `curl .../events-csv > data.csv`
2. Run analysis: `python3 analyze_kpi.py --csv data.csv`
3. Show comparison table: Baseline vs v1 KPI gain

---

## 📞 Troubleshooting

### Build Fails
```bash
# Clean everything and rebuild
.\mvnw.cmd clean package -DskipTests

# Check Java version
java -version  # Should be 21+
```

### Tests Fail
```bash
# Run single test for details
.\mvnw.cmd "-Dtest=MatchingInstrumentationServiceTest#testRecordRecommendation" test

# Check database (H2 in-memory used in tests)
# No setup needed, should auto-initialize
```

### Backend Won't Start
```bash
# Check port 8080 is free
netstat -ano | findstr 8080

# Or use different port
java -jar target/rezo-*.jar --server.port=9090
```

---

## 🎊 Success Criteria

You're done when:
- ✅ All 66 tests pass (`mvn clean test`)
- ✅ Backend starts without errors (`mvn spring-boot:run`)
- ✅ Can export KPI CSV (`curl .../events-csv`)
- ✅ Python script runs (`python3 analyze_kpi.py --demo`)
- ✅ Read all 3 docs (SOUTENANCE_SUMMARY, ALGORITHM, DIAGRAMS)

---

## 📖 Next Steps

1. **Read** → SOUTENANCE_SUMMARY.md (5 min)
2. **Read** → MATCHING_ALGORITHM.md (15 min)
3. **View** → ARCHITECTURE_DIAGRAMS.md (10 min)
4. **Run** → `mvn clean test` (35 sec)
5. **Demo** → Backend with Postman (optional)

---

**You're all set! Good luck with your soutenance! 🎊**

---

**Questions?** Check docs/ folder or view test files for implementation examples.
