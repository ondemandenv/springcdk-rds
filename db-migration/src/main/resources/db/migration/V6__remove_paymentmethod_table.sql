ALTER TABLE payment_profiles
    ADD COLUMN profile_payment_type TEXT NOT NULL;
ALTER TABLE payment_profiles
    DROP
        COLUMN payment_method_id;

DROP TABLE payment_methods
