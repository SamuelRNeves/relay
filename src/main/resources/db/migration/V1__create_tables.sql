-- Destinos que recebem os eventos
CREATE TABLE endpoints (
    id          UUID PRIMARY KEY,
    url         TEXT        NOT NULL,
    event_types TEXT[]      NOT NULL,
    active      BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL
);

-- Eventos publicados na API
CREATE TABLE events (
    id         UUID PRIMARY KEY,
    type       VARCHAR(100) NOT NULL,
    payload    JSONB        NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL
);

-- Cada tentativa de entregar um evento a um endpoint
CREATE TABLE delivery_attempts (
    id            UUID PRIMARY KEY,
    event_id      UUID        NOT NULL REFERENCES events (id),
    endpoint_id   UUID        NOT NULL REFERENCES endpoints (id),
    success       BOOLEAN     NOT NULL,
    status_code   INTEGER,
    error_message TEXT,
    duration_ms   BIGINT      NOT NULL,
    attempted_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_delivery_attempts_event ON delivery_attempts (event_id);
CREATE INDEX idx_endpoints_event_types ON endpoints USING GIN (event_types);
