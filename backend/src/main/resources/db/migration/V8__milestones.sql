-- V8 (F16): milestone plans and tasks of a group.
-- A milestone belongs to one group. Tasks belong to a milestone and may be assigned to a group member.
-- Completion percentage and overdue flags are NOT stored; they are calculated from task status and dates.
CREATE TABLE milestone (
    id             BIGSERIAL PRIMARY KEY,
    group_id       BIGINT       NOT NULL REFERENCES student_group(id) ON DELETE CASCADE,
    title          VARCHAR(200) NOT NULL CHECK (length(trim(title)) > 0),
    description    TEXT,
    phase          VARCHAR(20)  CHECK (phase IN ('PRE_THESIS_1','PRE_THESIS_2','FINAL')),
    due_date       DATE,
    sort_order     INTEGER      NOT NULL DEFAULT 0,
    from_template  BOOLEAN      NOT NULL DEFAULT FALSE,
    -- Set for milestones created by the track template and for the system "Follow-up actions" bucket,
    -- so the same milestone cannot be created twice for one group.
    template_key   VARCHAR(60),
    created_by     BIGINT       NOT NULL REFERENCES app_user(id),
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_milestone_group ON milestone (group_id, sort_order);
CREATE UNIQUE INDEX uq_milestone_template_key ON milestone (group_id, template_key) WHERE template_key IS NOT NULL;

CREATE TABLE milestone_task (
    id            BIGSERIAL PRIMARY KEY,
    milestone_id  BIGINT       NOT NULL REFERENCES milestone(id) ON DELETE CASCADE,
    title         VARCHAR(200) NOT NULL CHECK (length(trim(title)) > 0),
    description   TEXT,
    assignee_id   BIGINT       REFERENCES app_user(id),
    due_date      DATE,
    status        VARCHAR(20)  NOT NULL DEFAULT 'TODO' CHECK (status IN ('TODO','IN_PROGRESS','DONE')),
    -- Where the task came from. MANUAL for tasks typed by students; later features (meeting minutes,
    -- failed acceptance test cases) create tasks with their own source and a reference to the origin row.
    source        VARCHAR(30)  NOT NULL DEFAULT 'MANUAL',
    source_ref    VARCHAR(100),
    created_by    BIGINT       NOT NULL REFERENCES app_user(id),
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    completed_at  TIMESTAMP,
    version       BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX idx_task_milestone ON milestone_task (milestone_id);
CREATE INDEX idx_task_assignee_status ON milestone_task (assignee_id, status);
-- A generated task is created only once for the same origin (for example one failed test case).
CREATE UNIQUE INDEX uq_task_source_ref ON milestone_task (source, source_ref) WHERE source_ref IS NOT NULL;
