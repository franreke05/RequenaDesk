BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS public.client_access_codes (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES public.clients(id) ON DELETE CASCADE,
    owner_admin_id uuid NOT NULL REFERENCES public.users(id) ON DELETE RESTRICT,
    code_plain text,
    code_hash text NOT NULL UNIQUE,
    expires_at timestamptz NOT NULL,
    used_at timestamptz,
    created_by uuid NOT NULL REFERENCES public.users(id) ON DELETE RESTRICT,
    created_at timestamptz NOT NULL DEFAULT NOW()
);

ALTER TABLE public.client_access_codes
    ADD COLUMN IF NOT EXISTS code_plain text;

CREATE INDEX IF NOT EXISTS idx_client_access_codes_client_id ON public.client_access_codes(client_id);
CREATE INDEX IF NOT EXISTS idx_client_access_codes_owner_admin_id ON public.client_access_codes(owner_admin_id);
CREATE INDEX IF NOT EXISTS idx_client_access_codes_code_hash ON public.client_access_codes(code_hash);
CREATE INDEX IF NOT EXISTS idx_client_access_codes_expires_at ON public.client_access_codes(expires_at);

CREATE INDEX IF NOT EXISTS idx_client_access_codes_code_plain
    ON public.client_access_codes(code_plain);

WITH pending_clients AS (
    SELECT c.id AS client_id, c.owner_admin_id
    FROM public.clients c
    WHERE NOT EXISTS (
        SELECT 1
        FROM public.client_access_codes cac
        WHERE cac.client_id = c.id
          AND cac.code_plain IS NOT NULL
          AND cac.expires_at > NOW()
    )
),
generated_codes AS (
    SELECT
        client_id,
        owner_admin_id,
        'ORY-' || UPPER(SUBSTRING(REPLACE(gen_random_uuid()::text, '-', '') FROM 1 FOR 12)) AS code_plain
    FROM pending_clients
)
INSERT INTO public.client_access_codes (
    client_id,
    owner_admin_id,
    code_plain,
    code_hash,
    expires_at,
    created_by
)
SELECT
    client_id,
    owner_admin_id,
    code_plain,
    encode(digest(code_plain, 'sha256'), 'hex'),
    NOW() + INTERVAL '10 years',
    owner_admin_id
FROM generated_codes
ON CONFLICT (code_hash) DO NOTHING;

WITH oposibot AS (
    SELECT id AS client_id, owner_admin_id, contact_name, company_name, email
    FROM public.clients
    WHERE company_name ILIKE 'Oposibot%'
       OR product_name ILIKE 'Oposibot%'
       OR email::text ILIKE '%oposibot%'
    ORDER BY created_at ASC
    LIMIT 1
),
expired_previous AS (
    UPDATE public.client_access_codes cac
    SET expires_at = NOW()
    FROM oposibot o
    WHERE cac.client_id = o.client_id
      AND cac.code_plain IS DISTINCT FROM 'ORY-OPOSIBOT-2026'
      AND cac.expires_at > NOW()
    RETURNING cac.id
)
INSERT INTO public.client_access_codes (
    client_id,
    owner_admin_id,
    code_plain,
    code_hash,
    expires_at,
    created_by
)
SELECT
    client_id,
    owner_admin_id,
    'ORY-OPOSIBOT-2026',
    encode(digest('ORY-OPOSIBOT-2026', 'sha256'), 'hex'),
    NOW() + INTERVAL '10 years',
    owner_admin_id
FROM oposibot
ON CONFLICT (code_hash) DO UPDATE
SET client_id = EXCLUDED.client_id,
    owner_admin_id = EXCLUDED.owner_admin_id,
    code_plain = EXCLUDED.code_plain,
    expires_at = EXCLUDED.expires_at,
    used_at = NULL;

WITH oposibot AS (
    SELECT id AS client_id, contact_name, company_name, email
    FROM public.clients
    WHERE company_name ILIKE 'Oposibot%'
       OR product_name ILIKE 'Oposibot%'
       OR email::text ILIKE '%oposibot%'
    ORDER BY created_at ASC
    LIMIT 1
)
INSERT INTO public.users (
    client_id,
    name,
    email,
    password_hash,
    role,
    is_active
)
SELECT
    client_id,
    COALESCE(NULLIF(contact_name, ''), company_name),
    email,
    crypt('ORY-OPOSIBOT-2026', gen_salt('bf', 12)),
    'CLIENT',
    TRUE
FROM oposibot
ON CONFLICT (email) DO UPDATE
SET client_id = EXCLUDED.client_id,
    name = EXCLUDED.name,
    password_hash = EXCLUDED.password_hash,
    role = 'CLIENT',
    is_active = TRUE
WHERE users.role = 'CLIENT';

COMMIT;
