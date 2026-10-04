ALTER TABLE account_wallet
    ADD COLUMN IF NOT EXISTS balance_as_of TIMESTAMP WITH TIME ZONE;

COMMENT ON COLUMN account_wallet.balance_as_of IS 'Ledger timestamp of the projected balance; an older WalletBalanceChangedEvent replayed later is ignored';
