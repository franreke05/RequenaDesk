-- V18: which company uses the local PDF invoices of the desktop app (CLAUDE.md, lane A).
--
-- Lane A is OryKai's own invoicing: PDFs generated on the desktop and saved in
-- Escritorio/Facturas OryKai, never stored on the server. Other SaaS companies must not see
-- that screen; they will get server-side Verifactu invoicing (lane B) in phase 4.
--
-- The flag only says "show this screen"; no invoice data is stored. V17 named the company
-- of the oldest admin "OryKai", so that company's owner keeps the screen.

ALTER TABLE organizations ADD COLUMN IF NOT EXISTS local_pdf_invoices boolean NOT NULL DEFAULT FALSE;

UPDATE organizations SET local_pdf_invoices = TRUE
WHERE id = (
    SELECT m.organization_id
    FROM organization_members m
    JOIN users u ON u.id = m.user_id
    WHERE m.role = 'OWNER'
    ORDER BY u.created_at, u.id
    LIMIT 1
);
