-- V19: company plans and modules (phase 3 of the SaaS plan).
--
-- A company has one subscription row. Its effective plan is computed on read (no cron):
-- an expired TRIAL behaves as FREE and never turns into a charge. Prices are stored in
-- cents without VAT and are only ever quoted by the server. No money moves here: plan
-- changes are requested by the company and approved by the platform operator (OryKai),
-- who invoices manually (CLAUDE.md lane A). Existing companies start on BETA (everything,
-- free) until a date announced 60 days in advance.

CREATE TABLE IF NOT EXISTS subscription_plans (
    plan_key varchar(20) PRIMARY KEY,
    name varchar(60) NOT NULL,
    monthly_price_cents integer NOT NULL,
    annual_monthly_price_cents integer NOT NULL,
    -- Staff users included (gestoría not counted). NULL = unlimited.
    included_users integer,
    -- NULL = unlimited.
    max_clients integer,
    modules text[] NOT NULL,
    -- For FREE: modules the company can pick one of, on top of `modules`.
    selectable_free_modules text[] NOT NULL DEFAULT '{}',
    is_purchasable boolean NOT NULL,
    sort_order integer NOT NULL,
    CONSTRAINT subscription_plans_prices_check CHECK (monthly_price_cents >= 0 AND annual_monthly_price_cents >= 0)
);

INSERT INTO subscription_plans
    (plan_key, name, monthly_price_cents, annual_monthly_price_cents, included_users, max_clients, modules, selectable_free_modules, is_purchasable, sort_order)
VALUES
    ('FREE', 'Gratis', 0, 0, 1, 25, ARRAY['CORE'], ARRAY['WORK', 'SUPPORT', 'SALES', 'AGENDA'], TRUE, 10),
    ('AUTONOMO', 'Autónomo', 1200, 1000, 1, NULL, ARRAY['CORE', 'WORK', 'SALES', 'BILLING', 'AGENDA', 'PORTAL'], '{}', TRUE, 20),
    ('PRO', 'Pro', 2900, 2400, 3, NULL,
        ARRAY['CORE', 'WORK', 'SALES', 'BILLING', 'AGENDA', 'PORTAL', 'SUPPORT', 'DOCUMENTS', 'REPORTS'], '{}', TRUE, 30),
    ('TEAM', 'Equipo', 4900, 4100, 8, NULL,
        ARRAY['CORE', 'WORK', 'SALES', 'BILLING', 'AGENDA', 'PORTAL', 'SUPPORT', 'DOCUMENTS', 'REPORTS'], '{}', TRUE, 40),
    ('BETA', 'Beta', 0, 0, NULL, NULL,
        ARRAY['CORE', 'WORK', 'SALES', 'BILLING', 'AGENDA', 'PORTAL', 'SUPPORT', 'DOCUMENTS', 'REPORTS'], '{}', FALSE, 90)
ON CONFLICT (plan_key) DO NOTHING;

CREATE TABLE IF NOT EXISTS organization_subscriptions (
    organization_id uuid PRIMARY KEY REFERENCES organizations(id) ON DELETE CASCADE,
    plan_key varchar(20) NOT NULL REFERENCES subscription_plans(plan_key),
    status varchar(12) NOT NULL,
    billing_period varchar(10),
    trial_ends_at timestamptz,
    free_module varchar(20),
    beta_ends_on date,
    founder_discount_percent integer NOT NULL DEFAULT 0,
    started_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT organization_subscriptions_status_check CHECK (status IN ('TRIAL', 'ACTIVE', 'BETA')),
    CONSTRAINT organization_subscriptions_period_check CHECK (billing_period IS NULL OR billing_period IN ('MONTHLY', 'ANNUAL')),
    CONSTRAINT organization_subscriptions_trial_check CHECK (status <> 'TRIAL' OR trial_ends_at IS NOT NULL),
    CONSTRAINT organization_subscriptions_discount_check CHECK (founder_discount_percent BETWEEN 0 AND 100)
);

CREATE TABLE IF NOT EXISTS organization_plan_events (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id uuid NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    action varchar(30) NOT NULL,
    from_plan varchar(20),
    to_plan varchar(20),
    billing_period varchar(10),
    price_cents integer,
    actor_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
    at timestamptz NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_organization_plan_events_org ON organization_plan_events(organization_id, at);

CREATE TABLE IF NOT EXISTS organization_plan_requests (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id uuid NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    plan_key varchar(20) NOT NULL REFERENCES subscription_plans(plan_key),
    billing_period varchar(10) NOT NULL,
    -- Price quoted by the server when the request was made (cents/month, no VAT).
    quoted_monthly_price_cents integer NOT NULL,
    status varchar(12) NOT NULL DEFAULT 'REQUESTED',
    requested_by uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    -- When the requester explicitly accepted the quoted price.
    accepted_terms_at timestamptz NOT NULL,
    decided_by uuid REFERENCES users(id) ON DELETE SET NULL,
    decided_at timestamptz,
    note varchar(500),
    created_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT organization_plan_requests_status_check CHECK (status IN ('REQUESTED', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT organization_plan_requests_period_check CHECK (billing_period IN ('MONTHLY', 'ANNUAL'))
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_organization_plan_requests_one_open
    ON organization_plan_requests(organization_id) WHERE status = 'REQUESTED';

-- The platform operator approves plan requests: OryKai (the company with local PDF invoices).
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS is_platform_operator boolean NOT NULL DEFAULT FALSE;
UPDATE organizations SET is_platform_operator = TRUE WHERE local_pdf_invoices = TRUE;

-- Every existing company keeps everything for free during the beta.
INSERT INTO organization_subscriptions (organization_id, plan_key, status)
SELECT id, 'BETA', 'BETA' FROM organizations
ON CONFLICT (organization_id) DO NOTHING;

INSERT INTO organization_plan_events (organization_id, action, to_plan)
SELECT id, 'BETA_ASSIGNED', 'BETA' FROM organizations;

DROP TRIGGER IF EXISTS touch_organization_subscriptions_updated_at ON organization_subscriptions;
CREATE TRIGGER touch_organization_subscriptions_updated_at
BEFORE UPDATE ON organization_subscriptions
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DO $$
DECLARE
    private_table text;
    api_role text;
BEGIN
    FOREACH private_table IN ARRAY ARRAY[
        'subscription_plans', 'organization_subscriptions', 'organization_plan_events', 'organization_plan_requests'
    ]
    LOOP
        EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', private_table);
        EXECUTE format('REVOKE ALL PRIVILEGES ON TABLE public.%I FROM PUBLIC', private_table);
    END LOOP;

    FOREACH api_role IN ARRAY ARRAY['anon', 'authenticated']
    LOOP
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = api_role) THEN
            FOREACH private_table IN ARRAY ARRAY[
                'subscription_plans', 'organization_subscriptions', 'organization_plan_events', 'organization_plan_requests'
            ]
            LOOP
                EXECUTE format('REVOKE ALL PRIVILEGES ON TABLE public.%I FROM %I', private_table, api_role);
            END LOOP;
        END IF;
    END LOOP;
END;
$$;
