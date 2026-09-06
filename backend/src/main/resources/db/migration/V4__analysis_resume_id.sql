ALTER TABLE resume_analyses ADD COLUMN resume_id UUID;
UPDATE resume_analyses ra SET resume_id = rd.resume_id FROM resume_documents rd WHERE ra.document_id = rd.id;
ALTER TABLE resume_analyses ALTER COLUMN resume_id SET NOT NULL;
CREATE INDEX idx_analyses_resume ON resume_analyses(resume_id);
