ALTER TABLE payment_targets
    ALTER COLUMN target_key SET NOT NULL;
ALTER TABLE payment_targets
    ALTER COLUMN target_type SET NOT NULL;
ALTER TABLE payment_targets
    ADD UNIQUE (target_key, target_type)
