-- Existing H2 database only. Back up first; do not execute schema.sql/data.sql again.
ALTER TABLE member ADD COLUMN IF NOT EXISTS withdrawn BOOLEAN NOT NULL DEFAULT FALSE;
