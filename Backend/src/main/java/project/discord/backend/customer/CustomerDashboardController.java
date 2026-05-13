package project.discord.backend.customer;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import project.discord.backend.customer.dto.BotConfigUpdateRequest;
import project.discord.backend.customer.dto.CustomerDashboardResponse;
import project.discord.backend.user.domain.UserAccount;

@RestController
@RequestMapping("/api/customer")
public class CustomerDashboardController {

    private final CustomerDashboardService customerDashboardService;
    private final BotConfigService botConfigService;

    public CustomerDashboardController(CustomerDashboardService customerDashboardService, BotConfigService botConfigService) {
        this.customerDashboardService = customerDashboardService;
        this.botConfigService = botConfigService;
    }

    @GetMapping("/dashboard")
    public CustomerDashboardResponse dashboard(@AuthenticationPrincipal UserAccount user) {
        return customerDashboardService.getDashboard(user);
    }

    @PutMapping("/bots/{botId}/config")
    public CustomerDashboardResponse updateBotConfig(
            @AuthenticationPrincipal UserAccount user,
            @PathVariable Long botId,
            @RequestBody BotConfigUpdateRequest request
    ) {
        botConfigService.updateBotConfig(user, botId, request);
        return customerDashboardService.getDashboard(user);
    }
}
