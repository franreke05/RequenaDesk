-- Free beta business utilities. They are tenant-owned and deliberately do not
-- model issued fiscal invoices, accounting entries, tax declarations or SIF.

CREATE TABLE IF NOT EXISTS business_counterparties (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    kind varchar(16) NOT NULL,
    legal_name varchar(180) NOT NULL,
    tax_id varchar(40),
    email varchar(254),
    phone varchar(40),
    billing_address varchar(500),
    country_code char(2),
    is_archived boolean NOT NULL DEFAULT FALSE,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT business_counterparties_kind_check CHECK (kind IN ('CUSTOMER', 'SUPPLIER', 'BOTH')),
    CONSTRAINT business_counterparties_legal_name_check CHECK (length(btrim(legal_name)) > 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS business_counterparties_active_name_idx
    ON business_counterparties (client_id, lower(legal_name))
    WHERE is_archived = FALSE;

CREATE TABLE IF NOT EXISTS business_issuer_profiles (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    display_name varchar(180) NOT NULL,
    legal_name varchar(180),
    tax_id varchar(40),
    billing_address varchar(500),
    billing_email varchar(254),
    currency char(3) NOT NULL DEFAULT 'EUR',
    is_default boolean NOT NULL DEFAULT FALSE,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT business_issuer_profiles_currency_check CHECK (currency = 'EUR'),
    CONSTRAINT business_issuer_profiles_display_name_check CHECK (length(btrim(display_name)) > 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS business_issuer_profiles_default_idx
    ON business_issuer_profiles (client_id)
    WHERE is_default = TRUE;

CREATE TABLE IF NOT EXISTS business_finance_categories (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    direction varchar(12) NOT NULL,
    name varchar(100) NOT NULL,
    color_key varchar(32) NOT NULL DEFAULT 'neutral',
    suggested_pgc_account varchar(12),
    is_archived boolean NOT NULL DEFAULT FALSE,
    sort_order integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT business_finance_categories_direction_check CHECK (direction IN ('INCOME', 'EXPENSE')),
    CONSTRAINT business_finance_categories_name_check CHECK (length(btrim(name)) > 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS business_finance_categories_active_name_idx
    ON business_finance_categories (client_id, direction, lower(name))
    WHERE is_archived = FALSE;

CREATE TABLE IF NOT EXISTS business_sales_documents (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    document_kind varchar(20) NOT NULL,
    status varchar(16) NOT NULL DEFAULT 'DRAFT',
    display_reference varchar(64),
    issuer_name varchar(180) NOT NULL,
    customer_name varchar(180) NOT NULL,
    issue_date date NOT NULL,
    due_date date,
    currency char(3) NOT NULL DEFAULT 'EUR',
    notes varchar(2000),
    subtotal_cents bigint NOT NULL,
    tax_cents bigint NOT NULL,
    total_cents bigint NOT NULL,
    version integer NOT NULL DEFAULT 1,
    created_by_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW(),
    archived_at timestamptz,
    CONSTRAINT business_sales_documents_kind_check CHECK (document_kind IN ('DRAFT_INVOICE', 'PROFORMA')),
    CONSTRAINT business_sales_documents_status_check CHECK (status IN ('DRAFT', 'ARCHIVED', 'VOID')),
    CONSTRAINT business_sales_documents_currency_check CHECK (currency = 'EUR'),
    CONSTRAINT business_sales_documents_dates_check CHECK (due_date IS NULL OR due_date >= issue_date),
    CONSTRAINT business_sales_documents_amounts_check CHECK (subtotal_cents >= 0 AND tax_cents >= 0 AND total_cents >= 0),
    CONSTRAINT business_sales_documents_version_check CHECK (version > 0)
);

CREATE INDEX IF NOT EXISTS business_sales_documents_client_idx
    ON business_sales_documents (client_id, status, updated_at DESC, id DESC);

CREATE TABLE IF NOT EXISTS business_sales_document_lines (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id uuid NOT NULL REFERENCES business_sales_documents(id) ON DELETE RESTRICT,
    description varchar(1000) NOT NULL,
    quantity_milli bigint NOT NULL,
    unit_price_cents bigint NOT NULL,
    tax_rate_basis_points integer NOT NULL DEFAULT 0,
    discount_basis_points integer NOT NULL DEFAULT 0,
    subtotal_cents bigint NOT NULL,
    tax_cents bigint NOT NULL,
    total_cents bigint NOT NULL,
    sort_order integer NOT NULL,
    CONSTRAINT business_sales_document_lines_quantity_check CHECK (quantity_milli > 0),
    CONSTRAINT business_sales_document_lines_price_check CHECK (unit_price_cents >= 0),
    CONSTRAINT business_sales_document_lines_tax_check CHECK (tax_rate_basis_points BETWEEN 0 AND 10000),
    CONSTRAINT business_sales_document_lines_discount_check CHECK (discount_basis_points BETWEEN 0 AND 10000),
    CONSTRAINT business_sales_document_lines_amounts_check CHECK (subtotal_cents >= 0 AND tax_cents >= 0 AND total_cents >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS business_sales_document_lines_order_idx
    ON business_sales_document_lines (document_id, sort_order);

CREATE TABLE IF NOT EXISTS business_sales_document_events (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    document_id uuid NOT NULL REFERENCES business_sales_documents(id) ON DELETE RESTRICT,
    version integer NOT NULL,
    action varchar(32) NOT NULL,
    actor_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT business_sales_document_events_action_check CHECK (action IN ('CREATED', 'UPDATED', 'ARCHIVED', 'VOIDED'))
);

CREATE INDEX IF NOT EXISTS business_sales_document_events_client_idx
    ON business_sales_document_events (client_id, document_id, created_at DESC);

CREATE TABLE IF NOT EXISTS business_finance_entries (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    direction varchar(12) NOT NULL,
    status varchar(16) NOT NULL DEFAULT 'DRAFT',
    occurred_on date NOT NULL,
    counterparty_name varchar(180),
    category_name varchar(100),
    description varchar(1000) NOT NULL,
    net_cents bigint NOT NULL,
    tax_rate_basis_points integer NOT NULL DEFAULT 0,
    tax_cents bigint NOT NULL,
    gross_cents bigint NOT NULL,
    currency char(3) NOT NULL DEFAULT 'EUR',
    payment_status varchar(16) NOT NULL DEFAULT 'PENDING',
    external_reference varchar(120),
    void_reason varchar(500),
    version integer NOT NULL DEFAULT 1,
    created_by_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT business_finance_entries_direction_check CHECK (direction IN ('INCOME', 'EXPENSE')),
    CONSTRAINT business_finance_entries_status_check CHECK (status IN ('DRAFT', 'RECORDED', 'VOID')),
    CONSTRAINT business_finance_entries_payment_check CHECK (payment_status IN ('PENDING', 'PAID')),
    CONSTRAINT business_finance_entries_currency_check CHECK (currency = 'EUR'),
    CONSTRAINT business_finance_entries_amounts_check CHECK (net_cents >= 0 AND tax_cents >= 0 AND gross_cents >= 0),
    CONSTRAINT business_finance_entries_tax_check CHECK (tax_rate_basis_points BETWEEN 0 AND 10000),
    CONSTRAINT business_finance_entries_version_check CHECK (version > 0)
);

CREATE INDEX IF NOT EXISTS business_finance_entries_client_idx
    ON business_finance_entries (client_id, occurred_on DESC, id DESC);

CREATE TABLE IF NOT EXISTS business_finance_entry_events (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    entry_id uuid NOT NULL REFERENCES business_finance_entries(id) ON DELETE RESTRICT,
    version integer NOT NULL,
    action varchar(32) NOT NULL,
    actor_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT business_finance_entry_events_action_check CHECK (action IN ('CREATED', 'UPDATED', 'RECORDED', 'VOIDED'))
);

CREATE INDEX IF NOT EXISTS business_finance_entry_events_client_idx
    ON business_finance_entry_events (client_id, entry_id, created_at DESC);

CREATE TABLE IF NOT EXISTS business_file_attachments (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    storage_key varchar(255) NOT NULL,
    original_name varchar(255) NOT NULL,
    content_type varchar(100) NOT NULL,
    size_bytes bigint NOT NULL,
    sha256 char(64) NOT NULL,
    scan_status varchar(20) NOT NULL DEFAULT 'PENDING_SCAN',
    uploaded_by_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT business_file_attachments_content_type_check CHECK (content_type IN ('application/pdf', 'image/jpeg', 'image/png')),
    CONSTRAINT business_file_attachments_size_check CHECK (size_bytes > 0 AND size_bytes <= 10485760),
    CONSTRAINT business_file_attachments_scan_check CHECK (scan_status IN ('PENDING_SCAN', 'CLEAN', 'REJECTED')),
    CONSTRAINT business_file_attachments_storage_key_check CHECK (length(btrim(storage_key)) > 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS business_file_attachments_storage_key_idx
    ON business_file_attachments (storage_key);

CREATE TABLE IF NOT EXISTS business_finance_entry_attachments (
    entry_id uuid NOT NULL REFERENCES business_finance_entries(id) ON DELETE RESTRICT,
    attachment_id uuid NOT NULL REFERENCES business_file_attachments(id) ON DELETE RESTRICT,
    kind varchar(16) NOT NULL DEFAULT 'RECEIPT',
    PRIMARY KEY (entry_id, attachment_id),
    CONSTRAINT business_finance_entry_attachments_kind_check CHECK (kind IN ('RECEIPT', 'INVOICE', 'OTHER'))
);

DO $$
DECLARE
    table_name text;
    api_role text;
BEGIN
    FOREACH table_name IN ARRAY ARRAY[
        'business_counterparties', 'business_issuer_profiles', 'business_finance_categories',
        'business_sales_documents', 'business_sales_document_lines', 'business_sales_document_events',
        'business_finance_entries', 'business_finance_entry_events', 'business_file_attachments',
        'business_finance_entry_attachments'
    ]
    LOOP
        EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', table_name);
        EXECUTE format('REVOKE ALL PRIVILEGES ON TABLE public.%I FROM PUBLIC', table_name);
    END LOOP;
    FOREACH api_role IN ARRAY ARRAY['anon', 'authenticated']
    LOOP
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = api_role) THEN
            FOREACH table_name IN ARRAY ARRAY[
                'business_counterparties', 'business_issuer_profiles', 'business_finance_categories',
                'business_sales_documents', 'business_sales_document_lines', 'business_sales_document_events',
                'business_finance_entries', 'business_finance_entry_events', 'business_file_attachments',
                'business_finance_entry_attachments'
            ]
            LOOP
                EXECUTE format('REVOKE ALL PRIVILEGES ON TABLE public.%I FROM %I', table_name, api_role);
            END LOOP;
        END IF;
    END LOOP;
END;
$$;

DROP TRIGGER IF EXISTS touch_business_counterparties_updated_at ON business_counterparties;
CREATE TRIGGER touch_business_counterparties_updated_at BEFORE UPDATE ON business_counterparties FOR EACH ROW EXECUTE FUNCTION set_updated_at();
DROP TRIGGER IF EXISTS touch_business_issuer_profiles_updated_at ON business_issuer_profiles;
CREATE TRIGGER touch_business_issuer_profiles_updated_at BEFORE UPDATE ON business_issuer_profiles FOR EACH ROW EXECUTE FUNCTION set_updated_at();
DROP TRIGGER IF EXISTS touch_business_finance_categories_updated_at ON business_finance_categories;
CREATE TRIGGER touch_business_finance_categories_updated_at BEFORE UPDATE ON business_finance_categories FOR EACH ROW EXECUTE FUNCTION set_updated_at();
DROP TRIGGER IF EXISTS touch_business_sales_documents_updated_at ON business_sales_documents;
CREATE TRIGGER touch_business_sales_documents_updated_at BEFORE UPDATE ON business_sales_documents FOR EACH ROW EXECUTE FUNCTION set_updated_at();
DROP TRIGGER IF EXISTS touch_business_finance_entries_updated_at ON business_finance_entries;
CREATE TRIGGER touch_business_finance_entries_updated_at BEFORE UPDATE ON business_finance_entries FOR EACH ROW EXECUTE FUNCTION set_updated_at();
