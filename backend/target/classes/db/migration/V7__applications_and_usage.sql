CREATE TABLE applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    job_url VARCHAR(1000),
    location VARCHAR(255),
    application_date DATE,
    resume_version_id UUID REFERENCES resume_versions(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SAVED',
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_applications_user ON applications(user_id);

CREATE TABLE usage_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    period VARCHAR(7) NOT NULL,
    analysis_count INT NOT NULL DEFAULT 0,
    jd_match_count INT NOT NULL DEFAULT 0,
    optimization_count INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (user_id, period)
);
CREATE INDEX idx_usage_records_user_period ON usage_records(user_id, period);
