package project.discord.backend.payment;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import project.discord.backend.payment.dto.CheckoutRequest;
import project.discord.backend.payment.dto.CheckoutResponse;
import project.discord.backend.user.domain.UserAccount;

@RestController
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/api/customer/checkout")
    public CheckoutResponse createCheckout(
            @AuthenticationPrincipal UserAccount user,
            @RequestBody CheckoutRequest request
    ) {
        return paymentService.createCheckout(user, request);
    }

    @GetMapping("/api/customer/payments/{paymentId}")
    public CheckoutResponse getCheckout(
            @AuthenticationPrincipal UserAccount user,
            @PathVariable Long paymentId
    ) {
        return paymentService.getCheckout(user, paymentId);
    }

    @PostMapping("/api/webhooks/omise")
    public ResponseEntity<Void> omiseWebhook(@RequestBody Map<String, Object> payload) {
        paymentService.handleOmiseWebhook(payload);
        return ResponseEntity.ok().build();
    }
}
