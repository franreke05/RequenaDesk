BEGIN;

WITH ranked_labels AS (
    SELECT
        id,
        FIRST_VALUE(id) OVER (
            PARTITION BY LOWER(BTRIM(name))
            ORDER BY created_at ASC, id ASC
        ) AS canonical_id,
        ROW_NUMBER() OVER (
            PARTITION BY LOWER(BTRIM(name))
            ORDER BY created_at ASC, id ASC
        ) AS row_number
    FROM task_labels
),
duplicates AS (
    SELECT id, canonical_id
    FROM ranked_labels
    WHERE row_number > 1
)
UPDATE tasks t
SET label_id = d.canonical_id
FROM duplicates d
WHERE t.label_id = d.id;

WITH ranked_labels AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY LOWER(BTRIM(name))
            ORDER BY created_at ASC, id ASC
        ) AS row_number
    FROM task_labels
)
DELETE FROM task_labels tl
USING ranked_labels r
WHERE tl.id = r.id
  AND r.row_number > 1;

ALTER TABLE task_labels
    DROP CONSTRAINT IF EXISTS task_labels_name_owner_unique;

DROP INDEX IF EXISTS idx_task_labels_owner_admin_id;

ALTER TABLE task_labels
    ALTER COLUMN owner_admin_id DROP NOT NULL;

UPDATE task_labels
SET owner_admin_id = NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_task_labels_name_lower_unique
    ON task_labels (LOWER(BTRIM(name)));

COMMIT;
