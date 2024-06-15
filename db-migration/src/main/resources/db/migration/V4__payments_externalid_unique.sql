ALTER TABLE springcdk
    ALTER COLUMN external_payment_id SET NOT NULL;
ALTER TABLE springcdk
    ADD UNIQUE (external_payment_id);
