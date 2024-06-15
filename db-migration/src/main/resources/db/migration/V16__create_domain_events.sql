CREATE TABLE domain_events
(
    uuid                TEXT            PRIMARY KEY,
    event_name          TEXT            NOT NULL,
    aggregate_id        TEXT            NOT NULL,
    payload             JSONB           NOT NULL,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    event_at            TIMESTAMPTZ     NOT NULL,
    processed_at        TIMESTAMPTZ
)
