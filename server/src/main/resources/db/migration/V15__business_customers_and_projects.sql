-- V15: private commercial CRM plus projects, tasks, daily planning and time.
-- All tenant and actor identifiers are supplied by the Ktor server from its JWT.
-- These tables never reuse the administrative clients/tasks/board/time domain.

ALTER TABLE business_sales_customers
    ADD COLUMN IF NOT EXISTS customer_type varchar(16) NOT NULL DEFAULT 'COMPANY',
    ADD COLUMN IF NOT EXISTS commercial_stage varchar(16) NOT NULL DEFAULT 'CUSTOMER',
    ADD COLUMN IF NOT EXISTS commercial_source varchar(160);

UPDATE business_sales_customers
SET customer_type = 'COMPANY',
    commercial_stage = 'CUSTOMER'
WHERE customer_type IS NULL OR commercial_stage IS NULL;

ALTER TABLE business_sales_customers
    DROP CONSTRAINT IF EXISTS business_sales_customers_type_check,
    DROP CONSTRAINT IF EXISTS business_sales_customers_stage_check,
    ADD CONSTRAINT business_sales_customers_type_check
        CHECK (customer_type IN ('PERSON', 'COMPANY')),
    ADD CONSTRAINT business_sales_customers_stage_check
        CHECK (commercial_stage IN ('LEAD', 'PROSPECT', 'CUSTOMER', 'INACTIVE'));

CREATE INDEX IF NOT EXISTS business_sales_customers_stage_idx
    ON business_sales_customers (client_id, commercial_stage, status, id);

CREATE TABLE IF NOT EXISTS business_sales_customer_tags (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    name varchar(80) NOT NULL,
    color_hex varchar(7) NOT NULL,
    is_archived boolean NOT NULL DEFAULT FALSE,
    version integer NOT NULL DEFAULT 1,
    created_by_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT business_sales_customer_tags_name_check
        CHECK (length(btrim(name)) BETWEEN 1 AND 80),
    CONSTRAINT business_sales_customer_tags_color_check
        CHECK (color_hex ~ '^#[0-9A-Fa-f]{6}$'),
    CONSTRAINT business_sales_customer_tags_version_check CHECK (version > 0),
    UNIQUE (id, client_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS business_sales_customer_tags_name_idx
    ON business_sales_customer_tags (client_id, lower(name))
    WHERE is_archived = FALSE;

CREATE TABLE IF NOT EXISTS business_sales_customer_tag_links (
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    customer_id uuid NOT NULL,
    tag_id uuid NOT NULL,
    created_by_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    PRIMARY KEY (customer_id, tag_id),
    CONSTRAINT business_sales_customer_tag_links_customer_fk
        FOREIGN KEY (customer_id, client_id)
        REFERENCES business_sales_customers (id, client_id) ON DELETE CASCADE,
    CONSTRAINT business_sales_customer_tag_links_tag_fk
        FOREIGN KEY (tag_id, client_id)
        REFERENCES business_sales_customer_tags (id, client_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS business_sales_customer_tag_links_tenant_idx
    ON business_sales_customer_tag_links (client_id, tag_id, customer_id);

CREATE UNIQUE INDEX IF NOT EXISTS business_sales_contacts_activity_fk_idx
    ON business_sales_contacts (id, customer_id, client_id);

CREATE TABLE IF NOT EXISTS business_sales_customer_activities (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    customer_id uuid NOT NULL,
    contact_id uuid,
    activity_type varchar(16) NOT NULL,
    subject varchar(160) NOT NULL,
    details varchar(4000),
    occurred_at timestamptz,
    due_at timestamptz,
    completed_at timestamptz,
    version integer NOT NULL DEFAULT 1,
    created_by_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT business_sales_customer_activities_customer_fk
        FOREIGN KEY (customer_id, client_id)
        REFERENCES business_sales_customers (id, client_id) ON DELETE CASCADE,
    CONSTRAINT business_sales_customer_activities_contact_fk
        FOREIGN KEY (contact_id, customer_id, client_id)
        REFERENCES business_sales_contacts (id, customer_id, client_id) ON DELETE RESTRICT,
    CONSTRAINT business_sales_customer_activities_type_check
        CHECK (activity_type IN ('NOTE', 'CALL', 'EMAIL', 'MEETING', 'FOLLOW_UP')),
    CONSTRAINT business_sales_customer_activities_subject_check
        CHECK (length(btrim(subject)) BETWEEN 1 AND 160),
    CONSTRAINT business_sales_customer_activities_time_check
        CHECK (occurred_at IS NOT NULL OR due_at IS NOT NULL),
    CONSTRAINT business_sales_customer_activities_completion_check
        CHECK (completed_at IS NULL OR due_at IS NOT NULL),
    CONSTRAINT business_sales_customer_activities_version_check CHECK (version > 0),
    UNIQUE (id, client_id)
);

CREATE INDEX IF NOT EXISTS business_sales_customer_activities_customer_idx
    ON business_sales_customer_activities (client_id, customer_id, created_at DESC);
CREATE INDEX IF NOT EXISTS business_sales_customer_activities_next_action_idx
    ON business_sales_customer_activities (client_id, due_at)
    WHERE completed_at IS NULL AND due_at IS NOT NULL;

CREATE TABLE IF NOT EXISTS business_projects (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    customer_id uuid,
    name varchar(160) NOT NULL,
    description varchar(4000),
    start_date date,
    due_date date,
    status varchar(16) NOT NULL DEFAULT 'ACTIVE',
    version integer NOT NULL DEFAULT 1,
    created_by_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW(),
    completed_at timestamptz,
    CONSTRAINT business_projects_customer_fk
        FOREIGN KEY (customer_id, client_id)
        REFERENCES business_sales_customers (id, client_id) ON DELETE RESTRICT,
    CONSTRAINT business_projects_name_check
        CHECK (length(btrim(name)) BETWEEN 1 AND 160),
    CONSTRAINT business_projects_status_check
        CHECK (status IN ('ACTIVE', 'ON_HOLD', 'COMPLETED', 'ARCHIVED')),
    CONSTRAINT business_projects_dates_check
        CHECK (due_date IS NULL OR start_date IS NULL OR due_date >= start_date),
    CONSTRAINT business_projects_completion_check
        CHECK ((status = 'COMPLETED') = (completed_at IS NOT NULL)),
    CONSTRAINT business_projects_version_check CHECK (version > 0),
    UNIQUE (id, client_id)
);

CREATE INDEX IF NOT EXISTS business_projects_client_status_idx
    ON business_projects (client_id, status, due_date, id);

CREATE TABLE IF NOT EXISTS business_tasks (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    project_id uuid,
    direct_customer_id uuid,
    title varchar(200) NOT NULL,
    description varchar(4000),
    priority varchar(12) NOT NULL DEFAULT 'NORMAL',
    due_date date,
    status varchar(16) NOT NULL DEFAULT 'TODO',
    version integer NOT NULL DEFAULT 1,
    created_by_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW(),
    completed_at timestamptz,
    CONSTRAINT business_tasks_project_fk
        FOREIGN KEY (project_id, client_id)
        REFERENCES business_projects (id, client_id) ON DELETE RESTRICT,
    CONSTRAINT business_tasks_customer_fk
        FOREIGN KEY (direct_customer_id, client_id)
        REFERENCES business_sales_customers (id, client_id) ON DELETE RESTRICT,
    CONSTRAINT business_tasks_single_parent_check
        CHECK (project_id IS NULL OR direct_customer_id IS NULL),
    CONSTRAINT business_tasks_title_check
        CHECK (length(btrim(title)) BETWEEN 1 AND 200),
    CONSTRAINT business_tasks_priority_check
        CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    CONSTRAINT business_tasks_status_check
        CHECK (status IN ('TODO', 'IN_PROGRESS', 'BLOCKED', 'DONE', 'ARCHIVED')),
    CONSTRAINT business_tasks_completion_check
        CHECK ((status = 'DONE') = (completed_at IS NOT NULL)),
    CONSTRAINT business_tasks_version_check CHECK (version > 0),
    UNIQUE (id, client_id)
);

CREATE INDEX IF NOT EXISTS business_tasks_client_status_idx
    ON business_tasks (client_id, status, due_date, id);
CREATE INDEX IF NOT EXISTS business_tasks_project_idx
    ON business_tasks (client_id, project_id, status, id);

CREATE TABLE IF NOT EXISTS business_task_labels (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    name varchar(80) NOT NULL,
    color_hex varchar(7) NOT NULL,
    is_archived boolean NOT NULL DEFAULT FALSE,
    version integer NOT NULL DEFAULT 1,
    created_by_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT business_task_labels_name_check
        CHECK (length(btrim(name)) BETWEEN 1 AND 80),
    CONSTRAINT business_task_labels_color_check
        CHECK (color_hex ~ '^#[0-9A-Fa-f]{6}$'),
    CONSTRAINT business_task_labels_version_check CHECK (version > 0),
    UNIQUE (id, client_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS business_task_labels_name_idx
    ON business_task_labels (client_id, lower(name))
    WHERE is_archived = FALSE;

CREATE TABLE IF NOT EXISTS business_task_label_links (
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    task_id uuid NOT NULL,
    label_id uuid NOT NULL,
    created_by_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    PRIMARY KEY (task_id, label_id),
    CONSTRAINT business_task_label_links_task_fk
        FOREIGN KEY (task_id, client_id)
        REFERENCES business_tasks (id, client_id) ON DELETE CASCADE,
    CONSTRAINT business_task_label_links_label_fk
        FOREIGN KEY (label_id, client_id)
        REFERENCES business_task_labels (id, client_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS business_task_board_placements (
    task_id uuid PRIMARY KEY,
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    board_date date NOT NULL,
    position_x numeric(7,6) NOT NULL,
    position_y numeric(7,6) NOT NULL,
    created_by_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT business_task_board_placements_task_fk
        FOREIGN KEY (task_id, client_id)
        REFERENCES business_tasks (id, client_id) ON DELETE CASCADE,
    CONSTRAINT business_task_board_placements_position_x_check
        CHECK (position_x BETWEEN 0 AND 1),
    CONSTRAINT business_task_board_placements_position_y_check
        CHECK (position_y BETWEEN 0 AND 1)
);

CREATE INDEX IF NOT EXISTS business_task_board_placements_date_idx
    ON business_task_board_placements (client_id, board_date);

CREATE TABLE IF NOT EXISTS business_work_sessions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    task_id uuid NOT NULL,
    author_user_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    started_at timestamptz NOT NULL,
    ended_at timestamptz,
    status varchar(12) NOT NULL DEFAULT 'ACTIVE',
    time_source varchar(20) NOT NULL DEFAULT 'SERVER',
    business_timezone varchar(40) NOT NULL DEFAULT 'Europe/Madrid',
    last_checkpoint_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT business_work_sessions_task_fk
        FOREIGN KEY (task_id, client_id)
        REFERENCES business_tasks (id, client_id) ON DELETE CASCADE,
    CONSTRAINT business_work_sessions_status_check
        CHECK (status IN ('ACTIVE', 'PAUSED', 'FINISHED')),
    CONSTRAINT business_work_sessions_time_source_check
        CHECK (time_source = 'SERVER'),
    CONSTRAINT business_work_sessions_completion_check
        CHECK ((status = 'FINISHED') = (ended_at IS NOT NULL)),
    CONSTRAINT business_work_sessions_dates_check
        CHECK (ended_at IS NULL OR ended_at >= started_at),
    UNIQUE (id, client_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS business_work_sessions_one_live_idx
    ON business_work_sessions (client_id, author_user_id)
    WHERE status IN ('ACTIVE', 'PAUSED');
CREATE INDEX IF NOT EXISTS business_work_sessions_task_idx
    ON business_work_sessions (client_id, task_id, started_at DESC);

CREATE TABLE IF NOT EXISTS business_work_segments (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    session_id uuid NOT NULL,
    started_at timestamptz NOT NULL,
    ended_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT business_work_segments_session_fk
        FOREIGN KEY (session_id, client_id)
        REFERENCES business_work_sessions (id, client_id) ON DELETE CASCADE,
    CONSTRAINT business_work_segments_dates_check
        CHECK (ended_at IS NULL OR ended_at >= started_at)
);

CREATE UNIQUE INDEX IF NOT EXISTS business_work_segments_one_open_idx
    ON business_work_segments (session_id)
    WHERE ended_at IS NULL;

CREATE TABLE IF NOT EXISTS business_time_entries (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    task_id uuid NOT NULL,
    session_id uuid NOT NULL,
    author_user_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    work_date date NOT NULL,
    seconds integer NOT NULL,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT business_time_entries_task_fk
        FOREIGN KEY (task_id, client_id)
        REFERENCES business_tasks (id, client_id) ON DELETE CASCADE,
    CONSTRAINT business_time_entries_session_fk
        FOREIGN KEY (session_id, client_id)
        REFERENCES business_work_sessions (id, client_id) ON DELETE CASCADE,
    CONSTRAINT business_time_entries_seconds_check CHECK (seconds >= 0),
    UNIQUE (session_id, work_date)
);

CREATE INDEX IF NOT EXISTS business_time_entries_task_idx
    ON business_time_entries (client_id, task_id, work_date DESC);

CREATE TABLE IF NOT EXISTS business_project_audit_events (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    entity_type varchar(24) NOT NULL,
    entity_id uuid NOT NULL,
    action varchar(40) NOT NULL,
    actor_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
    version integer,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT business_project_audit_events_entity_check
        CHECK (entity_type IN ('PROJECT', 'TASK', 'BOARD', 'WORK_SESSION')),
    CONSTRAINT business_project_audit_events_metadata_check
        CHECK (jsonb_typeof(metadata) = 'object')
);

CREATE INDEX IF NOT EXISTS business_project_audit_events_client_idx
    ON business_project_audit_events (client_id, entity_type, entity_id, created_at DESC);

CREATE OR REPLACE FUNCTION remove_closed_business_task_placement()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.status IN ('DONE', 'ARCHIVED') THEN
        DELETE FROM business_task_board_placements WHERE task_id = NEW.id;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_business_tasks_remove_closed_placement ON business_tasks;
CREATE TRIGGER trg_business_tasks_remove_closed_placement
AFTER INSERT OR UPDATE OF status ON business_tasks
FOR EACH ROW EXECUTE FUNCTION remove_closed_business_task_placement();

DO $$
DECLARE
    touch_table text;
BEGIN
    FOREACH touch_table IN ARRAY ARRAY[
        'business_sales_customer_tags',
        'business_sales_customer_activities',
        'business_projects',
        'business_tasks',
        'business_task_labels',
        'business_task_board_placements',
        'business_work_sessions'
    ]
    LOOP
        EXECUTE format('DROP TRIGGER IF EXISTS touch_%I_updated_at ON public.%I', touch_table, touch_table);
        EXECUTE format(
            'CREATE TRIGGER touch_%I_updated_at BEFORE UPDATE ON public.%I FOR EACH ROW EXECUTE FUNCTION set_updated_at()',
            touch_table,
            touch_table
        );
    END LOOP;
END;
$$;

-- The two V15 pilots are free and requestable. Other products remain visible
-- but cannot receive new requests; existing ACTIVE subscriptions are preserved.
UPDATE product_catalog
SET monthly_price_cents = 0,
    is_available = FALSE,
    is_requestable = FALSE,
    updated_at = NOW()
WHERE product_key NOT IN ('BUSINESS_CUSTOMERS', 'BUSINESS_PROJECTS');

INSERT INTO product_catalog (
    product_key, name, short_description, category, icon_key,
    monthly_price_cents, currency, is_requestable, is_available, capabilities, sort_order
) VALUES (
    'BUSINESS_PROJECTS',
    'Proyectos y tareas',
    'Organiza proyectos, tareas, planificacion diaria y tiempo de trabajo.',
    'Organizacion',
    'list-checks',
    0,
    'EUR',
    TRUE,
    TRUE,
    '["projects", "tasks", "daily_board", "time_tracking"]'::jsonb,
    40
)
ON CONFLICT (product_key) DO UPDATE
SET name = EXCLUDED.name,
    short_description = EXCLUDED.short_description,
    category = EXCLUDED.category,
    icon_key = EXCLUDED.icon_key,
    monthly_price_cents = 0,
    currency = 'EUR',
    is_requestable = TRUE,
    is_available = TRUE,
    capabilities = EXCLUDED.capabilities,
    sort_order = EXCLUDED.sort_order,
    updated_at = NOW();

UPDATE product_catalog
SET name = 'Clientes y contactos',
    short_description = 'Gestiona fichas, contactos, etiquetas y seguimiento comercial privado.',
    monthly_price_cents = 0,
    is_requestable = TRUE,
    is_available = TRUE,
    capabilities = '["customers", "contacts", "tags", "activities"]'::jsonb,
    sort_order = 30,
    updated_at = NOW()
WHERE product_key = 'BUSINESS_CUSTOMERS';

DO $$
DECLARE
    private_table text;
    api_role text;
BEGIN
    FOREACH private_table IN ARRAY ARRAY[
        'business_sales_customer_tags',
        'business_sales_customer_tag_links',
        'business_sales_customer_activities',
        'business_projects',
        'business_tasks',
        'business_task_labels',
        'business_task_label_links',
        'business_task_board_placements',
        'business_work_sessions',
        'business_work_segments',
        'business_time_entries',
        'business_project_audit_events'
    ]
    LOOP
        EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', private_table);
        EXECUTE format('REVOKE ALL PRIVILEGES ON TABLE public.%I FROM PUBLIC', private_table);
    END LOOP;

    FOREACH api_role IN ARRAY ARRAY['anon', 'authenticated']
    LOOP
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = api_role) THEN
            FOREACH private_table IN ARRAY ARRAY[
                'business_sales_customer_tags',
                'business_sales_customer_tag_links',
                'business_sales_customer_activities',
                'business_projects',
                'business_tasks',
                'business_task_labels',
                'business_task_label_links',
                'business_task_board_placements',
                'business_work_sessions',
                'business_work_segments',
                'business_time_entries',
                'business_project_audit_events'
            ]
            LOOP
                EXECUTE format(
                    'REVOKE ALL PRIVILEGES ON TABLE public.%I FROM %I',
                    private_table,
                    api_role
                );
            END LOOP;
        END IF;
    END LOOP;
END;
$$;
