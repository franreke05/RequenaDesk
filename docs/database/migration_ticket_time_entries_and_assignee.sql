-- ============================================================
-- Migration: ticket_time_entries + ticket assignee endpoint
-- Date: 2026-05-27
-- ============================================================

-- Tabla de horas registradas contra tickets (independiente de tasks)
CREATE TABLE IF NOT EXISTS ticket_time_entries (
    id          uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id   uuid        NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    author_id   uuid        NOT NULL REFERENCES users(id)  ON DELETE RESTRICT,
    minutes     integer     NOT NULL CHECK (minutes > 0),
    work_date   date        NOT NULL DEFAULT CURRENT_DATE,
    note        text        NOT NULL DEFAULT '',
    billable    boolean     NOT NULL DEFAULT false,
    created_at  timestamptz NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tte_ticket_id   ON ticket_time_entries(ticket_id);
CREATE INDEX IF NOT EXISTS idx_tte_author_id   ON ticket_time_entries(author_id);
CREATE INDEX IF NOT EXISTS idx_tte_work_date   ON ticket_time_entries(work_date DESC);

-- Trigger: actualizar updated_at del ticket cuando se agrega una entrada de tiempo
CREATE OR REPLACE FUNCTION touch_ticket_on_time_entry()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    UPDATE tickets SET updated_at = NOW() WHERE id = NEW.ticket_id;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_tte_touch_ticket ON ticket_time_entries;
CREATE TRIGGER trg_tte_touch_ticket
AFTER INSERT ON ticket_time_entries
FOR EACH ROW EXECUTE FUNCTION touch_ticket_on_time_entry();
