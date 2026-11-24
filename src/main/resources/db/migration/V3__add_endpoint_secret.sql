-- Segredo usado para assinar (HMAC) as entregas de cada endpoint.
-- Fica em texto porque o Relay precisa dele para calcular a assinatura
-- (nao da para guardar so um hash, como se faz com senha).
ALTER TABLE endpoints ADD COLUMN secret VARCHAR(100);

-- Endpoints que ja existiam ganham um segredo aleatorio
UPDATE endpoints
SET secret = 'whsec_' || replace(gen_random_uuid()::text, '-', '') || replace(gen_random_uuid()::text, '-', '')
WHERE secret IS NULL;

ALTER TABLE endpoints ALTER COLUMN secret SET NOT NULL;
