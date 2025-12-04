-- Create a customer table
CREATE TABLE customer
(
    id             UUID PRIMARY KEY,
    first_name     VARCHAR(100) NOT NULL,
    last_name      VARCHAR(100) NOT NULL,
    middle_name    VARCHAR(100),
    email          VARCHAR(255) NOT NULL UNIQUE,
    phone_number   VARCHAR(20)  NOT NULL,
    date_of_birth  DATE         NOT NULL,
    nationality    VARCHAR(2)   NOT NULL,
    type           VARCHAR(50)  NOT NULL DEFAULT 'INDIVIDUAL',
    status         VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    address_id     UUID         NOT NULL UNIQUE,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at     TIMESTAMP,
    deleted_reason VARCHAR(500),
    version        INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT fk_customer_address FOREIGN KEY (address_id) REFERENCES customer_address (id),
    CONSTRAINT chk_customer_type CHECK (type IN ('INDIVIDUAL', 'BUSINESS')),
    CONSTRAINT chk_customer_status CHECK (status IN ('ACTIVE', 'PASSIVE', 'SUSPENDED'))
);

-- Create indexes
CREATE INDEX idx_customer_email ON customer (email);
CREATE INDEX idx_customer_phone_number ON customer (phone_number);

-- Add comments
COMMENT ON TABLE customer IS 'Stores customer information for individuals and businesses';
COMMENT ON COLUMN customer.id IS 'Unique identifier for customer';
COMMENT ON COLUMN customer.first_name IS 'Customer first name';
COMMENT ON COLUMN customer.last_name IS 'Customer last name';
COMMENT ON COLUMN customer.middle_name IS 'Customer middle name (optional)';
COMMENT ON COLUMN customer.email IS 'Customer email address (unique)';
COMMENT ON COLUMN customer.phone_number IS 'Customer phone number in international format';
COMMENT ON COLUMN customer.date_of_birth IS 'Customer date of birth';
COMMENT ON COLUMN customer.nationality IS '2-letter ISO country code for nationality';
COMMENT ON COLUMN customer.type IS 'Customer type: INDIVIDUAL or BUSINESS';
COMMENT ON COLUMN customer.status IS 'Customer status: ACTIVE, PASSIVE, or SUSPENDED';
COMMENT ON COLUMN customer.address_id IS 'Foreign key to customer_address table';
COMMENT ON COLUMN customer.created_at IS 'Timestamp when record was created';
COMMENT ON COLUMN customer.updated_at IS 'Timestamp when record was last updated';
COMMENT ON COLUMN customer.deleted_at IS 'Timestamp when record was soft deleted';
COMMENT ON COLUMN customer.deleted_reason IS 'Reason for soft deletion';
COMMENT ON COLUMN customer.version IS 'Optimistic locking version';

