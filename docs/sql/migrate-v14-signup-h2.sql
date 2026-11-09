-- Existing v14 DB only. Back up before applying once.
ALTER TABLE shops ADD COLUMN photo VARCHAR(255);
