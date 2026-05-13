USE discord_server_management;

CREATE TABLE IF NOT EXISTS automation_settings (
  setting_key VARCHAR(80) NOT NULL,
  setting_value VARCHAR(255) NOT NULL,
  description VARCHAR(255) NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS automation_runs (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  run_type VARCHAR(80) NOT NULL,
  status ENUM('SUCCESS', 'FAILED') NOT NULL DEFAULT 'SUCCESS',
  billing_marked_past_due INT UNSIGNED NOT NULL DEFAULT 0,
  feature_marked_past_due INT UNSIGNED NOT NULL DEFAULT 0,
  feature_canceled INT UNSIGNED NOT NULL DEFAULT 0,
  runtime_suspended INT UNSIGNED NOT NULL DEFAULT 0,
  notifications_created INT UNSIGNED NOT NULL DEFAULT 0,
  error_message VARCHAR(500) NULL,
  started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finished_at TIMESTAMP NULL,
  PRIMARY KEY (id),
  KEY idx_automation_runs_started_at (started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS customer_notifications (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  bot_id BIGINT UNSIGNED NULL,
  type ENUM('BILLING_REMINDER', 'SUBSCRIPTION_PAST_DUE', 'FEATURE_CANCELED', 'RUNTIME_SUSPENDED') NOT NULL,
  title VARCHAR(160) NOT NULL,
  message VARCHAR(500) NOT NULL,
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_customer_notifications_user_id (user_id),
  KEY idx_customer_notifications_created_at (created_at),
  CONSTRAINT fk_customer_notifications_user
    FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE,
  CONSTRAINT fk_customer_notifications_bot
    FOREIGN KEY (bot_id) REFERENCES discord_bots (id)
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO automation_settings (setting_key, setting_value, description)
VALUES
  ('automation.enabled', 'true', 'Run scheduled billing and runtime automation'),
  ('automation.reminder_days_before', '3', 'Create reminder notifications before current period ends'),
  ('automation.past_due_grace_days', '1', 'Days after period end before active subscriptions become past due'),
  ('automation.cancel_grace_days', '7', 'Days after period end before past-due feature subscriptions are canceled'),
  ('automation.runtime_suspend_enabled', 'false', 'Stop PM2 runtime when runtime feature is canceled; keep false until payment flow is live')
ON DUPLICATE KEY UPDATE
  description = VALUES(description);
