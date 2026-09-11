CREATE TABLE processed_webhook_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider VARCHAR(30) NOT NULL,
    event_id VARCHAR(255) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL,
    UNIQUE (provider, event_id)
);
