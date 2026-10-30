-- Execute ONLY on a deployment created from the original v1.1 schema.
-- Never run if appointments.sync_key already exists.
-- Backup the database before migration.
ALTER TABLE appointments
  ADD COLUMN sync_key VARCHAR(64) NULL UNIQUE,
  ADD COLUMN sync_version INT UNSIGNED NOT NULL DEFAULT 1;
