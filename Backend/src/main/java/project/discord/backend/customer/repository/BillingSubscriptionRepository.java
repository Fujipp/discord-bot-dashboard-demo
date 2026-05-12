package project.discord.backend.customer.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import project.discord.backend.customer.domain.BillingSubscription;
import project.discord.backend.customer.domain.SubscriptionStatus;

public interface BillingSubscriptionRepository extends CrudRepository<BillingSubscription, Long> {

    Optional<BillingSubscription> findFirstByUserIdAndStatusOrderByCurrentPeriodEndDesc(
            Long userId,
            SubscriptionStatus status
    );
}
