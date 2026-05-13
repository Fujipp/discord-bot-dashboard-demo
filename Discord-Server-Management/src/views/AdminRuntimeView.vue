<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import {
  CircleAlert,
  Database,
  HardDrive,
  MemoryStick,
  Power,
  RefreshCw,
  Save,
  ServerCog,
} from 'lucide-vue-next';
import { useAuthStore } from '@/stores/authStore';
import {
  assignRuntimeProcess,
  getAdminRuntimeDashboard,
  runAdminRuntimeAction,
  type AdminRuntimeDashboardResponse,
  type AdminRuntimeProcessResponse,
  type BotBillingMode,
  type RuntimeProcessResponse,
} from '@/services/customer';

type AssignmentDraft = {
  ownerUserId: string;
  botName: string;
  billingMode: BotBillingMode;
  monthlyPriceBaht: number;
  runtimeCurrentPeriodEnd: string;
};

const authStore = useAuthStore();
const adminRuntime = ref<AdminRuntimeDashboardResponse | null>(null);
const isLoading = ref(true);
const errorMessage = ref('');
const runningProcessAction = ref('');
const savingAssignment = ref('');
const selectedHostId = ref('');
const userSearch = ref('');
const processSearch = ref('');
const selectedProcessName = ref('');
const assignmentDrafts = ref<Record<string, AssignmentDraft>>({});

const processes = computed(() => adminRuntime.value?.processes ?? []);
const users = computed(() => adminRuntime.value?.users ?? []);
const hosts = computed(() => adminRuntime.value?.hosts ?? []);
const host = computed(() => adminRuntime.value?.host ?? null);
const visibleProcesses = computed(() => {
  const query = processSearch.value.trim().toLowerCase();
  const selectedName = selectedProcessName.value;

  return processes.value.filter((processInfo) => {
    const processName = processInfo.runtime.name.toLowerCase();
    const owner = `${processInfo.bot?.ownerUsername ?? ''} ${processInfo.bot?.ownerEmail ?? ''}`.toLowerCase();
    const matchesSearch = !query || processName.includes(query) || owner.includes(query);
    const matchesSelected = !selectedName || processInfo.runtime.name === selectedName;
    return matchesSearch && matchesSelected;
  });
});
const assignedCount = computed(() => processes.value.filter((processInfo) => processInfo.bot).length);
const onlineCount = computed(() => processes.value.filter((processInfo) => processInfo.runtime.status === 'online').length);
const memoryUsedPercent = computed(() => {
  if (!host.value?.memoryTotalBytes) return 0;
  return Math.round((host.value.memoryUsedBytes / host.value.memoryTotalBytes) * 100);
});

async function loadRuntimeDashboard() {
  authStore.loadSession();

  if (!authStore.accessToken) {
    isLoading.value = false;
    return;
  }

  isLoading.value = true;
  errorMessage.value = '';

  try {
    adminRuntime.value = await getAdminRuntimeDashboard(authStore.accessToken, {
      hostId: selectedHostId.value,
      userSearch: userSearch.value,
    });
    selectedHostId.value = adminRuntime.value.selectedHostId;
    syncAssignmentDrafts();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Could not load runtime dashboard';
  } finally {
    isLoading.value = false;
  }
}

async function runProcessAction(processName: string, action: 'start' | 'stop' | 'restart') {
  if (!authStore.accessToken) return;

  runningProcessAction.value = `${processName}:${action}`;
  errorMessage.value = '';

  try {
    await runAdminRuntimeAction(authStore.accessToken, selectedHostId.value, processName, action);
    await loadRuntimeDashboard();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Could not run runtime action';
  } finally {
    runningProcessAction.value = '';
  }
}

async function saveProcessAssignment(processInfo: AdminRuntimeProcessResponse) {
  if (!authStore.accessToken) return;

  const processName = processInfo.runtime.name;
  const draft = assignmentDraft(processName);

  if (!draft.ownerUserId) {
    errorMessage.value = 'Please select an owner before saving this process.';
    return;
  }

  savingAssignment.value = processName;
  errorMessage.value = '';

  try {
    await assignRuntimeProcess(authStore.accessToken, selectedHostId.value, processName, {
      ownerUserId: Number(draft.ownerUserId),
      botName: draft.botName || processName,
      billingMode: draft.billingMode,
      monthlyPriceCents: draft.billingMode === 'FREE' ? 0 : Math.round(Number(draft.monthlyPriceBaht || 0) * 100),
      runtimeCurrentPeriodEnd: draft.runtimeCurrentPeriodEnd || null,
    });
    await loadRuntimeDashboard();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Could not save process assignment';
  } finally {
    savingAssignment.value = '';
  }
}

function syncAssignmentDrafts() {
  const nextDrafts = { ...assignmentDrafts.value };

  for (const processInfo of processes.value) {
    const processName = processInfo.runtime.name;
    const assignedBot = processInfo.bot;

    nextDrafts[processName] = {
      ownerUserId: assignedBot?.ownerUserId ? String(assignedBot.ownerUserId) : nextDrafts[processName]?.ownerUserId ?? '',
      botName: assignedBot?.name ?? nextDrafts[processName]?.botName ?? processName,
      billingMode: assignedBot?.billingMode ?? nextDrafts[processName]?.billingMode ?? 'FREE',
      monthlyPriceBaht: assignedBot?.monthlyPriceCents ? assignedBot.monthlyPriceCents / 100 : nextDrafts[processName]?.monthlyPriceBaht ?? 0,
      runtimeCurrentPeriodEnd: assignedBot?.runtimeCurrentPeriodEnd ?? nextDrafts[processName]?.runtimeCurrentPeriodEnd ?? '',
    };
  }

  assignmentDrafts.value = nextDrafts;
}

function assignmentDraft(processName: string): AssignmentDraft {
  return assignmentDrafts.value[processName] ?? {
    ownerUserId: '',
    botName: processName,
    billingMode: 'FREE',
    monthlyPriceBaht: 0,
    runtimeCurrentPeriodEnd: '',
  };
}

function updateAssignmentDraft(processName: string, field: keyof AssignmentDraft, value: string | number) {
  assignmentDrafts.value = {
    ...assignmentDrafts.value,
    [processName]: {
      ...assignmentDraft(processName),
      [field]: value,
    },
  };
}

function runtimeUptime(processInfo: RuntimeProcessResponse) {
  if (!processInfo.uptimeMillis) return 'Stopped';

  const hours = Math.floor(processInfo.uptimeMillis / 1000 / 60 / 60);
  const days = Math.floor(hours / 24);

  if (days > 0) return `${days}d ${hours % 24}h`;
  if (hours > 0) return `${hours}h`;

  return '<1h';
}

function formatBytes(bytes: number) {
  if (bytes >= 1024 ** 3) return `${(bytes / 1024 ** 3).toFixed(1)} GB`;
  return `${(bytes / 1024 ** 2).toFixed(0)} MB`;
}

function formatMoney(cents: number) {
  return new Intl.NumberFormat('th-TH', {
    style: 'currency',
    currency: 'THB',
    maximumFractionDigits: 0,
  }).format(cents / 100);
}

function clearProcessFilters() {
  processSearch.value = '';
  selectedProcessName.value = '';
}

onMounted(loadRuntimeDashboard);
</script>

<template>
  <main class="admin-runtime-page">
    <section class="hero-section">
      <div>
        <p class="eyebrow">Admin Runtime</p>
        <h1>PM2 Control Center</h1>
        <p>เลือก VM, ค้นหาบอทหรือเจ้าของ, ตั้งราคา แล้วสั่ง start/stop/restart แบบเป็นระบบ</p>
      </div>

      <button type="button" class="primary-action" :disabled="isLoading" @click="loadRuntimeDashboard">
        <RefreshCw :class="['h-4 w-4', isLoading ? 'animate-spin' : '']" />
        Refresh
      </button>
    </section>

    <section class="ops-bar">
      <label>
        Runtime VM
        <select v-model="selectedHostId" @change="loadRuntimeDashboard">
          <option v-for="hostOption in hosts" :key="hostOption.id" :value="hostOption.id">
            {{ hostOption.name }} · {{ hostOption.region }} · {{ hostOption.host }}
          </option>
        </select>
      </label>

      <label>
        Bot / owner search
        <input
          v-model="processSearch"
          type="search"
          placeholder="Search process, owner, or email"
        />
      </label>

      <label>
        Select bot
        <select v-model="selectedProcessName">
          <option value="">All bots</option>
          <option v-for="processInfo in processes" :key="processInfo.runtime.name" :value="processInfo.runtime.name">
            {{ processInfo.runtime.name }}
          </option>
        </select>
      </label>

      <label>
        Owner directory
        <input
          v-model="userSearch"
          type="search"
          placeholder="Search users before assigning"
          @keyup.enter="loadRuntimeDashboard"
        />
      </label>

      <button type="button" class="secondary-action" @click="loadRuntimeDashboard">
        Search users
      </button>
      <button type="button" class="secondary-action" @click="clearProcessFilters">
        Clear bot filter
      </button>
    </section>

    <section class="health-grid">
      <article class="health-card">
        <HardDrive class="h-5 w-5" />
        <div>
          <span>Disk</span>
          <strong>{{ host ? formatBytes(host.diskAvailableBytes) : '-' }}</strong>
          <p>{{ host ? `${host.diskUsedPercent}% used · ${formatBytes(host.diskTotalBytes)} total` : 'Loading' }}</p>
        </div>
      </article>
      <article class="health-card" :class="{ warning: memoryUsedPercent >= 80 }">
        <MemoryStick class="h-5 w-5" />
        <div>
          <span>Memory</span>
          <strong>{{ host ? formatBytes(host.memoryAvailableBytes) : '-' }}</strong>
          <p>{{ host ? `${memoryUsedPercent}% used · swap ${formatBytes(host.swapUsedBytes)}` : 'Loading' }}</p>
        </div>
      </article>
      <article class="health-card">
        <ServerCog class="h-5 w-5" />
        <div>
          <span>Processes</span>
          <strong>{{ processes.length }}</strong>
          <p>{{ onlineCount }} online · {{ assignedCount }} assigned</p>
        </div>
      </article>
      <article class="health-card">
        <Database class="h-5 w-5" />
        <div>
          <span>Load</span>
          <strong>{{ host?.loadOneMinute ?? '-' }}</strong>
          <p>{{ host?.uptime ?? 'Loading uptime' }}</p>
        </div>
      </article>
    </section>

    <section v-if="errorMessage" class="notice-panel">
      <CircleAlert class="h-5 w-5" />
      <span>{{ errorMessage }}</span>
    </section>

    <section class="result-line">
      Showing {{ visibleProcesses.length }} of {{ processes.length }} processes · owner search returns {{ users.length }} users
    </section>

    <section class="process-grid">
      <article
        v-for="processInfo in visibleProcesses"
        :key="processInfo.runtime.name"
        class="process-card"
      >
        <div class="card-topline">
          <div class="process-title">
            <HardDrive class="h-5 w-5" />
            <div>
              <h2>{{ processInfo.runtime.name }}</h2>
              <p>pid {{ processInfo.runtime.pid || '-' }} · {{ runtimeUptime(processInfo.runtime) }}</p>
            </div>
          </div>
          <span :class="['status-pill', processInfo.runtime.status]">{{ processInfo.runtime.status }}</span>
        </div>

        <div class="quick-facts">
          <span>{{ processInfo.runtime.cpu }}% CPU</span>
          <span>{{ formatBytes(processInfo.runtime.memoryBytes) }}</span>
          <span>{{ processInfo.runtime.restartCount }} restarts</span>
        </div>

        <div class="assigned-summary">
          <span>Current owner</span>
          <strong>{{ processInfo.bot?.ownerUsername ?? 'Not assigned' }}</strong>
          <p>
            {{
              processInfo.bot
                ? `${processInfo.bot.billingMode} · ${formatMoney(processInfo.bot.monthlyPriceCents)}/mo · expires ${processInfo.bot.runtimeCurrentPeriodEnd ?? 'not set'}`
                : 'Select owner and save to show this bot on customer dashboard'
            }}
          </p>
        </div>

        <div class="form-grid">
          <label>
            Owner
            <select
              :value="assignmentDraft(processInfo.runtime.name).ownerUserId"
              @change="updateAssignmentDraft(processInfo.runtime.name, 'ownerUserId', ($event.target as HTMLSelectElement).value)"
            >
              <option value="">Select owner</option>
              <option v-for="userOption in users" :key="userOption.id" :value="String(userOption.id)">
                {{ userOption.username }} · {{ userOption.email }}
              </option>
            </select>
          </label>

          <label>
            Bot name
            <input
              :value="assignmentDraft(processInfo.runtime.name).botName"
              type="text"
              @input="updateAssignmentDraft(processInfo.runtime.name, 'botName', ($event.target as HTMLInputElement).value)"
            />
          </label>

          <label>
            Billing
            <select
              :value="assignmentDraft(processInfo.runtime.name).billingMode"
              @change="updateAssignmentDraft(processInfo.runtime.name, 'billingMode', ($event.target as HTMLSelectElement).value as BotBillingMode)"
            >
              <option value="FREE">Free</option>
              <option value="PAID">Paid</option>
            </select>
          </label>

          <label>
            THB / month
            <input
              :value="assignmentDraft(processInfo.runtime.name).monthlyPriceBaht"
              type="number"
              min="0"
              step="1"
              :disabled="assignmentDraft(processInfo.runtime.name).billingMode === 'FREE'"
              @input="updateAssignmentDraft(processInfo.runtime.name, 'monthlyPriceBaht', Number(($event.target as HTMLInputElement).value))"
            />
          </label>

          <label>
            Runtime expiry
            <input
              :value="assignmentDraft(processInfo.runtime.name).runtimeCurrentPeriodEnd"
              type="date"
              @input="updateAssignmentDraft(processInfo.runtime.name, 'runtimeCurrentPeriodEnd', ($event.target as HTMLInputElement).value)"
            />
          </label>
        </div>

        <div class="action-row">
          <button
            type="button"
            class="primary-action"
            :disabled="savingAssignment === processInfo.runtime.name"
            @click="saveProcessAssignment(processInfo)"
          >
            <Save class="h-4 w-4" />
            Save
          </button>
          <button
            type="button"
            class="secondary-action"
            :disabled="runningProcessAction === `${processInfo.runtime.name}:restart`"
            @click="runProcessAction(processInfo.runtime.name, 'restart')"
          >
            <RefreshCw class="h-4 w-4" />
            Restart
          </button>
          <button
            type="button"
            class="secondary-action"
            :disabled="runningProcessAction.includes(processInfo.runtime.name)"
            @click="runProcessAction(processInfo.runtime.name, processInfo.runtime.status === 'online' ? 'stop' : 'start')"
          >
            <Power class="h-4 w-4" />
            {{ processInfo.runtime.status === 'online' ? 'Stop' : 'Start' }}
          </button>
        </div>
      </article>
    </section>
  </main>
</template>

<style scoped>
.admin-runtime-page {
  width: min(100%, 1320px);
  margin: 0 auto;
  padding: 2rem 1rem 3rem;
  color: var(--color-text-primary);
}

.hero-section,
.ops-bar,
.health-card,
.notice-panel,
.process-card {
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: color-mix(in srgb, var(--color-surface) 94%, transparent);
  box-shadow: var(--shadow-soft);
}

.ops-bar {
  display: grid;
  grid-template-columns: 1.3fr 1fr 1fr 1fr auto auto;
  gap: 0.75rem;
  align-items: end;
  margin-top: 1rem;
  padding: 1rem;
}

.hero-section {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 1rem;
  padding: 1.5rem;
}

.eyebrow,
.health-card span,
.assigned-summary span,
.form-grid label,
.process-title p,
.quick-facts span {
  color: var(--color-text-muted);
  font-size: 0.76rem;
  font-weight: 850;
}

h1,
h2,
p {
  margin: 0;
}

h1 {
  margin-top: 0.25rem;
  font-size: clamp(2.1rem, 5vw, 3.5rem);
  font-weight: 900;
  line-height: 1;
}

h2 {
  font-size: 1.05rem;
  font-weight: 900;
}

.hero-section p:last-child {
  margin-top: 0.7rem;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.primary-action,
.secondary-action {
  display: inline-flex;
  min-height: 2.35rem;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 0 0.8rem;
  background: var(--color-surface-muted);
  color: var(--color-text-primary);
  font-weight: 850;
}

.primary-action {
  border-color: transparent;
  background: var(--color-primary);
  color: var(--color-surface);
}

.primary-action:disabled,
.secondary-action:disabled {
  cursor: wait;
  opacity: 0.55;
}

.health-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1rem;
  margin-top: 1rem;
}

.health-card {
  display: flex;
  min-height: 8rem;
  gap: 0.8rem;
  padding: 1rem;
}

.health-card svg {
  color: var(--color-secondary);
}

.health-card.warning svg,
.health-card.warning strong {
  color: var(--color-warning);
}

.health-card strong {
  display: block;
  margin-top: 0.2rem;
  font-size: 1.35rem;
  font-weight: 900;
}

.health-card p {
  margin-top: 0.2rem;
  color: var(--color-text-secondary);
  font-size: 0.86rem;
}

.notice-panel {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-top: 1rem;
  padding: 1rem;
  color: var(--color-error);
}

.process-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
  margin-top: 1rem;
}

.process-card {
  display: grid;
  gap: 1rem;
  padding: 1rem;
}

.card-topline,
.process-title,
.quick-facts,
.action-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.card-topline {
  justify-content: space-between;
}

.process-title {
  min-width: 0;
}

.process-title svg {
  flex: 0 0 auto;
  color: var(--color-secondary);
}

.process-title h2 {
  overflow-wrap: anywhere;
}

.status-pill {
  width: fit-content;
  border-radius: 999px;
  padding: 0.35rem 0.65rem;
  background: var(--color-surface-muted);
  color: var(--color-text-secondary);
  font-size: 0.75rem;
  font-weight: 900;
}

.status-pill.online {
  background: color-mix(in srgb, var(--color-success) 14%, transparent);
  color: var(--color-success);
}

.quick-facts {
  flex-wrap: wrap;
}

.quick-facts span {
  border: 1px solid var(--color-border);
  border-radius: 999px;
  padding: 0.35rem 0.6rem;
  background: var(--color-surface-muted);
}

.assigned-summary {
  border-top: 1px solid var(--color-divider);
  padding-top: 0.85rem;
}

.assigned-summary strong,
.assigned-summary span {
  display: block;
}

.assigned-summary p {
  margin-top: 0.25rem;
  color: var(--color-text-secondary);
  font-size: 0.88rem;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.form-grid label {
  display: grid;
  gap: 0.35rem;
}

.ops-bar label {
  display: grid;
  gap: 0.35rem;
  color: var(--color-text-muted);
  font-size: 0.76rem;
  font-weight: 850;
}

input,
select {
  min-height: 2.35rem;
  min-width: 0;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 0 0.6rem;
  background: var(--color-surface);
  color: var(--color-text-primary);
  font-weight: 750;
}

.result-line {
  margin-top: 1rem;
  color: var(--color-text-muted);
  font-size: 0.84rem;
  font-weight: 800;
}

input:disabled {
  opacity: 0.55;
}

.action-row {
  display: grid;
  grid-template-columns: 1.2fr 1fr 1fr;
}

@media (max-width: 1100px) {
  .health-grid,
  .process-grid,
  .ops-bar {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .hero-section {
    align-items: stretch;
    flex-direction: column;
  }

  .health-grid,
  .form-grid,
  .action-row {
    grid-template-columns: 1fr;
  }
}
</style>
