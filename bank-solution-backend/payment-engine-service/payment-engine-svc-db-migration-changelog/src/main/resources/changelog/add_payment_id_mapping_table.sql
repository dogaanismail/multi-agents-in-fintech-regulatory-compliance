-- Create payment_id_mapping table for reference number to payment ID lookup
-- This table provides fast lookup from external reference numbers to internal payment IDs
CREATE TABLE payment_id_mapping
(
    reference_number VARCHAR(255) PRIMARY KEY,
    payment_id       VARCHAR(255) NOT NULL,
    CONSTRAINT fk_payment_id_mapping_payment FOREIGN KEY (payment_id) 
        REFERENCES payment_status_view (payment_id) ON DELETE CASCADE
);

-- Create index for reverse lookup (payment_id to reference_number)
CREATE INDEX idx_payment_id_mapping_payment_id ON payment_id_mapping (payment_id);

-- Add comments for payment_id_mapping table
COMMENT ON TABLE payment_id_mapping IS 'Maps external reference numbers to internal payment IDs';
COMMENT ON COLUMN payment_id_mapping.reference_number IS 'External reference number (primary key)';
COMMENT ON COLUMN payment_id_mapping.payment_id IS 'Internal payment aggregate identifier';
