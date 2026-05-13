<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import {
  Activity,
  Bell,
  CircleAlert,
  Clock,
  CreditCard,
  Power,
  RefreshCw,
  Save,
  Settings,
  ShieldCheck,
  Zap,
} from 'lucide-vue-next';
import {
  getAdminAutomationDashboard,
  runAdminAutomationNow,
  updateAdminAutomationSettings,
  type AutomationDashboardResponse,
  type AutomationRunResponse,
} from '@/services/customer';
import { useAuthStore } from '@/stores/authStore';

const authStore = useAuthStore();
const dashboard = ref<AutomationDashboardResponse | null>(null);
const isLoading = ref(true);
const isSaving = ref(false);
const isRunning = ref(false);
const errorMessage = ref('');
const successMessage = ref('');

const policyDraft = reactive({
  enabled: true,
  runtimeSuspendEnabled: false,
  reminderDaysBefore: 3,
  pastDueGraceDays: 1,
  cancelGraceDays: 7,
});

const summary = computed(() => [
  {
    label: 'Automation',
    value: policyDraft.enabled ? 'Enabled' : 'Paused',
    detail: policyDraft.enabled ? 'Scheduled daily at 09:00 Bangkok' : 'Manual run is still available',
    icon: Activity,
  },
  {
    label: 'Billing',
    value: dashboard.value?.activeBillingSubscriptions ?? 0,
    detail: `${dashboard.value?.pastDueBillingSubscriptions ?? 0} past due`,
    icon: CreditCard,
  },
  {
    label: 'Features',
    value: dashboard.value?.activeFeatureSubscriptions ?? 0,
    detail: `${dashboard.value?.pastDueFeatureSubscriptions ?? 0} past due`,
    icon: Zap,
  },
  {
    label: 'Runtime suspend',
    value: policyDraft.runtimeSuspendEnabled ? 'Armed' : 'Safe',
    detail: policyDraft.runtimeSuspendEnabled ? 'Can stop unpaid runtime bots' : 'Will not stop PM2 processes',
    icon: Power,
  },
]);

const recentRuns = computed(() => dashboard.value?.recentRuns ?? []);

async function loadAutomationDashboard() {
  authStore.loadSession();
  if (!authStore.accessToken) {
    isLoading.value = false;
    return;
  }

  isLoading.value = true;
  errorMessage.value = '';

  try {
    dashboard.value = await getAdminAutomationDashboard(authStore.accessToken);
    syncPolicyDraft();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Could not load automation dashboard';
  } finally {
    isLoading.value = false;
  }
}

async function savePolicy() {
  if (!authStore.accessToken) return;

  isSaving.value = true;
  errorMessage.value = '';
  successMessage.value = '';

  try {
    dashboard.value = await updateAdminAutomationSettings(authStore.accessToken, {
      settings: {
        'automation.enabled': String(policyDraft.enabled),
        'automation.runtime_suspend_enabled': String(policyDraft.runtimeSuspendEnabled),
        'automation.reminder_days_before': String(normalizeDays(policyDraft.reminderDaysBefore)),
        'automation.past_due_grace_days': String(normalizeDays(policyDraft.pastDueGraceDays)),
        'automation.cancel_grace_days': String(normalizeDays(policyDraft.cancelGraceDays)),
      },
    });
    syncPolicyDraft();
    successMessage.value = 'Automation policy saved';
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Could not save automation policy';
  } finally {
    isSaving.value = false;
  }
}

async function runNow() {
  if (!authStore.accessToken) return;

  isRunning.value = true;
  errorMessage.value = '';
  successMessage.value = '';

  try {
    const run = await runAdminAutomationNow(authStore.accessToken);
    dashboard.value = await getAdminAutomationDashboard(authStore.accessToken);
    syncPolicyDraft();
    successMessage.value = run.status === 'SUCCESS' ? 'Automation run completed' : `Automation run ${run.status.toLowerCase()}`;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Could not run automation';
  } finally {
    isRunning.value = false;
  }
}

function syncPolicyDraft() {
  if (!dashboard.value) return;

  policyDraft.enabled = dashboard.value.enabled;
  policyDraft.runtimeSuspendEnabled = dashboard.value.runtimeSuspendEnabled;
  policyDraft.reminderDaysBefore = dashboard.value.reminderDaysBefore;
  policyDraft.pastDueGraceDays = dashboard.value.pastDueGraceDays;
  policyDraft.cancelGraceDays = dashboard.value.cancelGraceDays;
}

function normalizeDays(value: number) {
  return Math.max(0, Math.round(Number(value) || 0));
}

function runDuration(run: AutomationRunResponse) {
  if (!run.finishedAt) return 'Running';
  const millis = new Date(run.finishedAt).getTime() - new Date(run.startedAt).getTime();
  return `${Math.max(0, Math.round(millis / 1000))}s`;
}

function formatDateTime(value: string | null) {
  if (!value) return '-';

  return new Intl.DateTimeFormat('th-TH', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}

onMounted(loadAutomationDashboard);
</script>

<template>
  <main class="automation-page">
    <section class="hero-section">
      <div>
        <p class="eyebrow">Admin Automation</p>
        <h1>Automation Center</h1>
        <p>ตั้งรอบแจ้งเตือน, mark past due, cancel feature และควบคุม runtime suspension จาก policy เดียว</p>
      </div>

      <div class="hero-actions">
        <button type="button" class="secondary-action" :disabled="isLoading" @click="loadAutomationDashboard">
          <RefreshCw :class="['h-4 w-4', isLoading ? 'animate-spin' : '']" />
          Refresh
        </button>
        <button type="button" class="primary-action" :disabled="isRunning || isLoading" @click="runNow">
          <Zap :class="['h-4 w-4', isRunning ? 'animate-pulse' : '']" />
          Run now
        </button>
      </div>
    </section>

    <section class="summary-grid" aria-label="Automation summary">
      <article v-for="item in summary" :key="item.label" class="summary-card">
        <div class="icon-box">
          <component :is="item.icon" class="h-5 w-5" />
        </div>
        <div>
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <p>{{ item.detail }}</p>
        </div>
      </article>
    </section>

    <section v-if="errorMessage" class="notice-panel error">
      <CircleAlert class="h-5 w-5" />
      <span>{{ errorMessage }}</span>
    </section>

    <section v-if="successMessage" class="notice-panel success">
      <ShieldCheck class="h-5 w-5" />
      <span>{{ successMessage }}</span>
    </section>

    <section class="automation-grid">
      <article class="policy-panel">
        <div class="section-heading">
          <Settings class="h-5 w-5" />
          <div>
            <h2>Policy</h2>
            <p>ค่าตรงนี้คือกฎกลางสำหรับ subscription และ feature ที่จะถูกใช้ทั้ง scheduled run และ manual run</p>
          </div>
        </div>

        <div class="toggle-list">
          <label class="toggle-row">
            <span>
              <strong>Enable daily automation</strong>
              <small>รันอัตโนมัติทุกวัน 09:00 Asia/Bangkok</small>
            </span>
            <input v-model="policyDraft.enabled" type="checkbox" />
          </label>

          <label class="toggle-row warning">
            <span>
              <strong>Allow runtime suspension</strong>
              <small>เมื่อเปิด ระบบสามารถ stop PM2 ของ bot ที่ Runtime 24/7 ถูก cancel แล้ว</small>
            </span>
            <input v-model="policyDraft.runtimeSuspendEnabled" type="checkbox" />
          </label>
        </div>

        <div class="form-grid">
          <label>
            Reminder days before renewal
            <input v-model.number="policyDraft.reminderDaysBefore" type="number" min="0" step="1" />
          </label>
          <label>
            Past due grace days
            <input v-model.number="policyDraft.pastDueGraceDays" type="number" min="0" step="1" />
          </label>
          <label>
            Cancel grace days
            <input v-model.number="policyDraft.cancelGraceDays" type="number" min="0" step="1" />
          </label>
        </div>

        <div class="action-row">
          <button type="button" class="primary-action" :disabled="isSaving" @click="savePolicy">
            <Save class="h-4 w-4" />
            Save policy
          </button>
        </div>
      </article>

      <article class="policy-panel">
        <div class="section-heading">
          <Bell class="h-5 w-5" />
          <div>
            <h2>Automation Jobs</h2>
            <p>ลำดับงานที่ระบบจะทำในแต่ละ run เพื่อให้ billing, feature และ bot runtime อยู่สถานะเดียวกัน</p>
          </div>
        </div>

        <div class="job-list">
          <div>
            <Clock class="h-4 w-4" />
            <span>แจ้งเตือนก่อนต่ออายุ subscription</span>
          </div>
          <div>
            <CreditCard class="h-4 w-4" />
            <span>เปลี่ยน billing ที่เลย grace เป็น past due</span>
          </div>
          <div>
            <Zap class="h-4 w-4" />
            <span>เปลี่ยน feature ที่หมดอายุเป็น past due และ cancel หลังครบกำหนด</span>
          </div>
          <div>
            <Power class="h-4 w-4" />
            <span>หยุด PM2 runtime เฉพาะเมื่อเปิด runtime suspension</span>
          </div>
        </div>
      </article>
    </section>

    <section class="runs-panel">
      <div class="section-heading">
        <Activity class="h-5 w-5" />
        <div>
          <h2>Recent Runs</h2>
          <p>ประวัติ 10 ครั้งล่าสุด พร้อมจำนวนรายการที่ automation เปลี่ยนสถานะ</p>
        </div>
      </div>

      <div v-if="isLoading" class="notice-panel">
        <RefreshCw class="h-5 w-5 animate-spin" />
        <span>กำลังโหลด automation dashboard</span>
      </div>

      <div v-else-if="!recentRuns.length" class="empty-state">ยังไม่มี automation run</div>

      <div v-else class="run-table">
        <div class="table-head">
          <span>Run</span>
          <span>Status</span>
          <span>Changes</span>
          <span>Notifications</span>
          <span>Started</span>
          <span>Duration</span>
        </div>

        <div v-for="run in recentRuns" :key="run.id" class="table-row">
          <span>{{ run.runType }} #{{ run.id }}</span>
          <span :class="['status-pill', run.status.toLowerCase()]">{{ run.status }}</span>
          <span>
            billing {{ run.billingMarkedPastDue }} · feature {{ run.featureMarkedPastDue }} · canceled {{ run.featureCanceled }} · runtime {{ run.runtimeSuspended }}
          </span>
          <span>{{ run.notificationsCreated }}</span>
          <span>{{ formatDateTime(run.startedAt) }}</span>
          <span>{{ runDuration(run) }}</span>
          <small v-if="run.errorMessage" class="run-error">{{ run.errorMessage }}</small>
        </div>
      </div>
    </section>
  </main>
</template>

<style scoped>
.automation-page {
  width: min(100%, 1320px);
  margin: 0 auto;
  padding: 2rem 1rem 3rem;
  color: var(--color-text-primary);
}

.hero-section,
.summary-card,
.policy-panel,
.runs-panel,
.notice-panel {
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: color-mix(in srgb, var(--color-surface) 94%, transparent);
  box-shadow: var(--shadow-soft);
}

.hero-section {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 1rem;
  padding: 1.5rem;
}

.hero-actions,
.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.eyebrow,
.summary-card span,
.policy-panel p,
.runs-panel p,
.form-grid label,
.toggle-row small,
.table-head,
.empty-state {
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

.hero-section p:last-child,
.section-heading p {
  margin-top: 0.65rem;
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
  cursor: not-allowed;
  opacity: 0.65;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1rem;
  margin-top: 1rem;
}

.summary-card {
  display: flex;
  gap: 0.85rem;
  padding: 1rem;
}

.summary-card strong {
  display: block;
  margin-top: 0.25rem;
  font-size: 1.45rem;
  font-weight: 900;
}

.summary-card p {
  margin-top: 0.25rem;
  color: var(--color-text-secondary);
  font-size: 0.82rem;
}

.icon-box {
  display: inline-flex;
  width: 2.35rem;
  height: 2.35rem;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: var(--color-surface-muted);
  color: var(--color-primary);
}

.automation-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(340px, 0.75fr);
  gap: 1rem;
  margin-top: 1rem;
}

.policy-panel,
.runs-panel {
  padding: 1rem;
}

.section-heading {
  display: flex;
  gap: 0.75rem;
}

.toggle-list,
.job-list,
.form-grid,
.run-table {
  margin-top: 1rem;
}

.toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 0.85rem;
  background: var(--color-surface-muted);
}

.toggle-row + .toggle-row {
  margin-top: 0.75rem;
}

.toggle-row strong,
.toggle-row small {
  display: block;
}

.toggle-row strong {
  font-size: 0.92rem;
}

.toggle-row.warning {
  border-color: color-mix(in srgb, var(--color-warning) 45%, var(--color-border));
}

.toggle-row input {
  width: 1.2rem;
  height: 1.2rem;
  accent-color: var(--color-primary);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
}

.form-grid label {
  display: grid;
  gap: 0.35rem;
}

.form-grid input {
  min-height: 2.55rem;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 0 0.75rem;
  background: var(--color-surface-muted);
  color: var(--color-text-primary);
  font-weight: 800;
}

.job-list {
  display: grid;
  gap: 0.75rem;
}

.job-list div {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 0.85rem;
  background: var(--color-surface-muted);
  color: var(--color-text-secondary);
  font-weight: 800;
}

.runs-panel,
.notice-panel {
  margin-top: 1rem;
}

.notice-panel {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.9rem 1rem;
  color: var(--color-text-secondary);
  font-weight: 800;
}

.notice-panel.error {
  border-color: color-mix(in srgb, var(--color-danger) 45%, var(--color-border));
  color: var(--color-danger);
}

.notice-panel.success {
  border-color: color-mix(in srgb, var(--color-success) 45%, var(--color-border));
  color: var(--color-success);
}

.table-head,
.table-row {
  display: grid;
  grid-template-columns: 1fr 0.7fr 2.1fr 0.8fr 1fr 0.7fr;
  gap: 0.75rem;
  align-items: center;
  padding: 0.8rem 0;
}

.table-head {
  border-bottom: 1px solid var(--color-border);
  text-transform: uppercase;
}

.table-row {
  border-bottom: 1px solid color-mix(in srgb, var(--color-border) 60%, transparent);
  color: var(--color-text-secondary);
  font-size: 0.86rem;
  font-weight: 800;
}

.table-row > span:first-child {
  color: var(--color-text-primary);
  font-weight: 900;
}

.status-pill {
  display: inline-flex;
  width: fit-content;
  min-height: 1.75rem;
  align-items: center;
  border-radius: 999px;
  padding: 0 0.65rem;
  background: var(--color-surface-muted);
  color: var(--color-text-secondary);
  font-size: 0.72rem;
  font-weight: 900;
  text-transform: uppercase;
}

.status-pill.success {
  background: color-mix(in srgb, var(--color-success) 16%, transparent);
  color: var(--color-success);
}

.status-pill.failed {
  background: color-mix(in srgb, var(--color-danger) 14%, transparent);
  color: var(--color-danger);
}

.status-pill.running {
  background: color-mix(in srgb, var(--color-warning) 18%, transparent);
  color: var(--color-warning);
}

.run-error {
  grid-column: 1 / -1;
  color: var(--color-danger);
}

.empty-state {
  margin-top: 1rem;
  border: 1px dashed var(--color-border);
  border-radius: 8px;
  padding: 1rem;
  text-align: center;
}

@media (max-width: 1100px) {
  .summary-grid,
  .automation-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .automation-grid {
    grid-template-columns: 1fr;
  }

  .table-head {
    display: none;
  }

  .table-row {
    grid-template-columns: 1fr;
    gap: 0.35rem;
  }
}

@media (max-width: 760px) {
  .hero-section {
    align-items: stretch;
    flex-direction: column;
  }

  .summary-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .hero-actions,
  .primary-action,
  .secondary-action {
    width: 100%;
  }
}
</style>
