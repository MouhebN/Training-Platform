# MLA Jury Demo Script (≈ 5–7 min)

Show the **phases**, not only the Angular page.

## Admin UI (recommended for validation / DevOps demo)

After login as admin, open **MLA Center** (`/admin/mla`):

- Dataset size + sample rows  
- Pipeline phases  
- Accuracy / F1  
- Feature importance bars  
- **Train / retrain model** button (calls Python RandomForest)

Keep Python running (`uvicorn` on 8000). No terminal commands needed during the demo.

---

## Before demo

```bash
# Terminal A — Postgres (if needed)
docker start training-platform-postgres

# Terminal B — MLA
cd ml-service
source .venv/bin/activate
uvicorn app.main:app --port 8000

# Terminal C — Spring
mvn spring-boot:run

# Terminal D — Angular
cd frontend && npm start
```

Open in browser:
- App: http://localhost:4200  
- Admin MLA: http://localhost:4200/admin/mla  
- MLA Swagger (optional): http://127.0.0.1:8000/docs  
- Pipeline API: http://127.0.0.1:8000/pipeline  
- Metrics API: http://127.0.0.1:8000/metrics  

---

## Phase 1 — Dataset (1 min)

Say: *We built a synthetic labelled dataset of learner profile ↔ formation pairs.*

Show:

```bash
cd ml-service
wc -l data/formations_suggestions.csv
head -5 data/formations_suggestions.csv
```

Or open http://127.0.0.1:8000/pipeline → `dataset.rows` (~1000).

Explain columns: level, skills, goals, formation fields, `label` 0/1.

---

## Phase 2 — Features / profile analysis (1 min)

Say: *The model does not use raw text only — we engineer profile-analysis features.*

Open http://127.0.0.1:8000/pipeline → `features`:

- skill_overlap_ratio  
- missing_skills_count  
- level_distance  
- goal_match_score  
- same_level  

---

## Phase 3 — Training RandomForest (1–2 min)

Live retrain (optional but impressive):

```bash
cd ml-service
source .venv/bin/activate
python scripts/train.py
```

Show terminal output:

- Train/test size (800 / 200)  
- Accuracy (~0.96)  
- F1 (~0.97)  
- classification report  

Say: *Algorithm = RandomForestClassifier (scikit-learn).*

Then open http://127.0.0.1:8000/metrics

---

## Phase 4 — Serve + predict (1 min)

Swagger: http://127.0.0.1:8000/docs → `POST /suggest`

Example profile: Java + goals `spring backend`  
→ Spring Boot Fundamentals score high, French low.

Or open `/pipeline` → `model.featureImportances`.

---

## Phase 5 — Integration in the app (1–2 min)

1. Login as learner  
2. Open **Improvement plan**  
3. Show badge **MLA (Python model)**  
4. Show reasons with `(MLA)`  
5. Optional: stop uvicorn → refresh → badge becomes **Rules fallback**

---

## What to say (one sentence)

> Dataset → feature engineering (analyse du profil) → RandomForest train/evaluate → Python API → Spring Improvement Plan suggestions.

---

## Files to point at if asked

| Phase | File |
|---|---|
| Dataset | `ml-service/scripts/generate_dataset.py` |
| Train | `ml-service/scripts/train.py` |
| API | `ml-service/app/main.py` |
| Metrics | `ml-service/models/metrics.json` |
| Spring client | `MlSuggestionClient` + `LearnerIntelligenceService` |
