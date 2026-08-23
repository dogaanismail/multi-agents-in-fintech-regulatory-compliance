ALTER TABLE payment_request
    ADD COLUMN IF NOT EXISTS payment_scheme VARCHAR(50);
ALTER TABLE payment_request
    ADD COLUMN IF NOT EXISTS fixed_side VARCHAR(10);

UPDATE payment_request
SET payment_scheme = 'EXTERNAL_OUTBOUND'
WHERE payment_scheme IS NULL;
UPDATE payment_request
SET fixed_side = 'SELL'
WHERE fixed_side IS NULL;

ALTER TABLE payment_request
    ALTER COLUMN payment_scheme SET NOT NULL;
ALTER TABLE payment_request
    ALTER COLUMN fixed_side SET NOT NULL;

ALTER TABLE payment_request
    ADD CONSTRAINT chk_payment_scheme CHECK (payment_scheme IN
                                             ('INTERNAL_TRANSFER', 'EXTERNAL_OUTBOUND', 'EXTERNAL_INBOUND'));

ALTER TABLE payment_request
    ADD CONSTRAINT chk_fixed_side CHECK (fixed_side IN ('SELL', 'BUY'));

COMMENT ON COLUMN payment_request.payment_scheme IS 'How the payment is routed: internal book transfer or an external rail';
COMMENT ON COLUMN payment_request.fixed_side IS 'Which side of an FX conversion is exact: SELL fixes the debited amount, BUY fixes the credited amount';
