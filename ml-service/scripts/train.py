#!/usr/bin/env python3
"""Train RandomForest on the synthetic suggestion dataset and save model.pkl."""

from __future__ import annotations

import json
import sys
from pathlib import Path

import joblib
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score, classification_report, f1_score
from sklearn.model_selection import train_test_split

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from app.features import FEATURE_COLUMNS, build_feature_row  # noqa: E402

DATA = ROOT / "data" / "formations_suggestions.csv"
MODEL_OUT = ROOT / "models" / "suggestion_model.pkl"
METRICS_OUT = ROOT / "models" / "metrics.json"


def dataframe_to_features(df: pd.DataFrame) -> pd.DataFrame:
    rows = []
    for _, row in df.iterrows():
        rows.append(
            build_feature_row(
                learner_level=row["learner_level"],
                learner_skills=row["learner_skills"],
                learner_goals=row["learner_goals"],
                formation_level=row["formation_level"],
                formation_skills=row["formation_skills"],
                formation_title=row["formation_title"],
                formation_category=row["formation_category"],
            )
        )
    return pd.DataFrame(rows)[FEATURE_COLUMNS]


def main() -> None:
    if not DATA.exists():
        raise SystemExit(f"Dataset missing: {DATA}. Run scripts/generate_dataset.py first.")

    df = pd.read_csv(DATA)
    df = df.dropna(subset=["learner_level", "formation_level", "label"])
    df["learner_skills"] = df["learner_skills"].fillna("")
    df["learner_goals"] = df["learner_goals"].fillna("")
    df["formation_skills"] = df["formation_skills"].fillna("")

    X = dataframe_to_features(df)
    y = df["label"].astype(int)

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )

    model = RandomForestClassifier(
        n_estimators=200,
        max_depth=8,
        min_samples_leaf=2,
        random_state=42,
        n_jobs=-1,
    )
    model.fit(X_train, y_train)

    y_pred = model.predict(X_test)
    accuracy = float(accuracy_score(y_test, y_pred))
    f1 = float(f1_score(y_test, y_pred))
    report = classification_report(y_test, y_pred, digits=3)

    print("=== MLA Formation Suggestion Model ===")
    print(f"Train size: {len(X_train)}  Test size: {len(X_test)}")
    print(f"Accuracy: {accuracy:.3f}")
    print(f"F1:       {f1:.3f}")
    print(report)

    MODEL_OUT.parent.mkdir(parents=True, exist_ok=True)
    artifact = {
        "model": model,
        "feature_columns": FEATURE_COLUMNS,
        "version": "1.0.0",
    }
    joblib.dump(artifact, MODEL_OUT)
    METRICS_OUT.write_text(
        json.dumps({"accuracy": accuracy, "f1": f1, "train_size": len(X_train), "test_size": len(X_test)}, indent=2),
        encoding="utf-8",
    )
    print(f"Saved model → {MODEL_OUT}")
    print(f"Saved metrics → {METRICS_OUT}")


if __name__ == "__main__":
    main()
