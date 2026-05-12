package project.discord.backend.admin;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import project.discord.backend.admin.dto.AdminProcessAssignmentRequest;
import project.discord.backend.admin.dto.AdminRuntimeDashboardResponse;
import project.discord.backend.admin.dto.AdminRuntimeProcessResponse;
import project.discord.backend.runtime.dto.RuntimeProcessResponse;
import project.discord.backend.user.domain.UserAccount;
import project.discord.backend.user.domain.UserRole;

@RestController
@RequestMapping("/api/admin/runtime")
public class AdminRuntimeController {

    private final AdminRuntimeService adminRuntimeService;

    public AdminRuntimeController(AdminRuntimeService adminRuntimeService) {
        this.adminRuntimeService = adminRuntimeService;
    }

    @GetMapping("/processes")
    public AdminRuntimeDashboardResponse processes(
            @AuthenticationPrincipal UserAccount user,
            @RequestParam(required = false) String hostId,
            @RequestParam(required = false) String userSearch
    ) {
        requireAdmin(user);
        return adminRuntimeService.getDashboard(hostId, userSearch);
    }

    @PostMapping("/processes/{processName}/assignment")
    public AdminRuntimeProcessResponse assignProcess(
            @AuthenticationPrincipal UserAccount user,
            @RequestParam(required = false) String hostId,
            @PathVariable String processName,
            @RequestBody AdminProcessAssignmentRequest request
    ) {
        requireAdmin(user);
        return adminRuntimeService.assignProcess(hostId, processName, request);
    }

    @PostMapping("/processes/{processName}/{action}")
    public RuntimeProcessResponse runAction(
            @AuthenticationPrincipal UserAccount user,
            @RequestParam(required = false) String hostId,
            @PathVariable String processName,
            @PathVariable String action
    ) {
        requireAdmin(user);
        return adminRuntimeService.runAction(hostId, processName, action);
    }

    private void requireAdmin(UserAccount user) {
        if (user == null || user.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin runtime control is admin-only");
        }
    }
}
