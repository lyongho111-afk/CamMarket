-- Widen opening hours before using the requested shop catalog on an existing H2 database.
ALTER TABLE shops ALTER COLUMN busitime VARCHAR(100);
