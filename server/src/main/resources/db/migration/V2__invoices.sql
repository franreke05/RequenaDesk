CREATE SEQUENCE IF NOT EXISTS invoice_number_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS invoices (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_number text NOT NULL UNIQUE,
    client_id uuid NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    status text NOT NULL DEFAULT 'DRAFT',
    issued_at date NOT NULL DEFAULT CURRENT_DATE,
    due_at date,
    notes text,
    tax_percent numeric(5, 2) NOT NULL DEFAULT 0,
    created_by uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW(),
    sent_at timestamptz,
    paid_at timestamptz
);

CREATE TABLE IF NOT EXISTS invoice_items (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id uuid NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    description text NOT NULL,
    quantity numeric(10, 2) NOT NULL,
    unit_price numeric(10, 2) NOT NULL,
    sort_order integer NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'invoices_status_check') THEN
        ALTER TABLE invoices
            ADD CONSTRAINT invoices_status_check
            CHECK (status IN ('DRAFT', 'SENT', 'PAID', 'CANCELLED'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'invoices_tax_percent_check') THEN
        ALTER TABLE invoices
            ADD CONSTRAINT invoices_tax_percent_check CHECK (tax_percent >= 0 AND tax_percent <= 100);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'invoice_items_quantity_check') THEN
        ALTER TABLE invoice_items
            ADD CONSTRAINT invoice_items_quantity_check CHECK (quantity > 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'invoice_items_unit_price_check') THEN
        ALTER TABLE invoice_items
            ADD CONSTRAINT invoice_items_unit_price_check CHECK (unit_price >= 0);
    END IF;
END;
$$;

CREATE INDEX IF NOT EXISTS idx_invoices_client_id ON invoices(client_id);
CREATE INDEX IF NOT EXISTS idx_invoices_created_by ON invoices(created_by);
CREATE INDEX IF NOT EXISTS idx_invoices_status_issued_at ON invoices(status, issued_at DESC);
CREATE INDEX IF NOT EXISTS idx_invoice_items_invoice_id ON invoice_items(invoice_id);

DROP TRIGGER IF EXISTS trg_invoices_set_updated_at ON invoices;
CREATE TRIGGER trg_invoices_set_updated_at
BEFORE UPDATE ON invoices
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

ALTER TABLE invoices ENABLE ROW LEVEL SECURITY;
ALTER TABLE invoice_items ENABLE ROW LEVEL SECURITY;
