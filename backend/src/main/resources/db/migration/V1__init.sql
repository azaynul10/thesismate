-- V1: base user table. Add one new file per change (V2__..., V3__...). Never edit an applied migration.
CREATE TABLE app_user (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    full_name   VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL CHECK (role IN ('STUDENT','SUPERVISOR','COORDINATOR','EVALUATOR')),
    password    VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
