-- Adds formations.session_count for formation progress (séances / planned sessions).
-- Hibernate ddl-auto=update may not add NOT NULL columns on existing tables reliably.

ALTER TABLE formations
ADD COLUMN IF NOT EXISTS session_count integer;

UPDATE formations f
SET session_count = GREATEST(
    COALESCE((
        SELECT COUNT(*)::integer
        FROM training_sessions ts
        WHERE ts.formation_id = f.id
          AND ts.status <> 'CANCELLED'
    ), 0),
    1
)
WHERE session_count IS NULL;

ALTER TABLE formations
ALTER COLUMN session_count SET DEFAULT 1;

ALTER TABLE formations
ALTER COLUMN session_count SET NOT NULL;
