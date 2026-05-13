package project.discord.backend.automation;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import project.discord.backend.automation.dto.AutomationDashboardResponse;
import project.discord.backend.automation.dto.AutomationRunResponse;
import project.discord.backend.automation.dto.AutomationSettingResponse;
import project.discord.backend.automation.dto.AutomationSettingUpdateRequest;
import project.discord.backend.runtime.Pm2RuntimeService;

@Service
public class AutomationService {

    private static final Set<String> ALLOWED_SETTINGS = Set.of(
            "automation.enabled",
            "automation.reminder_days_before",
            "automation.past_due_grace_days",
            "automation.cancel_grace_days",
            "automation.runtime_suspend_enabled"
    );

    private final JdbcClient jdbcClient;
    private final Pm2RuntimeService pm2RuntimeService;

    public AutomationService(JdbcClient jdbcClient, Pm2RuntimeService pm2RuntimeService) {
        this.jdbcClient = jdbcClient;
        this.pm2RuntimeService = pm2RuntimeService;
    }

    public AutomationDashboardResponse getDashboard() {
        AutomationPolicy policy = loadPolicy();

        return new AutomationDashboardResponse(
                policy.enabled(),
                policy.runtimeSuspendEnabled(),
                policy.reminderDaysBefore(),
                policy.pastDueGraceDays(),
                policy.cancelGraceDays(),
                countByStatus("billing_subscriptions", "ACTIVE"),
                countByStatus("billing_subscriptions", "PAST_DUE"),
                countByStatus("bot_feature_subscriptions", "ACTIVE"),
                countByStatus("bot_feature_subscriptions", "PAST_DUE"),
                listSettings(),
                recentRuns()
        );
    }

    @Transactional
    public AutomationDashboardResponse updateSettings(AutomationSettingUpdateRequest request) {
        if (request.settings() == null) {
            return getDashboard();
        }

        for (Map.Entry<String, String> entry : request.settings().entrySet()) {
            if (!ALLOWED_SETTINGS.contains(entry.getKey())) {
                continue;
            }

            jdbcClient.sql("""
                    UPDATE automation_settings
                    SET setting_value = :value
                    WHERE setting_key = :key
                    """)
                    .param("key", entry.getKey())
                    .param("value", normalizeSettingValue(entry.getKey(), entry.getValue()))
                    .update();
        }

        return getDashboard();
    }

    @Transactional
    public AutomationRunResponse runManual() {
        return runAutomation("MANUAL");
    }

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Bangkok")
    @Transactional
    public void runScheduled() {
        if (loadPolicy().enabled()) {
            runAutomation("SCHEDULED");
        }
    }

    private AutomationRunResponse runAutomation(String runType) {
        Instant startedAt = Instant.now();
        Long runId = insertRun(runType, startedAt);

        try {
            AutomationPolicy policy = loadPolicy();
            LocalDate today = LocalDate.now();
            int reminders = createBillingReminders(today.plusDays(policy.reminderDaysBefore()));
            int billingPastDue = markBillingPastDue(today.minusDays(policy.pastDueGraceDays()));
            int featurePastDue = markFeaturePastDue(today.minusDays(policy.pastDueGraceDays()));
            int featureCanceled = cancelPastDueFeatures(today.minusDays(policy.cancelGraceDays()));
            int runtimeSuspended = policy.runtimeSuspendEnabled() ? suspendCanceledRuntimeBots() : 0;

            updateRunSuccess(runId, billingPastDue, featurePastDue, featureCanceled, runtimeSuspended, reminders);
        } catch (RuntimeException exception) {
            updateRunFailure(runId, exception.getMessage());
        }

        return recentRun(runId);
    }

    private int createBillingReminders(LocalDate reminderDate) {
        List<BillingReminderTarget> targets = jdbcClient.sql("""
                SELECT bs.user_id, bs.id AS subscription_id, bs.current_period_end, bs.monthly_total_cents
                FROM billing_subscriptions bs
                WHERE bs.status = 'ACTIVE'
                  AND bs.current_period_end <= :reminderDate
                  AND NOT EXISTS (
                    SELECT 1
                    FROM customer_notifications cn
                    WHERE cn.user_id = bs.user_id
                      AND cn.type = 'BILLING_REMINDER'
                      AND DATE(cn.created_at) = CURRENT_DATE
                  )
                """)
                .param("reminderDate", Date.valueOf(reminderDate))
                .query((rs, rowNum) -> new BillingReminderTarget(
                        rs.getLong("user_id"),
                        rs.getDate("current_period_end").toLocalDate(),
                        rs.getInt("monthly_total_cents")
                ))
                .list();

        for (BillingReminderTarget target : targets) {
            createNotification(
                    target.userId(),
                    null,
                    "BILLING_REMINDER",
                    "Subscription renewal is coming",
                    "Your subscription renews on " + target.currentPeriodEnd() + " for " + target.monthlyTotalCents() / 100 + " THB."
            );
        }

        return targets.size();
    }

    private int markBillingPastDue(LocalDate cutoffDate) {
        return jdbcClient.sql("""
                UPDATE billing_subscriptions
                SET status = 'PAST_DUE'
                WHERE status = 'ACTIVE'
                  AND current_period_end < :cutoffDate
                """)
                .param("cutoffDate", Date.valueOf(cutoffDate))
                .update();
    }

    private int markFeaturePastDue(LocalDate cutoffDate) {
        List<FeatureSubscriptionTarget> targets = expiredFeatureSubscriptions("ACTIVE", cutoffDate);
        for (FeatureSubscriptionTarget target : targets) {
            jdbcClient.sql("""
                    UPDATE bot_feature_subscriptions
                    SET status = 'PAST_DUE'
                    WHERE id = :id
                    """)
                    .param("id", target.subscriptionId())
                    .update();
            createNotification(
                    target.ownerUserId(),
                    target.botId(),
                    "SUBSCRIPTION_PAST_DUE",
                    "Feature subscription is past due",
                    target.featureName() + " for " + target.botName() + " is past due."
            );
        }

        return targets.size();
    }

    private int cancelPastDueFeatures(LocalDate cutoffDate) {
        List<FeatureSubscriptionTarget> targets = expiredFeatureSubscriptions("PAST_DUE", cutoffDate);
        for (FeatureSubscriptionTarget target : targets) {
            jdbcClient.sql("""
                    UPDATE bot_feature_subscriptions
                    SET status = 'CANCELED'
                    WHERE id = :id
                    """)
                    .param("id", target.subscriptionId())
                    .update();
            createNotification(
                    target.ownerUserId(),
                    target.botId(),
                    "FEATURE_CANCELED",
                    "Feature was canceled",
                    target.featureName() + " for " + target.botName() + " was canceled after the grace period."
            );
        }

        return targets.size();
    }

    private int suspendCanceledRuntimeBots() {
        List<RuntimeSuspensionTarget> targets = jdbcClient.sql("""
                SELECT DISTINCT db.id AS bot_id, db.owner_user_id, db.name, db.pm2_process_name
                FROM discord_bots db
                INNER JOIN bot_feature_subscriptions bfs ON bfs.bot_id = db.id
                INNER JOIN feature_catalog fc ON fc.id = bfs.feature_id
                WHERE fc.code = 'runtime-247'
                  AND bfs.status = 'CANCELED'
                  AND db.pm2_process_name IS NOT NULL
                  AND db.status <> 'OFFLINE'
                """)
                .query((rs, rowNum) -> new RuntimeSuspensionTarget(
                        rs.getLong("bot_id"),
                        rs.getLong("owner_user_id"),
                        rs.getString("name"),
                        rs.getString("pm2_process_name")
                ))
                .list();

        int suspended = 0;
        for (RuntimeSuspensionTarget target : targets) {
            try {
                pm2RuntimeService.runAction(target.pm2ProcessName(), "stop");
                jdbcClient.sql("UPDATE discord_bots SET status = 'OFFLINE' WHERE id = :botId")
                        .param("botId", target.botId())
                        .update();
                createNotification(
                        target.ownerUserId(),
                        target.botId(),
                        "RUNTIME_SUSPENDED",
                        "Runtime was suspended",
                        target.botName() + " was stopped because Runtime 24/7 is no longer active."
                );
                suspended++;
            } catch (RuntimeException ignored) {
                // Keep the automation run alive so other bots can still be processed.
            }
        }

        return suspended;
    }

    private List<FeatureSubscriptionTarget> expiredFeatureSubscriptions(String status, LocalDate cutoffDate) {
        return jdbcClient.sql("""
                SELECT
                  bfs.id AS subscription_id,
                  db.id AS bot_id,
                  db.owner_user_id,
                  db.name AS bot_name,
                  fc.name AS feature_name
                FROM bot_feature_subscriptions bfs
                INNER JOIN discord_bots db ON db.id = bfs.bot_id
                INNER JOIN feature_catalog fc ON fc.id = bfs.feature_id
                WHERE bfs.status = :status
                  AND bfs.current_period_end < :cutoffDate
                """)
                .param("status", status)
                .param("cutoffDate", Date.valueOf(cutoffDate))
                .query((rs, rowNum) -> new FeatureSubscriptionTarget(
                        rs.getLong("subscription_id"),
                        rs.getLong("bot_id"),
                        rs.getLong("owner_user_id"),
                        rs.getString("bot_name"),
                        rs.getString("feature_name")
                ))
                .list();
    }

    private void createNotification(Long userId, Long botId, String type, String title, String message) {
        jdbcClient.sql("""
                INSERT INTO customer_notifications (user_id, bot_id, type, title, message)
                VALUES (:userId, :botId, :type, :title, :message)
                """)
                .param("userId", userId)
                .param("botId", botId)
                .param("type", type)
                .param("title", title)
                .param("message", message)
                .update();
    }

    private Long insertRun(String runType, Instant startedAt) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql("""
                INSERT INTO automation_runs (run_type, started_at)
                VALUES (:runType, :startedAt)
                """)
                .param("runType", runType)
                .param("startedAt", Timestamp.from(startedAt))
                .update(keyHolder, "id");

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Could not create automation run");
        }

        return key.longValue();
    }

    private void updateRunSuccess(Long runId, int billingPastDue, int featurePastDue, int featureCanceled, int runtimeSuspended, int notifications) {
        jdbcClient.sql("""
                UPDATE automation_runs
                SET status = 'SUCCESS',
                    billing_marked_past_due = :billingPastDue,
                    feature_marked_past_due = :featurePastDue,
                    feature_canceled = :featureCanceled,
                    runtime_suspended = :runtimeSuspended,
                    notifications_created = :notifications,
                    finished_at = :finishedAt
                WHERE id = :runId
                """)
                .param("runId", runId)
                .param("billingPastDue", billingPastDue)
                .param("featurePastDue", featurePastDue)
                .param("featureCanceled", featureCanceled)
                .param("runtimeSuspended", runtimeSuspended)
                .param("notifications", notifications + featurePastDue + featureCanceled + runtimeSuspended)
                .param("finishedAt", Timestamp.from(Instant.now()))
                .update();
    }

    private void updateRunFailure(Long runId, String errorMessage) {
        jdbcClient.sql("""
                UPDATE automation_runs
                SET status = 'FAILED',
                    error_message = :errorMessage,
                    finished_at = :finishedAt
                WHERE id = :runId
                """)
                .param("runId", runId)
                .param("errorMessage", errorMessage == null ? "Unknown automation error" : errorMessage.substring(0, Math.min(500, errorMessage.length())))
                .param("finishedAt", Timestamp.from(Instant.now()))
                .update();
    }

    private AutomationRunResponse recentRun(Long runId) {
        return jdbcClient.sql("SELECT * FROM automation_runs WHERE id = :runId")
                .param("runId", runId)
                .query(this::mapRun)
                .single();
    }

    private List<AutomationRunResponse> recentRuns() {
        return jdbcClient.sql("""
                SELECT *
                FROM automation_runs
                ORDER BY started_at DESC
                LIMIT 10
                """)
                .query(this::mapRun)
                .list();
    }

    private List<AutomationSettingResponse> listSettings() {
        return jdbcClient.sql("""
                SELECT setting_key, setting_value, description
                FROM automation_settings
                ORDER BY setting_key ASC
                """)
                .query((rs, rowNum) -> new AutomationSettingResponse(
                        rs.getString("setting_key"),
                        rs.getString("setting_value"),
                        rs.getString("description")
                ))
                .list();
    }

    private AutomationRunResponse mapRun(ResultSet rs, int rowNum) throws SQLException {
        Timestamp finishedAt = rs.getTimestamp("finished_at");
        return new AutomationRunResponse(
                rs.getLong("id"),
                rs.getString("run_type"),
                rs.getString("status"),
                rs.getInt("billing_marked_past_due"),
                rs.getInt("feature_marked_past_due"),
                rs.getInt("feature_canceled"),
                rs.getInt("runtime_suspended"),
                rs.getInt("notifications_created"),
                rs.getString("error_message"),
                rs.getTimestamp("started_at").toInstant(),
                finishedAt == null ? null : finishedAt.toInstant()
        );
    }

    private int countByStatus(String table, String status) {
        return jdbcClient.sql("SELECT COUNT(*) FROM " + table + " WHERE status = :status")
                .param("status", status)
                .query(Integer.class)
                .single();
    }

    private AutomationPolicy loadPolicy() {
        Map<String, String> settings = jdbcClient.sql("SELECT setting_key, setting_value FROM automation_settings")
                .query((rs, rowNum) -> Map.entry(rs.getString("setting_key"), rs.getString("setting_value")))
                .list()
                .stream()
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return new AutomationPolicy(
                Boolean.parseBoolean(settings.getOrDefault("automation.enabled", "true")),
                parsePositiveInt(settings.get("automation.reminder_days_before"), 3),
                parsePositiveInt(settings.get("automation.past_due_grace_days"), 1),
                parsePositiveInt(settings.get("automation.cancel_grace_days"), 7),
                Boolean.parseBoolean(settings.getOrDefault("automation.runtime_suspend_enabled", "false"))
        );
    }

    private String normalizeSettingValue(String key, String value) {
        if (value == null) {
            return "false";
        }

        if (key.endsWith("_enabled") || key.equals("automation.enabled")) {
            return String.valueOf(Boolean.parseBoolean(value));
        }

        return String.valueOf(parsePositiveInt(value, 0));
    }

    private int parsePositiveInt(String value, int fallback) {
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private record AutomationPolicy(
            boolean enabled,
            int reminderDaysBefore,
            int pastDueGraceDays,
            int cancelGraceDays,
            boolean runtimeSuspendEnabled
    ) {
    }

    private record BillingReminderTarget(Long userId, LocalDate currentPeriodEnd, Integer monthlyTotalCents) {
    }

    private record FeatureSubscriptionTarget(Long subscriptionId, Long botId, Long ownerUserId, String botName, String featureName) {
    }

    private record RuntimeSuspensionTarget(Long botId, Long ownerUserId, String botName, String pm2ProcessName) {
    }
}
