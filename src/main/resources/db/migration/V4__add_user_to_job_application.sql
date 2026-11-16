-- Phase 11: every application belongs to a user.

-- Nullable for a moment, so the rows that already exist can be given an owner below.
ALTER TABLE job_application
    ADD COLUMN user_id BIGINT REFERENCES app_user (id);

-- Applications saved before users existed go to the oldest user.
UPDATE job_application
SET user_id = (SELECT MIN(id) FROM app_user);

-- From here on an application without an owner cannot exist.
-- If there are applications but no user yet, this fails and the whole migration is undone:
-- nothing is lost, and the fix is to register a user before this migration runs.
ALTER TABLE job_application
    ALTER COLUMN user_id SET NOT NULL;

-- The same ad can be saved by two users, but only once by each.
-- job_application_jobtech_id_key is the name PostgreSQL gave to the UNIQUE of V1.
-- The new index starts with user_id, so it also serves "the applications of this user".
ALTER TABLE job_application
    DROP CONSTRAINT job_application_jobtech_id_key;

ALTER TABLE job_application
    ADD CONSTRAINT job_application_user_id_jobtech_id_key UNIQUE (user_id, jobtech_id);
