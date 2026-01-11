-- Create a customer_address table
CREATE TABLE customer_address
(
    id             UUID PRIMARY KEY,
    city           VARCHAR(100) NOT NULL,
    country        VARCHAR(2)   NOT NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at     TIMESTAMP,
    deleted_reason VARCHAR(500),
    version        INTEGER      NOT NULL DEFAULT 0
);

-- Create indexes
CREATE INDEX idx_customer_address_country ON customer_address (country);
CREATE INDEX idx_customer_address_deleted_at ON customer_address (deleted_at);

-- Add comments
COMMENT ON TABLE customer_address IS 'Stores customer residential addresses';
COMMENT ON COLUMN customer_address.id IS 'Unique identifier for address';
COMMENT ON COLUMN customer_address.city IS 'City name';
COMMENT ON COLUMN customer_address.country IS '2-letter ISO country code';
COMMENT ON COLUMN customer_address.created_at IS 'Timestamp when record was created';
COMMENT ON COLUMN customer_address.updated_at IS 'Timestamp when record was last updated';
COMMENT ON COLUMN customer_address.deleted_at IS 'Timestamp when record was soft deleted';
COMMENT ON COLUMN customer_address.deleted_reason IS 'Reason for soft deletion';
COMMENT ON COLUMN customer_address.version IS 'Optimistic locking version';

