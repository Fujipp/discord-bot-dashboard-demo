package project.discord.backend.customer;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import project.discord.backend.customer.dto.CustomerDashboardResponse;
import project.discord.backend.user.domain.UserAccount;

@RestController
@RequestMapping("/api/customer")
public class CustomerDashboardController {

    private final CustomerDashboardService customerDashboardService;

    public CustomerDashboardController(CustomerDashboardService customerDashboardService) {
        this.customerDashboardService = customerDashboardService;
    }

    @GetMapping("/dashboard")
    public CustomerDashboardResponse dashboard(@AuthenticationPrincipal UserAccount user) {
        return customerDashboardService.getDashboard(user);
    }
}
