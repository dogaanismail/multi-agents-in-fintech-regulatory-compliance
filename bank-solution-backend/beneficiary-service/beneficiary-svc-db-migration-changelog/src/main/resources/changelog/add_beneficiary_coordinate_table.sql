CREATE TABLE beneficiary_coordinate
(
    id              UUID PRIMARY KEY,
    beneficiary_id  UUID        NOT NULL,
    payout_method   VARCHAR(10) NOT NULL,
    currency        VARCHAR(3)  NOT NULL,
    account_number  VARCHAR(8),
    sort_code       VARCHAR(6),
    iban            VARCHAR(34),
    bic             VARCHAR(11),
    bank_name       VARCHAR(255),
    bank_country    VARCHAR(2),
    address         VARCHAR(255),
    city            VARCHAR(255),
    country_code    VARCHAR(2),
    postcode        VARCHAR(12),
    state           VARCHAR(255),
    external_id     VARCHAR(255),
    wallet_provider VARCHAR(20),
    contact_number  VARCHAR(20),
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP,
    deleted_reason  VARCHAR(500),
    version         INTEGER     NOT NULL DEFAULT 0,
    CONSTRAINT fk_beneficiary_coordinate_beneficiary FOREIGN KEY (beneficiary_id) REFERENCES beneficiary (id),
    CONSTRAINT chk_beneficiary_coordinate_payout_method CHECK (payout_method IN ('LOCAL', 'SWIFT', 'WALLET'))
);

CREATE INDEX idx_beneficiary_coordinate_beneficiary_id ON beneficiary_coordinate (beneficiary_id);
CREATE UNIQUE INDEX uq_beneficiary_coordinate_external_id ON beneficiary_coordinate (external_id) WHERE external_id IS NOT NULL;

COMMENT ON TABLE beneficiary_coordinate IS 'Where a beneficiary is paid: one row per payout method and currency';
COMMENT ON COLUMN beneficiary_coordinate.payout_method IS 'LOCAL (domestic rails), SWIFT (international) or WALLET';
COMMENT ON COLUMN beneficiary_coordinate.currency IS 'ISO 4217 currency paid out on this coordinate';
COMMENT ON COLUMN beneficiary_coordinate.external_id IS 'Payout provider reference (e.g. Nium beneficiary hash id)';
