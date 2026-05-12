package project.discord.backend.customer.repository;

import java.util.List;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import project.discord.backend.customer.domain.BotFeatureSubscription;

public interface BotFeatureSubscriptionRepository extends CrudRepository<BotFeatureSubscription, Long> {

    @Query("""
            SELECT bfs.*
            FROM bot_feature_subscriptions bfs
            INNER JOIN discord_bots db ON db.id = bfs.bot_id
            WHERE db.owner_user_id = :ownerUserId
            ORDER BY bfs.current_period_end ASC
            """)
    List<BotFeatureSubscription> findByOwnerUserId(Long ownerUserId);
}
