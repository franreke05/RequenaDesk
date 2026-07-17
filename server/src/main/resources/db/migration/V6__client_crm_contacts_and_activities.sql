-- Final planned CRM schema change: operational contacts and internal follow-up.
-- These records are intentionally admin-only; client portal routes expose a
-- separate, sanitized read model.

CREATE TABLE IF NOT EXISTS client_contacts (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    full_name varchar(180) NOT NULL,
    email citext,
    phone varchar(80),
    role varchar(120),
    is_primary boolean NOT NULL DEFAULT FALSE,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS client_contacts_primary_per_client_idx
    ON client_contacts (client_id)
    WHERE is_primary;

CREATE INDEX IF NOT EXISTS client_contacts_client_id_idx
    ON client_contacts (client_id, full_name);

CREATE TABLE IF NOT EXISTS client_activities (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    contact_id uuid REFERENCES client_contacts(id) ON DELETE SET NULL,
    created_by uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    activity_type varchar(20) NOT NULL,
    subject varchar(220) NOT NULL,
    details text,
    due_date date,
    completed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT client_activities_type_check
        CHECK (activity_type IN ('CALL', 'EMAIL', 'MEETING', 'FOLLOW_UP', 'NOTE'))
);

CREATE INDEX IF NOT EXISTS client_activities_client_timeline_idx
    ON client_activities (client_id, completed_at, due_date, created_at DESC);

CREATE INDEX IF NOT EXISTS client_activities_contact_id_idx
    ON client_activities (contact_id);

DROP TRIGGER IF EXISTS touch_client_contacts_updated_at ON client_contacts;
CREATE TRIGGER touch_client_contacts_updated_at
BEFORE UPDATE ON client_contacts
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS touch_client_activities_updated_at ON client_activities;
CREATE TRIGGER touch_client_activities_updated_at
BEFORE UPDATE ON client_activities
FOR EACH ROW EXECUTE FUNCTION set_updated_at();
