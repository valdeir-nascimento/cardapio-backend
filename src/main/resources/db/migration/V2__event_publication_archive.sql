CREATE TABLE event_publication_archive (
    id UUID NOT NULL,
    listener_id VARCHAR(512) NOT NULL,
    event_type VARCHAR(512) NOT NULL,
    serialized_event TEXT NOT NULL,
    publication_date TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    completion_date TIMESTAMP(6) WITH TIME ZONE,
    PRIMARY KEY (id)
);

CREATE INDEX idx_event_publication_archive_completion_date
    ON event_publication_archive (completion_date);

CREATE INDEX idx_event_publication_archive_listener_id_serialized_event
    ON event_publication_archive (listener_id, serialized_event);
