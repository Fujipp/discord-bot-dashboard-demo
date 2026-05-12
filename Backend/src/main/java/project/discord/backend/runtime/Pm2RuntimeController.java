package project.discord.backend.runtime;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import project.discord.backend.runtime.dto.RuntimeCommandResponse;
import project.discord.backend.runtime.dto.RuntimeProcessResponse;
import project.discord.backend.user.domain.UserAccount;
import project.discord.backend.user.domain.UserRole;

@RestController
@RequestMapping("/api/customer/runtime")
public class Pm2RuntimeController {

    private final Pm2RuntimeService pm2RuntimeService;

    public Pm2RuntimeController(Pm2RuntimeService pm2RuntimeService) {
        this.pm2RuntimeService = pm2RuntimeService;
    }

    @GetMapping("/processes")
    public List<RuntimeProcessResponse> processes(@AuthenticationPrincipal UserAccount user) {
        requireAdmin(user);
        return pm2RuntimeService.listProcesses();
    }

    @PostMapping("/processes/{processName}/{action}")
    public RuntimeCommandResponse runAction(
            @AuthenticationPrincipal UserAccount user,
            @PathVariable String processName,
            @PathVariable String action
    ) {
        requireAdmin(user);
        return pm2RuntimeService.runAction(processName, action);
    }

    private void requireAdmin(UserAccount user) {
        if (user == null || user.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Runtime control is admin-only");
        }
    }
}
