-- Spring Modulith 2.0 Event Publication Registry
-- Schema derived from Spring Modulith JPA entity requirements

CREATE TABLE IF NOT EXISTS event_publication (
    id UUID PRIMARY KEY,
    listener_id VARCHAR(512) NOT NULL,
    event_type VARCHAR(512) NOT NULL,
    serialized_event TEXT NOT NULL,
    publication_date TIMESTAMP NOT NULL,
    completion_date TIMESTAMP,
    completion_attempts INTEGER DEFAULT 0,
    last_resubmission_date TIMESTAMP,
    status VARCHAR(50)
);

-- Index for finding incomplete publications (retry mechanism)
CREATE INDEX idx_event_publication_incomplete
    ON event_publication(completion_date)
    WHERE completion_date IS NULL;

-- Index for querying by listener
CREATE INDEX idx_event_publication_listener
    ON event_publication(listener_id);

-- Index for querying by event type
CREATE INDEX idx_event_publication_event_type
    ON event_publication(event_type);

-- Index for ordering by publication date
CREATE INDEX idx_event_publication_date
    ON event_publication(publication_date DESC);

COMMENT ON TABLE event_publication IS 'Spring Modulith 2.0 Event Publication Registry';
