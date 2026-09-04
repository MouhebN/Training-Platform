# Full Manual Test Plan — Training Platform

Use this before DevOps. For each step: **do the action → check expected result → mark Pass / Fail / Blocked**.

**Legend:** `[ ]` not done · `[x]` pass · `[F]` fail · `[B]` blocked

**Browsers:** Chrome (Admin + Trainer) · Firefox or Chrome Incognito (Learner)  
**URL:** http://localhost:4200  
**API:** http://localhost:8080  

---

## 0. Pre-flight

| # | Check | Expected | Result |
| ---: | --- | --- | --- |
| 0.1 | `docker ps` shows `training-platform-postgres` Up | Port 5432 mapped | [ ] |
| 0.2 | Backend running (`mvn spring-boot:run`) | No DB connection errors in logs | [ ] |
| 0.3 | Frontend running (`npm start`) | Opens on :4200 | [ ] |
| 0.4 | Open `/swagger-ui.html` (optional) | Swagger loads | [ ] |

**Accounts**

| Role | Email | Password |
| --- | --- | --- |
| Admin | admin@training.com | admin123 |
| Trainer | trainer.java@training.com | password |
| Trainer 2 | trainer.angular@training.com | password |
| Learner A | learner.amine@training.com | password |
| Learner B | learner.sara@training.com | password |

---

## 1. Auth & security

| # | Action | Expected | Result |
| ---: | --- | --- | --- |
| 1.1 | Open `/login` without token | Login page | [ ] |
| 1.2 | Wrong password 1–2 times | Error message, still unlocked | [ ] |
| 1.3 | Wrong password until lock (≥5) | Account locked message | [ ] |
| 1.4 | Login as admin after lock attempt on another user | Admin can unlock that user in Users | [ ] |
| 1.5 | Login admin | Redirect `/admin/dashboard` | [ ] |
| 1.6 | Paste learner URL `/learner/catalogue` while admin | Blocked / redirected (role guard) | [ ] |
| 1.7 | Logout | Back to login, token cleared | [ ] |
| 1.8 | Login learner | `/learner/dashboard` | [ ] |
| 1.9 | Open `/admin/dashboard` as learner | Blocked | [ ] |
| 1.10 | Register new learner (unique email) | Account created, can login | [ ] |
| 1.11 | Forgot password with valid email | Success message (email if SMTP works) | [ ] |
| 1.12 | Change password (logged in) | Old wrong → fail; correct → success; re-login with new password | [ ] |

---

## 2. Admin — catalogue

Login: **admin**

| # | Action | Expected | Result |
| ---: | --- | --- | --- |
| 2.1 | Categories: create | Appears in list | [ ] |
| 2.2 | Categories: edit / delete (if unused) | Updates / removes | [ ] |
| 2.3 | Formations: create with **sessionCount = 2** | Saved; shows “2 sessions” | [ ] |
| 2.4 | Formations: required skills selected | Visible on formation | [ ] |
| 2.5 | Formations: add chapters | Chapters saved | [ ] |
| 2.6 | Formations: deactivate | Not shown as active for learners (or marked inactive) | [ ] |
| 2.7 | Reactivate formation | Available again | [ ] |

---

## 3. Admin — trainers

| # | Action | Expected | Result |
| ---: | --- | --- | --- |
| 3.1 | Create trainer (email not used) | User TRAINER + profile created | [ ] |
| 3.2 | Set expertise skills | Saved | [ ] |
| 3.3 | Login as that trainer | Trainer space works | [ ] |

---

## 4. Admin — sessions & planning

| # | Action | Expected | Result |
| ---: | --- | --- | --- |
| 4.1 | Select formation with sessionCount=2 | Shows planned vs scheduled count | [ ] |
| 4.2 | Create session 1 (online, capacity 2) | Created | [ ] |
| 4.3 | Create session 2 for same formation | Created (2/2) | [ ] |
| 4.4 | Try create session 3 | **Blocked** (UI disabled and/or API error about limit) | [ ] |
| 4.5 | Pick trainer + time **outside** availability | Check conflicts → warning/blocking UNAVAILABLE | [ ] |
| 4.6 | Overlap two sessions same trainer | Check conflicts → TRAINER_TIME_CONFLICT blocking | [ ] |
| 4.7 | Suggest best trainers | Ranked list with score / reasons | [ ] |
| 4.8 | Use suggestion → fills form | Dates/trainer filled | [ ] |
| 4.9 | Edit session schedule | Learners notified if enrolled (notification) | [ ] |
| 4.10 | Cancel a session | Status CANCELLED; slot freed for new session | [ ] |
| 4.11 | After cancel, create another session | Allowed again if under sessionCount | [ ] |

---

## 5. Trainer — availability & my sessions

Login: **trainer.java**

| # | Action | Expected | Result |
| ---: | --- | --- | --- |
| 5.1 | Availability: add Mon 09:00–17:00 | Slot listed | [ ] |
| 5.2 | My sessions: grouped by formation | Formation header + nested session rows | [ ] |
| 5.3 | Filters: Upcoming / Live / Completed | List filters correctly | [ ] |
| 5.4 | Open chat on a session | Chat page loads | [ ] |
| 5.5 | Start session (OPEN/PLANNED) | Status → IN_PROGRESS; message shown | [ ] |
| 5.6 | Join live (online + IN_PROGRESS) | Classroom opens (Jitsi) | [ ] |
| 5.7 | Onsite session: Attendance panel | Expand list of enrollments, checkboxes | [ ] |

---

## 6. Learner — profile & intelligence

Login: **learner.amine** (Browser 2)

| # | Action | Expected | Result |
| ---: | --- | --- | --- |
| 6.1 | Dashboard loads | No crash; score / links | [ ] |
| 6.2 | Profile: empty-ish → save phone, bio, skills, goals | Saved | [ ] |
| 6.3 | Profile score increases | Higher /100 after completing fields | [ ] |
| 6.4 | Learning path | Steps with statuses; one RECOMMENDED_NEXT | [ ] |
| 6.5 | Improvement plan | Ranked suggestions with reasons | [ ] |
| 6.6 | Catalogue lists active formations | Titles visible | [ ] |
| 6.7 | Formation detail: skill gap | Match % + missing skills | [ ] |
| 6.8 | Formation detail: chapters | Readable | [ ] |
| 6.9 | Formation detail: sessions list | Open/planned sessions shown | [ ] |

---

## 7. Enrollment & waitlist

Use a session with **capacity = 1** (admin edit if needed).

| # | Action | Expected | Result |
| ---: | --- | --- | --- |
| 7.1 | Learner A enrolls | CONFIRMED | [ ] |
| 7.2 | Learner A enrolls again same session | Rejected / reopen rules OK (no duplicate confirmed) | [ ] |
| 7.3 | Learner B enrolls (capacity full) | WAITLISTED | [ ] |
| 7.4 | Learner A cancels | Cancel success | [ ] |
| 7.5 | Learner B becomes CONFIRMED | Auto-promotion + notification (and email if configured) | [ ] |
| 7.6 | Enroll cancelled/completed session | Rejected | [ ] |
| 7.7 | My enrollments: group by formation | Formation card → sessions inside | [ ] |
| 7.8 | Filters All / In progress / Completed | Correct | [ ] |

---

## 8. Chat (realtime)

Trainer browser + Learner browser, same session.

| # | Action | Expected | Result |
| ---: | --- | --- | --- |
| 8.1 | Learner sends message | Appears on both sides | [ ] |
| 8.2 | Trainer replies | Learner sees instantly | [ ] |
| 8.3 | Unread badge on My sessions / enrollments | Count updates | [ ] |
| 8.4 | Open chat → mark read | Unread clears | [ ] |
| 8.5 | Typing indicator (if implemented) | Shows when other types | [ ] |
| 8.6 | Non-enrolled learner opens chat URL | Access denied | [ ] |

---

## 9. Virtual classroom & attendance

**Prep:** Online session, capacity ≥1, learner CONFIRMED, trainer starts session.

| # | Action | Expected | Result |
| ---: | --- | --- | --- |
| 9.1 | Before Start: learner sees Join? | Join **hidden** or not available | [ ] |
| 9.2 | After Start: learner Join live | Classroom opens | [ ] |
| 9.3 | Trainer Join live | Same room (Jitsi) | [ ] |
| 9.4 | Video/audio UI usable | Can join room (network dependent) | [ ] |
| 9.5 | Stay connected ~2–3 min | No crash; heartbeat continues | [ ] |
| 9.6 | Learner leaves page without leave API | After ~90s treated stale (check attendance report if available) | [ ] |
| 9.7 | Trainer complete-smart / end with attendance | Qualified learners (≥70%) COMPLETED / Present | [ ] |
| 9.8 | Learner barely connected | Not qualified / not marked present | [ ] |
| 9.9 | Onsite: mark present + Complete session | Present → COMPLETED; absent stay CONFIRMED or as designed | [ ] |

---

## 10. Formation progress

Formation with **sessionCount = 2**, learner completes séance 1 then 2.

| # | Action | Expected | Result |
| ---: | --- | --- | --- |
| 10.1 | After 1/2 sessions completed | Progress ~50% on enrollments / path | [ ] |
| 10.2 | Skills of formation **not** all granted yet | Only at 100% (as designed) | [ ] |
| 10.3 | After 2/2 completed | Progress 100%; formation completed; skills granted | [ ] |
| 10.4 | Learning path updates | Step may become COMPLETED | [ ] |

---

## 11. Admin intelligence & workload

Login: **admin**

| # | Action | Expected | Result |
| ---: | --- | --- | --- |
| 11.1 | Dashboard intelligence loads | Health score + sections | [ ] |
| 11.2 | Full session shows risk | FULL / HIGH risk visible | [ ] |
| 11.3 | Overloaded trainer (if data) | Alert / recommendation | [ ] |
| 11.4 | Trainer workload page | Hours + session list | [ ] |
| 11.5 | Enrollments admin page | Can list / filter / update status | [ ] |

---

## 12. Notifications

| # | Action | Expected | Result |
| ---: | --- | --- | --- |
| 12.1 | Session start | Learner notified | [ ] |
| 12.2 | Waitlist promotion | Learner B notified | [ ] |
| 12.3 | Session reschedule | Enrolled learners notified | [ ] |
| 12.4 | Mark all read | Unread clears | [ ] |

---

## 13. Users admin

| # | Action | Expected | Result |
| ---: | --- | --- | --- |
| 13.1 | Deactivate learner | Cannot login | [ ] |
| 13.2 | Activate again | Can login | [ ] |
| 13.3 | Unlock locked user | Can login | [ ] |
| 13.4 | Admin cannot deactivate self | Error | [ ] |
| 13.5 | Delete test user | Removed; no orphan crash | [ ] |

---

## 14. UX / regression smoke

| # | Action | Expected | Result |
| ---: | --- | --- | --- |
| 14.1 | Refresh page while logged in | Still authenticated | [ ] |
| 14.2 | Mobile width (~375px) key pages | Usable (no broken layout) | [ ] |
| 14.3 | Empty states (no sessions) | Friendly message, no crash | [ ] |
| 14.4 | Network error (stop backend) | Error alert, not white screen | [ ] |
| 14.5 | Restart backend + frontend | App recovers | [ ] |

---

## 15. End-to-end “golden path” (must pass)

Do this once without stopping:

1. Admin creates formation (sessionCount=2) + 2 online sessions  
2. Trainer sets availability + starts session 1  
3. Learner completes profile → sees learning path → enrolls  
4. Chat message both ways  
5. Both join classroom  
6. Complete session with attendance  
7. Learner sees progress ~50%  
8. Repeat for session 2 → 100% + skills  
9. Admin opens intelligence dashboard  

| Golden path | Result |
| --- | --- |
| Full cycle OK | [ ] |

---

## Bug log

| ID | Step # | Severity (Blocker/Major/Minor) | What happened | Expected | Screenshot / note |
| --- | --- | --- | --- | --- | --- |
| B1 | | | | | |
| B2 | | | | | |
| B3 | | | | | |

---

## Sign-off

| Role | Tester | Date | Pass? |
| --- | --- | --- | --- |
| Manual QA | | | Yes / No |

**Ready for DevOps only if:** golden path = Pass, and no open **Blocker** bugs.

---

## Suggested order (≈ 90–120 min)

1. Pre-flight + Auth (15 min)  
2. Admin catalogue + sessions + planning (25 min)  
3. Trainer availability + start (10 min)  
4. Learner intelligence + enroll (15 min)  
5. Waitlist (10 min)  
6. Chat + classroom + attendance (20 min)  
7. Progress 2 séances (15 min)  
8. Intelligence + users + notifications (15 min)  
9. Golden path once more (10 min)
