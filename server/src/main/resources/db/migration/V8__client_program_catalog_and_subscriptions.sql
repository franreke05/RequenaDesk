-- Commercial catalog for the client portal. The catalog is server-authorized;
-- no client can activate or price a product directly.

CREATE TABLE IF NOT EXISTS product_catalog (
    product_key varchar(60) PRIMARY KEY,
    name varchar(120) NOT NULL,
    short_description varchar(360) NOT NULL,
    category varchar(80) NOT NULL,
    icon_key varchar(80) NOT NULL,
    monthly_price_cents integer NOT NULL,
    currency char(3) NOT NULL DEFAULT 'EUR',
    is_requestable boolean NOT NULL DEFAULT TRUE,
    is_available boolean NOT NULL DEFAULT TRUE,
    capabilities jsonb NOT NULL DEFAULT '[]'::jsonb,
    sort_order integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT product_catalog_key_check CHECK (product_key ~ '^[A-Z][A-Z0-9_]{1,59}$'),
    CONSTRAINT product_catalog_price_check CHECK (monthly_price_cents >= 0),
    CONSTRAINT product_catalog_currency_check CHECK (currency = 'EUR'),
    CONSTRAINT product_catalog_capabilities_array_check CHECK (jsonb_typeof(capabilities) = 'array')
);

CREATE TABLE IF NOT EXISTS client_product_subscriptions (
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    product_key varchar(60) NOT NULL REFERENCES product_catalog(product_key) ON DELETE RESTRICT,
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    monthly_price_cents integer NOT NULL DEFAULT 0,
    currency char(3) NOT NULL DEFAULT 'EUR',
    starts_on date NOT NULL DEFAULT CURRENT_DATE,
    ends_on date,
    approved_by_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
    approved_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW(),
    PRIMARY KEY (client_id, product_key),
    CONSTRAINT client_product_subscriptions_status_check CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CANCELLED')),
    CONSTRAINT client_product_subscriptions_price_check CHECK (monthly_price_cents >= 0),
    CONSTRAINT client_product_subscriptions_currency_check CHECK (currency = 'EUR'),
    CONSTRAINT client_product_subscriptions_dates_check CHECK (ends_on IS NULL OR ends_on >= starts_on)
);

CREATE INDEX IF NOT EXISTS client_product_subscriptions_active_idx
    ON client_product_subscriptions (client_id, status, starts_on);

CREATE TABLE IF NOT EXISTS client_program_requests (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    product_key varchar(60) NOT NULL REFERENCES product_catalog(product_key) ON DELETE RESTRICT,
    requested_by_user_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    status varchar(20) NOT NULL DEFAULT 'REQUESTED',
    customer_note varchar(500),
    admin_note varchar(500),
    quoted_monthly_price_cents integer,
    currency char(3) NOT NULL DEFAULT 'EUR',
    requested_at timestamptz NOT NULL DEFAULT NOW(),
    decided_at timestamptz,
    decided_by_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT client_program_requests_status_check CHECK (status IN ('REQUESTED', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT client_program_requests_quote_check CHECK (quoted_monthly_price_cents IS NULL OR quoted_monthly_price_cents > 0),
    CONSTRAINT client_program_requests_currency_check CHECK (currency = 'EUR'),
    CONSTRAINT client_program_requests_decision_check CHECK (
        (status = 'REQUESTED' AND decided_at IS NULL AND decided_by_user_id IS NULL)
        OR (status IN ('APPROVED', 'REJECTED', 'CANCELLED') AND decided_at IS NOT NULL)
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS client_program_requests_pending_unique_idx
    ON client_program_requests (client_id, product_key)
    WHERE status = 'REQUESTED';

CREATE INDEX IF NOT EXISTS client_program_requests_admin_queue_idx
    ON client_program_requests (status, requested_at, client_id);

CREATE TABLE IF NOT EXISTS client_subscription_events (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    product_key varchar(60) NOT NULL REFERENCES product_catalog(product_key) ON DELETE RESTRICT,
    request_id uuid REFERENCES client_program_requests(id) ON DELETE SET NULL,
    actor_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
    action varchar(40) NOT NULL,
    previous_status varchar(20),
    new_status varchar(20) NOT NULL,
    monthly_price_cents integer,
    currency char(3) NOT NULL DEFAULT 'EUR',
    created_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT client_subscription_events_action_check CHECK (action IN ('REQUESTED', 'APPROVED', 'REJECTED', 'ACTIVATED', 'SUSPENDED', 'CANCELLED', 'LEGACY_MIGRATED')),
    CONSTRAINT client_subscription_events_status_check CHECK (new_status IN ('REQUESTED', 'APPROVED', 'REJECTED', 'CANCELLED', 'ACTIVE', 'SUSPENDED')),
    CONSTRAINT client_subscription_events_price_check CHECK (monthly_price_cents IS NULL OR monthly_price_cents >= 0),
    CONSTRAINT client_subscription_events_currency_check CHECK (currency = 'EUR')
);

CREATE INDEX IF NOT EXISTS client_subscription_events_client_idx
    ON client_subscription_events (client_id, created_at DESC);

INSERT INTO product_catalog (
    product_key, name, short_description, category, icon_key,
    monthly_price_cents, currency, is_requestable, is_available, capabilities, sort_order
) VALUES
    (
        'SERVICE_SLA',
        'Servicio y SLA',
        'Seguimiento de soporte, consumo y revisiones de servicio.',
        'Servicio',
        'headphones',
        2500,
        'EUR',
        TRUE,
        TRUE,
        '["service_overview", "sla_tracking", "monthly_service_summary"]'::jsonb,
        10
    ),
    (
        'SHEETS',
        'Hojas de datos',
        'Plantillas de presupuesto, control y planificación para tu equipo.',
        'Utilidades',
        'table-2',
        1200,
        'EUR',
        TRUE,
        TRUE,
        '["budget_templates", "expense_tracking", "project_planning"]'::jsonb,
        20
    )
ON CONFLICT (product_key) DO UPDATE
SET
    name = EXCLUDED.name,
    short_description = EXCLUDED.short_description,
    category = EXCLUDED.category,
    icon_key = EXCLUDED.icon_key,
    monthly_price_cents = EXCLUDED.monthly_price_cents,
    currency = EXCLUDED.currency,
    is_requestable = EXCLUDED.is_requestable,
    is_available = EXCLUDED.is_available,
    capabilities = EXCLUDED.capabilities,
    sort_order = EXCLUDED.sort_order,
    updated_at = NOW();

-- Preserve existing manually granted Service/SLA access without assigning a
-- charge retroactively. Future requests are approved with an explicit price.
INSERT INTO client_product_subscriptions (
    client_id, product_key, status, monthly_price_cents, currency, starts_on, approved_at
)
SELECT client_id, component_key, 'ACTIVE', 0, 'EUR', CURRENT_DATE, NOW()
FROM client_component_entitlements
WHERE component_key = 'SERVICE_SLA'
ON CONFLICT (client_id, product_key) DO NOTHING;

INSERT INTO client_subscription_events (
    client_id, product_key, action, new_status, monthly_price_cents, currency
)
SELECT cce.client_id, cce.component_key, 'LEGACY_MIGRATED', 'ACTIVE', 0, 'EUR'
FROM client_component_entitlements cce
WHERE cce.component_key = 'SERVICE_SLA'
  AND NOT EXISTS (
      SELECT 1
      FROM client_subscription_events event
      WHERE event.client_id = cce.client_id
        AND event.product_key = cce.component_key
        AND event.action = 'LEGACY_MIGRATED'
  );

DROP TRIGGER IF EXISTS touch_product_catalog_updated_at ON product_catalog;
CREATE TRIGGER touch_product_catalog_updated_at
BEFORE UPDATE ON product_catalog
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS touch_client_product_subscriptions_updated_at ON client_product_subscriptions;
CREATE TRIGGER touch_client_product_subscriptions_updated_at
BEFORE UPDATE ON client_product_subscriptions
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

ALTER TABLE public.product_catalog ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.client_product_subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.client_program_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.client_subscription_events ENABLE ROW LEVEL SECURITY;

REVOKE ALL PRIVILEGES ON public.product_catalog FROM PUBLIC;
REVOKE ALL PRIVILEGES ON public.client_product_subscriptions FROM PUBLIC;
REVOKE ALL PRIVILEGES ON public.client_program_requests FROM PUBLIC;
REVOKE ALL PRIVILEGES ON public.client_subscription_events FROM PUBLIC;

DO $$
DECLARE
    api_role text;
BEGIN
    FOREACH api_role IN ARRAY ARRAY['anon', 'authenticated']
    LOOP
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = api_role) THEN
            EXECUTE format('REVOKE ALL PRIVILEGES ON TABLE public.product_catalog FROM %I', api_role);
            EXECUTE format('REVOKE ALL PRIVILEGES ON TABLE public.client_product_subscriptions FROM %I', api_role);
            EXECUTE format('REVOKE ALL PRIVILEGES ON TABLE public.client_program_requests FROM %I', api_role);
            EXECUTE format('REVOKE ALL PRIVILEGES ON TABLE public.client_subscription_events FROM %I', api_role);
        END IF;
    END LOOP;
END;
$$;
