ALTER TABLE resume_documents ADD COLUMN parsing_issues TEXT;
ALTER TABLE resume_documents ADD COLUMN deleted_at TIMESTAMPTZ;
CREATE INDEX idx_resumes_deleted_at ON resumes(deleted_at);
