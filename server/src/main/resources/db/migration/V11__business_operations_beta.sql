-- Operations beta: tenant-scoped booking and private-document metadata only.
-- Binary content lives exclusively in a private object store; this schema never stores blobs or public object URLs.

CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE business_booking_services (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    name varchar(120) NOT NULL,
    duration_minutes integer NOT NULL CHECK (duration_minutes BETWEEN 5 AND 720),
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (client_id, id)
);

CREATE TABLE business_booking_resources (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    name varchar(120) NOT NULL,
    time_zone varchar(80) NOT NULL,
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (client_id, id)
);

CREATE TABLE business_availability_rules (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    resource_id uuid NOT NULL,
    weekday smallint NOT NULL CHECK (weekday BETWEEN 1 AND 7),
    starts_at time NOT NULL,
    ends_at time NOT NULL,
    time_zone varchar(80) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT business_availability_rules_interval_check CHECK (starts_at < ends_at),
    CONSTRAINT business_availability_rules_resource_fk FOREIGN KEY (client_id, resource_id)
        REFERENCES business_booking_resources(client_id, id) ON DELETE CASCADE
);

CREATE TABLE business_availability_exceptions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    resource_id uuid NOT NULL,
    exception_date date NOT NULL,
    is_unavailable boolean NOT NULL DEFAULT true,
    starts_at time,
    ends_at time,
    time_zone varchar(80) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT business_availability_exceptions_interval_check CHECK (
        (starts_at IS NULL AND ends_at IS NULL) OR (starts_at IS NOT NULL AND ends_at IS NOT NULL AND starts_at < ends_at)
    ),
    CONSTRAINT business_availability_exceptions_resource_fk FOREIGN KEY (client_id, resource_id)
        REFERENCES business_booking_resources(client_id, id) ON DELETE CASCADE
);

CREATE TABLE business_appointments (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    service_id uuid NOT NULL,
    resource_id uuid NOT NULL,
    starts_at timestamptz NOT NULL,
    ends_at timestamptz NOT NULL,
    time_zone varchar(80) NOT NULL,
    status varchar(16) NOT NULL DEFAULT 'CONFIRMED',
    contact_name varchar(120),
    contact_email varchar(254),
    contact_phone varchar(40),
    notes varchar(1000),
    created_by uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    cancelled_at timestamptz,
    cancellation_reason varchar(500),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    time_range tstzrange GENERATED ALWAYS AS (tstzrange(starts_at, ends_at, '[)')) STORED,
    CONSTRAINT business_appointments_interval_check CHECK (starts_at < ends_at),
    CONSTRAINT business_appointments_status_check CHECK (status IN ('HELD', 'CONFIRMED', 'CANCELLED', 'COMPLETED', 'NO_SHOW')),
    CONSTRAINT business_appointments_service_fk FOREIGN KEY (client_id, service_id)
        REFERENCES business_booking_services(client_id, id) ON DELETE RESTRICT,
    CONSTRAINT business_appointments_resource_fk FOREIGN KEY (client_id, resource_id)
        REFERENCES business_booking_resources(client_id, id) ON DELETE RESTRICT,
    CONSTRAINT business_appointments_no_active_overlap EXCLUDE USING gist (
        client_id WITH =, resource_id WITH =, time_range WITH &&
    ) WHERE (status IN ('HELD', 'CONFIRMED'))
);

CREATE INDEX business_appointments_client_time_idx ON business_appointments (client_id, starts_at);

CREATE TABLE business_document_folders (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    name varchar(180) NOT NULL,
    parent_folder_id uuid,
    created_by uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (client_id, id),
    CONSTRAINT business_document_folders_parent_fk FOREIGN KEY (client_id, parent_folder_id)
        REFERENCES business_document_folders(client_id, id) ON DELETE RESTRICT
);

CREATE TABLE business_documents (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    folder_id uuid,
    title varchar(180) NOT NULL,
    status varchar(24) NOT NULL DEFAULT 'DRAFT',
    created_by uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (client_id, id),
    CONSTRAINT business_documents_status_check CHECK (status IN ('DRAFT', 'PENDING_UPLOAD', 'AVAILABLE', 'REJECTED', 'ARCHIVED')),
    CONSTRAINT business_documents_folder_fk FOREIGN KEY (client_id, folder_id)
        REFERENCES business_document_folders(client_id, id) ON DELETE RESTRICT
);

CREATE TABLE business_document_versions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    document_id uuid NOT NULL,
    version_number integer NOT NULL CHECK (version_number > 0),
    file_name varchar(255) NOT NULL,
    content_type varchar(127) NOT NULL,
    size_bytes bigint NOT NULL CHECK (size_bytes > 0 AND size_bytes <= 26214400),
    sha256 char(64),
    private_object_key varchar(500) NOT NULL,
    storage_intent_id varchar(255),
    scan_status varchar(16) NOT NULL DEFAULT 'PENDING',
    created_by uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (client_id, id),
    UNIQUE (client_id, document_id, version_number),
    CONSTRAINT business_document_versions_hash_check CHECK (sha256 IS NULL OR sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT business_document_versions_scan_check CHECK (scan_status IN ('PENDING', 'CLEAN', 'REJECTED')),
    CONSTRAINT business_document_versions_document_fk FOREIGN KEY (client_id, document_id)
        REFERENCES business_documents(client_id, id) ON DELETE CASCADE
);

CREATE INDEX business_document_versions_document_idx ON business_document_versions (client_id, document_id, version_number DESC);

CREATE TABLE business_document_confirmation_requests (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    document_version_id uuid NOT NULL,
    title varchar(180) NOT NULL,
    statement varchar(2000) NOT NULL,
    status varchar(16) NOT NULL DEFAULT 'PENDING',
    expires_at timestamptz,
    created_by uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (client_id, id),
    CONSTRAINT business_document_confirmation_request_status_check CHECK (status IN ('PENDING', 'ACCEPTED', 'REVOKED', 'EXPIRED')),
    CONSTRAINT business_document_confirmation_request_version_fk FOREIGN KEY (client_id, document_version_id)
        REFERENCES business_document_versions(client_id, id) ON DELETE RESTRICT
);

CREATE TABLE business_document_confirmations (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    confirmation_request_id uuid NOT NULL,
    document_version_id uuid NOT NULL,
    accepted_by_user_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    accepted_by_email varchar(254) NOT NULL,
    statement_snapshot varchar(2000) NOT NULL,
    document_sha256 char(64) NOT NULL CHECK (document_sha256 ~ '^[0-9a-f]{64}$'),
    accepted_at timestamptz NOT NULL DEFAULT now(),
    ip_address inet,
    user_agent varchar(512),
    UNIQUE (client_id, confirmation_request_id),
    CONSTRAINT business_document_confirmations_request_fk FOREIGN KEY (client_id, confirmation_request_id)
        REFERENCES business_document_confirmation_requests(client_id, id) ON DELETE RESTRICT,
    CONSTRAINT business_document_confirmations_version_fk FOREIGN KEY (client_id, document_version_id)
        REFERENCES business_document_versions(client_id, id) ON DELETE RESTRICT
);

CREATE TABLE business_operations_audit_events (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    actor_user_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    action varchar(60) NOT NULL,
    entity_type varchar(60) NOT NULL,
    entity_id uuid NOT NULL,
    occurred_at timestamptz NOT NULL DEFAULT now(),
    details varchar(1000)
);
CREATE INDEX business_operations_audit_events_lookup_idx ON business_operations_audit_events (client_id, entity_id, occurred_at);

CREATE TRIGGER touch_business_booking_services_updated_at BEFORE UPDATE ON business_booking_services FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER touch_business_booking_resources_updated_at BEFORE UPDATE ON business_booking_resources FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER touch_business_appointments_updated_at BEFORE UPDATE ON business_appointments FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER touch_business_documents_updated_at BEFORE UPDATE ON business_documents FOR EACH ROW EXECUTE FUNCTION set_updated_at();

ALTER TABLE business_booking_services ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_booking_resources ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_availability_rules ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_availability_exceptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_appointments ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_document_folders ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_documents ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_document_versions ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_document_confirmation_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_document_confirmations ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_operations_audit_events ENABLE ROW LEVEL SECURITY;

REVOKE ALL PRIVILEGES ON TABLE public.business_booking_services, public.business_booking_resources, public.business_availability_rules, public.business_availability_exceptions, public.business_appointments, public.business_document_folders, public.business_documents, public.business_document_versions, public.business_document_confirmation_requests, public.business_document_confirmations, public.business_operations_audit_events FROM PUBLIC;
DO $$
DECLARE api_role text;
BEGIN
    FOREACH api_role IN ARRAY ARRAY['anon', 'authenticated'] LOOP
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = api_role) THEN
            EXECUTE format('REVOKE ALL PRIVILEGES ON TABLE public.business_booking_services, public.business_booking_resources, public.business_availability_rules, public.business_availability_exceptions, public.business_appointments, public.business_document_folders, public.business_documents, public.business_document_versions, public.business_document_confirmation_requests, public.business_document_confirmations, public.business_operations_audit_events FROM %I', api_role);
        END IF;
    END LOOP;
END;
$$;
