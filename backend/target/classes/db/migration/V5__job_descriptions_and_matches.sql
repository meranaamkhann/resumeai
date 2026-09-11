CREATE TABLE job_descriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    raw_text TEXT NOT NULL,
    job_title VARCHAR(255),
    seniority VARCHAR(50),
    required_skills TEXT,
    preferred_skills TEXT,
    min_years_experience INT,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_jd_user ON job_descriptions(user_id);

CREATE TABLE job_matches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES resume_documents(id) ON DELETE CASCADE,
    job_description_id UUID NOT NULL REFERENCES job_descriptions(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    match_score INT NOT NULL,
    required_matched INT NOT NULL,
    required_total INT NOT NULL,
    preferred_matched INT NOT NULL,
    preferred_total INT NOT NULL,
    experience_match_label VARCHAR(20),
    matched_skills TEXT,
    missing_required_skills TEXT,
    missing_preferred_skills TEXT,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_job_matches_document ON job_matches(document_id);
CREATE INDEX idx_job_matches_jd ON job_matches(job_description_id);
