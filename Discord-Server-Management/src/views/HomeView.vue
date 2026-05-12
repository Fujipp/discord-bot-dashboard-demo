<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import {
  Activity,
  Bot,
  CheckCircle2,
  CircleAlert,
  CreditCard,
  MessageSquareText,
  PackagePlus,
  PlugZap,
  Plus,
  Radio,
  RefreshCw,
  Server,
  Settings,
  ShieldCheck,
  ShoppingBag,
  Sparkles,
} from 'lucide-vue-next';
import { useAuthStore } from '@/stores/authStore';
import {
  getCustomerDashboard,
  type BotResponse,
  type BotStatus,
  type CustomerDashboardResponse,
  type FeatureResponse,
} from '@/services/customer';

const authStore = useAuthStore();
const dashboard = ref<CustomerDashboardResponse | null>(null);
const isLoading = ref(true);
const errorMessage = ref('');

const statusText: Record<BotStatus, string> = {
  ONLINE: 'Online',
  OFFLINE: 'Offline',
  MAINTENANCE: 'Maintenance',
};

const statusIcon = {
  ONLINE: CheckCircle2,
  OFFLINE: CircleAlert,
  MAINTENANCE: CircleAlert,
};

const summaryStats = computed(() => [
  {
    label: 'My bots',
    value: dashboard.value?.summary.botCount ?? 0,
    detail: `${dashboard.value?.summary.onlineBotCount ?? 0} online`,
    icon: Bot,
  },
  {
    label: 'Servers',
    value: dashboard.value?.summary.connectedServerCount ?? 0,
    detail: 'Connected Discord servers',
    icon: Server,
  },
  {
    label: 'Commands',
    value: dashboard.value?.summary.commandCount ?? 0,
    detail: 'Enabled bot commands',
    icon: MessageSquareText,
  },
  {
    label: 'Active features',
    value: dashboard.value?.summary.activeFeatureCount ?? 0,
    detail: 'Paid modules in use',
    icon: Sparkles,
  },
]);

const bots = computed(() => dashboard.value?.bots ?? []);
const availableFeatures = computed(() => dashboard.value?.availableFeatures ?? []);
const featuredPlans = computed(() => availableFeatures.value.slice(0, 3));

const monthlyTotal = computed(() =>
  formatMoney(
    dashboard.value?.billing.monthlyTotalCents ?? 0,
    dashboard.value?.billing.currency ?? 'THB',
  ),
);

async function loadDashboard() {
  authStore.loadSession();

  if (!authStore.accessToken) {
    isLoading.value = false;
    return;
  }

  isLoading.value = true;
  errorMessage.value = '';

  try {
    dashboard.value = await getCustomerDashboard(authStore.accessToken);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Could not load dashboard';
  } finally {
    isLoading.value = false;
  }
}

function formatMoney(cents: number, currency: string) {
  return new Intl.NumberFormat('th-TH', {
    style: 'currency',
    currency,
    maximumFractionDigits: 0,
  }).format(cents / 100);
}

function formatDate(value: string | null) {
  if (!value) return 'No active billing cycle';

  return new Intl.DateTimeFormat('th-TH', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  }).format(new Date(value));
}

function featurePrice(feature: FeatureResponse) {
  return `${formatMoney(feature.monthlyPriceCents, feature.currency)}/mo`;
}

function botSubtitle(botInfo: BotResponse) {
  return `${botInfo.serverCount} servers · ${botInfo.commandCount} commands · ${botInfo.hostedRegion}`;
}

onMounted(loadDashboard);
</script>

<template>
  <main class="dashboard-page">
    <section class="hero-section">
      <div class="hero-copy">
        <p class="eyebrow">Customer dashboard</p>
        <h1>Discord Bot Portal</h1>
        <p>
          จัดการบอท Discord ของลูกค้า เช็คสถานะ 24/7 เลือกซื้อ feature รายเดือน และดูรอบบิลได้จากที่เดียว
        </p>
      </div>

      <div class="hero-actions">
        <button type="button" class="primary-action">
          <Plus class="h-4 w-4" />
          Connect bot
        </button>
        <button type="button" class="secondary-action">
          <ShoppingBag class="h-4 w-4" />
          Browse features
        </button>
      </div>
    </section>

    <section v-if="errorMessage" class="notice-panel error-panel">
      <CircleAlert class="h-5 w-5" />
      <div>
        <strong>โหลด Dashboard ไม่สำเร็จ</strong>
        <span>{{ errorMessage }}</span>
      </div>
      <button type="button" class="icon-action" aria-label="Retry dashboard" @click="loadDashboard">
        <RefreshCw class="h-4 w-4" />
      </button>
    </section>

    <section class="billing-strip" aria-label="Billing overview">
      <div class="billing-main">
        <div class="billing-icon">
          <CreditCard class="h-5 w-5" />
        </div>
        <div>
          <p class="eyebrow">Monthly subscription</p>
          <h2>{{ monthlyTotal }}</h2>
        </div>
      </div>

      <div class="billing-meta">
        <span>Status: {{ dashboard?.billing.status ?? 'CANCELED' }}</span>
        <span>Next billing: {{ formatDate(dashboard?.billing.currentPeriodEnd ?? null) }}</span>
      </div>
    </section>

    <section class="stats-grid" aria-label="Bot summary">
      <article v-for="stat in summaryStats" :key="stat.label" class="stat-card">
        <div class="stat-icon">
          <component :is="stat.icon" class="h-5 w-5" />
        </div>
        <div>
          <p>{{ stat.label }}</p>
          <strong>{{ stat.value }}</strong>
          <span>{{ stat.detail }}</span>
        </div>
      </article>
    </section>

    <section v-if="isLoading" class="notice-panel">
      <RefreshCw class="h-5 w-5 animate-spin" />
      <div>
        <strong>กำลังโหลดข้อมูลลูกค้า</strong>
        <span>กำลังดึงข้อมูล bot, feature และ subscription จาก backend</span>
      </div>
    </section>

    <section class="content-grid">
      <div class="bots-panel">
        <div class="section-heading">
          <div>
            <p class="eyebrow">Customer bots</p>
            <h2>My bots</h2>
          </div>
          <button type="button" class="icon-action" aria-label="Bot settings">
            <Settings class="h-4 w-4" />
          </button>
        </div>

        <div v-if="!isLoading && bots.length === 0" class="empty-state">
          <PlugZap class="h-8 w-8" />
          <h3>ยังไม่มี Bot ที่เชื่อมไว้</h3>
          <p>ลูกค้าสามารถเชื่อม Discord application ของตัวเอง แล้วเลือก feature รายเดือนเพื่อให้ระบบช่วยรัน 24/7</p>
          <button type="button" class="primary-action">
            <Plus class="h-4 w-4" />
            Connect first bot
          </button>
        </div>

        <div v-else class="bot-list">
          <article v-for="botInfo in bots" :key="botInfo.id" class="bot-card">
            <div class="bot-card-header">
              <div class="bot-identity">
                <div class="bot-avatar">
                  <img
                    v-if="botInfo.avatarUrl"
                    :src="botInfo.avatarUrl"
                    alt=""
                    class="h-full w-full object-cover"
                  />
                  <Bot v-else class="h-5 w-5" />
                </div>
                <div>
                  <h3>{{ botInfo.name }}</h3>
                  <p>{{ botSubtitle(botInfo) }}</p>
                </div>
              </div>

              <span :class="['status-pill', `status-${botInfo.status.toLowerCase()}`]">
                <component :is="statusIcon[botInfo.status]" class="h-3.5 w-3.5" />
                {{ statusText[botInfo.status] }}
              </span>
            </div>

            <div class="feature-list">
              <span v-for="feature in botInfo.activeFeatures" :key="feature.subscriptionId">
                {{ feature.name }}
              </span>
              <span v-if="botInfo.activeFeatures.length === 0">No paid features</span>
            </div>

            <div class="bot-footer">
              <div>
                <span>Uptime</span>
                <strong>{{ botInfo.uptimePercent }}%</strong>
              </div>
              <p>Last heartbeat: {{ formatDate(botInfo.lastHeartbeatAt) }}</p>
              <button type="button" class="manage-button">
                Manage
                <Settings class="h-4 w-4" />
              </button>
            </div>
          </article>
        </div>
      </div>

      <aside class="side-stack">
        <section class="insight-panel" aria-label="Feature marketplace">
          <div class="section-heading">
            <div>
              <p class="eyebrow">Feature store</p>
              <h2>Monthly add-ons</h2>
            </div>
            <PackagePlus class="h-5 w-5" />
          </div>

          <div class="feature-market-list">
            <article v-for="feature in featuredPlans" :key="feature.id" class="market-card">
              <div>
                <strong>{{ feature.name }}</strong>
                <span>{{ feature.category }}</span>
              </div>
              <p>{{ feature.description }}</p>
              <div class="market-footer">
                <span>{{ featurePrice(feature) }}</span>
                <button type="button" class="tiny-button">Buy</button>
              </div>
            </article>
          </div>
        </section>

        <section class="insight-panel" aria-label="System health">
          <div class="section-heading">
            <div>
              <p class="eyebrow">Hosting health</p>
              <h2>24/7 runtime</h2>
            </div>
            <Activity class="h-5 w-5" />
          </div>

          <div class="health-stack">
            <div class="health-row">
              <ShieldCheck class="h-4 w-4" />
              <div>
                <strong>DigitalOcean hosting</strong>
                <span>พร้อมต่อยอดเป็น worker/runtime ต่อ bot</span>
              </div>
            </div>
            <div class="health-row">
              <Radio class="h-4 w-4" />
              <div>
                <strong>Gateway monitor</strong>
                <span>เช็ค heartbeat และสถานะ online/offline</span>
              </div>
            </div>
          </div>
        </section>

      </aside>
    </section>
  </main>
</template>

<style scoped>
.dashboard-page {
  width: min(100%, 1280px);
  margin: 0 auto;
  padding: 2rem 1rem 3rem;
  color: var(--color-text-primary);
}

.hero-section,
.billing-strip,
.stat-card,
.bots-panel,
.insight-panel,
.bot-card,
.notice-panel,
.empty-state,
.market-card,
.runtime-card {
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: color-mix(in srgb, var(--color-surface) 94%, transparent);
  box-shadow: var(--shadow-soft);
}

.hero-section {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 1.5rem;
  padding: 1.5rem;
}

.hero-copy {
  max-width: 720px;
}

.eyebrow {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.72rem;
  font-weight: 850;
  letter-spacing: 0;
  text-transform: uppercase;
}

h1,
h2,
h3,
p {
  margin: 0;
}

h1 {
  margin-top: 0.25rem;
  font-size: clamp(2.25rem, 6vw, 4.1rem);
  font-weight: 900;
  line-height: 0.98;
}

h2 {
  margin-top: 0.2rem;
  font-size: 1.25rem;
  font-weight: 850;
}

h3 {
  font-size: 1rem;
  font-weight: 850;
}

.hero-copy > p:last-child {
  margin-top: 0.75rem;
  color: var(--color-text-secondary);
  font-size: 1rem;
  line-height: 1.65;
}

.hero-actions,
.billing-meta,
.bot-footer,
.market-footer {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.primary-action,
.secondary-action,
.manage-button,
.icon-action,
.tiny-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid transparent;
  border-radius: 8px;
  font-weight: 800;
  transition:
    transform 0.2s ease,
    border-color 0.2s ease,
    background-color 0.2s ease,
    color 0.2s ease;
}

.primary-action,
.secondary-action {
  min-height: 2.65rem;
  gap: 0.45rem;
  padding: 0 1rem;
  white-space: nowrap;
}

.primary-action {
  background: var(--color-primary);
  color: var(--color-surface);
}

.secondary-action,
.icon-action,
.manage-button,
.tiny-button {
  border-color: var(--color-border);
  background: var(--color-surface-muted);
  color: var(--color-text-secondary);
}

.primary-action:hover,
.secondary-action:hover,
.manage-button:hover,
.icon-action:hover,
.tiny-button:hover {
  transform: translateY(-1px);
}

.billing-strip,
.notice-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-top: 1rem;
  padding: 1rem;
}

.billing-main,
.bot-identity,
.health-row,
.notice-panel {
  display: flex;
  align-items: center;
  gap: 0.8rem;
}

.billing-icon,
.stat-icon,
.bot-avatar,
.icon-action {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 8px;
  background: var(--color-surface-muted);
  color: var(--color-secondary);
}

.billing-icon,
.bot-avatar {
  width: 2.8rem;
  height: 2.8rem;
  overflow: hidden;
}

.billing-meta {
  flex-wrap: wrap;
  justify-content: flex-end;
  color: var(--color-text-muted);
  font-size: 0.82rem;
  font-weight: 750;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1rem;
  margin-top: 1rem;
}

.stat-card {
  display: flex;
  min-height: 8.75rem;
  gap: 0.9rem;
  padding: 1rem;
}

.stat-icon,
.icon-action {
  width: 2.35rem;
  height: 2.35rem;
}

.stat-card p,
.bot-identity p,
.bot-footer span,
.health-row span,
.market-card span,
.runtime-heading span,
.runtime-metrics span,
.assignment-summary span,
.notice-panel span,
.empty-state p,
.market-card p {
  color: var(--color-text-muted);
}

.stat-card p,
.bot-footer span {
  font-size: 0.78rem;
  font-weight: 800;
  text-transform: uppercase;
}

.stat-card strong {
  display: block;
  margin-top: 0.25rem;
  font-size: 1.65rem;
  font-weight: 900;
}

.stat-card span,
.market-card p,
.runtime-heading span,
.empty-state p,
.notice-panel span {
  display: block;
  margin-top: 0.25rem;
  font-size: 0.88rem;
  line-height: 1.45;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 22rem;
  gap: 1rem;
  margin-top: 1rem;
}

.bots-panel,
.insight-panel {
  padding: 1rem;
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1rem;
}

.bot-list,
.side-stack,
.feature-market-list,
.health-stack,
.runtime-list {
  display: grid;
  gap: 0.85rem;
}

.bot-card,
.market-card,
.runtime-card,
.empty-state {
  padding: 1rem;
  box-shadow: none;
}

.empty-state {
  display: grid;
  justify-items: start;
  gap: 0.75rem;
  background: var(--color-surface-muted);
}

.empty-state svg {
  color: var(--color-secondary);
}

.bot-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.bot-identity {
  min-width: 0;
}

.bot-identity h3 {
  overflow-wrap: anywhere;
}

.bot-identity p {
  margin-top: 0.2rem;
  font-size: 0.88rem;
}

.status-pill {
  display: inline-flex;
  min-height: 1.8rem;
  flex: 0 0 auto;
  align-items: center;
  gap: 0.35rem;
  border-radius: 999px;
  padding: 0 0.65rem;
  font-size: 0.75rem;
  font-weight: 850;
}

.status-online {
  background: color-mix(in srgb, var(--color-success) 14%, transparent);
  color: var(--color-success);
}

.status-maintenance {
  background: color-mix(in srgb, var(--color-warning) 16%, transparent);
  color: var(--color-warning);
}

.status-offline {
  background: color-mix(in srgb, var(--color-error) 14%, transparent);
  color: var(--color-error);
}

.feature-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-top: 1rem;
}

.feature-list span {
  border: 1px solid var(--color-border);
  border-radius: 999px;
  padding: 0.35rem 0.6rem;
  background: var(--color-surface-muted);
  color: var(--color-text-secondary);
  font-size: 0.8rem;
  font-weight: 750;
}

.bot-footer {
  justify-content: space-between;
  margin-top: 1rem;
  border-top: 1px solid var(--color-divider);
  padding-top: 0.9rem;
}

.bot-footer > div {
  display: grid;
  gap: 0.15rem;
  min-width: 4.5rem;
}

.bot-footer strong {
  font-size: 1.05rem;
}

.bot-footer p {
  min-width: 0;
  flex: 1;
  color: var(--color-text-secondary);
  font-size: 0.88rem;
  line-height: 1.45;
}

.manage-button {
  min-height: 2.25rem;
  flex: 0 0 auto;
  gap: 0.4rem;
  padding: 0 0.75rem;
  color: var(--color-text-primary);
}

.market-card,
.runtime-card {
  display: grid;
  gap: 0.65rem;
}

.market-card div:first-child,
.runtime-heading,
.runtime-actions,
.runtime-metrics,
.assignment-form {
  display: flex;
  gap: 0.75rem;
}

.market-card div:first-child,
.runtime-heading {
  align-items: flex-start;
  justify-content: space-between;
}

.market-card span {
  font-size: 0.76rem;
  font-weight: 850;
}

.market-footer {
  justify-content: space-between;
}

.market-footer span {
  color: var(--color-text-primary);
}

.tiny-button {
  min-height: 2rem;
  gap: 0.35rem;
  padding: 0 0.7rem;
}

.tiny-button:disabled,
.icon-action:disabled {
  cursor: wait;
  opacity: 0.55;
}

.runtime-card {
  background: var(--color-surface-muted);
}

.runtime-heading strong,
.runtime-heading span {
  display: block;
}

.runtime-heading span,
.runtime-metrics span {
  margin-top: 0.2rem;
  font-size: 0.78rem;
  font-weight: 800;
}

.runtime-heading svg {
  color: var(--color-secondary);
}

.runtime-metrics,
.runtime-actions {
  flex-wrap: wrap;
}

.assignment-summary {
  border-top: 1px solid var(--color-divider);
  padding-top: 0.65rem;
}

.assignment-summary strong,
.assignment-summary span {
  display: block;
}

.assignment-summary span {
  margin-top: 0.15rem;
  font-size: 0.8rem;
  font-weight: 800;
}

.assignment-form {
  flex-wrap: wrap;
}

.assignment-form label {
  display: grid;
  flex: 1 1 8rem;
  gap: 0.3rem;
  color: var(--color-text-muted);
  font-size: 0.75rem;
  font-weight: 850;
}

.assignment-form input,
.assignment-form select {
  min-height: 2.15rem;
  min-width: 0;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 0 0.55rem;
  background: var(--color-surface);
  color: var(--color-text-primary);
  font-size: 0.84rem;
  font-weight: 750;
}

.assignment-form input:disabled {
  opacity: 0.55;
}

.runtime-error {
  margin-bottom: 0.75rem;
  border: 1px solid color-mix(in srgb, var(--color-error) 45%, var(--color-border));
  border-radius: 8px;
  padding: 0.75rem;
  color: var(--color-error);
  font-size: 0.86rem;
  font-weight: 750;
}

.health-row {
  align-items: flex-start;
  border-bottom: 1px solid var(--color-divider);
  padding-bottom: 0.85rem;
  color: var(--color-secondary);
}

.health-row:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.health-row div {
  display: grid;
  gap: 0.2rem;
}

.health-row strong,
.notice-panel strong {
  color: var(--color-text-primary);
  font-size: 0.92rem;
}

.error-panel {
  border-color: color-mix(in srgb, var(--color-error) 50%, var(--color-border));
  color: var(--color-error);
}

@media (max-width: 1100px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .dashboard-page {
    padding-top: 1rem;
  }

  .hero-section,
  .billing-strip,
  .bot-card-header,
  .bot-footer,
  .notice-panel {
    align-items: stretch;
    flex-direction: column;
  }

  .hero-actions,
  .billing-meta {
    justify-content: flex-start;
  }

  .primary-action,
  .secondary-action,
  .manage-button {
    width: 100%;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 420px) {
  .hero-actions {
    flex-direction: column;
  }

  .bot-card {
    padding: 0.85rem;
  }

  .status-pill {
    width: fit-content;
  }
}
</style>
