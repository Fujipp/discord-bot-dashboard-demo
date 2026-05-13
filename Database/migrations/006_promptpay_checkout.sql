USE discord_server_management;

ALTER TABLE payments
  ADD COLUMN checkout_reference VARCHAR(80) NULL AFTER provider_payment_id,
  ADD COLUMN bot_id BIGINT UNSIGNED NULL AFTER billing_subscription_id,
  ADD COLUMN provider_source_id VARCHAR(191) NULL AFTER provider_payment_id,
  ADD COLUMN qr_code_url VARCHAR(1024) NULL AFTER currency,
  ADD COLUMN expires_at TIMESTAMP NULL AFTER qr_code_url,
  ADD COLUMN failure_message VARCHAR(500) NULL AFTER paid_at,
  ADD UNIQUE KEY uq_payments_checkout_reference (checkout_reference),
  ADD UNIQUE KEY uq_payments_provider_payment_id (provider_payment_id),
  ADD KEY idx_payments_bot_id (bot_id),
  ADD CONSTRAINT fk_payments_bot
    FOREIGN KEY (bot_id) REFERENCES discord_bots (id)
    ON DELETE SET NULL;

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
