package project.discord.backend.payment;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import project.discord.backend.payment.OmisePromptPayClient.ChargeStatus;
import project.discord.backend.payment.OmisePromptPayClient.PromptPayCharge;
import project.discord.backend.payment.dto.CheckoutItemResponse;
import project.discord.backend.payment.dto.CheckoutRequest;
import project.discord.backend.payment.dto.CheckoutResponse;
import project.discord.backend.user.domain.UserAccount;

@Service
public class PaymentService {

    private final JdbcClient jdbcClient;
    private final OmisePromptPayClient omisePromptPayClient;

    public PaymentService(JdbcClient jdbcClient, OmisePromptPayClient omisePromptPayClient) {
        this.jdbcClient = jdbcClient;
        this.omisePromptPayClient = omisePromptPayClient;
    }

    @Transactional
    public CheckoutResponse createCheckout(UserAccount user, CheckoutRequest request) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login is required");
        }
        if (request == null || request.botId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please select a bot before checkout");
        }

        Long botId = requireOwnedBot(user.getId(), request.botId());
        List<CheckoutFeature> features = loadCheckoutFeatures(request.featureIds());
        if (features.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please select at least one feature");
        }

        String currency = features.get(0).currency();
        if (features.stream().anyMatch(feature -> !feature.currency().equals(currency))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "All checkout items must use the same currency");
        }

        int amountCents = features.stream().mapToInt(CheckoutFeature::amountCents).sum();
        if (amountCents < 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PromptPay checkout requires at least THB 10");
        }

        String checkoutReference = "CHK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 18).toUpperCase();
        PromptPayCharge charge = omisePromptPayClient.createPromptPayCharge(amountCents, currency, checkoutReference);
        Long paymentId = insertPayment(user.getId(), botId, charge, checkoutReference, amountCents, currency);
        insertPaymentItems(paymentId, features);

        return mapCheckout(paymentId);
    }

    public CheckoutResponse getCheckout(UserAccount user, Long paymentId) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login is required");
        }

        Long ownerId = jdbcClient.sql("SELECT user_id FROM payments WHERE id = :paymentId")
                .param("paymentId", paymentId)
                .query(Long.class)
                .optional()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        if (!ownerId.equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Payment belongs to another user");
        }

        return mapCheckout(paymentId);
    }

    @Transactional
    public void handleOmiseWebhook(Map<String, Object> payload) {
        if (payload == null) {
            return;
        }

        String eventKey = asString(payload.get("key"));
        if (!"charge.complete".equals(eventKey) && !"charge.create".equals(eventKey)) {
            return;
        }

        Object dataObject = payload.get("data");
        if (!(dataObject instanceof Map<?, ?> data)) {
            return;
        }

        String chargeId = asString(data.get("id"));
        String webhookStatus = asString(data.get("status"));
        String failureMessage = asString(data.get("failure_message"));
        if (chargeId == null) {
            return;
        }

        ChargeStatus chargeStatus;
        try {
            chargeStatus = omisePromptPayClient
                    .retrieveChargeStatus(chargeId)
                    .orElse(new ChargeStatus(chargeId, webhookStatus == null ? "pending" : webhookStatus, failureMessage));
        } catch (RuntimeException exception) {
            chargeStatus = new ChargeStatus(chargeId, webhookStatus == null ? "pending" : webhookStatus, failureMessage);
        }

        applyChargeStatus(chargeStatus.chargeId(), chargeStatus.status(), chargeStatus.failureMessage());
    }

    @Transactional
    public CheckoutResponse applyChargeStatus(String providerPaymentId, String providerStatus, String failureMessage) {
        PaymentRecord payment = jdbcClient.sql("""
                SELECT id, user_id, bot_id, status, amount_cents, currency
                FROM payments
                WHERE provider_payment_id = :providerPaymentId
                """)
                .param("providerPaymentId", providerPaymentId)
                .query(this::mapPayment)
                .optional()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        if ("successful".equalsIgnoreCase(providerStatus)) {
            if (!"PAID".equals(payment.status())) {
                Long billingSubscriptionId = activateBilling(payment.userId(), payment.amountCents(), payment.currency(), providerPaymentId);
                activateFeatures(payment.id(), payment.botId());
                jdbcClient.sql("""
                        UPDATE payments
                        SET status = 'PAID',
                            billing_subscription_id = :billingSubscriptionId,
                            paid_at = :paidAt,
                            failure_message = NULL
                        WHERE id = :paymentId
                        """)
                        .param("paymentId", payment.id())
                        .param("billingSubscriptionId", billingSubscriptionId)
                        .param("paidAt", Timestamp.from(Instant.now()))
                        .update();
            }
        } else if ("failed".equalsIgnoreCase(providerStatus) || "expired".equalsIgnoreCase(providerStatus)) {
            jdbcClient.sql("""
                    UPDATE payments
                    SET status = 'FAILED',
                        failure_message = :failureMessage
                    WHERE id = :paymentId
                      AND status = 'PENDING'
                    """)
                    .param("paymentId", payment.id())
                    .param("failureMessage", failureMessage == null ? providerStatus : failureMessage)
                    .update();
        }

        return mapCheckout(payment.id());
    }

    private Long requireOwnedBot(Long userId, Long botId) {
        return jdbcClient.sql("""
                SELECT id
                FROM discord_bots
                WHERE id = :botId
                  AND owner_user_id = :userId
                """)
                .param("botId", botId)
                .param("userId", userId)
                .query(Long.class)
                .optional()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected bot was not found"));
    }

    private List<CheckoutFeature> loadCheckoutFeatures(List<Long> featureIds) {
        Set<Long> uniqueFeatureIds = new LinkedHashSet<>(featureIds == null ? List.of() : featureIds);
        if (uniqueFeatureIds.isEmpty()) {
            return List.of();
        }

        List<CheckoutFeature> features = new ArrayList<>();
        for (Long featureId : uniqueFeatureIds) {
            CheckoutFeature feature = jdbcClient.sql("""
                    SELECT id, code, name, monthly_price_cents, promotion_price_cents, currency
                    FROM feature_catalog
                    WHERE id = :featureId
                      AND is_active = TRUE
                    """)
                    .param("featureId", featureId)
                    .query((rs, rowNum) -> new CheckoutFeature(
                            rs.getLong("id"),
                            rs.getString("code"),
                            rs.getString("name"),
                            rs.getObject("promotion_price_cents") == null ? rs.getInt("monthly_price_cents") : rs.getInt("promotion_price_cents"),
                            rs.getString("currency")
                    ))
                    .optional()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Feature not found: " + featureId));
            features.add(feature);
        }

        return features;
    }

    private Long insertPayment(Long userId, Long botId, PromptPayCharge charge, String checkoutReference, int amountCents, String currency) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql("""
                INSERT INTO payments (
                  user_id, bot_id, provider, provider_payment_id, provider_source_id,
                  checkout_reference, status, amount_cents, currency, qr_code_url, expires_at
                )
                VALUES (
                  :userId, :botId, 'OMISE_PROMPTPAY', :providerPaymentId, :providerSourceId,
                  :checkoutReference, 'PENDING', :amountCents, :currency, :qrCodeUrl, :expiresAt
                )
                """)
                .param("userId", userId)
                .param("botId", botId)
                .param("providerPaymentId", charge.chargeId())
                .param("providerSourceId", charge.sourceId())
                .param("checkoutReference", checkoutReference)
                .param("amountCents", amountCents)
                .param("currency", currency)
                .param("qrCodeUrl", charge.qrCodeUrl())
                .param("expiresAt", Timestamp.from(charge.expiresAt()))
                .update(keyHolder, "id");

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Could not create payment");
        }

        return key.longValue();
    }

    private void insertPaymentItems(Long paymentId, List<CheckoutFeature> features) {
        for (CheckoutFeature feature : features) {
            jdbcClient.sql("""
                    INSERT INTO payment_items (payment_id, feature_id, feature_code, feature_name, amount_cents, currency)
                    VALUES (:paymentId, :featureId, :featureCode, :featureName, :amountCents, :currency)
                    """)
                    .param("paymentId", paymentId)
                    .param("featureId", feature.id())
                    .param("featureCode", feature.code())
                    .param("featureName", feature.name())
                    .param("amountCents", feature.amountCents())
                    .param("currency", feature.currency())
                    .update();
        }
    }

    private Long activateBilling(Long userId, Integer amountCents, String currency, String providerPaymentId) {
        LocalDate today = LocalDate.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql("""
                INSERT INTO billing_subscriptions (
                  user_id, status, provider, provider_subscription_id, monthly_total_cents,
                  currency, current_period_start, current_period_end, cancel_at_period_end
                )
                VALUES (
                  :userId, 'ACTIVE', 'OMISE_PROMPTPAY', :providerPaymentId, :amountCents,
                  :currency, :periodStart, :periodEnd, FALSE
                )
                """)
                .param("userId", userId)
                .param("providerPaymentId", providerPaymentId)
                .param("amountCents", amountCents)
                .param("currency", currency)
                .param("periodStart", Date.valueOf(today))
                .param("periodEnd", Date.valueOf(today.plusMonths(1)))
                .update(keyHolder, "id");

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Could not create billing subscription");
        }

        return key.longValue();
    }

    private void activateFeatures(Long paymentId, Long botId) {
        LocalDate today = LocalDate.now();
        List<Long> featureIds = jdbcClient.sql("SELECT feature_id FROM payment_items WHERE payment_id = :paymentId")
                .param("paymentId", paymentId)
                .query(Long.class)
                .list();

        for (Long featureId : featureIds) {
            jdbcClient.sql("""
                    INSERT INTO bot_feature_subscriptions (
                      bot_id, feature_id, status, current_period_start, current_period_end, auto_renew
                    )
                    VALUES (:botId, :featureId, 'ACTIVE', :periodStart, :periodEnd, TRUE)
                    ON DUPLICATE KEY UPDATE
                      status = 'ACTIVE',
                      current_period_start = VALUES(current_period_start),
                      current_period_end = VALUES(current_period_end),
                      auto_renew = TRUE
                    """)
                    .param("botId", botId)
                    .param("featureId", featureId)
                    .param("periodStart", Date.valueOf(today))
                    .param("periodEnd", Date.valueOf(today.plusMonths(1)))
                    .update();
        }
    }

    private CheckoutResponse mapCheckout(Long paymentId) {
        CheckoutPayment payment = jdbcClient.sql("""
                SELECT id, checkout_reference, provider, provider_payment_id, status, amount_cents,
                       currency, qr_code_url, expires_at
                FROM payments
                WHERE id = :paymentId
                """)
                .param("paymentId", paymentId)
                .query((rs, rowNum) -> new CheckoutPayment(
                        rs.getLong("id"),
                        rs.getString("checkout_reference"),
                        rs.getString("provider"),
                        rs.getString("provider_payment_id"),
                        rs.getString("status"),
                        rs.getInt("amount_cents"),
                        rs.getString("currency"),
                        rs.getString("qr_code_url"),
                        rs.getTimestamp("expires_at") == null ? null : rs.getTimestamp("expires_at").toInstant()
                ))
                .single();

        List<CheckoutItemResponse> items = jdbcClient.sql("""
                SELECT feature_id, feature_code, feature_name, amount_cents, currency
                FROM payment_items
                WHERE payment_id = :paymentId
                ORDER BY id ASC
                """)
                .param("paymentId", paymentId)
                .query((rs, rowNum) -> new CheckoutItemResponse(
                        rs.getLong("feature_id"),
                        rs.getString("feature_code"),
                        rs.getString("feature_name"),
                        rs.getInt("amount_cents"),
                        rs.getString("currency")
                ))
                .list();

        return new CheckoutResponse(
                payment.id(),
                payment.checkoutReference(),
                payment.provider(),
                payment.providerPaymentId(),
                payment.status(),
                payment.amountCents(),
                payment.currency(),
                payment.qrCodeUrl(),
                payment.expiresAt(),
                items
        );
    }

    private PaymentRecord mapPayment(ResultSet rs, int rowNum) throws SQLException {
        return new PaymentRecord(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getLong("bot_id"),
                rs.getString("status"),
                rs.getInt("amount_cents"),
                rs.getString("currency")
        );
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private record CheckoutFeature(Long id, String code, String name, Integer amountCents, String currency) {
    }

    private record PaymentRecord(Long id, Long userId, Long botId, String status, Integer amountCents, String currency) {
    }

    private record CheckoutPayment(
            Long id,
            String checkoutReference,
            String provider,
            String providerPaymentId,
            String status,
            Integer amountCents,
            String currency,
            String qrCodeUrl,
            Instant expiresAt
    ) {
    }
}
