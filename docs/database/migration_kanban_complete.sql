-- ============================================================================
-- MIGRACIÓN COMPLETA: Sistema CRM Freelance + Kanban Boards
-- Para: Supabase
-- Fecha: 2026-05-17
-- VERSIÓN ROBUSTA: Maneja conflictos de constraints duplicadas
-- ============================================================================

-- ============================================================================
-- PASO 0: LIMPIAR TABLAS EXISTENTES (si hay conflictos)
-- ============================================================================

-- Eliminar constraints duplicadas si existen
ALTER TABLE IF EXISTS public.boards DROP CONSTRAINT IF EXISTS boards_owner_id_fkey;

-- ============================================================================
-- 1. TABLA: boards (Tableros Kanban)
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.boards (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  name character varying NOT NULL,
  description text DEFAULT ''::text,
  type character varying NOT NULL CHECK (type::text = ANY (ARRAY['GLOBAL'::character varying, 'PERSONAL'::character varying, 'CLIENT'::character varying]::text[])),
  owner_id uuid NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  client_id uuid REFERENCES public.clients(id) ON DELETE CASCADE,
  is_archived boolean NOT NULL DEFAULT false,
  is_public boolean NOT NULL DEFAULT false,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  archived_at timestamp with time zone,
  CONSTRAINT boards_pkey PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_boards_owner_id ON public.boards(owner_id);
CREATE INDEX IF NOT EXISTS idx_boards_client_id ON public.boards(client_id);
CREATE INDEX IF NOT EXISTS idx_boards_type ON public.boards(type);
CREATE INDEX IF NOT EXISTS idx_boards_is_archived ON public.boards(is_archived);


-- ============================================================================
-- 2. TABLA: board_columns (Columnas del Tablero)
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.board_columns (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  board_id uuid NOT NULL REFERENCES public.boards(id) ON DELETE CASCADE,
  name character varying NOT NULL,
  position integer NOT NULL,
  status character varying NOT NULL,
  color_hex character varying DEFAULT '#3498db'::character varying,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT board_columns_pkey PRIMARY KEY (id),
  CONSTRAINT board_columns_unique_per_board UNIQUE (board_id, position)
);

CREATE INDEX IF NOT EXISTS idx_board_columns_board_id ON public.board_columns(board_id);
CREATE INDEX IF NOT EXISTS idx_board_columns_status ON public.board_columns(status);


-- ============================================================================
-- 3. TABLA: board_cards (Tarjetas del Tablero - Relación con Tickets)
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.board_cards (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  board_id uuid NOT NULL REFERENCES public.boards(id) ON DELETE CASCADE,
  ticket_id uuid NOT NULL REFERENCES public.tickets(id) ON DELETE CASCADE,
  column_id uuid NOT NULL REFERENCES public.board_columns(id) ON DELETE CASCADE,
  position integer NOT NULL,
  is_hidden boolean NOT NULL DEFAULT false,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  moved_at timestamp with time zone,
  CONSTRAINT board_cards_pkey PRIMARY KEY (id),
  CONSTRAINT board_cards_unique_per_board UNIQUE (board_id, ticket_id)
);

CREATE INDEX IF NOT EXISTS idx_board_cards_board_id ON public.board_cards(board_id);
CREATE INDEX IF NOT EXISTS idx_board_cards_ticket_id ON public.board_cards(ticket_id);
CREATE INDEX IF NOT EXISTS idx_board_cards_column_id ON public.board_cards(column_id);
CREATE INDEX IF NOT EXISTS idx_board_cards_board_column ON public.board_cards(board_id, column_id);


-- ============================================================================
-- 4. TABLA: board_members (Miembros del Tablero)
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.board_members (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  board_id uuid NOT NULL REFERENCES public.boards(id) ON DELETE CASCADE,
  user_id uuid NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  role character varying NOT NULL DEFAULT 'VIEWER'::character varying
    CHECK (role::text = ANY (ARRAY['OWNER'::character varying, 'EDITOR'::character varying, 'VIEWER'::character varying]::text[])),
  added_at timestamp with time zone NOT NULL DEFAULT now(),
  added_by uuid NOT NULL REFERENCES public.users(id),
  CONSTRAINT board_members_pkey PRIMARY KEY (id),
  CONSTRAINT board_members_unique_per_board UNIQUE (board_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_board_members_board_id ON public.board_members(board_id);
CREATE INDEX IF NOT EXISTS idx_board_members_user_id ON public.board_members(user_id);
CREATE INDEX IF NOT EXISTS idx_board_members_role ON public.board_members(role);


-- ============================================================================
-- 5. TABLA: board_activity (Auditoría de Cambios en el Tablero)
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.board_activity (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  board_id uuid NOT NULL REFERENCES public.boards(id) ON DELETE CASCADE,
  card_id uuid NOT NULL REFERENCES public.board_cards(id) ON DELETE CASCADE,
  ticket_id uuid NOT NULL REFERENCES public.tickets(id) ON DELETE CASCADE,
  actor_id uuid REFERENCES public.users(id) ON DELETE SET NULL,
  action character varying NOT NULL
    CHECK (action::text = ANY (ARRAY['ADDED'::character varying, 'MOVED'::character varying, 'REMOVED'::character varying, 'HIDDEN'::character varying]::text[])),
  from_column_id uuid REFERENCES public.board_columns(id) ON DELETE SET NULL,
  to_column_id uuid REFERENCES public.board_columns(id) ON DELETE SET NULL,
  from_position integer,
  to_position integer,
  metadata jsonb DEFAULT '{}'::jsonb,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT board_activity_pkey PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_board_activity_board_id ON public.board_activity(board_id);
CREATE INDEX IF NOT EXISTS idx_board_activity_ticket_id ON public.board_activity(ticket_id);
CREATE INDEX IF NOT EXISTS idx_board_activity_actor_id ON public.board_activity(actor_id);
CREATE INDEX IF NOT EXISTS idx_board_activity_action ON public.board_activity(action);
CREATE INDEX IF NOT EXISTS idx_board_activity_created_at ON public.board_activity(created_at DESC);


-- ============================================================================
-- 6. VISTA: vw_board_with_tickets
-- ============================================================================
CREATE OR REPLACE VIEW public.vw_board_with_tickets AS
SELECT
  b.id as board_id,
  b.name as board_name,
  b.type as board_type,
  bc.id as card_id,
  bc.ticket_id,
  t.ticket_number,
  t.subject,
  t.priority,
  t.status,
  t.client_id,
  c.company_name,
  t.assignee_id,
  u.name as assignee_name,
  col.id as column_id,
  col.name as column_name,
  col.status as column_status,
  bc.position as card_position,
  bc.moved_at,
  bc.created_at as card_created_at
FROM public.boards b
LEFT JOIN public.board_cards bc ON b.id = bc.board_id
LEFT JOIN public.board_columns col ON bc.column_id = col.id
LEFT JOIN public.tickets t ON bc.ticket_id = t.id
LEFT JOIN public.clients c ON t.client_id = c.id
LEFT JOIN public.users u ON t.assignee_id = u.id
WHERE b.is_archived = false AND bc.is_hidden = false
ORDER BY b.id, col.position, bc.position;


-- ============================================================================
-- 7. FUNCIÓN: fn_create_default_board_columns
-- ============================================================================
CREATE OR REPLACE FUNCTION public.fn_create_default_board_columns(p_board_id uuid)
RETURNS void AS $$
BEGIN
  INSERT INTO public.board_columns (board_id, name, position, status, color_hex)
  VALUES (p_board_id, 'Abiertos', 1, 'OPEN', '#E74C3C');

  INSERT INTO public.board_columns (board_id, name, position, status, color_hex)
  VALUES (p_board_id, 'En Progreso', 2, 'IN_PROGRESS', '#F39C12');

  INSERT INTO public.board_columns (board_id, name, position, status, color_hex)
  VALUES (p_board_id, 'Pendiente Cliente', 3, 'PENDING_CLIENT', '#3498DB');

  INSERT INTO public.board_columns (board_id, name, position, status, color_hex)
  VALUES (p_board_id, 'Resuelto', 4, 'RESOLVED', '#27AE60');
END;
$$ LANGUAGE plpgsql;


-- ============================================================================
-- 8. FUNCIÓN: fn_move_board_card
-- ============================================================================
CREATE OR REPLACE FUNCTION public.fn_move_board_card(
  p_card_id uuid,
  p_to_column_id uuid,
  p_to_position integer,
  p_actor_id uuid
)
RETURNS void AS $$
DECLARE
  v_board_id uuid;
  v_from_column_id uuid;
  v_from_position integer;
  v_ticket_id uuid;
BEGIN
  SELECT board_id, column_id, position, ticket_id
  INTO v_board_id, v_from_column_id, v_from_position, v_ticket_id
  FROM public.board_cards
  WHERE id = p_card_id
  FOR UPDATE;

  UPDATE public.board_cards
  SET
    column_id = p_to_column_id,
    position = p_to_position,
    moved_at = now(),
    updated_at = now()
  WHERE id = p_card_id;

  INSERT INTO public.board_activity
  (board_id, card_id, ticket_id, actor_id, action, from_column_id, to_column_id, from_position, to_position)
  VALUES
  (v_board_id, p_card_id, v_ticket_id, p_actor_id, 'MOVED'::text, v_from_column_id, p_to_column_id, v_from_position, p_to_position);
END;
$$ LANGUAGE plpgsql;


-- ============================================================================
-- 9. FUNCIÓN: fn_add_ticket_to_board
-- ============================================================================
CREATE OR REPLACE FUNCTION public.fn_add_ticket_to_board(
  p_board_id uuid,
  p_ticket_id uuid,
  p_column_id uuid,
  p_actor_id uuid
)
RETURNS uuid AS $$
DECLARE
  v_next_position integer;
  v_card_id uuid;
BEGIN
  SELECT COALESCE(MAX(position), 0) + 1
  INTO v_next_position
  FROM public.board_cards
  WHERE board_id = p_board_id AND column_id = p_column_id;

  INSERT INTO public.board_cards
  (board_id, ticket_id, column_id, position)
  VALUES (p_board_id, p_ticket_id, p_column_id, v_next_position)
  RETURNING id INTO v_card_id;

  INSERT INTO public.board_activity
  (board_id, card_id, ticket_id, actor_id, action, to_column_id, to_position)
  VALUES
  (p_board_id, v_card_id, p_ticket_id, p_actor_id, 'ADDED'::text, p_column_id, v_next_position);

  RETURN v_card_id;
END;
$$ LANGUAGE plpgsql;


-- ============================================================================
-- 10. FUNCIÓN: fn_hide_board_card
-- ============================================================================
CREATE OR REPLACE FUNCTION public.fn_hide_board_card(
  p_card_id uuid,
  p_actor_id uuid
)
RETURNS void AS $$
DECLARE
  v_board_id uuid;
  v_ticket_id uuid;
BEGIN
  SELECT board_id, ticket_id INTO v_board_id, v_ticket_id
  FROM public.board_cards
  WHERE id = p_card_id;

  UPDATE public.board_cards
  SET is_hidden = true, updated_at = now()
  WHERE id = p_card_id;

  INSERT INTO public.board_activity
  (board_id, card_id, ticket_id, actor_id, action)
  VALUES (v_board_id, p_card_id, v_ticket_id, p_actor_id, 'HIDDEN'::text);
END;
$$ LANGUAGE plpgsql;


-- ============================================================================
-- 11. MEJORAS A LA TABLA: tickets
-- ============================================================================
ALTER TABLE public.tickets
ADD COLUMN IF NOT EXISTS board_order integer DEFAULT NULL;

CREATE INDEX IF NOT EXISTS idx_tickets_board_order ON public.tickets(board_order);


-- ============================================================================
-- 12. DATOS INICIALES: Tablero Global
-- ============================================================================
INSERT INTO public.boards (name, description, type, owner_id, is_public)
SELECT
  'Tablero Global',
  'Tablero compartido para todos los administradores',
  'GLOBAL'::character varying,
  u.id,
  true
FROM public.users u
WHERE u.role = 'ADMIN'::character varying
LIMIT 1
ON CONFLICT DO NOTHING;

-- Crear columnas predeterminadas para el tablero global
DO $$
DECLARE
  v_board_id uuid;
BEGIN
  SELECT id INTO v_board_id FROM public.boards WHERE type = 'GLOBAL'::character varying LIMIT 1;
  IF v_board_id IS NOT NULL THEN
    PERFORM public.fn_create_default_board_columns(v_board_id);
  END IF;
END $$;


-- ============================================================================
-- 13. TRIGGERS
-- ============================================================================

-- Trigger: Actualizar updated_at en boards
CREATE OR REPLACE FUNCTION public.fn_update_boards_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_update_boards_timestamp ON public.boards;
CREATE TRIGGER trigger_update_boards_timestamp
BEFORE UPDATE ON public.boards
FOR EACH ROW
EXECUTE FUNCTION public.fn_update_boards_timestamp();

-- Trigger: Actualizar updated_at en board_columns
CREATE OR REPLACE FUNCTION public.fn_update_board_columns_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_update_board_columns_timestamp ON public.board_columns;
CREATE TRIGGER trigger_update_board_columns_timestamp
BEFORE UPDATE ON public.board_columns
FOR EACH ROW
EXECUTE FUNCTION public.fn_update_board_columns_timestamp();

-- Trigger: Actualizar updated_at en board_cards
CREATE OR REPLACE FUNCTION public.fn_update_board_cards_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_update_board_cards_timestamp ON public.board_cards;
CREATE TRIGGER trigger_update_board_cards_timestamp
BEFORE UPDATE ON public.board_cards
FOR EACH ROW
EXECUTE FUNCTION public.fn_update_board_cards_timestamp();

-- ============================================================================
-- FIN DE LA MIGRACIÓN
-- ============================================================================
-- ✓ 5 nuevas tablas principales
-- ✓ 1 vista consolidada
-- ✓ 4 funciones especializadas
-- ✓ 3 triggers para auditoría automática
-- ✓ Mejoras a tabla tickets
-- ✓ Índices para optimizar rendimiento
-- ✓ Datos iniciales (Tablero Global)
-- ============================================================================
