ALTER TABLE payment_profiles ADD COLUMN gateway_identifier TEXT NOT NULL;
ALTER TABLE payment_profiles DROP COLUMN gateway_id;

ALTER TABLE gateway_payment_transactions ADD COLUMN IF NOT EXISTS gateway_identifier TEXT NOT NULL;
ALTER TABLE gateway_payment_transactions DROP COLUMN gateway_id;

DROP TABLE gateways;
