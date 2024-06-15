ALTER TABLE gateway_payment_transactions DROP COLUMN gateway_request;
ALTER TABLE gateway_payment_transactions ADD COLUMN IF NOT EXISTS gateway_request jsonb;


ALTER TABLE gateway_payment_transactions DROP COLUMN gateway_response;
ALTER TABLE gateway_payment_transactions ADD COLUMN IF NOT EXISTS gateway_response jsonb;