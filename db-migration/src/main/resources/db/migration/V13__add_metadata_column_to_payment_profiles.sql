ALTER TABLE payment_profiles
    ADD COLUMN IF NOT EXISTS metadata jsonb DEFAULT NULL;
