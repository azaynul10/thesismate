-- V9 (F17): weekly progress reports of Thesis and Project groups, and the reminder log.
-- A week is identified by its Monday (ISO week). One report per group and week.
CREATE TABLE weekly_report (
    id              BIGSERIAL PRIMARY KEY,
    group_id        BIGINT      NOT NULL REFERENCES student_group(id) ON DELETE CASCADE,
    week_start      DATE        NOT NULL CHECK (EXTRACT(ISODOW FROM week_start) = 1),
    done            TEXT        NOT NULL CHECK (length(trim(done)) > 0),
    next_plan       TEXT        NOT NULL CHECK (length(trim(next_plan)) > 0),
    blockers        TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED' CHECK (status IN ('SUBMITTED','APPROVED','RETURNED')),
    revision        INTEGER     NOT NULL DEFAULT 1 CHECK (revision > 0),
    submitted_by    BIGINT      NOT NULL REFERENCES app_user(id),
    submitted_at    TIMESTAMP   NOT NULL DEFAULT NOW(),
    reviewed_by     BIGINT      REFERENCES app_user(id),
    reviewed_at     TIMESTAMP,
    review_comment  TEXT,
    version         BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uq_weekly_report_group_week UNIQUE (group_id, week_start)
);
CREATE INDEX idx_weekly_report_status ON weekly_report (status, submitted_at);

-- One reminder per group and missing week, so the daily job never nags twice for the same week.
CREATE TABLE weekly_report_reminder (
    id          BIGSERIAL PRIMARY KEY,
    group_id    BIGINT    NOT NULL REFERENCES student_group(id) ON DELETE CASCADE,
    week_start  DATE      NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_weekly_reminder_group_week UNIQUE (group_id, week_start)
);
