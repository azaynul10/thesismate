-- V7 (F11): phase reports with version history, and the Turnitin files that gate each version.
-- One report per group and phase; every upload is a new row in report_version (nothing is overwritten).
CREATE TABLE report (
    id          BIGSERIAL PRIMARY KEY,
    group_id    BIGINT      NOT NULL REFERENCES student_group(id) ON DELETE CASCADE,
    phase       VARCHAR(20) NOT NULL CHECK (phase IN ('PRE_THESIS_1','PRE_THESIS_2','FINAL')),
    status      VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED' CHECK (status IN (
        'DRAFT','SUBMITTED','REVISION_REQUIRED','SUPERVISOR_APPROVED','COMMITTEE_APPROVED','REJECTED')),
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_report_group_phase UNIQUE (group_id, phase)
);

CREATE TABLE report_version (
    id             BIGSERIAL PRIMARY KEY,
    report_id      BIGINT       NOT NULL REFERENCES report(id) ON DELETE CASCADE,
    version_no     INTEGER      NOT NULL CHECK (version_no > 0),
    original_name  VARCHAR(255) NOT NULL,
    storage_key    VARCHAR(255) NOT NULL,
    size_bytes     BIGINT       NOT NULL CHECK (size_bytes > 0),
    sha256         VARCHAR(64)  NOT NULL,
    late           BOOLEAN      NOT NULL DEFAULT FALSE,
    locked         BOOLEAN      NOT NULL DEFAULT FALSE,
    uploaded_by    BIGINT       NOT NULL REFERENCES app_user(id),
    uploaded_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_report_version UNIQUE (report_id, version_no)
);

-- The Turnitin similarity report and AI report that must accompany every version (similarity max 15 percent).
CREATE TABLE plagiarism_report (
    id                       BIGSERIAL PRIMARY KEY,
    version_id               BIGINT        NOT NULL UNIQUE REFERENCES report_version(id) ON DELETE CASCADE,
    similarity_percent       NUMERIC(5,2)  NOT NULL CHECK (similarity_percent >= 0 AND similarity_percent <= 100),
    similarity_original_name VARCHAR(255)  NOT NULL,
    similarity_storage_key   VARCHAR(255)  NOT NULL,
    ai_original_name         VARCHAR(255)  NOT NULL,
    ai_storage_key           VARCHAR(255)  NOT NULL,
    created_at               TIMESTAMP     NOT NULL DEFAULT NOW()
);
