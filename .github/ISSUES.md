# GitHub Issues (paste manually, or run create_issues.sh)

Create milestones first: Sprint 1, Sprint 2, Sprint 3, Sprint 4.

---
## F1: Supervisor Capacity & Availability Management
Milestone: Sprint 1 · Labels: feature, module-A

**Module A: Discovery & Matching** · Sprint 1

Supervisors set research areas and a per-semester capacity (counted in teams). Used slots are calculated from real allocation records, availability closes automatically when full, and the coordinator can override capacity with a logged reason.

### Tasks
- [ ] Entity + Flyway migration
- [ ] Repository + service (business rules)
- [ ] REST controller + validation
- [ ] React page/components
- [ ] Unit tests (JUnit/Mockito)

### Done when
- [ ] Rules above work end-to-end
- [ ] PR reviewed and merged

---
## F2: Supervisor Discovery
Milestone: Sprint 2 · Labels: feature, module-A

**Module A: Discovery & Matching** · Sprint 2

Students search and filter supervisors by research area, keyword and remaining slots, with sorting and pagination. Profiles show only public information.

### Tasks
- [ ] Entity + Flyway migration
- [ ] Repository + service (business rules)
- [ ] REST controller + validation
- [ ] React page/components
- [ ] Unit tests (JUnit/Mockito)

### Done when
- [ ] Rules above work end-to-end
- [ ] PR reviewed and merged

---
## F3: Explainable Supervisor Matching
Milestone: Sprint 3 · Labels: feature, module-A

**Module A: Discovery & Matching** · Sprint 3

Ranks eligible supervisors for a team using a documented weighted score (interest overlap, remaining capacity, workload) and shows the score breakdown. Full supervisors are excluded. Rule-based, not AI.

### Tasks
- [ ] Entity + Flyway migration
- [ ] Repository + service (business rules)
- [ ] REST controller + validation
- [ ] React page/components
- [ ] Unit tests (JUnit/Mockito)

### Done when
- [ ] Rules above work end-to-end
- [ ] PR reviewed and merged

---
## F4: Verified Supervisor Feedback & Rating
Milestone: Sprint 4 · Labels: feature, module-A

**Module A: Discovery & Matching** · Sprint 4

Only students whose supervision is formally completed can rate that supervisor, once per supervision. The coordinator can hide inappropriate feedback; averages appear only after a minimum number of ratings.

### Tasks
- [ ] Entity + Flyway migration
- [ ] Repository + service (business rules)
- [ ] REST controller + validation
- [ ] React page/components
- [ ] Unit tests (JUnit/Mockito)

### Done when
- [ ] Rules above work end-to-end
- [ ] PR reviewed and merged

---
## F5: Thesis Topic Board & Eligibility Check
Milestone: Sprint 2 · Labels: feature, module-A

**Module A: Discovery & Matching** · Sprint 2

Supervisors publish topics with prerequisites, positions and a deadline. The system checks team eligibility, eligible teams apply through the F7 request workflow, and topics close when full or past the deadline.

### Tasks
- [ ] Entity + Flyway migration
- [ ] Repository + service (business rules)
- [ ] REST controller + validation
- [ ] React page/components
- [ ] Unit tests (JUnit/Mockito)

### Done when
- [ ] Rules above work end-to-end
- [ ] PR reviewed and merged

---
## F6: Thesis Team Formation & Membership Management
Milestone: Sprint 1 · Labels: feature, module-B

**Module B: Teams, Allocation & Collaboration** · Sprint 1

Create semester teams, invite members by student ID, accept/reject invitations, configurable size limits, one active team per student, and leader transfer. Membership changes after allocation need supervisor approval.

### Tasks
- [ ] Entity + Flyway migration
- [ ] Repository + service (business rules)
- [ ] REST controller + validation
- [ ] React page/components
- [ ] Unit tests (JUnit/Mockito)

### Done when
- [ ] Rules above work end-to-end
- [ ] PR reviewed and merged

---
## F7: Supervision Request & Allocation Workflow
Milestone: Sprint 2 · Labels: feature, module-B

**Module B: Teams, Allocation & Collaboration** · Sprint 2

Teams send a research pitch (optionally linked to a topic) with at most 3 pending requests. Supervisors accept or reject with a reason, requests auto-expire, and acceptance runs in one database transaction that reserves capacity and withdraws other requests.

### Tasks
- [ ] Entity + Flyway migration
- [ ] Repository + service (business rules)
- [ ] REST controller + validation
- [ ] React page/components
- [ ] Unit tests (JUnit/Mockito)

### Done when
- [ ] Rules above work end-to-end
- [ ] PR reviewed and merged

---
## F8: Supervision Change & Reallocation Workflow
Milestone: Sprint 3 · Labels: feature, module-B

**Module B: Teams, Allocation & Collaboration** · Sprint 3

Teams request a supervisor change with a reason and the coordinator decides. On approval the allocation is transferred in one transaction; if the new supervisor is full, the change fails and the current supervisor is kept.

### Tasks
- [ ] Entity + Flyway migration
- [ ] Repository + service (business rules)
- [ ] REST controller + validation
- [ ] React page/components
- [ ] Unit tests (JUnit/Mockito)

### Done when
- [ ] Rules above work end-to-end
- [ ] PR reviewed and merged

---
## F9: Milestone-Based Peer Contribution Evaluation
Milestone: Sprint 4 · Labels: feature, module-B

**Module B: Teams, Allocation & Collaboration** · Sprint 4

At each milestone, members confidentially rate each other with a rubric (no self-rating, once per milestone). Supervisors see only aggregated results and low contributors are flagged.

### Tasks
- [ ] Entity + Flyway migration
- [ ] Repository + service (business rules)
- [ ] REST controller + validation
- [ ] React page/components
- [ ] Unit tests (JUnit/Mockito)

### Done when
- [ ] Rules above work end-to-end
- [ ] PR reviewed and merged

---
## F10: Notification Center & Preferences
Milestone: Sprint 3 · Labels: feature, module-B

**Module B: Teams, Allocation & Collaboration** · Sprint 3

In-app notifications for workflow events (requests, decisions, revisions, meetings, deadlines) with unread counts and per-event notification preferences.

### Tasks
- [ ] Entity + Flyway migration
- [ ] Repository + service (business rules)
- [ ] REST controller + validation
- [ ] React page/components
- [ ] Unit tests (JUnit/Mockito)

### Done when
- [ ] Rules above work end-to-end
- [ ] PR reviewed and merged

---
## F11: Proposal Submission & Version Control
Milestone: Sprint 1 · Labels: feature, module-C

**Module C: Proposal & Review** · Sprint 1

Teams upload validated PDF proposals within open submission windows. Every resubmission creates a new version with full history, and an approved version cannot be replaced.

### Tasks
- [ ] Entity + Flyway migration
- [ ] Repository + service (business rules)
- [ ] REST controller + validation
- [ ] React page/components
- [ ] Unit tests (JUnit/Mockito)

### Done when
- [ ] Rules above work end-to-end
- [ ] PR reviewed and merged

---
## F12: Structured Supervisor Review & Revision Tracking
Milestone: Sprint 2 · Labels: feature, module-C

**Module C: Proposal & Review** · Sprint 2

Supervisors comment on specific sections of a proposal version, teams reply, and each thread is resolved or a revision is requested.

### Tasks
- [ ] Entity + Flyway migration
- [ ] Repository + service (business rules)
- [ ] REST controller + validation
- [ ] React page/components
- [ ] Unit tests (JUnit/Mockito)

### Done when
- [ ] Rules above work end-to-end
- [ ] PR reviewed and merged

---
## F13: Multi-Level Proposal Approval Workflow
Milestone: Sprint 3 · Labels: feature, module-C

**Module C: Proposal & Review** · Sprint 3

Proposal states: Draft, Submitted, Revision Required, Supervisor Approved, Coordinator Approved, Rejected. Role-based rules control every transition, and each decision is logged with its reason.

### Tasks
- [ ] Entity + Flyway migration
- [ ] Repository + service (business rules)
- [ ] REST controller + validation
- [ ] React page/components
- [ ] Unit tests (JUnit/Mockito)

### Done when
- [ ] Rules above work end-to-end
- [ ] PR reviewed and merged

---
## F14: Research Topic Similarity Check
Milestone: Sprint 4 · Labels: feature, module-C

**Module C: Proposal & Review** · Sprint 4

Compares a new title and abstract with registered topics using TF-IDF cosine similarity (written in Java) and shows the most similar topics for human review. A similarity warning, not plagiarism detection.

### Tasks
- [ ] Entity + Flyway migration
- [ ] Repository + service (business rules)
- [ ] REST controller + validation
- [ ] React page/components
- [ ] Unit tests (JUnit/Mockito)

### Done when
- [ ] Rules above work end-to-end
- [ ] PR reviewed and merged

---
## F15: Submission Window & Deadline Enforcement
Milestone: Sprint 1 · Labels: feature, module-C

**Module C: Proposal & Review** · Sprint 1

The coordinator sets submission phases with open/close times and a late policy. Submissions are validated against the windows, and per-team extensions require a reason.

### Tasks
- [ ] Entity + Flyway migration
- [ ] Repository + service (business rules)
- [ ] REST controller + validation
- [ ] React page/components
- [ ] Unit tests (JUnit/Mockito)

### Done when
- [ ] Rules above work end-to-end
- [ ] PR reviewed and merged

---
## F16: Thesis Milestone & Task Planning
Milestone: Sprint 1 · Labels: feature, module-D

**Module D: Progress, Meetings & Evaluation** · Sprint 1

Teams split milestones into assigned tasks with due dates. Completion % is calculated from task status, and overdue items are flagged.

### Tasks
- [ ] Entity + Flyway migration
- [ ] Repository + service (business rules)
- [ ] REST controller + validation
- [ ] React page/components
- [ ] Unit tests (JUnit/Mockito)

### Done when
- [ ] Rules above work end-to-end
- [ ] PR reviewed and merged

---
## F17: Weekly Progress Report & Sign-off
Milestone: Sprint 2 · Labels: feature, module-D

**Module D: Progress, Meetings & Evaluation** · Sprint 2

Teams submit one weekly report (done, next, blockers). Supervisors approve or return it, and missing weeks are tracked with reminders.

### Tasks
- [ ] Entity + Flyway migration
- [ ] Repository + service (business rules)
- [ ] REST controller + validation
- [ ] React page/components
- [ ] Unit tests (JUnit/Mockito)

### Done when
- [ ] Rules above work end-to-end
- [ ] PR reviewed and merged

---
## F18: Supervisor Meeting Scheduling & Minutes
Milestone: Sprint 3 · Labels: feature, module-D

**Module D: Progress, Meetings & Evaluation** · Sprint 3

Supervisors publish meeting slots and teams book them with double booking prevented. Rescheduling and cancellation follow a notice period, and minutes with follow-up actions are recorded.

### Tasks
- [ ] Entity + Flyway migration
- [ ] Repository + service (business rules)
- [ ] REST controller + validation
- [ ] React page/components
- [ ] Unit tests (JUnit/Mockito)

### Done when
- [ ] Rules above work end-to-end
- [ ] PR reviewed and merged

---
## F19: Defense Panel & Rubric-Based Evaluation
Milestone: Sprint 4 · Labels: feature, module-D

**Module D: Progress, Meetings & Evaluation** · Sprint 4

The coordinator assigns evaluators, who submit rubric scores independently. Scores lock after submission, and results are published only when all evaluations are in and released.

### Tasks
- [ ] Entity + Flyway migration
- [ ] Repository + service (business rules)
- [ ] REST controller + validation
- [ ] React page/components
- [ ] Unit tests (JUnit/Mockito)

### Done when
- [ ] Rules above work end-to-end
- [ ] PR reviewed and merged

---
## F20: Coordination Analytics & PDF Report
Milestone: Sprint 4 · Labels: feature, module-D

**Module D: Progress, Meetings & Evaluation** · Sprint 4

Coordinator dashboard of supervisor workload, unmatched teams, approval rates and overdue milestones, filtered by semester, computed from live records and exportable as PDF.

### Tasks
- [ ] Entity + Flyway migration
- [ ] Repository + service (business rules)
- [ ] REST controller + validation
- [ ] React page/components
- [ ] Unit tests (JUnit/Mockito)

### Done when
- [ ] Rules above work end-to-end
- [ ] PR reviewed and merged
