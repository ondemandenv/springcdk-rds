ALTER TABLE payment_transactions
    ADD COLUMN reason TEXT NULL;
ALTER TABLE payment_transactions
    DROP
        COLUMN payment_transaction_reason_id;

DROP TABLE payment_transaction_reasons
