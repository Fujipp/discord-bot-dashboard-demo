USE discord_server_management;

INSERT INTO feature_catalog (
  code,
  name,
  description,
  monthly_price_cents,
  currency,
  category,
  promotion_label,
  promotion_price_cents,
  is_featured,
  sort_order,
  is_active
)
VALUES (
  'test-package-10',
  'Test Package 10 THB',
  'Low-value checkout package for testing PromptPay payment flow, webhook confirmation, and feature activation.',
  1000,
  'THB',
  'SUPPORT',
  'Test only',
  NULL,
  FALSE,
  999,
  TRUE
)
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
  is_active = VALUES(is_active);
