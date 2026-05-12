package project.discord.backend.customer.domain;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("discord_bots")
public class DiscordBot {

    @Id
    private Long id;

    @Column("owner_user_id")
    private Long ownerUserId;

    @Column("discord_application_id")
    private String discordApplicationId;

    @Column("pm2_process_name")
    private String pm2ProcessName;

    private String name;

    @Column("avatar_url")
    private String avatarUrl;

    private BotStatus status = BotStatus.OFFLINE;

    @Column("billing_mode")
    private BotBillingMode billingMode = BotBillingMode.FREE;

    @Column("monthly_price_cents")
    private Integer monthlyPriceCents = 0;

    @Column("server_count")
    private Integer serverCount = 0;

    @Column("command_count")
    private Integer commandCount = 0;

    @Column("uptime_percent")
    private BigDecimal uptimePercent = BigDecimal.ZERO;

    @Column("hosted_region")
    private String hostedRegion = "sgp1";

    @Column("last_heartbeat_at")
    private Instant lastHeartbeatAt;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getDiscordApplicationId() {
        return discordApplicationId;
    }

    public void setDiscordApplicationId(String discordApplicationId) {
        this.discordApplicationId = discordApplicationId;
    }

    public String getPm2ProcessName() {
        return pm2ProcessName;
    }

    public void setPm2ProcessName(String pm2ProcessName) {
        this.pm2ProcessName = pm2ProcessName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public BotStatus getStatus() {
        return status;
    }

    public void setStatus(BotStatus status) {
        this.status = status;
    }

    public BotBillingMode getBillingMode() {
        return billingMode;
    }

    public void setBillingMode(BotBillingMode billingMode) {
        this.billingMode = billingMode;
    }

    public Integer getMonthlyPriceCents() {
        return monthlyPriceCents;
    }

    public void setMonthlyPriceCents(Integer monthlyPriceCents) {
        this.monthlyPriceCents = monthlyPriceCents;
    }

    public Integer getServerCount() {
        return serverCount;
    }

    public void setServerCount(Integer serverCount) {
        this.serverCount = serverCount;
    }

    public Integer getCommandCount() {
        return commandCount;
    }

    public void setCommandCount(Integer commandCount) {
        this.commandCount = commandCount;
    }

    public BigDecimal getUptimePercent() {
        return uptimePercent;
    }

    public void setUptimePercent(BigDecimal uptimePercent) {
        this.uptimePercent = uptimePercent;
    }

    public String getHostedRegion() {
        return hostedRegion;
    }

    public void setHostedRegion(String hostedRegion) {
        this.hostedRegion = hostedRegion;
    }

    public Instant getLastHeartbeatAt() {
        return lastHeartbeatAt;
    }

    public void setLastHeartbeatAt(Instant lastHeartbeatAt) {
        this.lastHeartbeatAt = lastHeartbeatAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
