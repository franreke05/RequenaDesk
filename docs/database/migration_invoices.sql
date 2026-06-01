-- Migration: Invoices module
-- Creates invoices and invoice_items tables for admin billing and client portal

CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_number TEXT NOT NULL UNIQUE,
    client_id UUID NOT NULL REFERENCES clients(id),
    status TEXT NOT NULL DEFAULT 'DRAFT',   -- DRAFT | SENT | PAID | CANCELLED
    issued_at DATE NOT NULL DEFAULT CURRENT_DATE,
    due_at DATE,
    notes TEXT,
    tax_percent DECIMAL(5,2) NOT NULL DEFAULT 0,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sent_at TIMESTAMPTZ,
    paid_at TIMESTAMPTZ
);

CREATE TABLE invoice_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    description TEXT NOT NULL,
    quantity DECIMAL(10,2) NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0
);

-- RLS policies: admin sees all, client sees only SENT/PAID invoices for their client_id
ALTER TABLE invoices ENABLE ROW LEVEL SECURITY;
ALTER TABLE invoice_items ENABLE ROW LEVEL SECURITY;

-- Sequence for human-readable invoice numbers (FAC-2026-001)
CREATE SEQUENCE IF NOT EXISTS invoice_number_seq START 1;
