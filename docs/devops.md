# DevOps — Docker only (no WSL / no VM)

Kubernetes is **skipped** for now (add later if time). Stack:

| Tool | URL | Default login |
|------|-----|----------------|
| Jenkins | http://localhost:8081 | admin / admin |
| SonarQube | http://localhost:9000 | admin / admin (change on first login) |
| Prometheus | http://localhost:9090 | — |
| Grafana | http://localhost:3000 | admin / admin |
| cAdvisor | http://localhost:8088 | — |

## Start

```bash
docker compose up -d
docker compose -f docker-compose.devops.yml up -d --build
```

---

## Connect Jenkins ↔ GitHub (push → auto pipeline)

### 1) Create the GitHub repository

On GitHub: **New repository** (private or public).

From the project root:

```bash
cd /home/mnaddari/Desktop/pi
git init
git add .
git commit -m "Initial training platform with Docker DevOps"
git branch -M main
git remote add origin https://github.com/<YOUR_USER>/<YOUR_REPO>.git
git push -u origin main
```

Do **not** commit `.env` (already gitignored — contains SMTP password).

### 2) GitHub credentials in Jenkins

1. GitHub → **Settings → Developer settings → Personal access tokens**  
   Create a token (classic) with `repo` scope (or fine-grained: Contents + Metadata read).
2. Jenkins → **Manage Jenkins → Credentials → Global → Add credentials**
   - Kind: **Username with password** (username = GitHub user, password = **token**)
   - ID: `github-credentials`  ← must match this id
3. (Optional) Sonar token as Secret text id `sonar-token`

### 3) Point Jenkins at the repo

In `.env` (or export before compose):

```env
GITHUB_REPO_URL=https://github.com/<YOUR_USER>/<YOUR_REPO>.git
GITHUB_BRANCH=main
GITHUB_CREDENTIALS_ID=github-credentials
```

Recreate Jenkins so the job is bound to GitHub SCM:

```bash
docker compose -f docker-compose.devops.yml up -d --build --force-recreate jenkins
```

If the job already existed from the old local mode:

- Delete job **training-platform-ci** in Jenkins UI, then recreate container as above  
  **or** configure the job manually:  
  **Pipeline → Definition: Pipeline script from SCM → Git** → your repo URL + credential + branch `main` + Script path `Jenkinsfile`  
  Check **GitHub hook trigger for GITScm polling**.

### 4) Auto-run on push

**Option A — Webhook (real “on push”, needs public URL)**

GitHub cannot reach `localhost`. Use a tunnel, e.g. Cloudflare Tunnel / ngrok:

```bash
# example with ngrok
ngrok http 8081
```

Then GitHub repo → **Settings → Webhooks → Add webhook**:

- Payload URL: `https://<your-tunnel>/github-webhook/`
- Content type: `application/json`
- Events: **Just the push event**

**Option B — Poll SCM (easiest on a laptop, no tunnel)**

Job → **Configure → Build Triggers** → **Poll SCM**:

```text
H/2 * * * *
```

(every ~2 minutes). Good enough for demos.

### 5) Test

```bash
# make a tiny change, then:
git add -A && git commit -m "ci: trigger jenkins" && git push
```

Watch **training-platform-ci** in Jenkins — it should start (webhook or after poll).

---

## Sonar token

1. SonarQube → change password → **My Account → Security → Generate Token**
2. Jenkins credential Secret text id: `sonar-token`
3. Rebuild

## Prometheus / Grafana

- Metrics: http://localhost:8080/actuator/prometheus  
- Grafana dashboard: **Training Platform Backend**

```bash
docker compose up -d --build backend
```

## Stop DevOps only

```bash
docker compose -f docker-compose.devops.yml down
```

## Kubernetes (later)

Not included. Compose above is the fast path for the defense.
