CREATE TABLE supervision_request (
    id               BIGSERIAL PRIMARY KEY,
    group_id         BIGINT      NOT NULL REFERENCES student_group(id) ON DELETE CASCADE,
    supervisor_id    BIGINT      NOT NULL REFERENCES app_user(id),
    supervisor_role  VARCHAR(20) NOT NULL CHECK (supervisor_role IN ('SUPERVISOR', 'CO_SUPERVISOR')),
    pitch            TEXT        NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                     CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'WITHDRAWN', 'EXPIRED')),
    rejection_reason TEXT,
    requested_by     BIGINT      NOT NULL REFERENCES app_user(id),
    created_at       TIMESTAMP   NOT NULL DEFAULT NOW(),
    expires_at       TIMESTAMP   NOT NULL,
    responded_at     TIMESTAMP
);

-- A group cannot have two pending requests to the same supervisor.
CREATE UNIQUE INDEX uq_pending_request ON supervision_request (group_id, supervisor_id) WHERE status = 'PENDING';
CREATE INDEX idx_request_supervisor_status ON supervision_request (supervisor_id, status);
CREATE INDEX idx_request_group_status ON supervision_request (group_id, status);

CREATE TABLE allocation (
    id              BIGSERIAL PRIMARY KEY,
    group_id        BIGINT      NOT NULL REFERENCES student_group(id) ON DELETE CASCADE,
    supervisor_id   BIGINT      NOT NULL REFERENCES app_user(id),
    supervisor_role VARCHAR(20) NOT NULL CHECK (supervisor_role IN ('SUPERVISOR', 'CO_SUPERVISOR')),
    semester        VARCHAR(30) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'ENDED')),
    request_id      BIGINT      NOT NULL UNIQUE REFERENCES supervision_request(id),
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- A supervisor is allocated to a group at most once at a time.
CREATE UNIQUE INDEX uq_active_allocation ON allocation (group_id, supervisor_id) WHERE status = 'ACTIVE';
CREATE INDEX idx_allocation_supervisor ON allocation (supervisor_id, semester, status);