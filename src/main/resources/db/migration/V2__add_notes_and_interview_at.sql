-- Phase 8: free-text notes and the interview date of each application.
-- Both are optional, so existing rows stay valid with NULL.

ALTER TABLE job_application
    ADD COLUMN notes        VARCHAR(2000),
    ADD COLUMN interview_at TIMESTAMP(6) WITH TIME ZONE;
