# Training Platform — MLA Service

Python FastAPI + scikit-learn service for **sujet 4**:  
*Analyse des profils pour suggestions des formations*.

## Pipeline

1. **Dataset** — synthetic profile↔formation pairs (`scripts/generate_dataset.py`)
2. **Train** — RandomForest on engineered features (`scripts/train.py`)
3. **Serve** — `POST /suggest` ranks formations for a learner profile
4. **Integrate** — Spring Boot Improvement Plan calls this service (Java fallback if down)

## Jury demo (phases)

See [docs/mla-jury-demo.md](../docs/mla-jury-demo.md).

Quick links while the service runs:

- http://127.0.0.1:8000/docs — Swagger  
- http://127.0.0.1:8000/pipeline — all phases snapshot  
- http://127.0.0.1:8000/metrics — accuracy / F1  

```bash
python scripts/generate_dataset.py
python scripts/train.py
uvicorn app.main:app --port 8000
```

## Setup

```bash
cd ml-service
python -m venv .venv
source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements.txt

python scripts/generate_dataset.py
python scripts/train.py
uvicorn app.main:app --reload --port 8000
```

Health: http://127.0.0.1:8000/health

## API

### `POST /suggest`

```json
{
  "profile": {
    "level": "BEGINNER",
    "skills": ["Java", "REST APIs"],
    "goals": "spring backend"
  },
  "formations": [
    {
      "id": 1,
      "title": "Spring Boot Fundamentals",
      "level": "BEGINNER",
      "requiredSkills": ["Java", "REST APIs", "PostgreSQL"],
      "category": "IT"
    }
  ]
}
```

Response: ranked `formationId`, `score` (0–1), `reasons[]`.

## Features used by the model

- skill_overlap_ratio
- missing_skills_count
- level_distance
- goal_match_score
- same_level

## Spring config

```yaml
app:
  ml:
    enabled: true
    base-url: http://127.0.0.1:8000
```
