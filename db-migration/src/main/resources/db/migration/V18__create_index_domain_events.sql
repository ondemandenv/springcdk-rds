CREATE INDEX IDX_domain_events_sequence_number ON domain_events (sequence_number) WHERE processed_at IS NULL;