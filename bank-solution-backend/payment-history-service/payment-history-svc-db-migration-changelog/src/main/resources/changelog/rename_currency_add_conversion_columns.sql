-- Rename currency column to from_currency (source currency)
ALTER TABLE payment_history
    RENAME COLUMN currency TO from_currency;

-- Add currency constraint on from_currency
ALTER TABLE payment_history
    ADD CONSTRAINT chk_payment_history_from_currency
        CHECK (from_currency IN ('AED', 'ALL', 'CHF', 'EUR', 'GBP', 'INR', 'JPY', 'MAD', 'MXN', 'NGN', 'PKR', 'TRY', 'USD'));

-- Add to_currency, converted_amount, applied_exchange_rate columns
ALTER TABLE payment_history
    ADD COLUMN IF NOT EXISTS to_currency            VARCHAR(3),
    ADD COLUMN IF NOT EXISTS converted_amount       DECIMAL(19, 4),
    ADD COLUMN IF NOT EXISTS applied_exchange_rate  DECIMAL(19, 8);

-- Add constraint for to_currency (NULL is allowed for same-currency payments)
ALTER TABLE payment_history
    ADD CONSTRAINT chk_payment_history_to_currency
        CHECK (to_currency IN ('AED', 'ALL', 'CHF', 'EUR', 'GBP', 'INR', 'JPY', 'MAD', 'MXN', 'NGN', 'PKR', 'TRY', 'USD'));
