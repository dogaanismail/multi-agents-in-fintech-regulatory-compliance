-- Create a customer_profile table
CREATE TABLE customer_profile
(
    id                            UUID PRIMARY KEY,
    customer_id                   UUID           NOT NULL UNIQUE,
    account_id                    UUID           NOT NULL,
    transaction_count             INTEGER        NOT NULL DEFAULT 0,
    total_amount                  NUMERIC(19, 4) NOT NULL DEFAULT 0,
    avg_amount                    NUMERIC(19, 4) NOT NULL DEFAULT 0,
    median_amount                 NUMERIC(19, 4) NOT NULL DEFAULT 0,
    max_amount                    NUMERIC(19, 4) NOT NULL DEFAULT 0,
    min_amount                    NUMERIC(19, 4) NOT NULL DEFAULT 0,
    std_amount                    NUMERIC(19, 4) NOT NULL DEFAULT 0,
    active_days                   INTEGER        NOT NULL DEFAULT 0,
    transactions_per_day          NUMERIC(10, 4) NOT NULL DEFAULT 0,
    first_transaction_date        DATE,
    last_transaction_date         DATE,
    cross_border_count            INTEGER        NOT NULL DEFAULT 0,
    cash_transaction_count        INTEGER        NOT NULL DEFAULT 0,
    large_transaction_count       INTEGER        NOT NULL DEFAULT 0,
    night_transaction_count       INTEGER        NOT NULL DEFAULT 0,
    weekend_transaction_count     INTEGER        NOT NULL DEFAULT 0,
    cross_border_ratio            NUMERIC(5, 4)  NOT NULL DEFAULT 0,
    cash_transaction_ratio        NUMERIC(5, 4)  NOT NULL DEFAULT 0,
    large_transaction_ratio       NUMERIC(5, 4)  NOT NULL DEFAULT 0,
    night_transaction_ratio       NUMERIC(5, 4)  NOT NULL DEFAULT 0,
    weekend_transaction_ratio     NUMERIC(5, 4)  NOT NULL DEFAULT 0,
    unique_receivers              INTEGER        NOT NULL DEFAULT 0,
    unique_receiver_countries     INTEGER        NOT NULL DEFAULT 0,
    receiver_diversity            NUMERIC(5, 4)  NOT NULL DEFAULT 0,
    unique_currencies             INTEGER        NOT NULL DEFAULT 0,
    amount_consistency            NUMERIC(5, 4)  NOT NULL DEFAULT 0,
    last_updated_at               TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version                       INTEGER        NOT NULL DEFAULT 0
);

-- Create indexes
CREATE UNIQUE INDEX idx_customer_profile_customer_id ON customer_profile (customer_id);
CREATE INDEX idx_customer_profile_account_id ON customer_profile (account_id);

-- Add comments
COMMENT ON TABLE customer_profile IS 'Stores aggregated customer behavioral features for fraud detection';
COMMENT ON COLUMN customer_profile.id IS 'Unique identifier for customer profile';
COMMENT ON COLUMN customer_profile.customer_id IS 'Reference to customer (unique)';
COMMENT ON COLUMN customer_profile.account_id IS 'Primary account ID for the customer';
COMMENT ON COLUMN customer_profile.transaction_count IS 'Total number of transactions';
COMMENT ON COLUMN customer_profile.total_amount IS 'Sum of all transaction amounts';
COMMENT ON COLUMN customer_profile.avg_amount IS 'Average transaction amount';
COMMENT ON COLUMN customer_profile.median_amount IS 'Median transaction amount';
COMMENT ON COLUMN customer_profile.max_amount IS 'Maximum transaction amount';
COMMENT ON COLUMN customer_profile.min_amount IS 'Minimum transaction amount';
COMMENT ON COLUMN customer_profile.std_amount IS 'Standard deviation of transaction amounts';
COMMENT ON COLUMN customer_profile.active_days IS 'Number of unique days with transactions';
COMMENT ON COLUMN customer_profile.transactions_per_day IS 'Average transactions per day';
COMMENT ON COLUMN customer_profile.first_transaction_date IS 'Date of first transaction';
COMMENT ON COLUMN customer_profile.last_transaction_date IS 'Date of most recent transaction';
COMMENT ON COLUMN customer_profile.cross_border_count IS 'Count of cross-border transactions';
COMMENT ON COLUMN customer_profile.cash_transaction_count IS 'Count of cash transactions (deposits/withdrawals)';
COMMENT ON COLUMN customer_profile.large_transaction_count IS 'Count of large transactions (>$10,000)';
COMMENT ON COLUMN customer_profile.night_transaction_count IS 'Count of night transactions (22:00-06:00)';
COMMENT ON COLUMN customer_profile.weekend_transaction_count IS 'Count of weekend transactions';
COMMENT ON COLUMN customer_profile.cross_border_ratio IS 'Ratio of cross-border transactions to total';
COMMENT ON COLUMN customer_profile.cash_transaction_ratio IS 'Ratio of cash transactions to total';
COMMENT ON COLUMN customer_profile.large_transaction_ratio IS 'Ratio of large transactions to total';
COMMENT ON COLUMN customer_profile.night_transaction_ratio IS 'Ratio of night transactions to total';
COMMENT ON COLUMN customer_profile.weekend_transaction_ratio IS 'Ratio of weekend transactions to total';
COMMENT ON COLUMN customer_profile.unique_receivers IS 'Count of unique receiver accounts';
COMMENT ON COLUMN customer_profile.unique_receiver_countries IS 'Count of unique receiver countries';
COMMENT ON COLUMN customer_profile.receiver_diversity IS 'Entropy-based diversity of receiver distribution';
COMMENT ON COLUMN customer_profile.unique_currencies IS 'Count of unique currencies used';
COMMENT ON COLUMN customer_profile.amount_consistency IS 'Measure of transaction amount consistency (1 - CV)';
COMMENT ON COLUMN customer_profile.last_updated_at IS 'Timestamp when profile was last recalculated';
COMMENT ON COLUMN customer_profile.version IS 'Optimistic locking version';