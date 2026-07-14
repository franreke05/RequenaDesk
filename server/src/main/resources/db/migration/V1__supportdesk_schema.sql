CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS citext;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE SEQUENCE IF NOT EXISTS ticket_number_seq START WITH 1 INCREMENT BY 1;

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION assign_ticket_number()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.ticket_number IS NULL OR BTRIM(NEW.ticket_number) = '' THEN
        NEW.ticket_number := 'RDS-' || LPAD(NEXTVAL('ticket_number_seq')::text, 6, '0');
    END IF;
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION touch_ticket_from_related()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    target_ticket_id uuid;
BEGIN
    target_ticket_id := COALESCE(NEW.ticket_id, OLD.ticket_id);
    IF target_ticket_id IS NOT NULL THEN
        UPDATE tickets SET updated_at = NOW() WHERE id = target_ticket_id;
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$;

CREATE TABLE IF NOT EXISTS clients (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_admin_id uuid NOT NULL,
    company_name varchar(180) NOT NULL,
    product_name varchar(180) NOT NULL,
    contact_name varchar(180) NOT NULL,
    email citext NOT NULL UNIQUE,
    account_status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    service_tier varchar(20) NOT NULL DEFAULT 'STANDARD',
    preferred_contact_channel varchar(20) NOT NULL DEFAULT 'TICKET',
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT clients_account_status_check
        CHECK (account_status IN ('ACTIVE', 'PAUSED', 'INACTIVE')),
    CONSTRAINT clients_service_tier_check
        CHECK (service_tier IN ('STANDARD', 'PRIORITY', 'VIP')),
    CONSTRAINT clients_preferred_contact_channel_check
        CHECK (preferred_contact_channel IN ('TICKET', 'EMAIL', 'WHATSAPP', 'CALL'))
);

CREATE TABLE IF NOT EXISTS users (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid REFERENCES clients(id) ON DELETE SET NULL,
    name varchar(180) NOT NULL,
    email citext NOT NULL UNIQUE,
    password_hash text NOT NULL,
    role varchar(20) NOT NULL,
    is_active boolean NOT NULL DEFAULT TRUE,
    last_login_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT users_role_check CHECK (role IN ('CLIENT', 'ADMIN'))
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash text NOT NULL UNIQUE,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS tickets (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    requester_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    assignee_id uuid REFERENCES users(id) ON DELETE SET NULL,
    ticket_number varchar(20) NOT NULL UNIQUE,
    subject varchar(220) NOT NULL,
    description text NOT NULL,
    category varchar(30) NOT NULL DEFAULT 'QUESTION',
    affected_app varchar(180) NOT NULL,
    platform varchar(20) NOT NULL DEFAULT 'DESKTOP',
    app_version varchar(80),
    steps_to_reproduce text,
    client_reference varchar(120),
    status varchar(30) NOT NULL DEFAULT 'OPEN',
    priority varchar(20) NOT NULL DEFAULT 'MEDIUM',
    waiting_on varchar(20) NOT NULL DEFAULT 'ADMIN',
    resolution_summary text,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW(),
    resolved_at timestamptz,
    closed_at timestamptz,
    CONSTRAINT tickets_category_check
        CHECK (category IN ('BUG', 'ACCESS', 'BILLING', 'CHANGE_REQUEST', 'QUESTION', 'OTHER')),
    CONSTRAINT tickets_platform_check
        CHECK (platform IN ('ANDROID', 'IOS', 'DESKTOP', 'WEB', 'BACKEND', 'OTHER')),
    CONSTRAINT tickets_status_check
        CHECK (status IN ('OPEN', 'IN_PROGRESS', 'PENDING_CLIENT', 'RESOLVED', 'CLOSED')),
    CONSTRAINT tickets_priority_check
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    CONSTRAINT tickets_waiting_on_check CHECK (waiting_on IN ('CLIENT', 'ADMIN'))
);

CREATE TABLE IF NOT EXISTS ticket_messages (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id uuid NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    author_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    body text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS internal_comments (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id uuid NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    author_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    body text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS attachments (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id uuid REFERENCES tickets(id) ON DELETE CASCADE,
    message_id uuid REFERENCES ticket_messages(id) ON DELETE CASCADE,
    file_name varchar(255) NOT NULL,
    content_type varchar(120) NOT NULL,
    storage_key text NOT NULL,
    size_bytes bigint NOT NULL,
    uploaded_by uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    uploaded_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT attachments_size_bytes_check CHECK (size_bytes >= 0),
    CONSTRAINT attachments_owner_check CHECK (
        (ticket_id IS NOT NULL AND message_id IS NULL)
        OR (ticket_id IS NULL AND message_id IS NOT NULL)
    )
);

CREATE TABLE IF NOT EXISTS ticket_events (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id uuid NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    actor_id uuid REFERENCES users(id) ON DELETE SET NULL,
    type varchar(50) NOT NULL,
    description text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS notification_devices (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    platform varchar(20) NOT NULL,
    token text NOT NULL UNIQUE,
    last_seen_at timestamptz NOT NULL DEFAULT NOW(),
    created_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT notification_devices_platform_check CHECK (platform IN ('ANDROID', 'IOS'))
);

CREATE TABLE IF NOT EXISTS task_labels (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_admin_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    name varchar(120) NOT NULL,
    color_hex varchar(7) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT task_labels_owner_name_key UNIQUE (owner_admin_id, name)
);

CREATE TABLE IF NOT EXISTS tasks (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid REFERENCES clients(id) ON DELETE SET NULL,
    owner_admin_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    label_id uuid NOT NULL REFERENCES task_labels(id) ON DELETE RESTRICT,
    title varchar(220) NOT NULL,
    description text NOT NULL DEFAULT '',
    due_date date,
    completed boolean NOT NULL DEFAULT FALSE,
    logged_minutes integer NOT NULL DEFAULT 0,
    logged_seconds integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT tasks_logged_minutes_check CHECK (logged_minutes >= 0),
    CONSTRAINT tasks_logged_seconds_check CHECK (logged_seconds >= 0)
);

CREATE TABLE IF NOT EXISTS time_logs (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id uuid NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    client_id uuid REFERENCES clients(id) ON DELETE SET NULL,
    author_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    minutes integer NOT NULL,
    seconds integer NOT NULL DEFAULT 0,
    work_date date NOT NULL,
    note text NOT NULL DEFAULT '',
    billable boolean NOT NULL DEFAULT FALSE,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT time_logs_minutes_check CHECK (minutes >= 0),
    CONSTRAINT time_logs_seconds_check CHECK (seconds > 0)
);

ALTER TABLE clients ADD COLUMN IF NOT EXISTS owner_admin_id uuid;
ALTER TABLE task_labels ADD COLUMN IF NOT EXISTS owner_admin_id uuid;
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS owner_admin_id uuid;
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS logged_seconds integer NOT NULL DEFAULT 0;
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS due_date date;
ALTER TABLE time_logs ADD COLUMN IF NOT EXISTS seconds integer NOT NULL DEFAULT 0;

UPDATE time_logs SET seconds = GREATEST(minutes * 60, 1) WHERE seconds <= 0;
UPDATE tasks SET logged_seconds = logged_minutes * 60 WHERE logged_seconds <= 0 AND logged_minutes > 0;
UPDATE tasks SET logged_minutes = logged_seconds / 60;

UPDATE clients
SET owner_admin_id = (
    SELECT id FROM users WHERE role = 'ADMIN' AND is_active = TRUE ORDER BY created_at LIMIT 1
)
WHERE owner_admin_id IS NULL;

UPDATE task_labels
SET owner_admin_id = (
    SELECT id FROM users WHERE role = 'ADMIN' AND is_active = TRUE ORDER BY created_at LIMIT 1
)
WHERE owner_admin_id IS NULL;

UPDATE tasks t
SET owner_admin_id = c.owner_admin_id
FROM clients c
WHERE t.client_id = c.id AND t.owner_admin_id IS NULL;

UPDATE tasks
SET owner_admin_id = (
    SELECT id FROM users WHERE role = 'ADMIN' AND is_active = TRUE ORDER BY created_at LIMIT 1
)
WHERE owner_admin_id IS NULL;

ALTER TABLE clients ALTER COLUMN owner_admin_id SET NOT NULL;
ALTER TABLE task_labels ALTER COLUMN owner_admin_id SET NOT NULL;
ALTER TABLE tasks ALTER COLUMN owner_admin_id SET NOT NULL;

ALTER TABLE clients DROP CONSTRAINT IF EXISTS clients_owner_admin_check;
ALTER TABLE task_labels DROP CONSTRAINT IF EXISTS task_labels_owner_admin_check;
ALTER TABLE tasks DROP CONSTRAINT IF EXISTS tasks_owner_admin_check;
ALTER TABLE task_labels DROP CONSTRAINT IF EXISTS task_labels_name_key;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'clients_owner_admin_id_fkey') THEN
        ALTER TABLE clients
            ADD CONSTRAINT clients_owner_admin_id_fkey
            FOREIGN KEY (owner_admin_id) REFERENCES users(id) ON DELETE RESTRICT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'task_labels_owner_admin_id_fkey') THEN
        ALTER TABLE task_labels
            ADD CONSTRAINT task_labels_owner_admin_id_fkey
            FOREIGN KEY (owner_admin_id) REFERENCES users(id) ON DELETE RESTRICT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'tasks_owner_admin_id_fkey') THEN
        ALTER TABLE tasks
            ADD CONSTRAINT tasks_owner_admin_id_fkey
            FOREIGN KEY (owner_admin_id) REFERENCES users(id) ON DELETE RESTRICT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'task_labels_owner_name_key') THEN
        ALTER TABLE task_labels
            ADD CONSTRAINT task_labels_owner_name_key UNIQUE (owner_admin_id, name);
    END IF;
END;
$$;

CREATE OR REPLACE FUNCTION enforce_admin_owner()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM users
        WHERE id = NEW.owner_admin_id AND role = 'ADMIN' AND is_active = TRUE
    ) THEN
        RAISE EXCEPTION 'owner_admin_id must reference an active admin user'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;

CREATE INDEX IF NOT EXISTS idx_users_client_id ON users(client_id);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_active_expiry
    ON refresh_tokens(expires_at) WHERE revoked_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_clients_owner_admin_id ON clients(owner_admin_id);
CREATE INDEX IF NOT EXISTS idx_tickets_client_id ON tickets(client_id);
CREATE INDEX IF NOT EXISTS idx_tickets_requester_id ON tickets(requester_id);
CREATE INDEX IF NOT EXISTS idx_tickets_assignee_id ON tickets(assignee_id);
CREATE INDEX IF NOT EXISTS idx_tickets_status ON tickets(status);
CREATE INDEX IF NOT EXISTS idx_tickets_priority ON tickets(priority);
CREATE INDEX IF NOT EXISTS idx_tickets_category ON tickets(category);
CREATE INDEX IF NOT EXISTS idx_tickets_platform ON tickets(platform);
CREATE INDEX IF NOT EXISTS idx_tickets_waiting_on ON tickets(waiting_on);
CREATE INDEX IF NOT EXISTS idx_tickets_created_at ON tickets(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_tickets_updated_at ON tickets(updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_tickets_subject_trgm ON tickets USING gin (subject gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_tickets_description_trgm ON tickets USING gin (description gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_tickets_ticket_number_trgm ON tickets USING gin (ticket_number gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_ticket_messages_ticket_id_created_at
    ON ticket_messages(ticket_id, created_at);
CREATE INDEX IF NOT EXISTS idx_ticket_messages_author_id ON ticket_messages(author_id);
CREATE INDEX IF NOT EXISTS idx_internal_comments_ticket_id_created_at
    ON internal_comments(ticket_id, created_at);
CREATE INDEX IF NOT EXISTS idx_internal_comments_author_id ON internal_comments(author_id);
CREATE INDEX IF NOT EXISTS idx_attachments_ticket_id ON attachments(ticket_id);
CREATE INDEX IF NOT EXISTS idx_attachments_message_id ON attachments(message_id);
CREATE INDEX IF NOT EXISTS idx_attachments_uploaded_by ON attachments(uploaded_by);
CREATE INDEX IF NOT EXISTS idx_ticket_events_ticket_id_created_at
    ON ticket_events(ticket_id, created_at);
CREATE INDEX IF NOT EXISTS idx_ticket_events_actor_id ON ticket_events(actor_id);
CREATE INDEX IF NOT EXISTS idx_notification_devices_user_id ON notification_devices(user_id);
CREATE INDEX IF NOT EXISTS idx_task_labels_owner_admin_id ON task_labels(owner_admin_id);
CREATE INDEX IF NOT EXISTS idx_tasks_client_id ON tasks(client_id);
CREATE INDEX IF NOT EXISTS idx_tasks_owner_admin_id ON tasks(owner_admin_id);
CREATE INDEX IF NOT EXISTS idx_tasks_label_id ON tasks(label_id);
CREATE INDEX IF NOT EXISTS idx_tasks_owner_completed ON tasks(owner_admin_id, completed);
CREATE INDEX IF NOT EXISTS idx_time_logs_task_id ON time_logs(task_id);
CREATE INDEX IF NOT EXISTS idx_time_logs_client_id ON time_logs(client_id);
CREATE INDEX IF NOT EXISTS idx_time_logs_work_date ON time_logs(work_date DESC);
CREATE INDEX IF NOT EXISTS idx_time_logs_author_id ON time_logs(author_id);
CREATE INDEX IF NOT EXISTS idx_time_logs_client_work_date ON time_logs(client_id, work_date DESC);

DROP TRIGGER IF EXISTS trg_clients_validate_owner ON clients;
CREATE TRIGGER trg_clients_validate_owner
BEFORE INSERT OR UPDATE OF owner_admin_id ON clients
FOR EACH ROW EXECUTE FUNCTION enforce_admin_owner();

DROP TRIGGER IF EXISTS trg_task_labels_validate_owner ON task_labels;
CREATE TRIGGER trg_task_labels_validate_owner
BEFORE INSERT OR UPDATE OF owner_admin_id ON task_labels
FOR EACH ROW EXECUTE FUNCTION enforce_admin_owner();

DROP TRIGGER IF EXISTS trg_tasks_validate_owner ON tasks;
CREATE TRIGGER trg_tasks_validate_owner
BEFORE INSERT OR UPDATE OF owner_admin_id ON tasks
FOR EACH ROW EXECUTE FUNCTION enforce_admin_owner();

DROP TRIGGER IF EXISTS trg_clients_set_updated_at ON clients;
CREATE TRIGGER trg_clients_set_updated_at
BEFORE UPDATE ON clients
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_users_set_updated_at ON users;
CREATE TRIGGER trg_users_set_updated_at
BEFORE UPDATE ON users
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_task_labels_set_updated_at ON task_labels;
CREATE TRIGGER trg_task_labels_set_updated_at
BEFORE UPDATE ON task_labels
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_tasks_set_updated_at ON tasks;
CREATE TRIGGER trg_tasks_set_updated_at
BEFORE UPDATE ON tasks
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_tickets_assign_ticket_number ON tickets;
CREATE TRIGGER trg_tickets_assign_ticket_number
BEFORE INSERT ON tickets
FOR EACH ROW EXECUTE FUNCTION assign_ticket_number();

DROP TRIGGER IF EXISTS trg_tickets_set_updated_at ON tickets;
CREATE TRIGGER trg_tickets_set_updated_at
BEFORE UPDATE ON tickets
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_ticket_messages_touch_ticket ON ticket_messages;
CREATE TRIGGER trg_ticket_messages_touch_ticket
AFTER INSERT OR UPDATE OR DELETE ON ticket_messages
FOR EACH ROW EXECUTE FUNCTION touch_ticket_from_related();

DROP TRIGGER IF EXISTS trg_internal_comments_touch_ticket ON internal_comments;
CREATE TRIGGER trg_internal_comments_touch_ticket
AFTER INSERT OR UPDATE OR DELETE ON internal_comments
FOR EACH ROW EXECUTE FUNCTION touch_ticket_from_related();

DROP TRIGGER IF EXISTS trg_ticket_events_touch_ticket ON ticket_events;
CREATE TRIGGER trg_ticket_events_touch_ticket
AFTER INSERT OR UPDATE OR DELETE ON ticket_events
FOR EACH ROW EXECUTE FUNCTION touch_ticket_from_related();

DROP TRIGGER IF EXISTS trg_attachments_resolve_ticket_id ON attachments;
DROP FUNCTION IF EXISTS attachment_ticket_id();

CREATE OR REPLACE FUNCTION touch_ticket_from_attachment()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    attachment_ticket_id uuid;
    attachment_message_id uuid;
BEGIN
    attachment_ticket_id := COALESCE(NEW.ticket_id, OLD.ticket_id);
    attachment_message_id := COALESCE(NEW.message_id, OLD.message_id);
    IF attachment_ticket_id IS NULL AND attachment_message_id IS NOT NULL THEN
        SELECT ticket_id INTO attachment_ticket_id
        FROM ticket_messages
        WHERE id = attachment_message_id;
    END IF;
    IF attachment_ticket_id IS NOT NULL THEN
        UPDATE tickets SET updated_at = NOW() WHERE id = attachment_ticket_id;
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$;

DROP TRIGGER IF EXISTS trg_attachments_touch_ticket ON attachments;
CREATE TRIGGER trg_attachments_touch_ticket
AFTER INSERT OR UPDATE OR DELETE ON attachments
FOR EACH ROW EXECUTE FUNCTION touch_ticket_from_attachment();

CREATE OR REPLACE VIEW admin_dashboard_summary AS
SELECT
    COUNT(*) FILTER (WHERE status IN ('OPEN', 'IN_PROGRESS'))::integer AS open_tickets,
    COUNT(*) FILTER (WHERE status = 'PENDING_CLIENT')::integer AS pending_client_tickets,
    COUNT(*) FILTER (WHERE status = 'RESOLVED' AND resolved_at::date = CURRENT_DATE)::integer AS resolved_today,
    COUNT(*) FILTER (WHERE priority = 'URGENT' AND status NOT IN ('RESOLVED', 'CLOSED'))::integer AS urgent_tickets,
    (SELECT COUNT(*) FROM clients WHERE account_status = 'ACTIVE')::integer AS active_clients
FROM tickets;

CREATE OR REPLACE VIEW ticket_queue AS
SELECT
    t.id,
    t.ticket_number,
    t.client_id,
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

CREATE OR REPLACE FUNCTION log_ticket_event(
    p_ticket_id uuid,
    p_actor_id uuid,
    p_type varchar,
    p_description text
)
RETURNS void
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO ticket_events (ticket_id, actor_id, type, description)
    VALUES (p_ticket_id, p_actor_id, p_type, p_description);
END;
$$;
