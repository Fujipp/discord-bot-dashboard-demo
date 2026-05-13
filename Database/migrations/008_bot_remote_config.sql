USE discord_server_management;

CREATE TABLE IF NOT EXISTS bot_config_entries (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  bot_id BIGINT UNSIGNED NOT NULL,
  config_key VARCHAR(120) NOT NULL,
  config_value TEXT NULL,
  is_secret BOOLEAN NOT NULL DEFAULT FALSE,
  scope VARCHAR(80) NOT NULL DEFAULT 'bot',
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_bot_config_entry (bot_id, config_key),
  KEY idx_bot_config_entries_bot_id (bot_id),
  CONSTRAINT fk_bot_config_entries_bot
    FOREIGN KEY (bot_id) REFERENCES discord_bots (id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
