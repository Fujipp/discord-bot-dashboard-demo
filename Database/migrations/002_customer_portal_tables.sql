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
  ('price-embed-cron', 'Price Embed Scheduler', 'Automatically refresh price embeds on a schedule with button-based child messages.', 8900, 'THB', 'AUTOMATION', NULL, NULL, FALSE, 130)
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
