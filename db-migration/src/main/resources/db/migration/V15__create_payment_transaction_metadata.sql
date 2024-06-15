CREATE TABLE payment_transaction_metadata
(
    id                     BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    payment_transaction_id BIGINT NOT NULL REFERENCES payment_transactions (id),
    metadata               JSONB  NOT NULL
);

CALL add_audit_columns(ARRAY ['payment_transaction_metadata']);