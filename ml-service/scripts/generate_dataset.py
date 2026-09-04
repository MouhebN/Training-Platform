#!/usr/bin/env python3
"""Generate a synthetic profile–formation suggestion dataset aligned to the platform catalogue."""

from __future__ import annotations

import csv
import random
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "data" / "formations_suggestions.csv"
RANDOM_SEED = 42
TARGET_ROWS = 1000

FORMATIONS = [
    {
        "formation_id": 1,
        "formation_title": "Spring Boot Fundamentals",
        "formation_level": "BEGINNER",
        "formation_skills": "Java|REST APIs|PostgreSQL",
        "formation_category": "IT",
        "goal_keywords": ["spring", "backend", "java", "api", "rest"],
    },
    {
        "formation_id": 2,
        "formation_title": "Advanced Spring Security",
        "formation_level": "ADVANCED",
        "formation_skills": "Java|Spring Boot|Spring Security|REST APIs",
        "formation_category": "Cybersecurity",
        "goal_keywords": ["security", "jwt", "spring", "auth"],
    },
    {
        "formation_id": 3,
        "formation_title": "Angular Essentials",
        "formation_level": "BEGINNER",
        "formation_skills": "Angular|TypeScript",
        "formation_category": "IT",
        "goal_keywords": ["angular", "frontend", "typescript", "ui"],
    },
    {
        "formation_id": 4,
        "formation_title": "Full Stack Java Angular",
        "formation_level": "INTERMEDIATE",
        "formation_skills": "Java|Spring Boot|Angular|TypeScript|REST APIs",
        "formation_category": "IT",
        "goal_keywords": ["full stack", "java", "angular", "fullstack"],
    },
    {
        "formation_id": 5,
        "formation_title": "Project Management Professional",
        "formation_level": "INTERMEDIATE",
        "formation_skills": "Project Management|Agile|Communication",
        "formation_category": "Management",
        "goal_keywords": ["project", "management", "agile", "scrum"],
    },
    {
        "formation_id": 6,
        "formation_title": "Business Communication",
        "formation_level": "INTERMEDIATE",
        "formation_skills": "Communication|Business English",
        "formation_category": "Management",
        "goal_keywords": ["communication", "business", "presentation"],
    },
    {
        "formation_id": 7,
        "formation_title": "Professional English for IT",
        "formation_level": "BEGINNER",
        "formation_skills": "Business English|Communication",
        "formation_category": "Languages",
        "goal_keywords": ["english", "it english", "language"],
    },
    {
        "formation_id": 8,
        "formation_title": "French Workplace Communication",
        "formation_level": "BEGINNER",
        "formation_skills": "French|Communication",
        "formation_category": "Languages",
        "goal_keywords": ["french", "francais", "language"],
    },
]

ALL_SKILLS = [
    "Java",
    "Spring Boot",
    "Spring Security",
    "REST APIs",
    "PostgreSQL",
    "Angular",
    "TypeScript",
    "Project Management",
    "Agile",
    "Communication",
    "Business English",
    "French",
]

LEVELS = ["BEGINNER", "INTERMEDIATE", "ADVANCED"]
LEVEL_IDX = {level: i for i, level in enumerate(LEVELS)}

COLUMNS = [
    "learner_level",
    "learner_skills",
    "learner_goals",
    "formation_id",
    "formation_title",
    "formation_level",
    "formation_skills",
    "formation_category",
    "label",
]


def skill_list(pipe: str) -> list[str]:
    return [s.strip() for s in pipe.split("|") if s.strip()]


def overlap_ratio(learner: list[str], required: list[str]) -> float:
    if not required:
        return 1.0
    ls = {s.lower() for s in learner}
    return sum(1 for s in required if s.lower() in ls) / len(required)


def make_positive_profile(formation: dict, rng: random.Random) -> tuple[str, str, str]:
    required = skill_list(formation["formation_skills"])
    keep = max(1, int(round(len(required) * rng.uniform(0.6, 1.0))))
    skills = required[:keep]
    # Add 0–2 related extras from same catalogue
    extras = [s for s in ALL_SKILLS if s not in skills]
    skills += rng.sample(extras, k=min(rng.randint(0, 2), len(extras)))
    level = formation["formation_level"]
    if rng.random() < 0.2 and LEVEL_IDX[level] > 0:
        level = LEVELS[LEVEL_IDX[level] - 1]
    goal = rng.choice(formation["goal_keywords"])
    if rng.random() < 0.3:
        goal = f"{goal} {rng.choice(['career', 'job', 'training'])}"
    return level, "|".join(skills), goal


def make_negative_profile(formation: dict, rng: random.Random) -> tuple[str, str, str]:
    required = {s.lower() for s in skill_list(formation["formation_skills"])}
    unrelated = [s for s in ALL_SKILLS if s.lower() not in required]
    skills = rng.sample(unrelated, k=min(rng.randint(1, 3), len(unrelated)))
    # Prefer much harder or wrong domain goals
    other = rng.choice([f for f in FORMATIONS if f["formation_id"] != formation["formation_id"]])
    if LEVEL_IDX[formation["formation_level"]] == 2:
        level = "BEGINNER"
    else:
        level = rng.choice([lv for lv in LEVELS if abs(LEVEL_IDX[lv] - LEVEL_IDX[formation["formation_level"]]) >= 1] or LEVELS)
    goal = rng.choice(other["goal_keywords"])
    return level, "|".join(skills), goal


def label_for(learner_level: str, learner_skills: str, learner_goals: str, formation: dict) -> int:
    required = skill_list(formation["formation_skills"])
    learner = skill_list(learner_skills)
    overlap = overlap_ratio(learner, required)
    level_gap = abs(LEVEL_IDX[learner_level] - LEVEL_IDX[formation["formation_level"]])
    goals = learner_goals.lower()
    goal_hit = any(k in goals for k in formation["goal_keywords"]) or formation["formation_category"].lower() in goals

    score = 0.0
    score += overlap * 0.5
    score += (0.25 if level_gap == 0 else 0.1 if level_gap == 1 else 0.0)
    score += 0.25 if goal_hit else 0.0
    return 1 if score >= 0.45 else 0


def main() -> None:
    rng = random.Random(RANDOM_SEED)
    rows: list[dict] = []

    # Balanced generation around each formation
    while len(rows) < TARGET_ROWS:
        formation = rng.choice(FORMATIONS)
        if rng.random() < 0.55:
            learner_level, learner_skills, learner_goals = make_positive_profile(formation, rng)
        else:
            learner_level, learner_skills, learner_goals = make_negative_profile(formation, rng)

        label = label_for(learner_level, learner_skills, learner_goals, formation)
        # Occasionally flip edge cases to keep noise realistic
        if rng.random() < 0.03:
            label = 1 - label

        rows.append(
            {
                "learner_level": learner_level,
                "learner_skills": learner_skills,
                "learner_goals": learner_goals,
                "formation_id": formation["formation_id"],
                "formation_title": formation["formation_title"],
                "formation_level": formation["formation_level"],
                "formation_skills": formation["formation_skills"],
                "formation_category": formation["formation_category"],
                "label": label,
            }
        )

    OUT.parent.mkdir(parents=True, exist_ok=True)
    with OUT.open("w", newline="", encoding="utf-8") as fh:
        writer = csv.DictWriter(fh, fieldnames=COLUMNS)
        writer.writeheader()
        writer.writerows(rows)

    positives = sum(1 for r in rows if r["label"] == 1)
    print(f"Wrote {len(rows)} rows to {OUT}")
    print(f"Positive labels: {positives} ({positives / len(rows):.1%})")
    print(f"Negative labels: {len(rows) - positives}")


if __name__ == "__main__":
    main()
