-- Real work sessions (start/pause/resume/stop) and their segments (tramos),
-- replacing the in-memory-only timer. time_logs becomes the immutable,
-- per-local-day billable unit derived from a session's segments; a session
-- that crosses local midnight produces one time_logs row per local date,
-- linked back to the same session_id.
--
-- Billing documents stay entirely local/PDF-only (no tables/columns here
-- reference them, nor any billed status or reservation) per CLAUDE.md.

CREATE TABLE IF NOT EXISTS work_sessions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id uuid NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    owner_admin_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    client_id uuid REFERENCES clients(id) ON DELETE SET NULL,
    author_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    started_at timestamptz NOT NULL,
    ended_at timestamptz,
    status varchar(12) NOT NULL DEFAULT 'ACTIVE',
    billable boolean NOT NULL,
    time_source varchar(20) NOT NULL DEFAULT 'SERVER',
    business_timezone varchar(40) NOT NULL DEFAULT 'Europe/Madrid',
    last_checkpoint_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT work_sessions_status_check CHECK (status IN ('ACTIVE', 'PAUSED', 'FINISHED')),
    CONSTRAINT work_sessions_time_source_check CHECK (time_source IN ('SERVER', 'LOCAL_PROVISIONAL')),
    CONSTRAINT work_sessions_ended_at_consistency CHECK ((status = 'FINISHED') = (ended_at IS NOT NULL)),
    CONSTRAINT work_sessions_ended_after_started CHECK (ended_at IS NULL OR ended_at >= started_at)
);

-- Only one non-finished (ACTIVE or PAUSED) session per tenant (owner_admin_id).
-- This is the DB-level guarantee behind "solo un temporizador activo"; combined
-- with domain-level and route-level checks so a race between two concurrent
-- start requests cannot create two live sessions for the same freelancer.
CREATE UNIQUE INDEX IF NOT EXISTS idx_work_sessions_single_active_per_owner
    ON work_sessions (owner_admin_id)
    WHERE status IN ('ACTIVE', 'PAUSED');

CREATE INDEX IF NOT EXISTS idx_work_sessions_task_id ON work_sessions(task_id);
CREATE INDEX IF NOT EXISTS idx_work_sessions_owner_admin_id ON work_sessions(owner_admin_id);
CREATE INDEX IF NOT EXISTS idx_work_sessions_status ON work_sessions(status);

DROP TRIGGER IF EXISTS trg_work_sessions_set_updated_at ON work_sessions;
CREATE TRIGGER trg_work_sessions_set_updated_at
BEFORE UPDATE ON work_sessions
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE IF NOT EXISTS work_segments (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id uuid NOT NULL REFERENCES work_sessions(id) ON DELETE CASCADE,
    started_at timestamptz NOT NULL,
    ended_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT work_segments_ended_after_started CHECK (ended_at IS NULL OR ended_at >= started_at)
);

-- Only one open (still running) segment per session - the "tramo activo".
CREATE UNIQUE INDEX IF NOT EXISTS idx_work_segments_single_open_per_session
    ON work_segments (session_id)
    WHERE ended_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_work_segments_session_id ON work_segments(session_id);

-- time_logs becomes the immutable per-(session, local work day) billable unit.
-- work_date is nullable ONLY for the migrated "historic time without a
-- breakdown" bucket (section 21 of the spec): real duration preserved, but
-- with no invented date and no session, so it cannot appear on a calendar day.
ALTER TABLE time_logs ALTER COLUMN work_date DROP NOT NULL;
ALTER TABLE time_logs ADD COLUMN IF NOT EXISTS session_id uuid REFERENCES work_sessions(id) ON DELETE SET NULL;
ALTER TABLE time_logs ADD COLUMN IF NOT EXISTS is_historic_migrated boolean NOT NULL DEFAULT FALSE;

ALTER TABLE time_logs DROP CONSTRAINT IF EXISTS time_logs_historic_date_consistency;
ALTER TABLE time_logs ADD CONSTRAINT time_logs_historic_date_consistency
    CHECK (is_historic_migrated = (work_date IS NULL));

CREATE INDEX IF NOT EXISTS idx_time_logs_session_id ON time_logs(session_id);

-- Guarantee (not just an assumption): at most one historic-migrated bucket
-- row per task, so re-running the backfill below can never duplicate it.
CREATE UNIQUE INDEX IF NOT EXISTS idx_time_logs_one_historic_per_task
    ON time_logs (task_id)
    WHERE is_historic_migrated;

-- Idempotent backfill: for every task whose accumulator (tasks.logged_minutes)
-- exceeds the sum of its time_logs rows, the excess predates per-session
-- tracking (or predates time_logs entirely) and is preserved as a single,
-- undated "historic time without breakdown" unit - never invented a date,
-- never attached to a session. Running this migration again is a no-op
-- because of the partial unique index above (ON CONFLICT DO NOTHING) and
-- because the WHERE clause re-evaluates the (now updated) sum each time.
INSERT INTO time_logs (task_id, client_id, author_id, minutes, seconds, work_date, note, billable, is_historic_migrated)
SELECT
    t.id,
    t.client_id,
    t.owner_admin_id,
    (t.logged_minutes - COALESCE(existing.sum_minutes, 0)),
    (t.logged_minutes - COALESCE(existing.sum_minutes, 0)) * 60,
    NULL,
    'Tiempo historico migrado (sin desglose diario)',
    (t.client_id IS NOT NULL),
    TRUE
FROM tasks t
LEFT JOIN (
    SELECT task_id, SUM(minutes) AS sum_minutes
    FROM time_logs
    WHERE NOT is_historic_migrated
    GROUP BY task_id
) existing ON existing.task_id = t.id
WHERE (t.logged_minutes - COALESCE(existing.sum_minutes, 0)) > 0
  AND NOT EXISTS (
      SELECT 1 FROM time_logs tl WHERE tl.task_id = t.id AND tl.is_historic_migrated
  )
ON CONFLICT DO NOTHING;

-- Same public-schema hardening applied to every table since V7: Supabase
-- exposes the public schema via PostgREST by default, so new tables must be
-- explicitly locked down rather than relying on the app server being the
-- only caller.
ALTER TABLE public.work_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.work_segments ENABLE ROW LEVEL SECURITY;

REVOKE ALL PRIVILEGES ON public.work_sessions FROM PUBLIC;
REVOKE ALL PRIVILEGES ON public.work_segments FROM PUBLIC;

DO $$
DECLARE
    api_role text;
BEGIN
    FOREACH api_role IN ARRAY ARRAY['anon', 'authenticated']
    LOOP
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = api_role) THEN
            EXECUTE format('REVOKE ALL PRIVILEGES ON TABLE public.work_sessions FROM %I', api_role);
            EXECUTE format('REVOKE ALL PRIVILEGES ON TABLE public.work_segments FROM %I', api_role);
        END IF;
    END LOOP;
END;
$$;
