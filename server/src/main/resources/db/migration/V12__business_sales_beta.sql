-- Free beta sales programs only: customers, quotes/sales and catalog/stock.
-- No fiscal invoices, payments, accounting records or subscription charges are created here.

CREATE TABLE IF NOT EXISTS business_sales_customers (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    display_name varchar(160) NOT NULL,
    tax_id varchar(40),
    email varchar(254),
    phone varchar(40),
    address varchar(500),
    status varchar(16) NOT NULL DEFAULT 'ACTIVE',
    version integer NOT NULL DEFAULT 1,
    created_by_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT business_sales_customers_status_check CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT business_sales_customers_name_check CHECK (length(btrim(display_name)) BETWEEN 1 AND 160),
    CONSTRAINT business_sales_customers_version_check CHECK (version > 0),
    UNIQUE (id, client_id)
);

CREATE INDEX IF NOT EXISTS business_sales_customers_client_status_idx
    ON business_sales_customers (client_id, status, id);

CREATE TABLE IF NOT EXISTS business_sales_contacts (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    customer_id uuid NOT NULL,
    full_name varchar(160) NOT NULL,
    role varchar(120),
    email varchar(254),
    phone varchar(40),
    is_primary boolean NOT NULL DEFAULT FALSE,
    status varchar(16) NOT NULL DEFAULT 'ACTIVE',
    version integer NOT NULL DEFAULT 1,
    created_by_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT business_sales_contacts_customer_tenant_fk
        FOREIGN KEY (customer_id, client_id) REFERENCES business_sales_customers (id, client_id) ON DELETE RESTRICT,
    CONSTRAINT business_sales_contacts_status_check CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT business_sales_contacts_name_check CHECK (length(btrim(full_name)) BETWEEN 1 AND 160),
    CONSTRAINT business_sales_contacts_version_check CHECK (version > 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS business_sales_contacts_one_primary_idx
    ON business_sales_contacts (customer_id)
    WHERE is_primary = TRUE AND status = 'ACTIVE';
CREATE INDEX IF NOT EXISTS business_sales_contacts_customer_idx
    ON business_sales_contacts (client_id, customer_id, id);

CREATE TABLE IF NOT EXISTS business_catalog_items (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    item_type varchar(16) NOT NULL,
    name varchar(160) NOT NULL,
    sku varchar(80),
    description varchar(1000),
    unit varchar(40) NOT NULL DEFAULT 'unidad',
    reference_price_cents bigint NOT NULL DEFAULT 0,
    currency char(3) NOT NULL DEFAULT 'EUR',
    tracks_stock boolean NOT NULL DEFAULT FALSE,
    stock_minimum_milli bigint,
    is_archived boolean NOT NULL DEFAULT FALSE,
    version integer NOT NULL DEFAULT 1,
    created_by_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT business_catalog_items_type_check CHECK (item_type IN ('PRODUCT', 'SERVICE')),
    CONSTRAINT business_catalog_items_name_check CHECK (length(btrim(name)) BETWEEN 1 AND 160),
    CONSTRAINT business_catalog_items_currency_check CHECK (currency = 'EUR'),
    CONSTRAINT business_catalog_items_price_check CHECK (reference_price_cents BETWEEN 0 AND 1000000000),
    CONSTRAINT business_catalog_items_stock_check CHECK (
        (item_type = 'PRODUCT' OR tracks_stock = FALSE)
        AND (tracks_stock = TRUE OR stock_minimum_milli IS NULL)
        AND (stock_minimum_milli IS NULL OR stock_minimum_milli >= 0)
    ),
    CONSTRAINT business_catalog_items_version_check CHECK (version > 0),
    UNIQUE (id, client_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS business_catalog_items_tenant_sku_idx
    ON business_catalog_items (client_id, lower(sku))
    WHERE sku IS NOT NULL;
CREATE INDEX IF NOT EXISTS business_catalog_items_client_idx
    ON business_catalog_items (client_id, item_type, is_archived, id);

CREATE TABLE IF NOT EXISTS business_stock_movements (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    item_id uuid NOT NULL,
    movement_type varchar(16) NOT NULL,
    delta_milli bigint NOT NULL,
    reason varchar(240) NOT NULL,
    reference_id uuid,
    idempotency_key varchar(120),
    created_by_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT business_stock_movements_item_tenant_fk
        FOREIGN KEY (item_id, client_id) REFERENCES business_catalog_items (id, client_id) ON DELETE RESTRICT,
    CONSTRAINT business_stock_movements_type_check CHECK (movement_type IN ('INITIAL', 'ADJUSTMENT', 'SALE', 'RETURN')),
    CONSTRAINT business_stock_movements_delta_check CHECK (delta_milli <> 0),
    CONSTRAINT business_stock_movements_reason_check CHECK (length(btrim(reason)) BETWEEN 1 AND 240)
);

CREATE UNIQUE INDEX IF NOT EXISTS business_stock_movements_idempotency_idx
    ON business_stock_movements (client_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS business_stock_movements_item_idx
    ON business_stock_movements (client_id, item_id, created_at DESC, id DESC);

CREATE TABLE IF NOT EXISTS business_sales_document_counters (
    client_id uuid PRIMARY KEY REFERENCES clients(id) ON DELETE RESTRICT,
    next_quote_number integer NOT NULL DEFAULT 1,
    next_sale_number integer NOT NULL DEFAULT 1,
    CONSTRAINT business_sales_document_counters_quote_check CHECK (next_quote_number > 0),
    CONSTRAINT business_sales_document_counters_sale_check CHECK (next_sale_number > 0)
);

CREATE TABLE IF NOT EXISTS business_sales_quotes (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    quote_number varchar(32) NOT NULL,
    customer_id uuid,
    buyer_name varchar(160) NOT NULL,
    buyer_email varchar(254),
    buyer_phone varchar(40),
    status varchar(16) NOT NULL DEFAULT 'DRAFT',
    issue_date date NOT NULL,
    valid_until date,
    notes varchar(2000),
    currency char(3) NOT NULL DEFAULT 'EUR',
    subtotal_cents bigint NOT NULL,
    tax_cents bigint NOT NULL,
    total_cents bigint NOT NULL,
    version integer NOT NULL DEFAULT 1,
    create_idempotency_key varchar(120) NOT NULL,
    created_by_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW(),
    sent_at timestamptz,
    accepted_at timestamptz,
    rejected_at timestamptz,
    expired_at timestamptz,
    CONSTRAINT business_sales_quotes_customer_tenant_fk
        FOREIGN KEY (customer_id, client_id) REFERENCES business_sales_customers (id, client_id) ON DELETE RESTRICT,
    CONSTRAINT business_sales_quotes_status_check CHECK (status IN ('DRAFT', 'SENT', 'ACCEPTED', 'REJECTED', 'EXPIRED')),
    CONSTRAINT business_sales_quotes_dates_check CHECK (valid_until IS NULL OR valid_until >= issue_date),
    CONSTRAINT business_sales_quotes_currency_check CHECK (currency = 'EUR'),
    CONSTRAINT business_sales_quotes_amounts_check CHECK (subtotal_cents >= 0 AND tax_cents >= 0 AND total_cents >= 0),
    CONSTRAINT business_sales_quotes_version_check CHECK (version > 0),
    UNIQUE (id, client_id),
    UNIQUE (client_id, quote_number),
    UNIQUE (client_id, create_idempotency_key)
);

CREATE INDEX IF NOT EXISTS business_sales_quotes_client_status_idx
    ON business_sales_quotes (client_id, status, id);

CREATE TABLE IF NOT EXISTS business_sales_quote_lines (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    quote_id uuid NOT NULL,
    position integer NOT NULL,
    source_catalog_item_id uuid,
    description varchar(1000) NOT NULL,
    quantity_milli bigint NOT NULL,
    unit_price_cents bigint NOT NULL,
    discount_basis_points integer NOT NULL DEFAULT 0,
    tax_basis_points integer NOT NULL DEFAULT 0,
    subtotal_cents bigint NOT NULL,
    tax_cents bigint NOT NULL,
    total_cents bigint NOT NULL,
    CONSTRAINT business_sales_quote_lines_quote_tenant_fk
        FOREIGN KEY (quote_id, client_id) REFERENCES business_sales_quotes (id, client_id) ON DELETE RESTRICT,
    CONSTRAINT business_sales_quote_lines_catalog_tenant_fk
        FOREIGN KEY (source_catalog_item_id, client_id) REFERENCES business_catalog_items (id, client_id) ON DELETE RESTRICT,
    CONSTRAINT business_sales_quote_lines_position_check CHECK (position > 0),
    CONSTRAINT business_sales_quote_lines_quantity_check CHECK (quantity_milli > 0),
    CONSTRAINT business_sales_quote_lines_price_check CHECK (unit_price_cents BETWEEN 0 AND 1000000000),
    CONSTRAINT business_sales_quote_lines_discount_check CHECK (discount_basis_points BETWEEN 0 AND 10000),
    CONSTRAINT business_sales_quote_lines_tax_check CHECK (tax_basis_points BETWEEN 0 AND 10000),
    CONSTRAINT business_sales_quote_lines_amounts_check CHECK (subtotal_cents >= 0 AND tax_cents >= 0 AND total_cents >= 0),
    UNIQUE (quote_id, position)
);

CREATE INDEX IF NOT EXISTS business_sales_quote_lines_quote_idx
    ON business_sales_quote_lines (client_id, quote_id, position);

CREATE TABLE IF NOT EXISTS business_sales_sales (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    sale_number varchar(32) NOT NULL,
    quote_id uuid NOT NULL,
    buyer_name varchar(160) NOT NULL,
    currency char(3) NOT NULL DEFAULT 'EUR',
    subtotal_cents bigint NOT NULL,
    tax_cents bigint NOT NULL,
    total_cents bigint NOT NULL,
    status varchar(16) NOT NULL DEFAULT 'CONFIRMED',
    conversion_idempotency_key varchar(120) NOT NULL,
    confirmed_by_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
    confirmed_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT business_sales_sales_quote_tenant_fk
        FOREIGN KEY (quote_id, client_id) REFERENCES business_sales_quotes (id, client_id) ON DELETE RESTRICT,
    CONSTRAINT business_sales_sales_currency_check CHECK (currency = 'EUR'),
    CONSTRAINT business_sales_sales_status_check CHECK (status IN ('CONFIRMED', 'CANCELLED')),
    CONSTRAINT business_sales_sales_amounts_check CHECK (subtotal_cents >= 0 AND tax_cents >= 0 AND total_cents >= 0),
    UNIQUE (id, client_id),
    UNIQUE (client_id, sale_number),
    UNIQUE (client_id, quote_id),
    UNIQUE (client_id, conversion_idempotency_key)
);

CREATE INDEX IF NOT EXISTS business_sales_sales_client_idx
    ON business_sales_sales (client_id, status, id);

CREATE TABLE IF NOT EXISTS business_sales_sale_lines (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    sale_id uuid NOT NULL,
    position integer NOT NULL,
    source_catalog_item_id uuid,
    description varchar(1000) NOT NULL,
    quantity_milli bigint NOT NULL,
    unit_price_cents bigint NOT NULL,
    discount_basis_points integer NOT NULL,
    tax_basis_points integer NOT NULL,
    subtotal_cents bigint NOT NULL,
    tax_cents bigint NOT NULL,
    total_cents bigint NOT NULL,
    CONSTRAINT business_sales_sale_lines_sale_tenant_fk
        FOREIGN KEY (sale_id, client_id) REFERENCES business_sales_sales (id, client_id) ON DELETE RESTRICT,
    CONSTRAINT business_sales_sale_lines_position_check CHECK (position > 0),
    CONSTRAINT business_sales_sale_lines_quantity_check CHECK (quantity_milli > 0),
    CONSTRAINT business_sales_sale_lines_amounts_check CHECK (subtotal_cents >= 0 AND tax_cents >= 0 AND total_cents >= 0),
    UNIQUE (sale_id, position)
);

CREATE INDEX IF NOT EXISTS business_sales_sale_lines_sale_idx
    ON business_sales_sale_lines (client_id, sale_id, position);

CREATE TABLE IF NOT EXISTS business_sales_audit_events (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    entity_type varchar(32) NOT NULL,
    entity_id uuid NOT NULL,
    action varchar(40) NOT NULL,
    actor_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
    version integer,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT business_sales_audit_events_metadata_object_check CHECK (jsonb_typeof(metadata) = 'object')
);

CREATE INDEX IF NOT EXISTS business_sales_audit_events_client_idx
    ON business_sales_audit_events (client_id, entity_type, entity_id, created_at DESC);

-- Read-only convenience projection. It is private like every other beta table;
-- Ktor additionally scopes all queries by the authenticated tenant.
CREATE OR REPLACE VIEW business_catalog_stock_summary AS
SELECT
    item.client_id,
    item.id AS item_id,
    COALESCE(SUM(movement.delta_milli), 0)::bigint AS available_milli
FROM business_catalog_items item
LEFT JOIN business_stock_movements movement
    ON movement.client_id = item.client_id AND movement.item_id = item.id
GROUP BY item.client_id, item.id;

DO $$
DECLARE
    table_name text;
    api_role text;
BEGIN
    FOREACH table_name IN ARRAY ARRAY[
        'business_sales_customers', 'business_sales_contacts', 'business_catalog_items',
        'business_stock_movements', 'business_sales_document_counters', 'business_sales_quotes',
        'business_sales_quote_lines', 'business_sales_sales', 'business_sales_sale_lines',
        'business_sales_audit_events'
    ]
    LOOP
        EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', table_name);
        EXECUTE format('REVOKE ALL PRIVILEGES ON TABLE public.%I FROM PUBLIC', table_name);
    END LOOP;
    EXECUTE 'REVOKE ALL PRIVILEGES ON TABLE public.business_catalog_stock_summary FROM PUBLIC';

    FOREACH api_role IN ARRAY ARRAY['anon', 'authenticated']
    LOOP
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = api_role) THEN
            FOREACH table_name IN ARRAY ARRAY[
                'business_sales_customers', 'business_sales_contacts', 'business_catalog_items',
                'business_stock_movements', 'business_sales_document_counters', 'business_sales_quotes',
                'business_sales_quote_lines', 'business_sales_sales', 'business_sales_sale_lines',
                'business_sales_audit_events', 'business_catalog_stock_summary'
            ]
            LOOP
                EXECUTE format('REVOKE ALL PRIVILEGES ON TABLE public.%I FROM %I', table_name, api_role);
            END LOOP;
        END IF;
    END LOOP;
END;
$$;

DROP TRIGGER IF EXISTS touch_business_sales_customers_updated_at ON business_sales_customers;
CREATE TRIGGER touch_business_sales_customers_updated_at BEFORE UPDATE ON business_sales_customers FOR EACH ROW EXECUTE FUNCTION set_updated_at();
DROP TRIGGER IF EXISTS touch_business_sales_contacts_updated_at ON business_sales_contacts;
CREATE TRIGGER touch_business_sales_contacts_updated_at BEFORE UPDATE ON business_sales_contacts FOR EACH ROW EXECUTE FUNCTION set_updated_at();
DROP TRIGGER IF EXISTS touch_business_catalog_items_updated_at ON business_catalog_items;
CREATE TRIGGER touch_business_catalog_items_updated_at BEFORE UPDATE ON business_catalog_items FOR EACH ROW EXECUTE FUNCTION set_updated_at();
DROP TRIGGER IF EXISTS touch_business_sales_quotes_updated_at ON business_sales_quotes;
CREATE TRIGGER touch_business_sales_quotes_updated_at BEFORE UPDATE ON business_sales_quotes FOR EACH ROW EXECUTE FUNCTION set_updated_at();
