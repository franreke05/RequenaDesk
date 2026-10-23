-- V17: companies ("organizations") as the tenant boundary.
--
-- Until V16 every admin-side row hung off owner_admin_id (one admin = one tenant).
-- From here on the tenant is organization_id; owner_admin_id stays as "created by"
-- (the acting staff user) so existing triggers and FKs keep working.
--
-- Backfill: one organization per existing ADMIN user, who becomes its OWNER. This
-- preserves today's isolation between admins exactly. The oldest admin's company is
-- named "OryKai". Only the four root tables carry organization_id; everything else
-- (tickets, time logs, CRM, business programs...) is reached through clients/tasks.

CREATE TABLE IF NOT EXISTS organizations (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name varchar(180) NOT NULL,
    legal_form varchar(20),
    sector varchar(120),
    team_size varchar(20),
    has_accountant boolean,
    onboarding_goals text[] NOT NULL DEFAULT '{}',
    onboarding_completed_at timestamptz,
    legal_name varchar(180),
    tax_id varchar(20),
    address_line varchar(240),
    postal_code varchar(12),
    city varchar(120),
    province varchar(120),
    country varchar(2),
    legacy_owner_admin_id uuid UNIQUE,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT organizations_legal_form_check
        CHECK (legal_form IS NULL OR legal_form IN ('AUTONOMO', 'SOCIEDAD'))
);

CREATE TABLE IF NOT EXISTS organization_members (
    organization_id uuid NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role varchar(20) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    PRIMARY KEY (organization_id, user_id),
    -- Phase 2: a staff user belongs to exactly one company.
    CONSTRAINT organization_members_user_key UNIQUE (user_id),
    CONSTRAINT organization_members_role_check
        CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER', 'ACCOUNTANT'))
);

CREATE TABLE IF NOT EXISTS organization_invitations (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id uuid NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    code_hash text NOT NULL UNIQUE,
    role varchar(20) NOT NULL,
    created_by uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at timestamptz NOT NULL,
    accepted_by uuid REFERENCES users(id) ON DELETE SET NULL,
    accepted_at timestamptz,
    revoked_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT organization_invitations_role_check
        CHECK (role IN ('ADMIN', 'MEMBER', 'ACCOUNTANT'))
);

CREATE TABLE IF NOT EXISTS legal_acceptances (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    document varchar(20) NOT NULL,
    version varchar(40) NOT NULL,
    accepted_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT legal_acceptances_document_check CHECK (document IN ('TERMS', 'PRIVACY'))
);

ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified_at timestamptz;

-- One company per existing admin; the oldest one is OryKai.
INSERT INTO organizations (name, legacy_owner_admin_id, created_at)
SELECT
    CASE
        WHEN ROW_NUMBER() OVER (ORDER BY u.created_at, u.id) = 1 THEN 'OryKai'
        ELSE 'Empresa de ' || u.name
    END,
    u.id,
    u.created_at
FROM users u
WHERE u.role = 'ADMIN'
  AND NOT EXISTS (SELECT 1 FROM organizations o WHERE o.legacy_owner_admin_id = u.id);

INSERT INTO organization_members (organization_id, user_id, role)
SELECT o.id, o.legacy_owner_admin_id, 'OWNER'
FROM organizations o
WHERE o.legacy_owner_admin_id IS NOT NULL
ON CONFLICT DO NOTHING;

-- Root tables get the tenant column, backfilled from the admin that owned each row.
ALTER TABLE clients ADD COLUMN IF NOT EXISTS organization_id uuid REFERENCES organizations(id) ON DELETE RESTRICT;
ALTER TABLE task_labels ADD COLUMN IF NOT EXISTS organization_id uuid REFERENCES organizations(id) ON DELETE RESTRICT;
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS organization_id uuid REFERENCES organizations(id) ON DELETE RESTRICT;
ALTER TABLE work_sessions ADD COLUMN IF NOT EXISTS organization_id uuid REFERENCES organizations(id) ON DELETE RESTRICT;

UPDATE clients c SET organization_id = o.id
FROM organizations o WHERE o.legacy_owner_admin_id = c.owner_admin_id AND c.organization_id IS NULL;
UPDATE task_labels l SET organization_id = o.id
FROM organizations o WHERE o.legacy_owner_admin_id = l.owner_admin_id AND l.organization_id IS NULL;
UPDATE tasks t SET organization_id = o.id
FROM organizations o WHERE o.legacy_owner_admin_id = t.owner_admin_id AND t.organization_id IS NULL;
UPDATE work_sessions w SET organization_id = o.id
FROM organizations o WHERE o.legacy_owner_admin_id = w.owner_admin_id AND w.organization_id IS NULL;

ALTER TABLE clients ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE task_labels ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE tasks ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE work_sessions ALTER COLUMN organization_id SET NOT NULL;

ALTER TABLE organizations DROP COLUMN IF EXISTS legacy_owner_admin_id;

CREATE INDEX IF NOT EXISTS idx_clients_organization_id ON clients(organization_id);
CREATE INDEX IF NOT EXISTS idx_task_labels_organization_id ON task_labels(organization_id);
CREATE INDEX IF NOT EXISTS idx_tasks_organization_id ON tasks(organization_id);
CREATE INDEX IF NOT EXISTS idx_work_sessions_organization_id ON work_sessions(organization_id);
CREATE INDEX IF NOT EXISTS idx_organization_invitations_org ON organization_invitations(organization_id);
CREATE INDEX IF NOT EXISTS idx_legal_acceptances_user ON legal_acceptances(user_id);

-- Uniqueness is now per company: two companies may each have a client with the same
-- email, and label names only need to be unique inside one company.
ALTER TABLE clients DROP CONSTRAINT IF EXISTS clients_email_key;
ALTER TABLE clients ADD CONSTRAINT clients_organization_email_key UNIQUE (organization_id, email);
ALTER TABLE task_labels DROP CONSTRAINT IF EXISTS task_labels_owner_name_key;
ALTER TABLE task_labels ADD CONSTRAINT task_labels_organization_name_key UNIQUE (organization_id, name);

-- Reporting views now carry the tenant so they can be filtered per company.
DROP VIEW IF EXISTS admin_dashboard_summary;
CREATE VIEW admin_dashboard_summary AS
SELECT
    c.organization_id,
    COUNT(t.id) FILTER (WHERE t.status IN ('OPEN', 'IN_PROGRESS'))::integer AS open_tickets,
    COUNT(t.id) FILTER (WHERE t.status = 'PENDING_CLIENT')::integer AS pending_client_tickets,
    COUNT(t.id) FILTER (WHERE t.status = 'RESOLVED' AND t.resolved_at::date = CURRENT_DATE)::integer AS resolved_today,
    COUNT(t.id) FILTER (WHERE t.priority = 'URGENT' AND t.status NOT IN ('RESOLVED', 'CLOSED'))::integer AS urgent_tickets,
    COUNT(DISTINCT c.id) FILTER (WHERE c.account_status = 'ACTIVE')::integer AS active_clients
FROM clients c
LEFT JOIN tickets t ON t.client_id = c.id
GROUP BY c.organization_id;

DROP VIEW IF EXISTS ticket_queue;
CREATE VIEW ticket_queue AS
SELECT
    t.id,
    t.ticket_number,
    t.client_id,
    c.organization_id,
    c.company_name,
    c.product_name,
    t.subject,
    t.category,
    t.platform,
    t.status,
    t.priority,
    t.waiting_on,
    t.assignee_id,
    t.created_at,
    t.updated_at
FROM tickets t
JOIN clients c ON c.id = t.client_id;

DROP TRIGGER IF EXISTS touch_organizations_updated_at ON organizations;
CREATE TRIGGER touch_organizations_updated_at
BEFORE UPDATE ON organizations
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Same lockdown as V7–V16: Ktor is the only gateway, no public API role gets access.
DO $$
DECLARE
    private_table text;
    api_role text;
BEGIN
    FOREACH private_table IN ARRAY ARRAY[
        'organizations',
        'organization_members',
        'organization_invitations',
        'legal_acceptances'
    ]
    LOOP
        EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', private_table);
        EXECUTE format('REVOKE ALL PRIVILEGES ON TABLE public.%I FROM PUBLIC', private_table);
    END LOOP;

    FOREACH api_role IN ARRAY ARRAY['anon', 'authenticated']
    LOOP
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = api_role) THEN
            FOREACH private_table IN ARRAY ARRAY[
                'organizations',
                'organization_members',
                'organization_invitations',
                'legal_acceptances'
            ]
            LOOP
                EXECUTE format('REVOKE ALL PRIVILEGES ON TABLE public.%I FROM %I', private_table, api_role);
            END LOOP;
            EXECUTE format('REVOKE ALL PRIVILEGES ON TABLE public.admin_dashboard_summary FROM %I', api_role);
            EXECUTE format('REVOKE ALL PRIVILEGES ON TABLE public.ticket_queue FROM %I', api_role);
        END IF;
    END LOOP;
END;
$$;
