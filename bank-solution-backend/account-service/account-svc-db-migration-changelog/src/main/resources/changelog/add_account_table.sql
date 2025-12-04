-- Create an account table
CREATE TABLE account
(
    id              UUID PRIMARY KEY,
    customer_id     UUID         NOT NULL,
    account_number  VARCHAR(20)  NOT NULL UNIQUE,
    account_type    VARCHAR(50)  NOT NULL DEFAULT 'CHECKING',
    account_status  VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    opening_date    DATE         NOT NULL,
    closing_date    DATE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP,
    deleted_reason  VARCHAR(500),
    version         INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT chk_account_type CHECK (account_type IN ('CHECKING', 'SAVINGS', 'BUSINESS')),
    CONSTRAINT chk_account_status CHECK (account_status IN ('ACTIVE', 'SUSPENDED', 'CLOSED'))
);

-- Create indexes for the account table
CREATE INDEX idx_account_customer_id ON account (customer_id);
CREATE INDEX idx_account_account_number ON account (account_number);
CREATE INDEX idx_account_account_type ON account (account_type);
CREATE INDEX idx_account_account_status ON account (account_status);
CREATE INDEX idx_account_opening_date ON account (opening_date);
CREATE INDEX idx_account_deleted_at ON account (deleted_at);
CREATE INDEX idx_account_created_at ON account (created_at);

-- Add comments for account table
COMMENT ON TABLE account IS 'Stores account information for customers';
COMMENT ON COLUMN account.id IS 'Unique identifier for account';
COMMENT ON COLUMN account.customer_id IS 'Reference to customer who owns the account';
COMMENT ON COLUMN account.account_number IS 'Unique account number';
COMMENT ON COLUMN account.account_type IS 'Type of account: CHECKING, SAVINGS, or BUSINESS';
COMMENT ON COLUMN account.account_status IS 'Account status: ACTIVE, SUSPENDED, or CLOSED';
COMMENT ON COLUMN account.opening_date IS 'Date when the account was opened';
COMMENT ON COLUMN account.closing_date IS 'Date when the account was closed (if applicable)';
COMMENT ON COLUMN account.created_at IS 'Timestamp when record was created';
COMMENT ON COLUMN account.updated_at IS 'Timestamp when record was last updated';
COMMENT ON COLUMN account.deleted_at IS 'Timestamp when record was soft deleted';
COMMENT ON COLUMN account.deleted_reason IS 'Reason for soft deletion';
COMMENT ON COLUMN account.version IS 'Optimistic locking version';

