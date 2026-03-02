-- Rename currency → from_currency and add to_currency in transaction_snapshot table
ALTER TABLE transaction_snapshot RENAME COLUMN currency TO from_currency;

-- Add currency constraint on from_currency
ALTER TABLE transaction_snapshot
    ADD CONSTRAINT chk_transaction_snapshot_from_currency
        CHECK (from_currency IN ('AED', 'ALL', 'CHF', 'EUR', 'GBP', 'INR', 'JPY', 'MAD', 'MXN', 'NGN', 'PKR', 'TRY', 'USD'));

ALTER TABLE transaction_snapshot
    ADD COLUMN to_currency VARCHAR(10) NOT NULL DEFAULT '';

-- Back-fill: for existing rows, to_currency defaults to from_currency (same-currency assumption)
UPDATE transaction_snapshot SET to_currency = from_currency WHERE to_currency = '';

-- Remove the default so future inserts must supply the value explicitly
ALTER TABLE transaction_snapshot ALTER COLUMN to_currency DROP DEFAULT;

-- Add constraint for to_currency
ALTER TABLE transaction_snapshot
    ADD CONSTRAINT chk_transaction_snapshot_to_currency
        CHECK (to_currency IN ('AED', 'ALL', 'CHF', 'EUR', 'GBP', 'INR', 'JPY', 'MAD', 'MXN', 'NGN', 'PKR', 'TRY', 'USD'));

-- Update column comments
COMMENT ON COLUMN transaction_snapshot.from_currency IS 'Source currency of the transaction (ISO 4217 code)';
COMMENT ON COLUMN transaction_snapshot.to_currency IS 'Destination currency of the transaction (ISO 4217 code); equals from_currency for same-currency payments';
