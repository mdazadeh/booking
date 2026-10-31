-- VaghYar 1.5.1: non-destructive optional schedule policy migration.
-- Run once via phpMyAdmin; no changes to existing clients, appointments, invitations or requests.
-- The old 10:00-21:00 daily web booking window remains active until the admin explicitly publishes.
CREATE TABLE IF NOT EXISTS booking_schedule_policy (
  id TINYINT UNSIGNED NOT NULL PRIMARY KEY,
  version INT UNSIGNED NOT NULL DEFAULT 1,
  published TINYINT(1) NOT NULL DEFAULT 0,
  days_json LONGTEXT NULL,
  updated_at DATETIME NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT IGNORE INTO booking_schedule_policy(id,version,published,days_json)
VALUES(1,1,0,NULL);
