package project.discord.backend.automation;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import project.discord.backend.automation.dto.AutomationDashboardResponse;
import project.discord.backend.automation.dto.AutomationRunResponse;
import project.discord.backend.automation.dto.AutomationSettingUpdateRequest;
import project.discord.backend.user.domain.UserAccount;
import project.discord.backend.user.domain.UserRole;

@RestController
@RequestMapping("/api/admin/automation")
public class AutomationAdminController {

    private final AutomationService automationService;

    public AutomationAdminController(AutomationService automationService) {
        this.automationService = automationService;
    }

    @GetMapping
    public AutomationDashboardResponse dashboard(@AuthenticationPrincipal UserAccount user) {
        requireAdmin(user);
        return automationService.getDashboard();
    }

    @PutMapping("/settings")
    public AutomationDashboardResponse updateSettings(
            @AuthenticationPrincipal UserAccount user,
            @RequestBody AutomationSettingUpdateRequest request
    ) {
        requireAdmin(user);
        return automationService.updateSettings(request);
    }

    @PostMapping("/run")
    public AutomationRunResponse runNow(@AuthenticationPrincipal UserAccount user) {
        requireAdmin(user);
        return automationService.runManual();
    }

    private void requireAdmin(UserAccount user) {
        if (user == null || user.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Automation control is admin-only");
        }
    }
}
