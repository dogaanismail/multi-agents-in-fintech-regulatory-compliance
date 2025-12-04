-- Create the account_balance table
CREATE TABLE account_balance
(
    id                 UUID PRIMARY KEY,
    account_id         UUID           NOT NULL,
    currency           VARCHAR(3)     NOT NULL,
    available_balance  DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    pending_balance    DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    created_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at         TIMESTAMP,
    deleted_reason     VARCHAR(500),
    version            INTEGER        NOT NULL DEFAULT 0,
    CONSTRAINT fk_account_balance_account FOREIGN KEY (account_id) REFERENCES account (id),
    CONSTRAINT uq_account_currency UNIQUE (account_id, currency),
    CONSTRAINT chk_currency CHECK (currency IN ('TRY', 'USD', 'EUR', 'GBP', 'JPY', 'CHF')),
    CONSTRAINT chk_available_balance_non_negative CHECK (available_balance >= 0),
    CONSTRAINT chk_pending_balance_non_negative CHECK (pending_balance >= 0)
);

-- Create indexes for the account_balance table
CREATE INDEX idx_account_balance_account_id ON account_balance (account_id);
CREATE INDEX idx_account_balance_currency ON account_balance (currency);

-- Add comments for the account_balance table
COMMENT ON TABLE account_balance IS 'Stores multi-currency balance information for accounts';
COMMENT ON COLUMN account_balance.id IS 'Unique identifier for balance record';
COMMENT ON COLUMN account_balance.account_id IS 'Foreign key to account table';
COMMENT ON COLUMN account_balance.currency IS 'Currency code (ISO 4217)';
COMMENT ON COLUMN account_balance.available_balance IS 'Available balance that can be used immediately';
COMMENT ON COLUMN account_balance.pending_balance IS 'Pending balance from transactions being processed';
COMMENT ON COLUMN account_balance.created_at IS 'Timestamp when record was created';
COMMENT ON COLUMN account_balance.updated_at IS 'Timestamp when record was last updated';
COMMENT ON COLUMN account_balance.deleted_at IS 'Timestamp when record was soft deleted';
COMMENT ON COLUMN account_balance.deleted_reason IS 'Reason for soft deletion';
COMMENT ON COLUMN account_balance.version IS 'Optimistic locking version';

