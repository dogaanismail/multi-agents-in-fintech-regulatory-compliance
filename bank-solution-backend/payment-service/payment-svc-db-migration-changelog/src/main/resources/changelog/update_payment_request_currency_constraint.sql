ALTER TABLE payment_request
    DROP CONSTRAINT IF EXISTS chk_currency;

ALTER TABLE payment_request
    ADD CONSTRAINT chk_currency CHECK (currency IN
                                       ('AED', 'ALL', 'CHF', 'EUR', 'GBP', 'INR', 'JPY', 'MAD', 'MXN', 'NGN', 'PKR',
                                        'TRY', 'USD'));

ALTER TABLE payment_request
    ADD COLUMN IF NOT EXISTS converted_amount      DECIMAL(19, 4),
    ADD COLUMN IF NOT EXISTS target_currency       VARCHAR(3),
    ADD COLUMN IF NOT EXISTS applied_exchange_rate DECIMAL(19, 8);

-- Add constraint for target_currency (NULL is allowed for same-currency payments)
ALTER TABLE payment_request
    ADD CONSTRAINT chk_target_currency
        CHECK (target_currency IN ('AED', 'ALL', 'CHF', 'EUR', 'GBP', 'INR', 'JPY', 'MAD', 'MXN', 'NGN', 'PKR', 'TRY', 'USD'));
