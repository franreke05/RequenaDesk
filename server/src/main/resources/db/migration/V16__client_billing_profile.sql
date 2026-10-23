-- Fiscal data of each client, used as the recipient when an invoice is generated.
-- These are client attributes, not invoices: invoices are still generated as local PDFs and never stored.
ALTER TABLE public.clients
    ADD COLUMN IF NOT EXISTS billing_legal_name varchar(200) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS billing_tax_id varchar(20) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS billing_address_line varchar(250) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS billing_postal_code varchar(12) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS billing_city varchar(120) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS billing_province varchar(120) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS billing_country varchar(80) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS billing_email varchar(254) NOT NULL DEFAULT '';

-- Tax ids are stored normalized (upper case, letters and digits only) so lookups and printing are stable.
ALTER TABLE public.clients DROP CONSTRAINT IF EXISTS clients_billing_tax_id_format_check;
ALTER TABLE public.clients
    ADD CONSTRAINT clients_billing_tax_id_format_check
        CHECK (billing_tax_id = '' OR billing_tax_id ~ '^[A-Z0-9]{5,20}$');
