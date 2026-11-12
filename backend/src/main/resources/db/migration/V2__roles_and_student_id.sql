-- V2: roles follow the feature document (adds CHAIRPERSON) and students get a student ID.
ALTER TABLE app_user DROP CONSTRAINT app_user_role_check;
ALTER TABLE app_user ADD CONSTRAINT app_user_role_check
    CHECK (role IN ('STUDENT','SUPERVISOR','COORDINATOR','CHAIRPERSON','EVALUATOR'));
ALTER TABLE app_user ADD COLUMN student_id VARCHAR(20) UNIQUE;