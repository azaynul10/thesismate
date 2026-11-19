-- V6 (F15): submission windows, track deliverable checklist and per-group extensions.
CREATE TABLE submission_window (
    id           BIGSERIAL PRIMARY KEY,
    semester     VARCHAR(30) NOT NULL,
    phase        VARCHAR(20) NOT NULL CHECK (phase IN ('PRE_THESIS_1','PRE_THESIS_2','FINAL')),
    track        VARCHAR(20) NOT NULL CHECK (track IN ('THESIS','PROJECT','INTERNSHIP')),
    opens_at     TIMESTAMP   NOT NULL,
    closes_at    TIMESTAMP   NOT NULL,
    late_policy  VARCHAR(20) NOT NULL DEFAULT 'NO_LATE' CHECK (late_policy IN ('NO_LATE','LATE_ALLOWED')),
    late_until   TIMESTAMP,
    created_by   BIGINT      NOT NULL REFERENCES app_user(id),
    created_at   TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_window_closes_after_opens CHECK (closes_at > opens_at),
    -- NO_LATE has no late_until; LATE_ALLOWED needs a late_until after closes_at.
    CONSTRAINT ck_window_late_until CHECK (
        (late_policy = 'NO_LATE' AND late_until IS NULL)
        OR (late_policy = 'LATE_ALLOWED' AND late_until IS NOT NULL AND late_until > closes_at)),
    -- One window per semester, phase and track.
    CONSTRAINT uq_window_semester_phase_track UNIQUE (semester, phase, track)
);

-- Which deliverables a track must hand in for a phase (the checklist).
CREATE TABLE deliverable_requirement (
    id                BIGSERIAL PRIMARY KEY,
    phase             VARCHAR(20) NOT NULL CHECK (phase IN ('PRE_THESIS_1','PRE_THESIS_2','FINAL')),
    track             VARCHAR(20) NOT NULL CHECK (track IN ('THESIS','PROJECT','INTERNSHIP')),
    deliverable_type  VARCHAR(30) NOT NULL CHECK (deliverable_type IN (
        'REPORT','TURNITIN_SIMILARITY','TURNITIN_AI','POSTER','IEEE_PAPER','PROTOTYPE_RELEASE','INDUSTRY_EVALUATION')),
    mandatory         BOOLEAN     NOT NULL DEFAULT TRUE,
    created_by        BIGINT      NOT NULL REFERENCES app_user(id),
    created_at        TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_requirement UNIQUE (phase, track, deliverable_type)
);

-- A coordinator can give one group a later closing time for one window; a reason is required.
CREATE TABLE submission_extension (
    id             BIGSERIAL PRIMARY KEY,
    group_id       BIGINT       NOT NULL REFERENCES student_group(id) ON DELETE CASCADE,
    window_id      BIGINT       NOT NULL REFERENCES submission_window(id) ON DELETE CASCADE,
    new_closes_at  TIMESTAMP    NOT NULL,
    reason         VARCHAR(500) NOT NULL CHECK (length(trim(reason)) > 0),
    granted_by     BIGINT       NOT NULL REFERENCES app_user(id),
    granted_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_extension_group_window UNIQUE (group_id, window_id)
);
