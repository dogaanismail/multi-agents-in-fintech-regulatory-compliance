ALTER TABLE account
    DROP CONSTRAINT IF EXISTS chk_account_type;

ALTER TABLE account
    ADD CONSTRAINT chk_account_type CHECK (account_type IN ('CHECKING', 'SAVINGS', 'BUSINESS', 'LEDGER'));

ALTER TABLE account_balance
    DROP CONSTRAINT IF EXISTS chk_currency;

ALTER TABLE account_balance
    ADD CONSTRAINT chk_currency CHECK (currency IN ('AED', 'ALL', 'CHF', 'EUR', 'GBP', 'INR', 'JPY', 'MAD', 'MXN', 'NGN', 'PKR', 'TRY', 'USD'));
