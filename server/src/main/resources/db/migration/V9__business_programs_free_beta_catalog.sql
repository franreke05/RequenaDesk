-- Seven business applications for the controlled beta. Access is still granted
-- only from an administrator-approved request; the server stores a zero price
-- so no beta approval can create a subscription charge.

ALTER TABLE client_program_requests
    DROP CONSTRAINT IF EXISTS client_program_requests_quote_check;

ALTER TABLE client_program_requests
    ADD CONSTRAINT client_program_requests_quote_check
    CHECK (quoted_monthly_price_cents IS NULL OR quoted_monthly_price_cents >= 0);

-- Keep old rows for audit/FK integrity, but do not expose legacy commercial
-- entries in the new seven-program beta catalog.
UPDATE product_catalog
SET
    monthly_price_cents = 0,
    is_requestable = FALSE,
    is_available = FALSE,
    updated_at = NOW()
WHERE product_key IN ('SERVICE_SLA', 'SHEETS');

INSERT INTO product_catalog (
    product_key, name, short_description, category, icon_key,
    monthly_price_cents, currency, is_requestable, is_available, capabilities, sort_order
) VALUES
    ('BUSINESS_INVOICING', 'Facturación', 'Crea borradores, series internas y documentos comerciales para revisar durante la beta.', 'Finanzas', 'receipt-text', 0, 'EUR', TRUE, TRUE, '["invoice_drafts", "invoice_lines", "payment_status"]'::jsonb, 10),
    ('BUSINESS_ACCOUNTING', 'Contabilidad y gastos', 'Registra gastos, categorías y una visión de caja para el control interno del negocio.', 'Finanzas', 'landmark', 0, 'EUR', TRUE, TRUE, '["expenses", "categories", "cash_summary"]'::jsonb, 20),
    ('BUSINESS_CUSTOMERS', 'Clientes y contactos', 'Centraliza empresas, contactos y notas comerciales de tu negocio.', 'Ventas', 'users', 0, 'EUR', TRUE, TRUE, '["companies", "contacts", "notes"]'::jsonb, 30),
    ('BUSINESS_QUOTES', 'Presupuestos y ventas', 'Prepara presupuestos con líneas, estados y seguimiento comercial.', 'Ventas', 'file-text', 0, 'EUR', TRUE, TRUE, '["quote_drafts", "quote_lines", "sales_pipeline"]'::jsonb, 40),
    ('BUSINESS_CATALOG', 'Productos, servicios y stock', 'Gestiona catálogo, precios de referencia y existencias básicas.', 'Operaciones', 'package', 0, 'EUR', TRUE, TRUE, '["products", "services", "stock_levels"]'::jsonb, 50),
    ('BUSINESS_BOOKINGS', 'Agenda y reservas', 'Organiza citas, reservas y disponibilidad de tu equipo.', 'Operaciones', 'calendar-days', 0, 'EUR', TRUE, TRUE, '["appointments", "availability", "booking_status"]'::jsonb, 60),
    ('BUSINESS_DOCUMENTS', 'Documentos y firmas', 'Prepara documentos, controla versiones y recoge aceptación durante la beta.', 'Operaciones', 'file-signature', 0, 'EUR', TRUE, TRUE, '["document_drafts", "versions", "signature_requests"]'::jsonb, 70)
ON CONFLICT (product_key) DO UPDATE
SET
    name = EXCLUDED.name,
    short_description = EXCLUDED.short_description,
    category = EXCLUDED.category,
    icon_key = EXCLUDED.icon_key,
    monthly_price_cents = 0,
    currency = EXCLUDED.currency,
    is_requestable = TRUE,
    is_available = TRUE,
    capabilities = EXCLUDED.capabilities,
    sort_order = EXCLUDED.sort_order,
    updated_at = NOW();
