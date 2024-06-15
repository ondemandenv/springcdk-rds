ALTER TABLE payment_transactions
    ALTER COLUMN reason SET NOT NULL;

ALTER TABLE payment_transactions
    ALTER COLUMN external_payment_transaction_id SET NOT NULL;
