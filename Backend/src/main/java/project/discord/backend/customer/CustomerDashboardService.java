package project.discord.backend.customer;

import java.util.Collections;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import project.discord.backend.customer.domain.BotStatus;
import project.discord.backend.customer.domain.BotFeatureSubscription;
import project.discord.backend.customer.domain.FeatureCatalogItem;
import project.discord.backend.customer.domain.SubscriptionStatus;
import project.discord.backend.customer.dto.BillingSummaryResponse;
import project.discord.backend.customer.dto.BotConfigResponse;
import project.discord.backend.customer.dto.BotFeatureResponse;
import project.discord.backend.customer.dto.BotResponse;
import project.discord.backend.customer.dto.CustomerDashboardResponse;
import project.discord.backend.customer.dto.DashboardSummaryResponse;
import project.discord.backend.customer.dto.FeatureResponse;
import project.discord.backend.customer.repository.BillingSubscriptionRepository;
import project.discord.backend.customer.repository.BotFeatureSubscriptionRepository;
import project.discord.backend.customer.repository.DiscordBotRepository;
import project.discord.backend.customer.repository.FeatureCatalogRepository;
import project.discord.backend.user.domain.UserAccount;

@Service
public class CustomerDashboardService {

    private final DiscordBotRepository discordBotRepository;
    private final FeatureCatalogRepository featureCatalogRepository;
    private final BotFeatureSubscriptionRepository botFeatureSubscriptionRepository;
    private final BillingSubscriptionRepository billingSubscriptionRepository;
    private final JdbcClient jdbcClient;

    public CustomerDashboardService(
            DiscordBotRepository discordBotRepository,
            FeatureCatalogRepository featureCatalogRepository,
            BotFeatureSubscriptionRepository botFeatureSubscriptionRepository,
            BillingSubscriptionRepository billingSubscriptionRepository,
            JdbcClient jdbcClient
    ) {
        this.discordBotRepository = discordBotRepository;
        this.featureCatalogRepository = featureCatalogRepository;
        this.botFeatureSubscriptionRepository = botFeatureSubscriptionRepository;
        this.billingSubscriptionRepository = billingSubscriptionRepository;
        this.jdbcClient = jdbcClient;
    }

    public CustomerDashboardResponse getDashboard(UserAccount user) {
        List<FeatureCatalogItem> features = featureCatalogRepository.findByActiveTrueOrderBySortOrderAscMonthlyPriceCentsAsc();
        Map<Long, FeatureCatalogItem> featuresById = features.stream()
                .collect(Collectors.toMap(FeatureCatalogItem::getId, Function.identity()));

        List<BotFeatureSubscription> subscriptions = botFeatureSubscriptionRepository.findByOwnerUserId(user.getId());
        Map<Long, List<BotFeatureSubscription>> subscriptionsByBotId = subscriptions.stream()
                .collect(Collectors.groupingBy(BotFeatureSubscription::getBotId));

        List<project.discord.backend.customer.domain.DiscordBot> customerBots = discordBotRepository.findByOwnerUserIdOrderByCreatedAtDesc(user.getId());
        Map<Long, List<BotConfigResponse>> configByBotId = loadConfigEntries(
                customerBots.stream().map(project.discord.backend.customer.domain.DiscordBot::getId).toList()
        );

        List<BotResponse> bots = customerBots.stream()
                .map(bot -> BotResponse.from(
                        bot,
                        subscriptionsByBotId.getOrDefault(bot.getId(), Collections.emptyList()).stream()
                                .map(subscription -> toBotFeature(subscription, featuresById))
                                .filter(Objects::nonNull)
                                .toList(),
                        configByBotId.getOrDefault(bot.getId(), Collections.emptyList())
                ))
                .toList();

        DashboardSummaryResponse summary = new DashboardSummaryResponse(
                bots.size(),
                (int) bots.stream().filter(bot -> bot.status() == BotStatus.ONLINE).count(),
                bots.stream().mapToInt(BotResponse::serverCount).sum(),
                bots.stream().mapToInt(BotResponse::commandCount).sum(),
                (int) subscriptions.stream()
                        .filter(subscription -> subscription.getStatus() == SubscriptionStatus.ACTIVE)
                        .map(BotFeatureSubscription::getFeatureId)
                        .distinct()
                        .count()
        );

        BillingSummaryResponse billing = billingSubscriptionRepository
                .findFirstByUserIdAndStatusOrderByCurrentPeriodEndDesc(user.getId(), SubscriptionStatus.ACTIVE)
                .map(BillingSummaryResponse::from)
                .orElseGet(() -> BillingSummaryResponse.fromBotMonthlyTotal(
                        bots.stream().mapToInt(bot -> bot.monthlyPriceCents() == null ? 0 : bot.monthlyPriceCents()).sum()
                ));

        return new CustomerDashboardResponse(
                summary,
                billing,
                bots,
                features.stream().map(FeatureResponse::from).toList()
        );
    }

    private BotFeatureResponse toBotFeature(
            BotFeatureSubscription subscription,
            Map<Long, FeatureCatalogItem> featuresById
    ) {
        FeatureCatalogItem feature = featuresById.get(subscription.getFeatureId());
        if (feature == null) {
            return null;
        }

        return BotFeatureResponse.from(subscription, feature);
    }

    private Map<Long, List<BotConfigResponse>> loadConfigEntries(List<Long> botIds) {
        if (botIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return jdbcClient.sql("""
                SELECT bot_id, config_key, config_value, is_secret, scope, updated_at
                FROM bot_config_entries
                WHERE bot_id IN (:botIds)
                ORDER BY config_key ASC
                """)
                .param("botIds", botIds)
                .query(this::mapConfigEntry)
                .list()
                .stream()
                .collect(Collectors.groupingBy(ConfigEntryRow::botId, Collectors.mapping(ConfigEntryRow::response, Collectors.toList())));
    }

    private ConfigEntryRow mapConfigEntry(ResultSet rs, int rowNum) throws SQLException {
        boolean secret = rs.getBoolean("is_secret");
        return new ConfigEntryRow(
                rs.getLong("bot_id"),
                new BotConfigResponse(
                        rs.getString("config_key"),
                        secret ? "••••••••" : rs.getString("config_value"),
                        secret,
                        rs.getString("scope"),
                        rs.getTimestamp("updated_at").toInstant()
                )
        );
    }

    private record ConfigEntryRow(Long botId, BotConfigResponse response) {
    }
}
