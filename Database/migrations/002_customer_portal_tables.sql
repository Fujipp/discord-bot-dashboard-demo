USE discord_server_management;

CREATE TABLE IF NOT EXISTS discord_bots (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  owner_user_id BIGINT UNSIGNED NOT NULL,
  discord_application_id VARCHAR(64) NOT NULL,
  name VARCHAR(100) NOT NULL,
  avatar_url VARCHAR(1024) NULL,
  status ENUM('ONLINE', 'OFFLINE', 'MAINTENANCE') NOT NULL DEFAULT 'OFFLINE',
  server_count INT UNSIGNED NOT NULL DEFAULT 0,
  command_count INT UNSIGNED NOT NULL DEFAULT 0,
  uptime_percent DECIMAL(5,2) NOT NULL DEFAULT 0.00,
  hosted_region VARCHAR(64) NOT NULL DEFAULT 'sgp1',
  last_heartbeat_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_discord_bots_application_id (discord_application_id),
  KEY idx_discord_bots_owner_user_id (owner_user_id),
  CONSTRAINT fk_discord_bots_owner
    FOREIGN KEY (owner_user_id) REFERENCES users (id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS feature_catalog (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  code VARCHAR(64) NOT NULL,
  name VARCHAR(100) NOT NULL,
  description VARCHAR(500) NOT NULL,
  monthly_price_cents INT UNSIGNED NOT NULL,
  currency CHAR(3) NOT NULL DEFAULT 'THB',
  category ENUM('MODERATION', 'MUSIC', 'SUPPORT', 'ANALYTICS', 'AUTOMATION', 'AI') NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_feature_catalog_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS bot_feature_subscriptions (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  bot_id BIGINT UNSIGNED NOT NULL,
  feature_id BIGINT UNSIGNED NOT NULL,
  status ENUM('TRIALING', 'ACTIVE', 'PAST_DUE', 'CANCELED') NOT NULL DEFAULT 'ACTIVE',
  current_period_start DATE NOT NULL,
  current_period_end DATE NOT NULL,
  auto_renew BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_bot_feature_subscription (bot_id, feature_id),
  KEY idx_bot_feature_subscriptions_feature_id (feature_id),
  CONSTRAINT fk_bot_feature_subscriptions_bot
    FOREIGN KEY (bot_id) REFERENCES discord_bots (id)
    ON DELETE CASCADE,
  CONSTRAINT fk_bot_feature_subscriptions_feature
    FOREIGN KEY (feature_id) REFERENCES feature_catalog (id)
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS billing_subscriptions (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  status ENUM('TRIALING', 'ACTIVE', 'PAST_DUE', 'CANCELED') NOT NULL DEFAULT 'ACTIVE',
  provider VARCHAR(40) NOT NULL DEFAULT 'MANUAL',
  provider_subscription_id VARCHAR(191) NULL,
  monthly_total_cents INT UNSIGNED NOT NULL DEFAULT 0,
  currency CHAR(3) NOT NULL DEFAULT 'THB',
  current_period_start DATE NOT NULL,
  current_period_end DATE NOT NULL,
  cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_billing_subscriptions_user_id (user_id),
  CONSTRAINT fk_billing_subscriptions_user
    FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS payments (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  billing_subscription_id BIGINT UNSIGNED NULL,
  provider VARCHAR(40) NOT NULL DEFAULT 'MANUAL',
  provider_payment_id VARCHAR(191) NULL,
  status ENUM('PENDING', 'PAID', 'FAILED', 'REFUNDED') NOT NULL DEFAULT 'PENDING',
  amount_cents INT UNSIGNED NOT NULL,
  currency CHAR(3) NOT NULL DEFAULT 'THB',
  paid_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_payments_user_id (user_id),
  KEY idx_payments_billing_subscription_id (billing_subscription_id),
  CONSTRAINT fk_payments_user
    FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE,
  CONSTRAINT fk_payments_billing_subscription
    FOREIGN KEY (billing_subscription_id) REFERENCES billing_subscriptions (id)
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO feature_catalog (code, name, description, monthly_price_cents, currency, category)
VALUES
  ('auto-moderation', 'Auto Moderation', 'Filter spam, links, mentions, and raids before they damage the community.', 9900, 'THB', 'MODERATION'),
  ('ticket-center', 'Ticket Center', 'Create support panels, private ticket channels, transcripts, and staff notes.', 14900, 'THB', 'SUPPORT'),
  ('music-247', 'Music 24/7', 'Keep audio sessions alive with queue control, playlists, and DJ role permissions.', 19900, 'THB', 'MUSIC'),
  ('analytics-lite', 'Analytics Lite', 'Track command usage, server growth, and member engagement trends.', 7900, 'THB', 'ANALYTICS'),
  ('ai-commands', 'AI Commands', 'Add AI-powered slash commands for helpers, summaries, and community utilities.', 24900, 'THB', 'AI')
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  description = VALUES(description),
  monthly_price_cents = VALUES(monthly_price_cents),
  currency = VALUES(currency),
  category = VALUES(category),
  is_active = TRUE;
