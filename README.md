# ThesisMate

**Thesis Supervision & Research Management Platform**, a CSE470 (Software Engineering) project, BRAC University, Fall 2026.

ThesisMate replaces scattered emails, spreadsheets and chat messages with one system where students form thesis teams, find and request supervisors, submit and revise proposals, and track progress through to the final defense.

## Users
- **Student / Team Leader**: forms teams, requests supervisors, submits proposals and weekly reports
- **Supervisor**: manages capacity, reviews proposals, signs off reports, runs meetings
- **Thesis Coordinator**: sets windows, approves proposals, assigns defense panels, views analytics
- **Defense Evaluator**: submits rubric scores

## Tech stack
| Layer | Technology |
|---|---|
| View | React (Vite, TypeScript), Bootstrap |
| Controller | Spring MVC REST controllers (Spring Boot 3, Java 21) |
| Model | JPA entities, Spring Data repositories, service layer |
| Database | PostgreSQL + Flyway migrations |
| Security | Spring Security (role-based access) |
| Testing | JUnit 5, Mockito |
| Reports | OpenPDF |

> No MERN stack (MongoDB/Express/Node backend) and no Python backend, as required by the course rules.

## MVC structure
```
backend/src/main/java/com/thesismate/
├── controller/   ← Controller: REST endpoints, request validation
├── service/      ← Model: business rules (capacity, workflows, scoring)
├── repository/   ← Model: Spring Data JPA data access
├── model/        ← Model: JPA entities
├── dto/          ← request/response objects
└── config/       ← security & app configuration
frontend/src/
├── pages/        ← View: one page per screen
├── components/   ← View: reusable UI parts
└── api/          ← calls to the Spring Boot REST API
```

## Features (20)

### Module A: Discovery & Matching
- **F1. Supervisor Capacity & Availability Management**: Supervisors set research areas and a per-semester capacity (counted in teams). Used slots are calculated from real allocation records, availability closes automatically when full, and the coordinator can override capacity with a logged reason.
- **F2. Supervisor Discovery**: Students search and filter supervisors by research area, keyword and remaining slots, with sorting and pagination. Profiles show only public information.
- **F3. Explainable Supervisor Matching**: Ranks eligible supervisors for a team using a documented weighted score (interest overlap, remaining capacity, workload) and shows the score breakdown. Full supervisors are excluded. Rule-based, not AI.
- **F4. Verified Supervisor Feedback & Rating**: Only students whose supervision is formally completed can rate that supervisor, once per supervision. The coordinator can hide inappropriate feedback; averages appear only after a minimum number of ratings.
- **F5. Thesis Topic Board & Eligibility Check**: Supervisors publish topics with prerequisites, positions and a deadline. The system checks team eligibility, eligible teams apply through the F7 request workflow, and topics close when full or past the deadline.

### Module B: Teams, Allocation & Collaboration
- **F6. Thesis Team Formation & Membership Management**: Create semester teams, invite members by student ID, accept/reject invitations, configurable size limits, one active team per student, and leader transfer. Membership changes after allocation need supervisor approval.
- **F7. Supervision Request & Allocation Workflow**: Teams send a research pitch (optionally linked to a topic) with at most 3 pending requests. Supervisors accept or reject with a reason, requests auto-expire, and acceptance runs in one database transaction that reserves capacity and withdraws other requests.
- **F8. Supervision Change & Reallocation Workflow**: Teams request a supervisor change with a reason and the coordinator decides. On approval the allocation is transferred in one transaction; if the new supervisor is full, the change fails and the current supervisor is kept.
- **F9. Milestone-Based Peer Contribution Evaluation**: At each milestone, members confidentially rate each other with a rubric (no self-rating, once per milestone). Supervisors see only aggregated results and low contributors are flagged.
- **F10. Notification Center & Preferences**: In-app notifications for workflow events (requests, decisions, revisions, meetings, deadlines) with unread counts and per-event notification preferences.

### Module C: Proposal & Review
- **F11. Proposal Submission & Version Control**: Teams upload validated PDF proposals within open submission windows. Every resubmission creates a new version with full history, and an approved version cannot be replaced.
- **F12. Structured Supervisor Review & Revision Tracking**: Supervisors comment on specific sections of a proposal version, teams reply, and each thread is resolved or a revision is requested.
- **F13. Multi-Level Proposal Approval Workflow**: Proposal states: Draft, Submitted, Revision Required, Supervisor Approved, Coordinator Approved, Rejected. Role-based rules control every transition, and each decision is logged with its reason.
- **F14. Research Topic Similarity Check**: Compares a new title and abstract with registered topics using TF-IDF cosine similarity (written in Java) and shows the most similar topics for human review. A similarity warning, not plagiarism detection.
- **F15. Submission Window & Deadline Enforcement**: The coordinator sets submission phases with open/close times and a late policy. Submissions are validated against the windows, and per-team extensions require a reason.

### Module D: Progress, Meetings & Evaluation
- **F16. Thesis Milestone & Task Planning**: Teams split milestones into assigned tasks with due dates. Completion % is calculated from task status, and overdue items are flagged.
- **F17. Weekly Progress Report & Sign-off**: Teams submit one weekly report (done, next, blockers). Supervisors approve or return it, and missing weeks are tracked with reminders.
- **F18. Supervisor Meeting Scheduling & Minutes**: Supervisors publish meeting slots and teams book them with double booking prevented. Rescheduling and cancellation follow a notice period, and minutes with follow-up actions are recorded.
- **F19. Defense Panel & Rubric-Based Evaluation**: The coordinator assigns evaluators, who submit rubric scores independently. Scores lock after submission, and results are published only when all evaluations are in and released.
- **F20. Coordination Analytics & PDF Report**: Coordinator dashboard of supervisor workload, unmatched teams, approval rates and overdue milestones, filtered by semester, computed from live records and exportable as PDF.

## Sprint plan
| Sprint | Features |
|---|---|
| 1 | F1, F6, F11, F15, F16 |
| 2 | F2, F5, F7, F12, F17 |
| 3 | F3, F8, F10, F13, F18 |
| 4 | F4, F9, F14, F19, F20 |

## Getting started
**Requirements:** Java 21, Maven 3.9+, Node 20+, PostgreSQL 15+

```bash
# 1. Database
createdb thesismate

# 2. Backend (http://localhost:8080)
cd backend
cp .env.example .env        # edit DB username/password
mvn spring-boot:run

# 3. Frontend (http://localhost:5173)
cd frontend
npm install
npm run dev
```
Check the backend is running: `GET http://localhost:8080/api/health`

## Team
| Name | Student ID | GitHub | Module |
|---|---|---|---|
| Zaynul Abedin Miah | | [@azaynul10](https://github.com/azaynul10) | A |
| | | | B |
| | | | C |
| | | | D |

Faculty: Nazmul Islam (NZU), CSE470 Section 02

## Workflow
See [CONTRIBUTING.md](CONTRIBUTING.md).
