# Training Platform

Full-stack training & certification platform (Spring Boot + Angular + PostgreSQL + MLA).

## Structure

```text
backend/      Spring Boot API
frontend/    Angular UI
ml-service/  Python MLA (FastAPI + sklearn)
docs/        Feature & demo docs
docker-compose.yml
```

## Quick start (Docker — recommended)

```bash
cp .env.example .env
# optional: set MAIL_* when the client gives SMTP credentials
docker compose up --build
```

| Service   | URL |
|-----------|-----|
| Frontend  | http://localhost:4200 |
| Backend   | http://localhost:8080 |
| Swagger   | http://localhost:8080/swagger-ui.html |
| MLA       | http://localhost:8000/docs |

## Demo accounts (seeded on first boot)

| Role    | Email | Password |
|---------|-------|----------|
| Admin   | admin@training.com | admin123 |
| Trainer | trainer.java@training.com | password |
| Trainer | trainer.angular@training.com | password |
| Trainer | trainer.management@training.com | password |
| Learner | learner.amine@training.com | password |
| Learner | learner.sara@training.com | password |
| Learner | learner.nour@training.com | password |
| Learner | learner.mehdi@training.com | password |

## DevOps (Jenkins + Sonar + Prometheus + Grafana)

All Docker — no WSL / no VM. **Kubernetes skipped** (optional later).

```bash
docker compose up -d
docker compose -f docker-compose.devops.yml up -d --build
```

Details: see local `docs/devops.md` (not pushed to GitHub).

| Tool | URL |
|------|-----|
| Jenkins | http://localhost:8081 |
| SonarQube | http://localhost:9000 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 |

## SMTP

Leave `MAIL_USERNAME` / `MAIL_PASSWORD` empty until the client provides SMTP.  
Set them in `.env` (used by docker compose) or export env vars for local runs.

## Notes

- DataSeeder creates catalogue, trainers, learners, availability, sessions, and enrollments when the DB is empty.
- Frontend in Docker is served by nginx and proxies `/api` + `/ws` to the backend.
