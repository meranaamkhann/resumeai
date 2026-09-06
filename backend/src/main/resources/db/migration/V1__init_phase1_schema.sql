CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    plan VARCHAR(20) NOT NULL DEFAULT 'FREE',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE resumes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    label VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_resumes_user ON resumes(user_id);

CREATE TABLE resume_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resume_id UUID NOT NULL REFERENCES resumes(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    original_filename VARCHAR(255),
    storage_key VARCHAR(500) NOT NULL UNIQUE,
    detected_mime_type VARCHAR(255),
    declared_extension VARCHAR(20),
    file_size_bytes BIGINT NOT NULL,
    content_hash VARCHAR(64),
    extracted_text TEXT,
    parsing_confidence INT,
    status VARCHAR(20) NOT NULL DEFAULT 'UPLOADED',
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_documents_resume ON resume_documents(resume_id);
CREATE INDEX idx_documents_user_hash ON resume_documents(user_id, content_hash);

CREATE TABLE resume_analyses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES resume_documents(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    overall_score INT NOT NULL,
    ats_parsing_score INT NOT NULL,
    keyword_alignment_score INT NOT NULL,
    structure_score INT NOT NULL,
    content_quality_score INT NOT NULL,
    experience_relevance_score INT NOT NULL,
    impact_score INT NOT NULL,
    formatting_score INT NOT NULL,
    issues TEXT NOT NULL,
    detected_sections TEXT,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_analyses_document ON resume_analyses(document_id);
CREATE INDEX idx_analyses_user ON resume_analyses(user_id);
