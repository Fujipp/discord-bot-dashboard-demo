package project.discord.backend.admin;

import java.time.Instant;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import project.discord.backend.admin.dto.AdminProcessAssignmentRequest;
import project.discord.backend.admin.dto.AdminRuntimeBotResponse;
import project.discord.backend.admin.dto.AdminRuntimeDashboardResponse;
import project.discord.backend.admin.dto.AdminRuntimeProcessResponse;
import project.discord.backend.admin.dto.AdminUserOptionResponse;
import project.discord.backend.customer.domain.BotBillingMode;
import project.discord.backend.customer.domain.BotStatus;
import project.discord.backend.customer.domain.DiscordBot;
import project.discord.backend.customer.repository.DiscordBotRepository;
import project.discord.backend.runtime.Pm2RuntimeService;
import project.discord.backend.runtime.dto.RuntimeProcessResponse;
import project.discord.backend.user.domain.UserAccount;
import project.discord.backend.user.repository.UserRepository;

@Service
public class AdminRuntimeService {

    private static final Pattern SAFE_PROCESS_NAME = Pattern.compile("^[a-zA-Z0-9._:-]{1,80}$");

    private final Pm2RuntimeService pm2RuntimeService;
    private final DiscordBotRepository discordBotRepository;
    private final UserRepository userRepository;
    private final JdbcClient jdbcClient;

    public AdminRuntimeService(
            Pm2RuntimeService pm2RuntimeService,
            DiscordBotRepository discordBotRepository,
            UserRepository userRepository,
            JdbcClient jdbcClient
    ) {
        this.pm2RuntimeService = pm2RuntimeService;
        this.discordBotRepository = discordBotRepository;
        this.userRepository = userRepository;
        this.jdbcClient = jdbcClient;
    }

    public AdminRuntimeDashboardResponse getDashboard(String hostId, String userSearch) {
        String selectedHostId = normalizeHostId(hostId);
        List<UserAccount> userOptions = searchUsers(userSearch);
        Map<Long, UserAccount> usersById = userOptions.stream()
                .collect(Collectors.toMap(UserAccount::getId, Function.identity()));

        return new AdminRuntimeDashboardResponse(
                selectedHostId,
                pm2RuntimeService.listHosts(),
                pm2RuntimeService.getHostMetrics(selectedHostId),
                pm2RuntimeService.listProcesses(selectedHostId).stream()
                        .map(process -> toRuntimeProcess(process, usersById))
                        .toList(),
                userOptions.stream()
                        .map(AdminUserOptionResponse::from)
                        .toList()
        );
    }

    public AdminRuntimeProcessResponse assignProcess(String hostId, String processName, AdminProcessAssignmentRequest request) {
        String selectedHostId = normalizeHostId(hostId);
        validateProcessName(processName);

        UserAccount owner = userRepository.findById(request.ownerUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Owner user was not found"));

        RuntimeProcessResponse runtimeProcess = pm2RuntimeService.listProcesses(selectedHostId).stream()
                .filter(process -> process.name().equals(processName))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "pm2 process was not found"));

        DiscordBot bot = discordBotRepository.findByPm2ProcessName(processName).orElseGet(DiscordBot::new);
        Instant now = Instant.now();

        if (bot.getId() == null) {
            bot.setCreatedAt(now);
            bot.setDiscordApplicationId("pm2:" + processName);
            bot.setPm2ProcessName(processName);
            bot.setServerCount(0);
            bot.setCommandCount(0);
        }

        bot.setOwnerUserId(owner.getId());
        bot.setName(normalizeBotName(request.botName(), processName));
        bot.setStatus(toBotStatus(runtimeProcess.status()));
        bot.setBillingMode(request.billingMode() == null ? BotBillingMode.FREE : request.billingMode());
        bot.setMonthlyPriceCents(normalizePrice(bot.getBillingMode(), request.monthlyPriceCents()));
        bot.setUpdatedAt(now);

        DiscordBot savedBot = discordBotRepository.save(bot);
        updateRuntimeExpiry(savedBot.getId(), request.runtimeCurrentPeriodEnd());
        return new AdminRuntimeProcessResponse(runtimeProcess, AdminRuntimeBotResponse.from(savedBot, owner, runtimeExpiry(savedBot.getId())));
    }

    public RuntimeProcessResponse runAction(String hostId, String processName, String action) {
        String selectedHostId = normalizeHostId(hostId);
        return pm2RuntimeService.runAction(selectedHostId, processName, action).processes().stream()
                .filter(process -> process.name().equals(processName))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "pm2 process was not found"));
    }

    private AdminRuntimeProcessResponse toRuntimeProcess(
            RuntimeProcessResponse runtimeProcess,
            Map<Long, UserAccount> usersById
    ) {
        AdminRuntimeBotResponse bot = discordBotRepository.findByPm2ProcessName(runtimeProcess.name())
                .map(assignedBot -> AdminRuntimeBotResponse.from(
                        assignedBot,
                        usersById.getOrDefault(
                                assignedBot.getOwnerUserId(),
                                userRepository.findById(assignedBot.getOwnerUserId()).orElse(null)
                        ),
                        runtimeExpiry(assignedBot.getId())
                ))
                .orElse(null);

        return new AdminRuntimeProcessResponse(runtimeProcess, bot);
    }

    private LocalDate runtimeExpiry(Long botId) {
        return jdbcClient.sql("""
                SELECT bfs.current_period_end
                FROM bot_feature_subscriptions bfs
                INNER JOIN feature_catalog fc ON fc.id = bfs.feature_id
                WHERE bfs.bot_id = :botId
                  AND fc.code = 'runtime-247'
                ORDER BY bfs.current_period_end DESC
                LIMIT 1
                """)
                .param("botId", botId)
                .query(LocalDate.class)
                .optional()
                .orElse(null);
    }

    private void updateRuntimeExpiry(Long botId, LocalDate runtimeCurrentPeriodEnd) {
        if (runtimeCurrentPeriodEnd == null) {
            return;
        }

        Long featureId = jdbcClient.sql("SELECT id FROM feature_catalog WHERE code = 'runtime-247'")
                .query(Long.class)
                .optional()
                .orElse(null);
        if (featureId == null) {
            return;
        }

        LocalDate startDate = LocalDate.now();
        jdbcClient.sql("""
                INSERT INTO bot_feature_subscriptions (
                  bot_id, feature_id, status, current_period_start, current_period_end, auto_renew
                )
                VALUES (:botId, :featureId, 'ACTIVE', :startDate, :endDate, TRUE)
                ON DUPLICATE KEY UPDATE
                  status = 'ACTIVE',
                  current_period_end = VALUES(current_period_end),
                  auto_renew = TRUE
                """)
                .param("botId", botId)
                .param("featureId", featureId)
                .param("startDate", Date.valueOf(startDate))
                .param("endDate", Date.valueOf(runtimeCurrentPeriodEnd))
                .update();
    }

    private BotStatus toBotStatus(String runtimeStatus) {
        if ("online".equalsIgnoreCase(runtimeStatus)) {
            return BotStatus.ONLINE;
        }

        if ("stopping".equalsIgnoreCase(runtimeStatus) || "launching".equalsIgnoreCase(runtimeStatus)) {
            return BotStatus.MAINTENANCE;
        }

        return BotStatus.OFFLINE;
    }

    private String normalizeBotName(String botName, String processName) {
        if (botName == null || botName.trim().isEmpty()) {
            return processName;
        }

        String trimmedName = botName.trim();
        if (trimmedName.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bot name must be 100 characters or fewer");
        }

        return trimmedName;
    }

    private Integer normalizePrice(BotBillingMode billingMode, Integer monthlyPriceCents) {
        if (billingMode == BotBillingMode.FREE) {
            return 0;
        }

        if (monthlyPriceCents == null || monthlyPriceCents < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Monthly price must be zero or greater");
        }

        return monthlyPriceCents;
    }

    private void validateProcessName(String processName) {
        if (processName == null || !SAFE_PROCESS_NAME.matcher(processName).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pm2 process name");
        }
    }

    private String normalizeHostId(String hostId) {
        if (hostId == null || hostId.isBlank()) {
            return pm2RuntimeService.primaryHostId();
        }

        return hostId.trim();
    }

    private List<UserAccount> searchUsers(String userSearch) {
        String query = userSearch == null ? "" : userSearch.trim();
        if (query.length() < 2) {
            return userRepository.findAllByOrderByCreatedAtDesc().stream().limit(25).toList();
        }

        return userRepository.searchByEmailOrUsername(query, 25);
    }
}
