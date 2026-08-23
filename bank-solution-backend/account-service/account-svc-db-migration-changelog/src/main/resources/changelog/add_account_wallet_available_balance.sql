ALTER TABLE account_wallet
    ADD COLUMN available_balance DECIMAL(19, 2) NOT NULL DEFAULT 0.00;

COMMENT ON COLUMN account_wallet.available_balance IS 'Posted balance minus pending debits, projected from the ledger; what the customer can spend';
