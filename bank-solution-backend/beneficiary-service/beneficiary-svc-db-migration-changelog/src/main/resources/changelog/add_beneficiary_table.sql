CREATE TABLE beneficiary
(
    id             UUID PRIMARY KEY,
    customer_id    UUID        NOT NULL,
    type           VARCHAR(20) NOT NULL,
    alias          VARCHAR(255),
    company_name   VARCHAR(140),
    first_name     VARCHAR(70),
    last_name      VARCHAR(70),
    status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at     TIMESTAMP,
    deleted_reason VARCHAR(500),
    version        INTEGER     NOT NULL DEFAULT 0,
    CONSTRAINT chk_beneficiary_type CHECK (type IN ('INDIVIDUAL', 'COMPANY')),
    CONSTRAINT chk_beneficiary_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED'))
);

CREATE INDEX idx_beneficiary_customer_id ON beneficiary (customer_id);

COMMENT ON TABLE beneficiary IS 'Payees a customer can send money to';
COMMENT ON COLUMN beneficiary.customer_id IS 'Owning customer (customer-service id)';
COMMENT ON COLUMN beneficiary.type IS 'INDIVIDUAL uses first/last name, COMPANY uses company name';
COMMENT ON COLUMN beneficiary.alias IS 'Customer-chosen display name';
COMMENT ON COLUMN beneficiary.status IS 'ACTIVE, INACTIVE or DELETED';
COMMENT ON COLUMN beneficiary.deleted_reason IS 'Reason given for soft deletion';
