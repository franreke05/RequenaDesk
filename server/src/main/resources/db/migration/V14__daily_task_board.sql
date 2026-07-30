-- Daily task planning board. A placement belongs to exactly one task and one
-- calendar day. Expired rows are intentionally retained (bounded to one row
-- per task) so re-planning can atomically overwrite them without a cleanup job.

ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS created_by_user_id uuid;

-- Existing client tasks are attributed to the oldest active portal user for
-- that client. Internal tasks and the rare client without a portal user fall
-- back to the tenant administrator.
UPDATE tasks task
SET created_by_user_id = COALESCE(
    (
        SELECT portal_user.id
        FROM users portal_user
        WHERE portal_user.client_id = task.client_id
        ORDER BY portal_user.is_active DESC, portal_user.created_at ASC, portal_user.id ASC
        LIMIT 1
    ),
    task.owner_admin_id
)
WHERE task.created_by_user_id IS NULL;

ALTER TABLE tasks
    ALTER COLUMN created_by_user_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'tasks_created_by_user_id_fkey'
    ) THEN
        ALTER TABLE tasks
            ADD CONSTRAINT tasks_created_by_user_id_fkey
            FOREIGN KEY (created_by_user_id) REFERENCES users(id) ON DELETE RESTRICT;
    END IF;
END
$$;

CREATE INDEX IF NOT EXISTS idx_tasks_created_by_user_id
    ON tasks(created_by_user_id);

CREATE TABLE IF NOT EXISTS task_board_placements (
    task_id uuid PRIMARY KEY REFERENCES tasks(id) ON DELETE CASCADE,
    board_date date NOT NULL,
    position_x numeric(7,6) NOT NULL,
    position_y numeric(7,6) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT task_board_placements_position_x_check
        CHECK (position_x >= 0 AND position_x <= 1),
    CONSTRAINT task_board_placements_position_y_check
        CHECK (position_y >= 0 AND position_y <= 1)
);

CREATE INDEX IF NOT EXISTS idx_task_board_placements_date
    ON task_board_placements(board_date);

DROP TRIGGER IF EXISTS trg_task_board_placements_set_updated_at ON task_board_placements;
CREATE TRIGGER trg_task_board_placements_set_updated_at
BEFORE UPDATE ON task_board_placements
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE OR REPLACE FUNCTION remove_completed_task_board_placement()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.completed THEN
        DELETE FROM task_board_placements WHERE task_id = NEW.id;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_tasks_remove_completed_board_placement ON tasks;
CREATE TRIGGER trg_tasks_remove_completed_board_placement
AFTER UPDATE OF completed ON tasks
FOR EACH ROW EXECUTE FUNCTION remove_completed_task_board_placement();

-- Supabase exposes public tables through PostgREST. The Ktor server is the only
-- allowed data boundary for this admin-only board.
ALTER TABLE public.task_board_placements ENABLE ROW LEVEL SECURITY;

REVOKE ALL PRIVILEGES ON public.task_board_placements FROM PUBLIC;

DO $$
DECLARE
    api_role text;
BEGIN
    FOREACH api_role IN ARRAY ARRAY['anon', 'authenticated']
    LOOP
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = api_role) THEN
            EXECUTE format(
                'REVOKE ALL PRIVILEGES ON TABLE public.task_board_placements FROM %I',
                api_role
            );
        END IF;
    END LOOP;
END
$$;
