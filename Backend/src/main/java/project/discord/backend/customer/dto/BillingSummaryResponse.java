package project.discord.backend.customer.dto;

import java.time.LocalDate;

import project.discord.backend.customer.domain.BillingSubscription;
import project.discord.backend.customer.domain.SubscriptionStatus;

public record BillingSummaryResponse(
        SubscriptionStatus status,
        Integer monthlyTotalCents,
        String currency,
        LocalDate currentPeriodEnd,
        Boolean cancelAtPeriodEnd
) {

    public static BillingSummaryResponse empty() {
        return new BillingSummaryResponse(
                SubscriptionStatus.CANCELED,
                0,
                "THB",
                null,
                false
        );
    }

    public static BillingSummaryResponse fromBotMonthlyTotal(Integer monthlyTotalCents) {
        if (monthlyTotalCents == null || monthlyTotalCents <= 0) {
            return empty();
        }

        return new BillingSummaryResponse(
                SubscriptionStatus.ACTIVE,
                monthlyTotalCents,
                "THB",
                null,
                false
        );
    }

    public static BillingSummaryResponse from(BillingSubscription subscription) {
        return new BillingSummaryResponse(
                subscription.getStatus(),
                subscription.getMonthlyTotalCents(),
                subscription.getCurrency(),
                subscription.getCurrentPeriodEnd(),
                subscription.getCancelAtPeriodEnd()
        );
    }
}
