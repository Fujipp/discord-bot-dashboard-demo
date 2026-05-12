USE discord_server_management;

UPDATE feature_catalog
SET
  category = 'SUPPORT',
  is_active = FALSE
WHERE code IN ('auto-moderation', 'ticket-center', 'music-247', 'analytics-lite', 'ai-commands');

ALTER TABLE feature_catalog
  MODIFY category ENUM('SHOP', 'PAYMENT', 'ROBLOX', 'ENGAGEMENT', 'RUNTIME', 'ADMIN', 'AUTOMATION', 'SUPPORT') NOT NULL;

ALTER TABLE feature_catalog
  ADD COLUMN promotion_label VARCHAR(100) NULL AFTER category,
  ADD COLUMN promotion_price_cents INT UNSIGNED NULL AFTER promotion_label,
  ADD COLUMN promotion_ends_at TIMESTAMP NULL AFTER promotion_price_cents,
  ADD COLUMN is_featured BOOLEAN NOT NULL DEFAULT FALSE AFTER promotion_ends_at,
  ADD COLUMN sort_order INT NOT NULL DEFAULT 100 AFTER is_featured;

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
