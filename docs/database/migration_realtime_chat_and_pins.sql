BEGIN;

ALTER TABLE public.ticket_messages
    ADD COLUMN IF NOT EXISTS client_message_id text;

CREATE UNIQUE INDEX IF NOT EXISTS idx_ticket_messages_client_message_id_unique
    ON public.ticket_messages(ticket_id, author_id, client_message_id)
    WHERE client_message_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_ticket_messages_ticket_id_created_at_desc
    ON public.ticket_messages(ticket_id, created_at DESC, id DESC);

CREATE TABLE IF NOT EXISTS public.pinned_ticket_threads (
    user_id uuid NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    ticket_id uuid NOT NULL REFERENCES public.tickets(id) ON DELETE CASCADE,
    pinned_at timestamptz NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, ticket_id)
);

CREATE INDEX IF NOT EXISTS idx_pinned_ticket_threads_user_pinned_at
    ON public.pinned_ticket_threads(user_id, pinned_at DESC);

CREATE INDEX IF NOT EXISTS idx_pinned_ticket_threads_ticket_id
    ON public.pinned_ticket_threads(ticket_id);

CREATE TABLE IF NOT EXISTS public.pinned_tasks (
    user_id uuid NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    task_id uuid NOT NULL REFERENCES public.tasks(id) ON DELETE CASCADE,
    pinned_at timestamptz NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, task_id)
);

CREATE INDEX IF NOT EXISTS idx_pinned_tasks_user_pinned_at
    ON public.pinned_tasks(user_id, pinned_at DESC);

CREATE INDEX IF NOT EXISTS idx_pinned_tasks_task_id
    ON public.pinned_tasks(task_id);

COMMIT;
