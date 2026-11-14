CREATE TABLE supervisor_profile (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL UNIQUE REFERENCES app_user(id) ON DELETE CASCADE,
    bio         TEXT,
    office_room VARCHAR(100),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE supervisor_research_area (
    profile_id  BIGINT       NOT NULL REFERENCES supervisor_profile(id) ON DELETE CASCADE,
    area        VARCHAR(120) NOT NULL,
    PRIMARY KEY (profile_id, area)
);

CREATE TABLE supervisor_track (
    profile_id  BIGINT      NOT NULL REFERENCES supervisor_profile(id) ON DELETE CASCADE,
    track       VARCHAR(20) NOT NULL CHECK (track IN ('THESIS', 'PROJECT', 'INTERNSHIP')),
    PRIMARY KEY (profile_id, track)
);

CREATE TABLE supervisor_capacity (
    id          BIGSERIAL PRIMARY KEY,
    profile_id  BIGINT      NOT NULL REFERENCES supervisor_profile(id) ON DELETE CASCADE,
    semester    VARCHAR(30) NOT NULL,
    capacity    INTEGER     NOT NULL CHECK (capacity >= 0),
    version     BIGINT      NOT NULL DEFAULT 0,
    UNIQUE (profile_id, semester)
);

CREATE TABLE capacity_override (
    id             BIGSERIAL PRIMARY KEY,
    profile_id     BIGINT      NOT NULL REFERENCES supervisor_profile(id) ON DELETE CASCADE,
    semester       VARCHAR(30) NOT NULL,
    old_capacity   INTEGER     NOT NULL,
    new_capacity   INTEGER     NOT NULL CHECK (new_capacity >= 0),
    reason         TEXT        NOT NULL,
    coordinator_id BIGINT      NOT NULL REFERENCES app_user(id),
    created_at     TIMESTAMP   NOT NULL DEFAULT NOW()
);