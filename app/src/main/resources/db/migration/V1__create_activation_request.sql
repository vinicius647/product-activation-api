CREATE TABLE activation_request (
    id              UUID                     NOT NULL,
    external_id     VARCHAR(64)              NOT NULL,
    customer_id     VARCHAR(64)              NOT NULL,
    product_code    VARCHAR(64)              NOT NULL,
    client_id       VARCHAR(100)             NOT NULL,
    status          VARCHAR(20)              NOT NULL,
    attempts        INTEGER                  NOT NULL,
    next_attempt_at TIMESTAMP WITH TIME ZONE,
    last_error      VARCHAR(500),
    correlation_id  VARCHAR(64),
    version         BIGINT                   NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_activation_request PRIMARY KEY (id),
    -- Duplicate protection: two inserts with the same external_id can never both succeed.
    CONSTRAINT uk_activation_request_external_id UNIQUE (external_id),
    CONSTRAINT ck_activation_request_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'))
);

-- Dispatcher: WHERE status = 'PENDING' AND next_attempt_at <= now ORDER BY next_attempt_at.
-- Also used by the reconciliation query on status = 'PROCESSING'.
CREATE INDEX idx_activation_request_dispatch ON activation_request (status, next_attempt_at);

-- Listing: always filtered by client_id, optionally by customer_id or status, ordered by created_at.
CREATE INDEX idx_activation_request_client_customer ON activation_request (client_id, customer_id, created_at);
CREATE INDEX idx_activation_request_client_status ON activation_request (client_id, status, created_at);
