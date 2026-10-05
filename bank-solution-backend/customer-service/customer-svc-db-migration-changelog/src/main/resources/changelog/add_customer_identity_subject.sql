ALTER TABLE customer
    ADD COLUMN identity_subject VARCHAR(255);

CREATE UNIQUE INDEX uk_customer_identity_subject ON customer (identity_subject);

COMMENT ON COLUMN customer.identity_subject IS 'Subject of the bank-customers login that owns this customer; null for customers created by staff';
