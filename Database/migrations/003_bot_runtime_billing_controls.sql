USE discord_server_management;

ALTER TABLE discord_bots
  ADD COLUMN pm2_process_name VARCHAR(100) NULL AFTER discord_application_id,
  ADD COLUMN billing_mode ENUM('FREE', 'PAID') NOT NULL DEFAULT 'FREE' AFTER status,
  ADD COLUMN monthly_price_cents INT UNSIGNED NOT NULL DEFAULT 0 AFTER billing_mode;

ALTER TABLE discord_bots
  ADD UNIQUE KEY uq_discord_bots_pm2_process_name (pm2_process_name);
