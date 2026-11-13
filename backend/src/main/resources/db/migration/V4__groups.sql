-- V4 (F6): imported student records, groups, members and invitations.
CREATE TABLE student_record (
    id                 BIGSERIAL PRIMARY KEY,
    student_id         VARCHAR(20)  NOT NULL UNIQUE,
    credits_completed  INTEGER      NOT NULL CHECK (credits_completed >= 0),
    cgpa               NUMERIC(3,2) NOT NULL CHECK (cgpa >= 0 AND cgpa <= 4),
    imported_by        BIGINT       NOT NULL REFERENCES app_user(id),
    imported_at        TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE student_group (
    id          BIGSERIAL PRIMARY KEY,
    track       VARCHAR(20) NOT NULL CHECK (track IN ('THESIS','PROJECT','INTERNSHIP')),
    semester    VARCHAR(30) NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','DISBANDED')),
    leader_id   BIGINT      NOT NULL REFERENCES app_user(id),
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE TABLE group_member (
    id          BIGSERIAL PRIMARY KEY,
    group_id    BIGINT    NOT NULL REFERENCES student_group(id) ON DELETE CASCADE,
    member_id   BIGINT    NOT NULL REFERENCES app_user(id),
    active      BOOLEAN   NOT NULL DEFAULT TRUE,
    joined_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (group_id, member_id)
);
-- One active group per student, enforced by the database itself.
CREATE UNIQUE INDEX uq_one_active_group ON group_member (member_id) WHERE active;

CREATE TABLE group_invitation (
    id            BIGSERIAL PRIMARY KEY,
    group_id      BIGINT      NOT NULL REFERENCES student_group(id) ON DELETE CASCADE,
    invitee_id    BIGINT      NOT NULL REFERENCES app_user(id),
    invited_by    BIGINT      NOT NULL REFERENCES app_user(id),
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','ACCEPTED','DECLINED','CANCELLED')),
    created_at    TIMESTAMP   NOT NULL DEFAULT NOW(),
    responded_at  TIMESTAMP
);
CREATE UNIQUE INDEX uq_pending_invitation ON group_invitation (group_id, invitee_id) WHERE status = 'PENDING';