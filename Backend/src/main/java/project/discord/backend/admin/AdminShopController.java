package project.discord.backend.admin;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import project.discord.backend.admin.dto.AdminShopFeatureRequest;
import project.discord.backend.customer.dto.FeatureResponse;
import project.discord.backend.user.domain.UserAccount;
import project.discord.backend.user.domain.UserRole;

@RestController
@RequestMapping("/api/admin/shop")
public class AdminShopController {

    private final AdminShopService adminShopService;

    public AdminShopController(AdminShopService adminShopService) {
        this.adminShopService = adminShopService;
    }

    @GetMapping("/features")
    public List<FeatureResponse> features(@AuthenticationPrincipal UserAccount user) {
        requireAdmin(user);
        return adminShopService.listFeatures();
    }

    @PutMapping("/features/{featureId}")
    public FeatureResponse updateFeature(
            @AuthenticationPrincipal UserAccount user,
            @PathVariable Long featureId,
            @RequestBody AdminShopFeatureRequest request
    ) {
        requireAdmin(user);
        return adminShopService.updateFeature(featureId, request);
    }

    private void requireAdmin(UserAccount user) {
        if (user == null || user.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Shop management is admin-only");
        }
    }
}
