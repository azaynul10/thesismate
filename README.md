# ThesisMate

**Thesis, Project & Internship Management Platform** — CSE470 Software Engineering project, BRAC University, Fall 2026, Section 02.

ThesisMate replaces scattered emails, spreadsheets and chat messages with one system where students form groups, find supervisors, submit reports, and track progress through Pre-Thesis 1, Pre-Thesis 2 and Final Defense.

## Users

- **Student** — forms groups, requests supervisors, submits reports
- **Supervisor / Co-supervisor** — manages capacity, reviews work, signs off reports
- **Coordinator** — sets submission windows, manages allocations and panels
- **Chairperson / Evaluator** — evaluates defenses
- **Industry supervisor** — external, works through secure links (no account)

## Tech stack

| Layer | Technology |
|---|---|
| View | React 18 (Vite, TypeScript), Bootstrap |
| Controller | Spring MVC REST controllers (Spring Boot 3.5.16, Java 25) |
| Model | JPA entities, Spring Data repositories, service layer |
| Database | PostgreSQL + Flyway migrations |
| Security | Spring Security (session cookie, CSRF, role-based access) |

```
backend/src/main/java/com/thesismate/
├── controller/   REST endpoints, request validation
├── service/      business rules
├── repository/   Spring Data JPA
├── model/        JPA entities
├── dto/          request/response objects
└── config/       security & app configuration
frontend/src/
├── pages/        one page per screen
├── components/   reusable UI parts
└── api/          calls to the REST API
```

## Features (24)

**Module A — Supervisor side (Zaynul):** F1 capacity & availability · F2 supervisor discovery · F3 explainable matching · F4 verified feedback & rating · F5 topic & idea board · F21 internship placement & company approval

**Module B — Groups & workflow (Prisun):** F6 track registration & group formation · F7 supervision request & allocation · F8 supervision change & reallocation · F9 peer contribution evaluation · F10 notification center · F22 industry supervisor verification & internship evaluation

**Module C — Reports & approvals (Aivee):** F11 report submission & version control · F12 supervisor review & revision tracking · F13 multi-level approval workflow · F14 topic similarity check · F15 submission windows & plagiarism gate · F23 prototype release & repo tracking

**Module D — Progress & evaluation (Apol):** F16 milestone & task planning · F17 weekly progress report & sign-off · F18 meeting scheduling & minutes · F19 evaluation panel & track rubrics · F20 coordination analytics & PDF report · F24 project demo & acceptance testing

## Current status

Foundation merged (PR #1): Java 25 + Spring Boot 3.5.16, Flyway V1–V2 (`app_user`, roles, student ID), BCrypt login with session cookies, student registration, role rules, CSRF protection and session-rotation hardening, dev-profile seed accounts, 7 tests.

**Sprint 1 in progress:** F1, F2, F6, F7, F11, F15, F16, F17.

## Getting started

**Requirements:** Java 25, Maven 3.9+, Node 20+, PostgreSQL 15+

```bash
# 1. Database — local PostgreSQL, or Docker:
docker run -d --name thesismate-dev-db -e POSTGRES_PASSWORD=<password> \
  -e POSTGRES_DB=thesismate -e POSTGRES_USER=thesismate -p 5433:5432 postgres:16-alpine

# 2. Backend → http://localhost:8080
cd backend
cp .env.example .env        # set DB_URL, DB_USERNAME, DB_PASSWORD
mvn spring-boot:run -Dspring-boot.run.profiles=dev   # dev profile = seed accounts

# 3. Frontend → http://localhost:5173
cd frontend
npm install
npm run dev
```

Check: `GET http://localhost:8080/api/health` → `{"status":"UP"}`. Tests: `mvn test`.

## Team

| Name | Student ID | GitHub | Module |
|---|---|---|---|
| Zaynul Abedin Miah | 22341034 | [@azaynul10](https://github.com/azaynul10) | A |
| Mahdinam Saif Prisun | 23201131 | [@PrisunSaif](https://github.com/PrisunSaif) | B |
| Aivee Aktar | 23301568 | [@ivyaktar](https://github.com/ivyaktar) | C |
| Asif Ahmed Apol | 22101322 | [@AsifAhmedApol](https://github.com/AsifAhmedApol) | D |

Faculty: Nazmul Islam (NZU), CSE470 Section 02

## Workflow

One branch per feature (`feature/F6-groups`), commit messages start with the feature ID (`F6: ...`). Never edit an applied Flyway migration — add a new file. See [CONTRIBUTING.md](CONTRIBUTING.md).
