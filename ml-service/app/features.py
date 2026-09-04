"""Shared feature engineering for profile–formation matching (MLA)."""

from __future__ import annotations

import re
import unicodedata
from typing import Iterable

LEVEL_MAP = {"BEGINNER": 0, "INTERMEDIATE": 1, "ADVANCED": 2}

FEATURE_COLUMNS = [
    "skill_overlap_ratio",
    "missing_skills_count",
    "level_distance",
    "goal_match_score",
    "same_level",
]


def normalize_text(value: str | None) -> str:
    if not value:
        return ""
    text = unicodedata.normalize("NFD", str(value))
    text = "".join(ch for ch in text if unicodedata.category(ch) != "Mn")
    text = text.strip().lower()
    text = text.replace("frensh", "french").replace("francais", "french")
    return text


def parse_skills(value: str | Iterable[str] | None) -> list[str]:
    if value is None:
        return []
    if isinstance(value, str):
        parts = re.split(r"[|,;]+", value)
        return [normalize_text(p) for p in parts if normalize_text(p)]
    return [normalize_text(str(p)) for p in value if normalize_text(str(p))]


def level_value(level: str | None) -> int:
    if not level:
        return 0
    return LEVEL_MAP.get(str(level).strip().upper(), 0)


def tokenize(value: str) -> set[str]:
    return {tok for tok in re.split(r"\W+", normalize_text(value)) if len(tok) >= 3}


def skill_overlap_ratio(learner_skills: list[str], formation_skills: list[str]) -> float:
    if not formation_skills:
        return 1.0
    learner = set(learner_skills)
    matched = sum(1 for skill in formation_skills if skill in learner)
    return matched / len(formation_skills)


def missing_skills_count(learner_skills: list[str], formation_skills: list[str]) -> int:
    learner = set(learner_skills)
    return sum(1 for skill in formation_skills if skill not in learner)


def goal_match_score(goals: str, title: str, category: str, formation_skills: list[str]) -> float:
    goal_tokens = tokenize(goals)
    if not goal_tokens:
        return 0.0
    corpus_tokens = tokenize(title) | tokenize(category)
    for skill in formation_skills:
        corpus_tokens |= tokenize(skill)
    if not corpus_tokens:
        return 0.0
    overlap = len(goal_tokens & corpus_tokens)
    return overlap / max(len(goal_tokens), 1)


def build_feature_row(
    learner_level: str,
    learner_skills: list[str] | str,
    learner_goals: str,
    formation_level: str,
    formation_skills: list[str] | str,
    formation_title: str,
    formation_category: str,
) -> dict[str, float]:
    l_skills = parse_skills(learner_skills)
    f_skills = parse_skills(formation_skills)
    l_level = level_value(learner_level)
    f_level = level_value(formation_level)
    return {
        "skill_overlap_ratio": skill_overlap_ratio(l_skills, f_skills),
        "missing_skills_count": float(missing_skills_count(l_skills, f_skills)),
        "level_distance": float(abs(l_level - f_level)),
        "goal_match_score": goal_match_score(learner_goals, formation_title, formation_category, f_skills),
        "same_level": 1.0 if l_level == f_level else 0.0,
    }


def reasons_from_features(features: dict[str, float], score: float) -> list[str]:
    reasons: list[str] = []
    if features["goal_match_score"] >= 0.25:
        reasons.append("Matches your learning goals (MLA).")
    if features["skill_overlap_ratio"] >= 0.6:
        reasons.append("Strong skill match with this formation (MLA).")
    elif features["missing_skills_count"] > 0:
        reasons.append("Helps close skill gaps (MLA).")
    if features["same_level"] >= 1.0:
        reasons.append("Aligned with your current level (MLA).")
    elif features["level_distance"] >= 2:
        reasons.append("Level gap is large — consider prerequisites (MLA).")
    if score >= 0.75:
        reasons.append("High MLA recommendation score.")
    elif score >= 0.5:
        reasons.append("Moderate MLA recommendation score.")
    if not reasons:
        reasons.append("Ranked by MLA profile–formation model.")
    return reasons
