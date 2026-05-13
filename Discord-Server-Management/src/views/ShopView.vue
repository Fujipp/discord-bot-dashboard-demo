<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import {
  Bot,
  Check,
  CircleAlert,
  CreditCard,
  Layers3,
  PackageCheck,
  RefreshCw,
  Search,
  Server,
  ShoppingCart,
  Sparkles,
  Megaphone,
  Zap,
} from 'lucide-vue-next';
import { useRouter } from 'vue-router';
import {
  createCustomerCheckout,
  getCustomerDashboard,
  type CustomerDashboardResponse,
  type FeatureCategory,
  type FeatureResponse,
} from '@/services/customer';
import { useAuthStore } from '@/stores/authStore';

type ShopMode = 'features' | 'packs';

type FeaturePack = {
  code: string;
  name: string;
  description: string;
  featureCodes: string[];
  badge: string;
};

const authStore = useAuthStore();
const router = useRouter();
const dashboard = ref<CustomerDashboardResponse | null>(null);
const isLoading = ref(true);
const isCheckingOut = ref(false);
const errorMessage = ref('');
const checkoutMessage = ref('');
const activeMode = ref<ShopMode>('packs');
const searchQuery = ref('');
const activeCategory = ref<'ALL' | FeatureCategory>('ALL');
const selectedBotId = ref<number | null>(null);

const categoryLabels: Record<FeatureCategory, string> = {
  SHOP: 'Shop',
  PAYMENT: 'Payment',
  ROBLOX: 'Roblox',
  ENGAGEMENT: 'Engagement',
  RUNTIME: 'Runtime',
  ADMIN: 'Admin',
  AUTOMATION: 'Automation',
  SUPPORT: 'Support',
};

const packs: FeaturePack[] = [
  {
    code: 'test-package',
    name: 'Test Package',
    description: 'แพ็กเกจสำหรับทดสอบ checkout, PromptPay QR, webhook และการเปิด feature ราคา 10 บาท',
    featureCodes: ['test-package-10'],
    badge: 'Test',
  },
  {
    code: 'starter-shop',
    name: 'Starter Shop Bot',
    description: 'เริ่มขายของใน Discord ด้วยออเดอร์, สถานะร้าน, payment panel และ runtime 24/7',
    featureCodes: ['runtime-247', 'shop-orders', 'shop-status', 'payment-embed'],
    badge: 'Best start',
  },
  {
    code: 'payment-pro',
    name: 'Payment Pro',
    description: 'ระบบรับเงินครบขึ้นสำหรับร้านที่ต้องการ PromptPay, SlipOK, TrueMoney และ wallet credit',
    featureCodes: ['payment-embed', 'promptpay-slipok', 'truemoney-voucher', 'wallet-credit'],
    badge: 'Revenue',
  },
  {
    code: 'roblox-seller',
    name: 'Roblox Seller Pack',
    description: 'ขาย Robux เป็นระบบ มีแพ็กเกจ, ตรวจ Roblox user, หักเครดิต และจัดอันดับลูกค้า',
    featureCodes: ['runtime-247', 'roblox-seller', 'wallet-credit', 'top-spender-rank'],
    badge: 'Premium',
  },
  {
    code: 'operations-suite',
    name: 'Operations Suite',
    description: 'เครื่องมือหลังบ้านสำหรับร้านที่ต้องประกาศ, DM, ส่งไฟล์ และให้บอทอยู่ voice ตลอดเวลา',
    featureCodes: ['admin-message-tools', 'shop-status', 'voice-keeper', 'price-embed-cron'],
    badge: 'Admin',
  },
  {
    code: 'engagement-growth',
    name: 'Engagement Growth',
    description: 'เพิ่ม activity ด้วยระบบรีวิว, เครดิตรีวิว, rank spender และ scheduled price embed',
    featureCodes: ['review-credit', 'top-spender-rank', 'price-embed-cron'],
    badge: 'Growth',
  },
];

const features = computed(() => dashboard.value?.availableFeatures ?? []);
const bots = computed(() => dashboard.value?.bots ?? []);
const featureByCode = computed(() => new Map(features.value.map((feature) => [feature.code, feature])));
const featuredPromotions = computed(() =>
  features.value
    .filter((feature) => feature.featured || feature.promotionPriceCents)
    .slice(0, 3),
);

const categories = computed(() => {
  const used = new Set(features.value.map((feature) => feature.category));
  return Array.from(used).sort((a, b) => categoryLabels[a].localeCompare(categoryLabels[b]));
});

const filteredFeatures = computed(() => {
  const query = searchQuery.value.trim().toLowerCase();

  return features.value.filter((feature) => {
    const matchesCategory = activeCategory.value === 'ALL' || feature.category === activeCategory.value;
    const matchesQuery =
      !query ||
      feature.name.toLowerCase().includes(query) ||
      feature.description.toLowerCase().includes(query) ||
      categoryLabels[feature.category].toLowerCase().includes(query);

    return matchesCategory && matchesQuery;
  });
});

const resolvedPacks = computed(() =>
  packs
    .map((pack) => {
      const packFeatures = pack.featureCodes
        .map((code) => featureByCode.value.get(code))
        .filter((feature): feature is FeatureResponse => Boolean(feature));
      const totalCents = packFeatures.reduce((sum, feature) => sum + feature.monthlyPriceCents, 0);
      const promoAdjustedTotal = packFeatures.reduce((sum, feature) => sum + effectivePrice(feature), 0);
      const discountCents = Math.round(totalCents * 0.15);

      return {
        ...pack,
        features: packFeatures,
        totalCents,
        packPriceCents: Math.max(0, promoAdjustedTotal - discountCents),
        discountCents: Math.max(0, totalCents - Math.max(0, promoAdjustedTotal - discountCents)),
        currency: packFeatures[0]?.currency ?? 'THB',
      };
    })
    .filter((pack) => pack.features.length > 0),
);

const visiblePacks = computed(() => {
  const query = searchQuery.value.trim().toLowerCase();
  if (!query) return resolvedPacks.value;

  return resolvedPacks.value.filter(
    (pack) =>
      pack.name.toLowerCase().includes(query) ||
      pack.description.toLowerCase().includes(query) ||
      pack.features.some((feature) => feature.name.toLowerCase().includes(query)),
  );
});

const shopSummary = computed(() => [
  {
    label: 'Feature เดี่ยว',
    value: features.value.length,
    detail: 'เลือกซื้อเฉพาะ module ที่ต้องใช้',
    icon: Sparkles,
  },
  {
    label: 'Pack รายเดือน',
    value: resolvedPacks.value.length,
    detail: 'รวม feature ที่ใช้ร่วมกันบ่อย',
    icon: Layers3,
  },
  {
    label: 'Runtime ready',
    value: features.value.some((feature) => feature.code === 'runtime-247') ? '24/7' : '-',
    detail: 'รองรับ PM2 hosting บน VM',
    icon: Server,
  },
]);

async function loadShop() {
  authStore.loadSession();

  if (!authStore.accessToken) {
    isLoading.value = false;
    return;
  }

  isLoading.value = true;
  errorMessage.value = '';

  try {
    dashboard.value = await getCustomerDashboard(authStore.accessToken);
    selectedBotId.value = selectedBotId.value ?? dashboard.value.bots[0]?.id ?? null;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Could not load shop catalog';
  } finally {
    isLoading.value = false;
  }
}

async function buyFeature(feature: FeatureResponse) {
  await createCheckout([feature.id]);
}

async function buyPack(pack: { code: string; features: FeatureResponse[] }) {
  await createCheckout(pack.features.map((feature) => feature.id), pack.code);
}

async function createCheckout(featureIds: number[], packCode?: string) {
  authStore.loadSession();
  if (!authStore.accessToken || !selectedBotId.value) {
    checkoutMessage.value = 'กรุณาเลือก bot ก่อนเริ่ม checkout';
    return;
  }

  isCheckingOut.value = true;
  errorMessage.value = '';
  checkoutMessage.value = '';

  try {
    const checkout = await createCustomerCheckout(authStore.accessToken, {
      botId: selectedBotId.value,
      packCode,
      featureIds,
    });
    await router.push({ name: 'checkout', params: { paymentId: checkout.paymentId } });
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Could not create payment checkout';
  } finally {
    isCheckingOut.value = false;
  }
}

function formatMoney(cents: number, currency = 'THB') {
  return new Intl.NumberFormat('th-TH', {
    style: 'currency',
    currency,
    maximumFractionDigits: 0,
  }).format(cents / 100);
}

function categoryLabel(category: FeatureCategory) {
  return categoryLabels[category] ?? category;
}

function effectivePrice(feature: FeatureResponse) {
  return feature.promotionPriceCents ?? feature.monthlyPriceCents;
}

function promotionText(feature: FeatureResponse) {
  if (!feature.promotionPriceCents) return feature.promotionLabel ?? 'Featured';
  return feature.promotionLabel ?? 'Promotion';
}

onMounted(loadShop);
</script>

<template>
  <main class="shop-page">
    <section class="page-hero">
      <div>
        <p class="eyebrow">Feature marketplace</p>
        <h1>Shop</h1>
        <p>
          เลือกซื้อ feature เดี่ยวสำหรับบอทที่มีอยู่ หรือเลือกเป็น pack รายเดือนที่รวมระบบสำคัญไว้พร้อมใช้งาน
        </p>
      </div>

      <div class="hero-actions" role="tablist" aria-label="Shop mode">
        <button
          type="button"
          :class="['mode-button', { active: activeMode === 'packs' }]"
          role="tab"
          :aria-selected="activeMode === 'packs'"
          @click="activeMode = 'packs'"
        >
          <PackageCheck class="h-4 w-4" />
          Packs
        </button>
        <button
          type="button"
          :class="['mode-button', { active: activeMode === 'features' }]"
          role="tab"
          :aria-selected="activeMode === 'features'"
          @click="activeMode = 'features'"
        >
          <Sparkles class="h-4 w-4" />
          Features
        </button>
      </div>
    </section>

    <section class="announcement-panel" aria-label="Shop announcements">
      <div class="announcement-icon">
        <Megaphone class="h-5 w-5" />
      </div>
      <div>
        <p class="eyebrow">Shop announcement</p>
        <h2>Launch promotion สำหรับร้านที่เริ่มใช้ Runtime และ Payment เดือนนี้</h2>
        <p>
          Feature ที่ติดป้าย promotion จะใช้ราคาพิเศษในหน้า Shop และถูกนำไปคำนวณราคา Pack อัตโนมัติ
        </p>
      </div>
      <div class="announcement-items">
        <span v-for="feature in featuredPromotions" :key="feature.code">
          {{ feature.name }} · {{ formatMoney(effectivePrice(feature), feature.currency) }}
        </span>
      </div>
    </section>

    <section class="summary-grid" aria-label="Shop summary">
      <article v-for="item in shopSummary" :key="item.label" class="summary-card">
        <div class="summary-icon">
          <component :is="item.icon" class="h-5 w-5" />
        </div>
        <div>
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <p>{{ item.detail }}</p>
        </div>
      </article>
    </section>

    <section v-if="errorMessage" class="notice-panel error-panel">
      <CircleAlert class="h-5 w-5" />
      <div>
        <strong>{{ isCheckingOut ? 'สร้าง Checkout ไม่สำเร็จ' : 'โหลด Shop ไม่สำเร็จ' }}</strong>
        <span>{{ errorMessage }}</span>
      </div>
      <button type="button" class="icon-button" aria-label="Retry shop catalog" @click="loadShop">
        <RefreshCw class="h-4 w-4" />
      </button>
    </section>

    <section class="toolbar" aria-label="Shop filters">
      <label class="bot-selector">
        <Bot class="h-4 w-4" />
        <select v-model="selectedBotId">
          <option :value="null" disabled>เลือก bot สำหรับ checkout</option>
          <option v-for="botInfo in bots" :key="botInfo.id" :value="botInfo.id">
            {{ botInfo.name }}
          </option>
        </select>
      </label>

      <label class="search-box">
        <Search class="h-4 w-4" />
        <input v-model="searchQuery" type="search" placeholder="Search features, packs, payments..." />
      </label>

      <div v-if="activeMode === 'features'" class="category-tabs" aria-label="Feature categories">
        <button type="button" :class="{ active: activeCategory === 'ALL' }" @click="activeCategory = 'ALL'">
          All
        </button>
        <button
          v-for="category in categories"
          :key="category"
          type="button"
          :class="{ active: activeCategory === category }"
          @click="activeCategory = category"
        >
          {{ categoryLabel(category) }}
        </button>
      </div>
    </section>

    <section v-if="isLoading" class="notice-panel">
      <RefreshCw class="h-5 w-5 animate-spin" />
      <div>
        <strong>กำลังโหลดสินค้า</strong>
        <span>กำลังดึง feature catalog และ pack ที่พร้อมขาย</span>
      </div>
    </section>

    <section v-if="activeMode === 'packs'" class="pack-grid" aria-label="Feature packs">
      <article v-for="pack in visiblePacks" :key="pack.code" class="pack-card">
        <div class="pack-topline">
          <span>{{ pack.badge }}</span>
          <PackageCheck class="h-5 w-5" />
        </div>

        <h2>{{ pack.name }}</h2>
        <p>{{ pack.description }}</p>

        <div class="pack-price">
          <strong>{{ formatMoney(pack.packPriceCents, pack.currency) }}</strong>
          <span>/ เดือน</span>
        </div>
        <p class="discount-line">ประหยัด {{ formatMoney(pack.discountCents, pack.currency) }} จากการซื้อแยก</p>

        <ul>
          <li v-for="feature in pack.features" :key="feature.code">
            <Check class="h-4 w-4" />
            <span>{{ feature.name }}</span>
          </li>
        </ul>

        <button type="button" class="primary-action" :disabled="isCheckingOut || !selectedBotId" @click="buyPack(pack)">
          <ShoppingCart class="h-4 w-4" />
          เลือก Pack นี้
        </button>
      </article>
    </section>

    <section v-else class="feature-list" aria-label="Single features">
      <article v-for="feature in filteredFeatures" :key="feature.code" class="feature-row">
        <div class="feature-icon">
          <Bot v-if="feature.category === 'RUNTIME'" class="h-5 w-5" />
          <CreditCard v-else-if="feature.category === 'PAYMENT'" class="h-5 w-5" />
          <Zap v-else class="h-5 w-5" />
        </div>

        <div class="feature-copy">
          <div>
            <span>{{ categoryLabel(feature.category) }}</span>
            <h2>{{ feature.name }}</h2>
          </div>
          <p>{{ feature.description }}</p>
        </div>

        <div class="feature-buy">
          <div v-if="feature.promotionPriceCents" class="promo-tag">
            {{ promotionText(feature) }}
          </div>
          <strong>{{ formatMoney(effectivePrice(feature), feature.currency) }}</strong>
          <del v-if="feature.promotionPriceCents">{{ formatMoney(feature.monthlyPriceCents, feature.currency) }}</del>
          <span>/ เดือน</span>
          <button type="button" class="secondary-action" :disabled="isCheckingOut || !selectedBotId" @click="buyFeature(feature)">
            <ShoppingCart class="h-4 w-4" />
            เพิ่ม
          </button>
        </div>
      </article>
    </section>

    <section
      v-if="!isLoading && ((activeMode === 'packs' && visiblePacks.length === 0) || (activeMode === 'features' && filteredFeatures.length === 0))"
      class="empty-state"
    >
      <Search class="h-8 w-8" />
      <h2>ไม่พบสินค้าที่ค้นหา</h2>
      <p>ลองเปลี่ยนคำค้นหรือเลือกหมวดหมู่อื่น</p>
    </section>
  </main>
</template>

<style scoped>
.shop-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
  width: min(100%, 1320px);
  margin: 0 auto;
  padding: 2rem 1rem 3rem;
  color: var(--color-text-primary);
}

.page-hero,
.announcement-panel,
.toolbar,
.notice-panel {
  border: 1px solid var(--color-border);
  background: color-mix(in srgb, var(--color-surface) 94%, transparent);
  box-shadow: var(--shadow-soft);
  backdrop-filter: blur(18px);
}

.page-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 1rem;
  min-height: 220px;
  padding: 1.5rem;
  border-radius: 8px;
}

.page-hero h1 {
  margin: 0.25rem 0 0;
  font-size: clamp(2.1rem, 5vw, 3.5rem);
  font-weight: 900;
  line-height: 1;
  letter-spacing: 0;
}

.page-hero p {
  max-width: 720px;
  margin: 0.7rem 0 0;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.eyebrow {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0;
  text-transform: uppercase;
}

.hero-actions,
.category-tabs {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.announcement-panel {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) minmax(16rem, auto);
  gap: 1rem;
  align-items: center;
  padding: 1rem;
  border-radius: 8px;
}

.announcement-icon {
  display: grid;
  place-items: center;
  width: 2.6rem;
  height: 2.6rem;
  border-radius: 8px;
  background: var(--color-surface-muted);
  color: var(--color-secondary);
}

.announcement-panel h2 {
  margin: 0.2rem 0 0;
  font-size: 1.05rem;
  font-weight: 900;
}

.announcement-panel p:last-child {
  margin: 0.35rem 0 0;
  color: var(--color-text-secondary);
  line-height: 1.55;
}

.announcement-items {
  display: grid;
  gap: 0.5rem;
}

.announcement-items span {
  border: 1px solid var(--color-border);
  border-radius: 999px;
  padding: 0.4rem 0.7rem;
  background: var(--color-surface-muted);
  color: var(--color-text-secondary);
  font-size: 0.78rem;
  font-weight: 850;
  white-space: nowrap;
}

.mode-button,
.category-tabs button,
.primary-action,
.secondary-action,
.icon-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 0;
  border-radius: 8px;
  font-weight: 800;
  cursor: pointer;
  transition: transform 160ms ease, border-color 160ms ease, background 160ms ease;
}

.mode-button,
.category-tabs button {
  min-height: 38px;
  padding: 0 14px;
  border: 1px solid var(--color-border);
  color: var(--color-text-secondary);
  background: var(--color-surface-muted);
}

.mode-button.active,
.category-tabs button.active {
  color: var(--color-surface);
  background: var(--color-primary);
  border-color: transparent;
}

.summary-grid,
.pack-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.summary-card,
.pack-card,
.feature-row,
.empty-state {
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: color-mix(in srgb, var(--color-surface) 94%, transparent);
  box-shadow: var(--shadow-soft);
}

.summary-card {
  display: flex;
  gap: 14px;
  min-height: 118px;
  padding: 18px;
}

.summary-icon,
.feature-icon {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  flex: 0 0 auto;
  border-radius: 8px;
  background: var(--color-surface-muted);
  color: var(--color-secondary);
}

.summary-card span,
.feature-copy span {
  color: var(--color-text-muted);
  font-size: 12px;
  font-weight: 800;
  text-transform: uppercase;
}

.summary-card strong {
  display: block;
  margin-top: 4px;
  font-size: 28px;
}

.summary-card p,
.feature-copy p,
.discount-line {
  margin: 6px 0 0;
  color: var(--color-text-muted);
  line-height: 1.55;
}

.notice-panel {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  border-radius: 8px;
}

.notice-panel strong,
.notice-panel span {
  display: block;
}

.notice-panel span {
  color: var(--color-text-muted);
  margin-top: 2px;
}

.error-panel {
  border-color: color-mix(in srgb, var(--color-error) 38%, var(--color-border));
  color: var(--color-error);
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px;
  border-radius: 8px;
}

.bot-selector,
.search-box {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 42px;
  padding: 0 12px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  color: var(--color-secondary);
  background: var(--color-surface);
}

.bot-selector {
  flex: 0 1 280px;
}

.search-box {
  flex: 1 1 320px;
}

.bot-selector select,
.search-box input {
  width: 100%;
  border: 0;
  outline: 0;
  color: var(--color-text-primary);
  background: transparent;
  font: inherit;
}

.search-box input::placeholder {
  color: var(--color-text-muted);
}

.pack-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 460px;
  padding: 1rem;
}

.pack-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--color-secondary);
}

.pack-topline span {
  padding: 6px 10px;
  border-radius: 999px;
  color: var(--color-surface);
  background: var(--color-primary);
  font-size: 12px;
  font-weight: 900;
}

.pack-card h2,
.feature-copy h2,
.empty-state h2 {
  margin: 0;
  letter-spacing: 0;
}

.pack-card p {
  margin: 0;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.pack-price {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.pack-price strong {
  font-size: 1.45rem;
}

.pack-price span,
.feature-buy span {
  color: var(--color-text-muted);
}

.discount-line {
  color: var(--color-success);
}

.pack-card ul {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.pack-card li {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  color: var(--color-text-primary);
}

.pack-card li svg {
  color: var(--color-success);
  flex: 0 0 auto;
  margin-top: 2px;
}

.primary-action,
.secondary-action {
  min-height: 42px;
  padding: 0 16px;
}

.primary-action:disabled,
.secondary-action:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.primary-action {
  margin-top: auto;
  color: var(--color-surface);
  background: var(--color-primary);
}

.secondary-action {
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
  background: var(--color-surface-muted);
}

.icon-button {
  width: 38px;
  height: 38px;
  color: var(--color-text-secondary);
  background: var(--color-surface-muted);
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.feature-row {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) auto;
  gap: 16px;
  align-items: center;
  min-height: 132px;
  padding: 18px;
}

.feature-copy {
  min-width: 0;
}

.feature-buy {
  display: grid;
  grid-template-columns: auto auto;
  gap: 2px 8px;
  align-items: baseline;
  min-width: 190px;
  justify-content: end;
}

.feature-buy strong {
  font-size: 22px;
}

.feature-buy del {
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.promo-tag {
  grid-column: 1 / -1;
  width: fit-content;
  border-radius: 999px;
  padding: 0.25rem 0.55rem;
  background: color-mix(in srgb, var(--color-success) 14%, transparent);
  color: var(--color-success);
  font-size: 0.72rem;
  font-weight: 900;
}

.feature-buy button {
  grid-column: 1 / -1;
  margin-top: 8px;
}

.empty-state {
  display: grid;
  place-items: center;
  gap: 10px;
  min-height: 220px;
  padding: 32px;
  color: var(--color-text-muted);
  text-align: center;
}

.empty-state svg {
  color: var(--color-secondary);
}

.empty-state p {
  margin: 0;
}

@media (max-width: 1100px) {
  .summary-grid,
  .pack-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .page-hero,
  .toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .announcement-panel {
    grid-template-columns: 1fr;
  }

}

@media (max-width: 720px) {
  .page-hero {
    padding: 22px;
  }

  .summary-grid,
  .pack-grid {
    grid-template-columns: 1fr;
  }

  .feature-row {
    grid-template-columns: 1fr;
  }

  .feature-buy {
    justify-content: stretch;
    min-width: 0;
  }

  .feature-buy button {
    width: 100%;
  }
}
</style>
