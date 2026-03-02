-- Rename currency column to from_currency (source currency)
ALTER TABLE risk_check_request
    RENAME COLUMN currency TO from_currency;

-- Rename the currency constraint to reflect the new from_currency column name
ALTER TABLE risk_check_request
    RENAME CONSTRAINT chk_risk_check_request_currency TO chk_risk_check_request_from_currency;

-- Add to_currency column (destination/received currency, nullable)
ALTER TABLE risk_check_request
    ADD COLUMN IF NOT EXISTS to_currency VARCHAR(3);

-- Add constraint for to_currency (NULL is allowed for same-currency payments)
ALTER TABLE risk_check_request
    ADD CONSTRAINT chk_risk_check_request_to_currency
        CHECK (to_currency IN ('AED', 'ALL', 'CHF', 'EUR', 'GBP', 'INR', 'JPY', 'MAD', 'MXN', 'NGN', 'PKR', 'TRY', 'USD'));
