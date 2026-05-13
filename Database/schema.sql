CREATE DATABASE IF NOT EXISTS discord_server_management
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE discord_server_management;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  email VARCHAR(320) NOT NULL,
  username VARCHAR(32) NOT NULL,
  password_hash VARCHAR(255) NULL,
  age TINYINT UNSIGNED NULL,
  avatar_url VARCHAR(1024) NULL,
  role ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER',
  status ENUM('ACTIVE', 'DISABLED', 'BANNED') NOT NULL DEFAULT 'ACTIVE',
  email_verified BOOLEAN NOT NULL DEFAULT FALSE,
  last_login_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_users_email (email),
  UNIQUE KEY uq_users_username (username),
  CONSTRAINT chk_users_age CHECK (age IS NULL OR age BETWEEN 13 AND 120)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS oauth_accounts (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  provider ENUM('DISCORD', 'GOOGLE', 'GITHUB') NOT NULL,
  provider_user_id VARCHAR(191) NOT NULL,
  provider_username VARCHAR(191) NULL,
  provider_email VARCHAR(320) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_oauth_provider_user (provider, provider_user_id),
  KEY idx_oauth_accounts_user_id (user_id),
  CONSTRAINT fk_oauth_accounts_user
    FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS discord_bots (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  owner_user_id BIGINT UNSIGNED NOT NULL,
  discord_application_id VARCHAR(64) NOT NULL,
  pm2_process_name VARCHAR(100) NULL,
  name VARCHAR(100) NOT NULL,
  avatar_url VARCHAR(1024) NULL,
  status ENUM('ONLINE', 'OFFLINE', 'MAINTENANCE') NOT NULL DEFAULT 'OFFLINE',
  billing_mode ENUM('FREE', 'PAID') NOT NULL DEFAULT 'FREE',
  monthly_price_cents INT UNSIGNED NOT NULL DEFAULT 0,
  server_count INT UNSIGNED NOT NULL DEFAULT 0,
  command_count INT UNSIGNED NOT NULL DEFAULT 0,
  uptime_percent DECIMAL(5,2) NOT NULL DEFAULT 0.00,
  hosted_region VARCHAR(64) NOT NULL DEFAULT 'sgp1',
  last_heartbeat_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_discord_bots_application_id (discord_application_id),
  UNIQUE KEY uq_discord_bots_pm2_process_name (pm2_process_name),
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
  category ENUM('SHOP', 'PAYMENT', 'ROBLOX', 'ENGAGEMENT', 'RUNTIME', 'ADMIN', 'AUTOMATION', 'SUPPORT') NOT NULL,
  promotion_label VARCHAR(100) NULL,
  promotion_price_cents INT UNSIGNED NULL,
  promotion_ends_at TIMESTAMP NULL,
  is_featured BOOLEAN NOT NULL DEFAULT FALSE,
  sort_order INT NOT NULL DEFAULT 100,
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
  bot_id BIGINT UNSIGNED NULL,
  provider VARCHAR(40) NOT NULL DEFAULT 'MANUAL',
  provider_payment_id VARCHAR(191) NULL,
  provider_source_id VARCHAR(191) NULL,
  checkout_reference VARCHAR(80) NULL,
  status ENUM('PENDING', 'PAID', 'FAILED', 'REFUNDED') NOT NULL DEFAULT 'PENDING',
  amount_cents INT UNSIGNED NOT NULL,
  currency CHAR(3) NOT NULL DEFAULT 'THB',
  qr_code_url VARCHAR(1024) NULL,
  expires_at TIMESTAMP NULL,
  paid_at TIMESTAMP NULL,
  failure_message VARCHAR(500) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_payments_checkout_reference (checkout_reference),
  UNIQUE KEY uq_payments_provider_payment_id (provider_payment_id),
  KEY idx_payments_user_id (user_id),
  KEY idx_payments_billing_subscription_id (billing_subscription_id),
  KEY idx_payments_bot_id (bot_id),
  CONSTRAINT fk_payments_user
    FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE,
  CONSTRAINT fk_payments_billing_subscription
    FOREIGN KEY (billing_subscription_id) REFERENCES billing_subscriptions (id)
    ON DELETE SET NULL,
  CONSTRAINT fk_payments_bot
    FOREIGN KEY (bot_id) REFERENCES discord_bots (id)
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS payment_items (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  payment_id BIGINT UNSIGNED NOT NULL,
  feature_id BIGINT UNSIGNED NOT NULL,
  feature_code VARCHAR(64) NOT NULL,
  feature_name VARCHAR(100) NOT NULL,
  amount_cents INT UNSIGNED NOT NULL,
  currency CHAR(3) NOT NULL DEFAULT 'THB',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_payment_item_feature (payment_id, feature_id),
  KEY idx_payment_items_feature_id (feature_id),
  CONSTRAINT fk_payment_items_payment
    FOREIGN KEY (payment_id) REFERENCES payments (id)
    ON DELETE CASCADE,
  CONSTRAINT fk_payment_items_feature
    FOREIGN KEY (feature_id) REFERENCES feature_catalog (id)
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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

INSERT INTO feature_catalog (code, name, description, monthly_price_cents, currency, category, promotion_label, promotion_price_cents, is_featured, sort_order)
VALUES
  ('runtime-247', 'Runtime 24/7', 'Host one Discord bot process on managed PM2 runtime with restart control and uptime monitoring.', 9900, 'THB', 'RUNTIME', 'Launch price', 7900, TRUE, 10),
  ('shop-orders', 'Shop Orders', 'Record customer orders, product name, quantity, price, attachments, and order log embeds.', 12900, 'THB', 'SHOP', NULL, NULL, TRUE, 20),
  ('shop-status', 'Shop Status', 'Open and close store announcements with status embeds and channel name updates.', 7900, 'THB', 'SHOP', NULL, NULL, FALSE, 30),
  ('payment-embed', 'Payment Embed', 'Publish a payment panel with buttons, menus, configurable channels, roles, and admin permissions.', 9900, 'THB', 'PAYMENT', NULL, NULL, TRUE, 40),
  ('promptpay-slipok', 'PromptPay + SlipOK', 'Generate PromptPay payment flow, receive slips, verify payments, and notify staff channels.', 17900, 'THB', 'PAYMENT', 'Popular', 15900, TRUE, 50),
  ('truemoney-voucher', 'TrueMoney Voucher', 'Accept TrueMoney gift links, validate wallet topups, and credit users automatically.', 14900, 'THB', 'PAYMENT', NULL, NULL, FALSE, 60),
  ('wallet-credit', 'Wallet Credit', 'Manage user balance, add/update/delete credit, check balances, and keep topup history.', 9900, 'THB', 'PAYMENT', NULL, NULL, FALSE, 70),
  ('roblox-seller', 'Roblox Seller', 'Validate Roblox users and groups, show Robux packages, confirm purchases, and track payout results.', 24900, 'THB', 'ROBLOX', 'Premium pack core', 21900, TRUE, 80),
  ('top-spender-rank', 'Top Spender Rank', 'Rank customers by spending, refresh TOP roles, and display leaderboards.', 7900, 'THB', 'ENGAGEMENT', NULL, NULL, FALSE, 90),
  ('review-credit', 'Review Credit', 'Count review messages, sync credit counters, refresh replies, and rename review channels.', 9900, 'THB', 'ENGAGEMENT', NULL, NULL, FALSE, 100),
  ('admin-message-tools', 'Admin Message Tools', 'Send messages, send files, edit bot messages, and DM customers through modal workflows.', 8900, 'THB', 'ADMIN', NULL, NULL, FALSE, 110),
  ('voice-keeper', 'Voice Keeper 24/7', 'Keep a bot connected to a voice channel around the clock with join and leave controls.', 6900, 'THB', 'AUTOMATION', NULL, NULL, FALSE, 120),
  ('price-embed-cron', 'Price Embed Scheduler', 'Automatically refresh price embeds on a schedule with button-based child messages.', 8900, 'THB', 'AUTOMATION', NULL, NULL, FALSE, 130),
  ('test-package-10', 'Test Package 10 THB', 'Low-value checkout package for testing PromptPay payment flow, webhook confirmation, and feature activation.', 1000, 'THB', 'SUPPORT', 'Test only', NULL, FALSE, 999)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  description = VALUES(description),
  monthly_price_cents = VALUES(monthly_price_cents),
  currency = VALUES(currency),
  category = VALUES(category),
  promotion_label = VALUES(promotion_label),
  promotion_price_cents = VALUES(promotion_price_cents),
  is_featured = VALUES(is_featured),
  sort_order = VALUES(sort_order),
  is_active = TRUE;
