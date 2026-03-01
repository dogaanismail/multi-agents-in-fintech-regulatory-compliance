-- Rename currency column to from_currency (source currency)
ALTER TABLE payment_request
    RENAME COLUMN currency TO from_currency;

-- Rename target_currency column to to_currency (destination currency)
ALTER TABLE payment_request
    RENAME COLUMN target_currency TO to_currency;

-- Rename constraints to reflect the new column names
ALTER TABLE payment_request
    RENAME CONSTRAINT chk_currency TO chk_from_currency;

ALTER TABLE payment_request
    RENAME CONSTRAINT chk_target_currency TO chk_to_currency;
