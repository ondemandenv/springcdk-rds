CREATE OR REPLACE PROCEDURE add_audit_columns(tbls TEXT[])
    LANGUAGE plpgsql
AS
$$
DECLARE
    tbl TEXT;
BEGIN
    FOREACH tbl IN ARRAY tbls
        LOOP
            EXECUTE (
                CONCAT('alter table ', tbl,
                       ' add column IF NOT EXISTS created_datetime timestamptz not null default now();')
                );
            EXECUTE (CONCAT('alter table ', tbl,
                            ' add column if not exists created_by text not null;')
                );
            EXECUTE (CONCAT('alter table ', tbl,
                            ' add column if not exists last_modified_datetime timestamptz;')
                );
            EXECUTE (CONCAT('alter table ', tbl,
                            ' add column if not exists last_modified_by text;')
                );
        END LOOP;
END;
$$;

CALL add_audit_columns(ARRAY [
    'payment_targets',
    'payment_methods',
    'gateways',
    'payment_profiles',
    'payment_metadata',
    'payment_transaction_reasons',
    'springcdk',
    'payment_transactions',
    'gateway_payment_transactions',
    'line_items',
    'payment_transactions_line_items'
    ])
