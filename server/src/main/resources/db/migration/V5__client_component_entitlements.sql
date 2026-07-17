CREATE TABLE IF NOT EXISTS client_component_entitlements (
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    component_key varchar(60) NOT NULL,
    enabled_at timestamptz NOT NULL DEFAULT NOW(),
    PRIMARY KEY (client_id, component_key),
    CONSTRAINT client_component_entitlements_component_key_check
        CHECK (component_key IN ('SERVICE_SLA'))
);

CREATE INDEX IF NOT EXISTS client_component_entitlements_client_id_idx
    ON client_component_entitlements (client_id);

-- Existing priority/VIP accounts retain their existing service expectations after the
-- module is introduced. New grants are managed manually by an administrator.
INSERT INTO client_component_entitlements (client_id, component_key)
SELECT id, 'SERVICE_SLA'
FROM clients
WHERE service_tier IN ('PRIORITY', 'VIP')
ON CONFLICT (client_id, component_key) DO NOTHING;
