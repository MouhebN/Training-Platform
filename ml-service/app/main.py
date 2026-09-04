"""FastAPI MLA service: profile analysis → formation suggestions."""

from __future__ import annotations

import csv
import json
from pathlib import Path
from typing import Any

import joblib
import pandas as pd
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from app.features import FEATURE_COLUMNS, build_feature_row, reasons_from_features

ROOT = Path(__file__).resolve().parents[1]
MODEL_PATH = ROOT / "models" / "suggestion_model.pkl"
METRICS_PATH = ROOT / "models" / "metrics.json"
DATASET_PATH = ROOT / "data" / "formations_suggestions.csv"

app = FastAPI(
    title="Training Platform MLA Service",
    description="Profile analysis for formation suggestions (sujet 4 MLA)",
    version="1.0.0",
)

_artifact: dict[str, Any] | None = None


class ProfilePayload(BaseModel):
    level: str = "BEGINNER"
    skills: list[str] = Field(default_factory=list)
    goals: str = ""


class FormationPayload(BaseModel):
    id: int
    title: str
    level: str = "BEGINNER"
    requiredSkills: list[str] = Field(default_factory=list)
    category: str = ""


class SuggestRequest(BaseModel):
    profile: ProfilePayload
    formations: list[FormationPayload]


class SuggestionItem(BaseModel):
    formationId: int
    score: float
    reasons: list[str]


class SuggestResponse(BaseModel):
    source: str = "mla-python"
    modelVersion: str = "1.0.0"
    suggestions: list[SuggestionItem]


def load_model() -> dict[str, Any]:
    global _artifact
    if _artifact is not None:
        return _artifact
    if not MODEL_PATH.exists():
        raise HTTPException(
            status_code=503,
            detail=f"Model not found at {MODEL_PATH}. Run scripts/train.py first.",
        )
    _artifact = joblib.load(MODEL_PATH)
    return _artifact


def dataset_summary() -> dict[str, Any]:
    if not DATASET_PATH.exists():
        return {"exists": False, "rows": 0}
    with DATASET_PATH.open(encoding="utf-8") as fh:
        rows = list(csv.DictReader(fh))
    positives = sum(1 for row in rows if str(row.get("label", "")).strip() == "1")
    return {
        "exists": True,
        "path": str(DATASET_PATH),
        "rows": len(rows),
        "positiveLabels": positives,
        "negativeLabels": len(rows) - positives,
        "columns": list(rows[0].keys()) if rows else [],
    }


@app.on_event("startup")
def startup() -> None:
    if MODEL_PATH.exists():
        load_model()


@app.get("/health")
def health() -> dict[str, Any]:
    ready = MODEL_PATH.exists()
    return {
        "status": "ok" if ready else "degraded",
        "modelLoaded": ready,
        "modelPath": str(MODEL_PATH),
    }


@app.get("/metrics")
def metrics() -> dict[str, Any]:
    """Training evaluation metrics for jury demo."""
    if not METRICS_PATH.exists():
        raise HTTPException(status_code=404, detail="metrics.json missing. Run scripts/train.py first.")
    data = json.loads(METRICS_PATH.read_text(encoding="utf-8"))
    return {
        "algorithm": "RandomForestClassifier",
        "features": FEATURE_COLUMNS,
        "metrics": data,
        "modelPath": str(MODEL_PATH),
    }


@app.get("/pipeline")
def pipeline() -> dict[str, Any]:
    """Full MLA pipeline snapshot for demo/presentation."""
    model_info: dict[str, Any] = {"loaded": False}
    try:
        artifact = load_model()
        model = artifact["model"]
        model_info = {
            "loaded": True,
            "version": artifact.get("version", "1.0.0"),
            "algorithm": type(model).__name__,
            "nEstimators": getattr(model, "n_estimators", None),
            "featureImportances": {
                name: round(float(value), 4)
                for name, value in zip(FEATURE_COLUMNS, getattr(model, "feature_importances_", []))
            },
        }
    except HTTPException:
        pass

    metrics_data = None
    if METRICS_PATH.exists():
        metrics_data = json.loads(METRICS_PATH.read_text(encoding="utf-8"))

    return {
        "phases": [
            "1. Dataset generation (synthetic profile-formation pairs)",
            "2. Cleaning + feature engineering (profile analysis)",
            "3. Train/test split + RandomForest training",
            "4. Evaluation (accuracy, F1)",
            "5. Model serve via POST /suggest",
            "6. Spring Boot Improvement Plan integration",
        ],
        "dataset": dataset_summary(),
        "features": FEATURE_COLUMNS,
        "training": metrics_data,
        "model": model_info,
        "endpoints": {
            "health": "GET /health",
            "metrics": "GET /metrics",
            "pipeline": "GET /pipeline",
            "datasetSample": "GET /dataset/sample",
            "retrain": "POST /retrain",
            "suggest": "POST /suggest",
            "docs": "GET /docs",
        },
    }


@app.get("/dataset/sample")
def dataset_sample(limit: int = 10) -> dict[str, Any]:
    """Preview dataset rows for admin UI."""
    summary = dataset_summary()
    if not summary.get("exists"):
        raise HTTPException(status_code=404, detail="Dataset missing. Run scripts/generate_dataset.py first.")
    limit = max(1, min(limit, 50))
    with DATASET_PATH.open(encoding="utf-8") as fh:
        rows = list(csv.DictReader(fh))[:limit]
    return {"summary": summary, "sample": rows}


@app.post("/retrain")
def retrain() -> dict[str, Any]:
    """Regenerate features training from CSV and reload the in-memory model (demo)."""
    global _artifact
    import subprocess
    import sys

    if not DATASET_PATH.exists():
        generate = subprocess.run(
            [sys.executable, str(ROOT / "scripts" / "generate_dataset.py")],
            cwd=str(ROOT),
            capture_output=True,
            text=True,
            check=False,
        )
        if generate.returncode != 0:
            raise HTTPException(status_code=500, detail=f"Dataset generation failed: {generate.stderr}")

    train = subprocess.run(
        [sys.executable, str(ROOT / "scripts" / "train.py")],
        cwd=str(ROOT),
        capture_output=True,
        text=True,
        check=False,
    )
    if train.returncode != 0:
        raise HTTPException(status_code=500, detail=f"Training failed: {train.stderr or train.stdout}")

    _artifact = None
    load_model()
    metrics_data = json.loads(METRICS_PATH.read_text(encoding="utf-8")) if METRICS_PATH.exists() else {}
    return {
        "status": "trained",
        "algorithm": "RandomForestClassifier",
        "metrics": metrics_data,
        "trainLog": train.stdout[-2000:] if train.stdout else "",
        "pipeline": pipeline(),
    }


@app.post("/suggest", response_model=SuggestResponse)
def suggest(request: SuggestRequest) -> SuggestResponse:
    artifact = load_model()
    model = artifact["model"]
    version = artifact.get("version", "1.0.0")

    if not request.formations:
        return SuggestResponse(modelVersion=version, suggestions=[])

    feature_rows = []
    meta = []
    for formation in request.formations:
        features = build_feature_row(
            learner_level=request.profile.level,
            learner_skills=request.profile.skills,
            learner_goals=request.profile.goals,
            formation_level=formation.level,
            formation_skills=formation.requiredSkills,
            formation_title=formation.title,
            formation_category=formation.category,
        )
        feature_rows.append(features)
        meta.append((formation.id, features))

    X = pd.DataFrame(feature_rows)[FEATURE_COLUMNS]
    if hasattr(model, "predict_proba"):
        scores = model.predict_proba(X)[:, 1]
    else:
        scores = model.predict(X).astype(float)

    ranked = sorted(
        (
            SuggestionItem(
                formationId=formation_id,
                score=round(float(score), 4),
                reasons=reasons_from_features(features, float(score)),
            )
            for (formation_id, features), score in zip(meta, scores)
        ),
        key=lambda item: item.score,
        reverse=True,
    )
    return SuggestResponse(modelVersion=version, suggestions=ranked)
