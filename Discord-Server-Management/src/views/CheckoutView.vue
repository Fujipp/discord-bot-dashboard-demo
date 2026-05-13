<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  ArrowLeft,
  CheckCircle2,
  CircleAlert,
  Clock,
  CreditCard,
  FileText,
  PackageCheck,
  RefreshCw,
  ShieldCheck,
} from 'lucide-vue-next';
import { getCustomerPayment, type CheckoutResponse } from '@/services/customer';
import { useAuthStore } from '@/stores/authStore';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const checkout = ref<CheckoutResponse | null>(null);
const isLoading = ref(true);
const errorMessage = ref('');

const paymentId = computed(() => Number(route.params.paymentId));
const isPaid = computed(() => checkout.value?.status === 'PAID');
const isFailed = computed(() => checkout.value?.status === 'FAILED');
const isPending = computed(() => checkout.value?.status === 'PENDING');

const statusCopy = computed(() => {
  if (isPaid.value) return 'Payment confirmed';
  if (isFailed.value) return 'Payment failed';
  return 'Awaiting payment';
});

const statusDetail = computed(() => {
  if (isPaid.value) return 'ระบบยืนยันยอดและเปิด Feature ให้ bot เรียบร้อยแล้ว';
  if (isFailed.value) return 'รายการนี้ไม่สำเร็จ กรุณากลับไปสร้าง checkout ใหม่อีกครั้ง';
  return 'สแกน QR ด้วยแอปธนาคาร แล้วรอ webhook ยืนยันสถานะอัตโนมัติ';
});

async function loadCheckout() {
  authStore.loadSession();
  if (!authStore.accessToken || Number.isNaN(paymentId.value)) {
    isLoading.value = false;
    errorMessage.value = 'Payment reference is invalid';
    return;
  }

  isLoading.value = true;
  errorMessage.value = '';

  try {
    checkout.value = await getCustomerPayment(authStore.accessToken, paymentId.value);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Could not load checkout';
  } finally {
    isLoading.value = false;
  }
}

function formatMoney(cents: number, currency = 'THB') {
  return new Intl.NumberFormat('th-TH', {
    style: 'currency',
    currency,
    maximumFractionDigits: 0,
  }).format(cents / 100);
}

function formatDateTime(value: string | null) {
  if (!value) return '-';

  return new Intl.DateTimeFormat('th-TH', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}

function goBackToShop() {
  router.push({ name: 'shop' });
}

onMounted(loadCheckout);
</script>

<template>
  <main class="checkout-page">
    <section class="checkout-hero">
      <button type="button" class="icon-action" aria-label="Back to shop" @click="goBackToShop">
        <ArrowLeft class="h-4 w-4" />
      </button>

      <div>
        <p class="eyebrow">Secure Checkout</p>
        <h1>Payment</h1>
        <p>ตรวจสอบรายการ ชำระเงินผ่าน PromptPay และรอระบบเปิด feature ให้บอทของคุณอัตโนมัติ</p>
      </div>

      <span v-if="checkout" :class="['status-pill', checkout.status.toLowerCase()]">
        {{ checkout.status }}
      </span>
    </section>

    <section v-if="errorMessage" class="notice-panel error">
      <CircleAlert class="h-5 w-5" />
      <span>{{ errorMessage }}</span>
    </section>

    <section v-if="isLoading" class="notice-panel">
      <RefreshCw class="h-5 w-5 animate-spin" />
      <span>กำลังโหลดข้อมูลการชำระเงิน</span>
    </section>

    <section v-if="checkout" class="checkout-grid">
      <article class="payment-card">
        <div class="section-heading">
          <CreditCard class="h-5 w-5" />
          <div>
            <h2>{{ statusCopy }}</h2>
            <p>{{ statusDetail }}</p>
          </div>
        </div>

        <div class="qr-frame">
          <img
            v-if="checkout.qrCodeUrl && isPending"
            :src="checkout.qrCodeUrl"
            alt="PromptPay QR code"
          />
          <CheckCircle2 v-else-if="isPaid" class="result-icon success" />
          <CircleAlert v-else class="result-icon failed" />
        </div>

        <div class="payment-total">
          <span>Total due</span>
          <strong>{{ formatMoney(checkout.amountCents, checkout.currency) }}</strong>
          <p>{{ checkout.provider }} · {{ checkout.checkoutReference }}</p>
        </div>

        <div class="action-row">
          <button type="button" class="primary-action" :disabled="isLoading" @click="loadCheckout">
            <RefreshCw :class="['h-4 w-4', isLoading ? 'animate-spin' : '']" />
            Refresh status
          </button>
          <button type="button" class="secondary-action" @click="goBackToShop">
            Back to shop
          </button>
        </div>
      </article>

      <aside class="summary-panel">
        <div class="section-heading">
          <FileText class="h-5 w-5" />
          <div>
            <h2>Order Summary</h2>
            <p>รายละเอียดรายการที่กำลังเปิดใช้งานหลังชำระเงินสำเร็จ</p>
          </div>
        </div>

        <div class="meta-list">
          <div>
            <span>Reference</span>
            <strong>{{ checkout.checkoutReference }}</strong>
          </div>
          <div>
            <span>Provider ID</span>
            <strong>{{ checkout.providerPaymentId }}</strong>
          </div>
          <div>
            <span>Expires</span>
            <strong>{{ formatDateTime(checkout.expiresAt) }}</strong>
          </div>
        </div>

        <div class="item-list">
          <article v-for="item in checkout.items" :key="item.featureId">
            <PackageCheck class="h-4 w-4" />
            <div>
              <strong>{{ item.name }}</strong>
              <span>{{ item.code }}</span>
            </div>
            <em>{{ formatMoney(item.amountCents, item.currency) }}</em>
          </article>
        </div>

        <div class="assurance-box">
          <ShieldCheck class="h-5 w-5" />
          <div>
            <strong>Webhook confirmation</strong>
            <span>ระบบจะใช้ webhook จาก payment provider เป็นแหล่งยืนยันยอดหลัก</span>
          </div>
        </div>
      </aside>
    </section>

    <section v-if="checkout" class="timeline-panel">
      <article :class="{ active: true }">
        <Clock class="h-4 w-4" />
        <div>
          <strong>Created</strong>
          <span>Checkout ถูกสร้างและพร้อมรับชำระเงิน</span>
        </div>
      </article>
      <article :class="{ active: isPending || isPaid }">
        <CreditCard class="h-4 w-4" />
        <div>
          <strong>Payment</strong>
          <span>ลูกค้าชำระเงินผ่าน PromptPay QR</span>
        </div>
      </article>
      <article :class="{ active: isPaid }">
        <CheckCircle2 class="h-4 w-4" />
        <div>
          <strong>Activation</strong>
          <span>เปิด feature และต่อรอบ subscription อัตโนมัติ</span>
        </div>
      </article>
    </section>
  </main>
</template>

<style scoped>
.checkout-page {
  width: min(100%, 1180px);
  margin: 0 auto;
  padding: 2rem 1rem 3rem;
  color: var(--color-text-primary);
}

.checkout-hero,
.payment-card,
.summary-panel,
.timeline-panel,
.notice-panel {
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: color-mix(in srgb, var(--color-surface) 94%, transparent);
  box-shadow: var(--shadow-soft);
}

.checkout-hero {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 1rem;
  align-items: end;
  padding: 1.5rem;
}

.eyebrow,
.payment-total span,
.meta-list span,
.item-list span,
.timeline-panel span,
.assurance-box span {
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

.checkout-hero p:last-child,
.section-heading p {
  margin-top: 0.65rem;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.icon-action,
.primary-action,
.secondary-action {
  display: inline-flex;
  min-height: 2.5rem;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-surface-muted);
  color: var(--color-text-primary);
  font-weight: 850;
}

.icon-action {
  width: 2.5rem;
  padding: 0;
}

.primary-action {
  border-color: transparent;
  padding: 0 0.9rem;
  background: var(--color-primary);
  color: var(--color-surface);
}

.secondary-action {
  padding: 0 0.9rem;
}

.primary-action:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.checkout-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(340px, 0.9fr);
  gap: 1rem;
  margin-top: 1rem;
}

.payment-card,
.summary-panel {
  padding: 1rem;
}

.section-heading {
  display: flex;
  gap: 0.75rem;
}

.qr-frame {
  display: grid;
  place-items: center;
  min-height: 320px;
  margin-top: 1rem;
  border: 1px dashed var(--color-border);
  border-radius: 8px;
  background: var(--color-surface-muted);
}

.qr-frame img {
  width: min(280px, 85vw);
  height: min(280px, 85vw);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: #fff;
  object-fit: contain;
}

.result-icon {
  width: 5rem;
  height: 5rem;
}

.result-icon.success {
  color: var(--color-success);
}

.result-icon.failed {
  color: var(--color-danger);
}

.payment-total {
  margin-top: 1rem;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 1rem;
  background: var(--color-surface-muted);
}

.payment-total strong {
  display: block;
  margin-top: 0.25rem;
  font-size: 2rem;
  font-weight: 900;
}

.payment-total p {
  margin-top: 0.35rem;
  color: var(--color-text-secondary);
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
  margin-top: 1rem;
}

.meta-list,
.item-list {
  display: grid;
  gap: 0.75rem;
  margin-top: 1rem;
}

.meta-list div,
.item-list article,
.assurance-box {
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 0.85rem;
  background: var(--color-surface-muted);
}

.meta-list strong,
.item-list strong {
  display: block;
  margin-top: 0.2rem;
  overflow-wrap: anywhere;
}

.item-list article {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 0.75rem;
  align-items: center;
}

.item-list em {
  color: var(--color-text-secondary);
  font-style: normal;
  font-weight: 900;
}

.assurance-box {
  display: flex;
  gap: 0.7rem;
  margin-top: 1rem;
}

.assurance-box svg {
  color: var(--color-success);
  flex: 0 0 auto;
}

.status-pill {
  display: inline-flex;
  min-height: 1.85rem;
  align-items: center;
  border-radius: 999px;
  padding: 0 0.75rem;
  background: var(--color-surface-muted);
  color: var(--color-text-secondary);
  font-size: 0.72rem;
  font-weight: 900;
}

.status-pill.paid {
  background: color-mix(in srgb, var(--color-success) 16%, transparent);
  color: var(--color-success);
}

.status-pill.failed {
  background: color-mix(in srgb, var(--color-danger) 14%, transparent);
  color: var(--color-danger);
}

.status-pill.pending {
  background: color-mix(in srgb, var(--color-warning) 18%, transparent);
  color: var(--color-warning);
}

.notice-panel {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  margin-top: 1rem;
  padding: 0.9rem 1rem;
  color: var(--color-text-secondary);
  font-weight: 800;
}

.notice-panel.error {
  border-color: color-mix(in srgb, var(--color-danger) 45%, var(--color-border));
  color: var(--color-danger);
}

.timeline-panel {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
  margin-top: 1rem;
  padding: 1rem;
}

.timeline-panel article {
  display: flex;
  gap: 0.65rem;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 0.85rem;
  background: var(--color-surface-muted);
  opacity: 0.6;
}

.timeline-panel article.active {
  opacity: 1;
}

.timeline-panel strong,
.timeline-panel span {
  display: block;
}

@media (max-width: 900px) {
  .checkout-hero,
  .checkout-grid,
  .timeline-panel {
    grid-template-columns: 1fr;
  }

  .status-pill,
  .action-row .primary-action,
  .action-row .secondary-action {
    width: 100%;
  }
}
</style>
