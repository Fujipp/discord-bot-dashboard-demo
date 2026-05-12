package project.discord.backend.customer.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import project.discord.backend.customer.domain.DiscordBot;

public interface DiscordBotRepository extends CrudRepository<DiscordBot, Long> {

    List<DiscordBot> findByOwnerUserIdOrderByCreatedAtDesc(Long ownerUserId);

    Optional<DiscordBot> findByPm2ProcessName(String pm2ProcessName);
}
