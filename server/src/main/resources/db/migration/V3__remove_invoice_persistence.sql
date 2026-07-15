-- Invoices are generated on demand and never persisted. Only invoice_number_seq
-- stays, since sequential numbering still needs a durable, atomic counter.
DROP TABLE IF EXISTS invoice_items;
DROP TABLE IF EXISTS invoices;
