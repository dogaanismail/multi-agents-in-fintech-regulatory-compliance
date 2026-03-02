CREATE TABLE exchange_rate
(
    id            UUID PRIMARY KEY,
    currency_pair VARCHAR(6)               NOT NULL,
    rate          DECIMAL(19, 8)           NOT NULL,
    fetched_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version       INTEGER                  NOT NULL DEFAULT 0,
    CONSTRAINT uq_exchange_rate_pair UNIQUE (currency_pair),
    CONSTRAINT chk_exchange_rate_positive CHECK (rate > 0)
);

CREATE INDEX idx_exchange_rate_currency_pair ON exchange_rate (currency_pair);
CREATE INDEX idx_exchange_rate_fetched_at ON exchange_rate (fetched_at);
