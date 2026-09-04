# Training Platform — Client Feature Guide & Demo Scenario

Hand this document to anyone who needs to understand and demo the app (client / teacher / jury).

**Stack:** Spring Boot + Angular + PostgreSQL + JWT + WebSocket + Jitsi  
**Frontend:** http://localhost:4200  
**Backend:** http://localhost:8080  

---

## Demo accounts

| Role | Email | Password |
| --- | --- | --- |
| Admin | `admin@training.com` | `admin123` |
| Trainer | `trainer.java@training.com` | `password` |
| Learner | `learner.amine@training.com` | `password` |
| Learner | `learner.sara@training.com` | `password` |

Use **two browsers** (or one normal + one private window) for live classroom / chat demos.

---

## How to present the features (recommended story)

Present them as **modules**, not as 15 separate “AI” features:

1. **Core platform** — catalogue, sessions, enrollments, users  
2. **Learner intelligence** — profile score, skill gap, improvement plan, learning path  
3. **Smart planning** — conflicts, trainer suggestions, workload  
4. **Live session layer** — chat, virtual classroom, auto attendance  
5. **Admin decision support** — intelligence center, alerts, recommended actions  

---

# Part A — Full feature catalogue

## A1. Core / security features

| # | Feature | What the user sees | How it works | Main files |
| ---: | --- | --- | --- | --- |
| 1 | **JWT login & roles** | Separate spaces for Admin / Trainer / Learner | Login returns JWT; Angular stores it and sends `Authorization: Bearer …`; Spring Security + role guards block unauthorized routes | `AuthService.java`, `Jwt*`, `auth.guard.ts`, `role.guard.ts`, `app.routes.ts` |
| 2 | **Account security** | Forgot/reset password, change password, lock after failed logins, admin unlock/activate/deactivate | Failed logins increment counter; after 5 failures account locks; admin can unlock; password reset via SMTP token | `AuthService.java`, `User.java`, `UserAccountService.java`, `AdminUserController.java`, auth Angular pages |
| 3 | **Categories & formations catalogue** | Admin CRUD + learner catalogue with filters | Formations belong to a category, have level/price/duration/`sessionCount`, required skills, chapters | `Formation*`, `Category*`, `Chapter*`, admin/learner Angular pages |
| 4 | **Trainer profiles & availability** | Admin creates trainers; trainers set weekly slots | Trainer = user + profile + expertise + weekly availability used later by planning | `TrainerService.java`, `TrainerAvailability*`, `availability.component.ts` |
| 5 | **Sessions lifecycle** | Admin plans sessions; trainer starts/completes; capacity & online/onsite | Status flow: `PLANNED` → `OPEN` → `IN_PROGRESS` → `COMPLETED` / `CANCELLED`; formation `sessionCount` limits how many non-cancelled sessions can exist | `TrainingSessionService.java`, `Formation.sessionCount`, admin `sessions.component.ts`, trainer `my-sessions.component.ts` |
| 6 | **Enrollments + waitlist** | Learner enrolls; if full → waitlisted; cancel promotes next | Capacity check; cancel frees a seat and auto-promotes oldest waitlisted learner + notification/email | `EnrollmentService.java`, `EnrollmentStatus`, enrollments UI |
| 7 | **Notifications** | Bell / list of events | In-app notifications for enroll, cancel, session start/reschedule, waitlist promotion; email for some events | `NotificationService.java`, `EmailService.java`, notification frontend service |
| 8 | **User admin** | List users, activate/deactivate, unlock, delete | Account lifecycle admin tools (security/admin, not “intelligence”) | `AdminUserController.java`, `users.component.ts` |

## A2. Advanced métier features

| # | Feature | What the user sees | How it works (logic) | Main files |
| ---: | --- | --- | --- | --- |
| 9 | **Profile completion score** | Score /100 + missing fields | Weighted fields: name, phone, bio, level, skills, goals | `LearnerIntelligenceService.profileScore`, learner dashboard/profile |
| 10 | **Skill gap analysis** | Match % + missing skills for one formation | Compare learner skills vs formation required skills; ready if match ≥ 60% | `LearnerIntelligenceService.skillGap`, formation detail page |
| 11 | **Improvement plan** | Ranked formation suggestions (HIGH/MEDIUM/LOW/DONE) | Uses goals, level, skill match, open sessions; typo-tolerant goal matching | `LearnerIntelligenceService.improvementPlan`, `improvement-plan.component.ts` |
| 12 | **Learning path generator** | Ordered path with statuses: COMPLETED / IN_PROGRESS / RECOMMENDED_NEXT / LOCKED / AVAILABLE | Scores all active formations, builds personal order, picks next step; progress linked to formation session completion | `LearningPathService.java`, `learning-path.component.ts` |
| 13 | **Formation progress (séances)** | X% complete after finishing sessions | Progress = completed sessions / planned `sessionCount`; skills granted only at 100% | `FormationProgressService.java`, session complete flow |
| 14 | **Smart session planning** | “Suggest trainers/slots” + “Check conflicts” | Checks trainer time overlap, availability coverage, location conflicts, inactive formation; scores trainers (expertise + workload + fit) | `SessionPlanningService.java`, admin `sessions.component.ts` |
| 15 | **Trainer workload** | Hours / level LOW→OVERLOADED | Planned hours over next period; helps avoid overbooking | planning workload DTOs, `trainer-workload.component.ts` |
| 16 | **Admin Intelligence Center** | Health score, alerts, high-demand formations, session risks, missing skills, recommended actions | Aggregates platform data into decision support (not just counters) | `AdminIntelligenceCenterService.java`, admin dashboard |
| 17 | **Realtime session chat** | WhatsApp-like chat per session | WebSocket STOMP + JWT; persist messages; unread counts; typing; access only for assigned trainer / enrolled learner / admin | `ChatService.java`, `ChatWebSocketController.java`, `session-chat.component.ts` |
| 18 | **Smart Virtual Attendance** | Join live Jitsi classroom; auto present at 70% | Join/heartbeat/leave intervals; stale after ~90s; attendance % = connected time / session duration; complete-smart marks qualified learners | `VirtualAttendanceService.java`, `session-classroom.component.ts`, `application.yml` `app.attendance.*` |

> **Note for teacher:** Features 9–12 are one **Learner Intelligence** family (related screens, shared scoring ideas). Features 14–16 are **Admin decision support**. Features 17–18 are the **live session** layer.

---

# Part B — Full demo scenario (≈ 25–35 min)

Follow this order. It shows the whole business cycle.

### Before starting

1. Start Postgres: `docker start training-platform-postgres`  
2. Backend: `mvn spring-boot:run` (port 8080)  
3. Frontend: `cd frontend && npm start` (port 4200)  
4. Open **Chrome** (admin + trainer) and **Firefox / private** (learner)

---

### Scene 1 — Admin: platform & planning (8 min)

1. Login as **admin** (`admin@training.com` / `admin123`)
2. Open **Dashboard / Intelligence**
   - Show health score, alerts, high-demand / risks / workload insights
3. Open **Formations**
   - Show a formation with **Number of sessions (séances)** (e.g. 2 or 4)
   - Explain: progress is based on completed séances / planned count
4. Open **Sessions**
   - Pick a formation
   - Show “X sessions planned · Y already scheduled”
   - Try **Check conflicts** and **Suggest best trainers**
   - Create/open an **online** session if needed (capacity small, e.g. 1–2, if you want waitlist later)
5. Open **Users** briefly
   - Show lock/activate/deactivate (account security)
6. Open **Trainer workload**
   - Show overload levels

**Talking point:** Admin does not only CRUD — the platform helps decide *who* to assign and *what* is at risk.

---

### Scene 2 — Trainer: availability & sessions (5 min)

1. Logout → login as **trainer** (`trainer.java@training.com` / `password`)
2. Open **Availability**
   - Show weekly slots (used by planning)
3. Open **My sessions**
   - Show sessions **grouped by formation**
   - Filters: Upcoming / Live / Completed
4. Pick an online session that is OPEN/PLANNED → click **Start**
   - Status becomes **Live / IN_PROGRESS**
5. Click **Join live session** (leave this tab open)

**Talking point:** Trainer controls the live session lifecycle.

---

### Scene 3 — Learner: intelligence + enroll (8 min)

1. In the second browser, login as **learner** (`learner.amine@training.com` / `password`)
2. Open **Profile**
   - Show skills + goals; mention **profile score**
3. Open **Learning path**
   - Show ordered steps, recommended next, locked advanced items
4. Open **Improvement plan**
   - Show prioritized suggestions (same intelligence family, different view)
5. Open **Catalogue** → open a formation
   - Show **skill gap** (match % / missing skills)
6. Enroll in the **session the trainer just started** (or an open one)
7. Open **My enrollments**
   - Show grouping **by formation → sessions inside**
   - Progress % for the formation
8. Click **Chat** → send a message
9. Click **Join live session** when session is IN_PROGRESS

**Talking point:** Learner is guided (path / plan / gap), then joins the live class.

---

### Scene 4 — Live classroom + auto attendance (6 min)

1. Trainer browser: classroom open (Jitsi)
2. Learner browser: joined classroom
3. Explain:
   - Heartbeat every ~30s
   - Presence intervals stored
   - Need ~**70%** of session time connected to be marked present
4. Stay connected a bit (for a short demo, use a short session window if possible)
5. Trainer ends / completes smart attendance (or complete flow)
6. Back to learner **My enrollments**
   - Show completed / Present status
7. If formation has multiple séances: complete enough to show progress rising (25%, 50%…)

**Talking point:** Attendance is measured, not only “clicked join once”.

---

### Scene 5 — Waitlist (optional, 3 min)

1. Admin sets a session capacity to **1**
2. Learner A enrolls → CONFIRMED
3. Learner B enrolls → WAITLISTED
4. Learner A cancels → Learner B auto-promoted + notified

**Talking point:** Enrollment has real capacity rules, not only insert rows.

---

### Scene 6 — Wrap-up for the teacher (1 min)

Say clearly:

> The platform covers the full training cycle: catalogue → smart planning → enrollment/waitlist → live classroom with measured attendance → progress by séance → personalized learner guidance → admin decision support.

---

# Part C — Quick “what to click” cheat sheet

| Goal | Who | Where |
| --- | --- | --- |
| Intelligence dashboard | Admin | Dashboard |
| Limit séances per formation | Admin | Formations → session count |
| Conflict / trainer suggest | Admin | Sessions |
| Start live class | Trainer | My sessions → Start → Join live |
| Learning path | Learner | Learning path |
| Improvement suggestions | Learner | Improvement plan |
| Skill gap | Learner | Catalogue → formation detail |
| Chat | Trainer/Learner | Session → Chat |
| Auto attendance | Both | Classroom while IN_PROGRESS |
| Enrollments by formation | Learner | My enrollments |

---

# Part D — How to run (reminder)

```bash
# 1) Database
docker start training-platform-postgres

# 2) Backend (from project root)
mvn spring-boot:run

# 3) Frontend
cd frontend && npm start
```

Open http://localhost:4200

---

