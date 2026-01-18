-- Create a transaction_snapshot table
CREATE TABLE transaction_snapshot
(
    id                          UUID PRIMARY KEY,
    customer_id                 UUID         NOT NULL,
    account_id                  UUID         NOT NULL,
    payment_id                  VARCHAR(100) NOT NULL UNIQUE,
    amount                      NUMERIC(19, 4) NOT NULL,
    currency                    VARCHAR(10)  NOT NULL,
    payment_type                VARCHAR(50)  NOT NULL,
    sender_bank_location        VARCHAR(50),
    receiver_bank_location      VARCHAR(50),
    is_cross_border             BOOLEAN      NOT NULL DEFAULT FALSE,
    is_cash_transaction         BOOLEAN      NOT NULL DEFAULT FALSE,
    is_large_transaction        BOOLEAN      NOT NULL DEFAULT FALSE,
    is_night_transaction        BOOLEAN      NOT NULL DEFAULT FALSE,
    is_weekend_transaction      BOOLEAN      NOT NULL DEFAULT FALSE,
    receiver_account_id         VARCHAR(100),
    receiver_country            VARCHAR(50),
    transaction_date            DATE         NOT NULL,
    transaction_time            TIME         NOT NULL,
    transaction_timestamp       TIMESTAMP    NOT NULL,
    created_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_transaction_snapshot_customer_id ON transaction_snapshot (customer_id);
CREATE INDEX idx_transaction_snapshot_account_id ON transaction_snapshot (account_id);
CREATE INDEX idx_transaction_snapshot_payment_id ON transaction_snapshot (payment_id);
CREATE INDEX idx_transaction_snapshot_timestamp ON transaction_snapshot (transaction_timestamp);

-- Add comments
COMMENT ON TABLE transaction_snapshot IS 'Stores individual transaction snapshots for customer profile calculations';
COMMENT ON COLUMN transaction_snapshot.id IS 'Unique identifier for transaction snapshot';
COMMENT ON COLUMN transaction_snapshot.customer_id IS 'Reference to customer who made the transaction';
COMMENT ON COLUMN transaction_snapshot.account_id IS 'Source account ID for the transaction';
COMMENT ON COLUMN transaction_snapshot.payment_id IS 'Reference to payment ID (unique)';
COMMENT ON COLUMN transaction_snapshot.amount IS 'Transaction amount';
COMMENT ON COLUMN transaction_snapshot.currency IS 'Transaction currency (ISO 4217 code)';
COMMENT ON COLUMN transaction_snapshot.payment_type IS 'Type of payment: TRANSFER_IN, TRANSFER_OUT, DEPOSIT, WITHDRAWAL';
COMMENT ON COLUMN transaction_snapshot.sender_bank_location IS 'Location of sender bank';
COMMENT ON COLUMN transaction_snapshot.receiver_bank_location IS 'Location of receiver bank';
COMMENT ON COLUMN transaction_snapshot.is_cross_border IS 'Flag indicating if transaction is cross-border';
COMMENT ON COLUMN transaction_snapshot.is_cash_transaction IS 'Flag indicating if transaction is cash (deposit/withdrawal)';
COMMENT ON COLUMN transaction_snapshot.is_large_transaction IS 'Flag indicating if transaction exceeds $10,000';
COMMENT ON COLUMN transaction_snapshot.is_night_transaction IS 'Flag indicating if transaction occurred between 22:00-06:00';
COMMENT ON COLUMN transaction_snapshot.is_weekend_transaction IS 'Flag indicating if transaction occurred on weekend';
COMMENT ON COLUMN transaction_snapshot.receiver_account_id IS 'Destination account ID';
COMMENT ON COLUMN transaction_snapshot.receiver_country IS 'Country of receiver';
COMMENT ON COLUMN transaction_snapshot.transaction_date IS 'Date portion of transaction';
COMMENT ON COLUMN transaction_snapshot.transaction_time IS 'Time portion of transaction';
COMMENT ON COLUMN transaction_snapshot.transaction_timestamp IS 'Full timestamp of transaction';
COMMENT ON COLUMN transaction_snapshot.created_at IS 'Timestamp when snapshot was stored';