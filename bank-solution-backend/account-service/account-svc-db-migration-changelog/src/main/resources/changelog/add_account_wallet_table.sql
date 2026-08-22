CREATE TABLE account_wallet
(
    id                UUID PRIMARY KEY,
    account_id        UUID           NOT NULL,
    ledger_account_id UUID           NOT NULL,
    wallet_status     VARCHAR(50)    NOT NULL DEFAULT 'ACTIVE',
    currency          VARCHAR(3)     NOT NULL,
    balance           DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    is_primary        BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at        TIMESTAMP,
    deleted_reason    VARCHAR(500),
    version           INTEGER        NOT NULL DEFAULT 0,
    CONSTRAINT fk_account_wallet_account FOREIGN KEY (account_id) REFERENCES account (id),
    CONSTRAINT uq_account_wallet_account_currency UNIQUE (account_id, currency),
    CONSTRAINT uq_account_wallet_ledger_account_id UNIQUE (ledger_account_id),
    CONSTRAINT chk_account_wallet_status CHECK (wallet_status IN ('ACTIVE', 'SUSPENDED', 'CLOSED')),
    CONSTRAINT chk_account_wallet_currency CHECK (currency IN
        ('AED', 'ALL', 'CHF', 'EUR', 'GBP', 'INR', 'JPY', 'MAD', 'MXN', 'NGN', 'PKR', 'TRY', 'USD'))
);

CREATE INDEX idx_account_wallet_account_id ON account_wallet (account_id);
CREATE INDEX idx_account_wallet_currency ON account_wallet (currency);

COMMENT ON TABLE account_wallet IS 'Customer holdings per currency; balances are owned by the ledger and projected here';
COMMENT ON COLUMN account_wallet.ledger_account_id IS 'Identifier of the corresponding TigerBeetle wallet account';
COMMENT ON COLUMN account_wallet.balance IS 'Balance projected from the ledger; never computed by account-service';
COMMENT ON COLUMN account_wallet.is_primary IS 'Whether this is the account default currency wallet';
