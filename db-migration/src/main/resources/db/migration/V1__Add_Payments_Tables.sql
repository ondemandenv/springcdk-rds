CREATE TABLE payment_targets
(
    id          BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    target_type TEXT NOT NULL,
    target_key  TEXT NOT NULL
);

CREATE TABLE payment_methods
(
    id            INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name          TEXT    NOT NULL,
    allow_payment BOOLEAN NOT NULL,
    allow_refund  BOOLEAN NOT NULL
);

CREATE TABLE gateways
(
    id                         INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    gateway_subscription_token TEXT NOT NULL,
    start_date                 DATE NOT NULL,
    end_date                   DATE
);

CREATE TABLE payment_profiles
(
    id                 BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    external_id        TEXT    NOT NULL,
    payment_method_id  INT     NOT NULL REFERENCES payment_methods (id),
    gateway_id         INT     NOT NULL REFERENCES gateways (id),
    is_default         BOOLEAN NOT NULL,
    is_reusable        BOOLEAN NOT NULL,
    is_active          BOOLEAN NOT NULL,
    customer_id        TEXT    NOT NULL,
    method_information JSONB   NOT NULL
);

CREATE TABLE payment_transaction_reasons
(
    id        INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    reason    TEXT    NOT NULL,
    is_active BOOLEAN NOT NULL
);

CREATE TABLE springcdk
(
    id                  BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    external_payment_id TEXT        NOT NULL,
    payment_target_id   BIGINT      NOT NULL REFERENCES payment_targets (id),
    payment_datetime    TIMESTAMPTZ NOT NULL,
    stopped_datetime    TIMESTAMPTZ,
    requested_by        TEXT        NOT NULL,
    currency            TEXT        NOT NULL,
    currency_amount     DECIMAL     NOT NULL
);

CREATE TABLE payment_transactions
(
    id                            BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    payment_id                    BIGINT  NOT NULL REFERENCES springcdk (id),
    payment_profile_id            BIGINT  NOT NULL REFERENCES payment_profiles (id),
    payment_transaction_type      TEXT    NOT NULL,
    payment_transaction_reason_id INT     NOT NULL REFERENCES payment_transaction_reasons (id),
    source                        TEXT    NOT NULL,
    currency_amount               DECIMAL NOT NULL,
    transaction_status            TEXT    NOT NULL
);

CREATE TABLE payment_metadata
(
    id         BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    payment_id BIGINT NOT NULL REFERENCES springcdk (id),
    metadata   JSONB  NOT NULL
);

CREATE TABLE gateway_payment_transactions
(
    id                       BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    payment_transaction_id   BIGINT NOT NULL REFERENCES payment_transactions (id),
    gateway_transaction_type TEXT   NOT NULL,
    gateway_id               INT    NOT NULL REFERENCES gateways (id),
    status                   TEXT   NOT NULL,
    gateway_request          TEXT,
    gateway_response         TEXT
);

CREATE TABLE line_items
(
    id                    BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    external_line_item_id TEXT    NOT NULL,
    description           TEXT    NOT NULL,
    currency_amount       DECIMAL NOT NULL
);

CREATE TABLE payment_transactions_line_items
(
    id                      BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    payment_transaction_id  BIGINT  NOT NULL REFERENCES payment_transactions (id),
    line_item_id            BIGINT  NOT NULL REFERENCES line_items (id),
    currency_amount_applied DECIMAL NOT NULL
);
