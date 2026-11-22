-- Fila de entregas: uma linha por par (evento, endpoint)
CREATE TABLE deliveries (
    id               UUID PRIMARY KEY,
    event_id         UUID        NOT NULL REFERENCES events (id),
    endpoint_id      UUID        NOT NULL REFERENCES endpoints (id),
    status           VARCHAR(20) NOT NULL,
    attempt_count    INTEGER     NOT NULL DEFAULT 0,
    next_attempt_at  TIMESTAMPTZ NOT NULL,
    locked_until     TIMESTAMPTZ,
    last_status_code INTEGER,
    last_error       TEXT,
    created_at       TIMESTAMPTZ NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_deliveries_event_endpoint UNIQUE (event_id, endpoint_id)
);

-- Indices parciais: so cobrem as linhas que o worker procura, entao continuam
-- pequenos mesmo com milhoes de entregas ja concluidas
CREATE INDEX idx_deliveries_pending ON deliveries (next_attempt_at) WHERE status = 'PENDING';
CREATE INDEX idx_deliveries_in_progress ON deliveries (locked_until) WHERE status = 'IN_PROGRESS';

-- Cada tentativa passa a apontar para a entrega a que pertence.
-- Nulo nas tentativas antigas, feitas antes da fila existir.
ALTER TABLE delivery_attempts ADD COLUMN delivery_id UUID REFERENCES deliveries (id);
CREATE INDEX idx_delivery_attempts_delivery ON delivery_attempts (delivery_id);
