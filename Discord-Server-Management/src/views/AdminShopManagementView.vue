<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { CircleAlert, RefreshCw, Save, Search, ShoppingBag, Tags } from 'lucide-vue-next';
import {
  getAdminShopFeatures,
  updateAdminShopFeature,
  type AdminShopFeaturePayload,
  type FeatureCategory,
  type FeatureResponse,
} from '@/services/customer';
import { useAuthStore } from '@/stores/authStore';

type FeatureDraft = {
  name: string;
  description: string;
  monthlyPriceBaht: number;
  currency: string;
  category: FeatureCategory;
  promotionLabel: string;
  promotionPriceBaht: number;
  promotionEndsAt: string;
  featured: boolean;
  sortOrder: number;
  active: boolean;
};

const authStore = useAuthStore();
const features = ref<FeatureResponse[]>([]);
const drafts = ref<Record<number, FeatureDraft>>({});
const isLoading = ref(true);
const errorMessage = ref('');
const savingFeatureId = ref<number | null>(null);
const searchQuery = ref('');

const filteredFeatures = computed(() => {
  const query = searchQuery.value.trim().toLowerCase();
  if (!query) return features.value;

  return features.value.filter(
    (feature) =>
      feature.name.toLowerCase().includes(query) ||
      feature.code.toLowerCase().includes(query) ||
      feature.category.toLowerCase().includes(query),
  );
});

const summary = computed(() => {
  const active = features.value.filter((feature) => feature.active).length;
  const promoted = features.value.filter((feature) => feature.promotionPriceCents).length;
  const featured = features.value.filter((feature) => feature.featured).length;

  return [
    { label: 'Active features', value: active, detail: `${features.value.length} total`, icon: ShoppingBag },
    { label: 'Promotions', value: promoted, detail: 'Has promotion price', icon: Tags },
    { label: 'Featured', value: featured, detail: 'Shown with priority', icon: Save },
  ];
});

async function loadFeatures() {
  authStore.loadSession();
  if (!authStore.accessToken) {
    isLoading.value = false;
    return;
  }

  isLoading.value = true;
  errorMessage.value = '';

  try {
    features.value = await getAdminShopFeatures(authStore.accessToken);
    syncDrafts();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Could not load shop management';
  } finally {
    isLoading.value = false;
  }
}

async function saveFeature(feature: FeatureResponse) {
  if (!authStore.accessToken) return;
  const draft = drafts.value[feature.id];
  if (!draft) return;

  savingFeatureId.value = feature.id;
  errorMessage.value = '';

  const payload: AdminShopFeaturePayload = {
    name: draft.name,
    description: draft.description,
    monthlyPriceCents: Math.round(Number(draft.monthlyPriceBaht || 0) * 100),
    currency: draft.currency || 'THB',
    category: draft.category,
    promotionLabel: draft.promotionLabel.trim() || null,
    promotionPriceCents: draft.promotionPriceBaht > 0 ? Math.round(Number(draft.promotionPriceBaht) * 100) : null,
    promotionEndsAt: draft.promotionEndsAt ? new Date(draft.promotionEndsAt).toISOString() : null,
    featured: draft.featured,
    sortOrder: Math.max(0, Number(draft.sortOrder || 0)),
    active: draft.active,
  };

  try {
    const updatedFeature = await updateAdminShopFeature(authStore.accessToken, feature.id, payload);
    features.value = features.value.map((item) => (item.id === updatedFeature.id ? updatedFeature : item));
    syncDraft(updatedFeature);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Could not save feature';
  } finally {
    savingFeatureId.value = null;
  }
}

function syncDrafts() {
  for (const feature of features.value) {
    syncDraft(feature);
  }
}

function syncDraft(feature: FeatureResponse) {
  drafts.value = {
    ...drafts.value,
    [feature.id]: {
      name: feature.name,
      description: feature.description,
      monthlyPriceBaht: feature.monthlyPriceCents / 100,
      currency: feature.currency,
      category: feature.category,
      promotionLabel: feature.promotionLabel ?? '',
      promotionPriceBaht: feature.promotionPriceCents ? feature.promotionPriceCents / 100 : 0,
      promotionEndsAt: feature.promotionEndsAt ? feature.promotionEndsAt.slice(0, 16) : '',
      featured: feature.featured,
      sortOrder: feature.sortOrder,
      active: feature.active,
    },
  };
}

function featureDraft(featureId: number) {
  return drafts.value[featureId];
}

function formatMoney(cents: number, currency = 'THB') {
  return new Intl.NumberFormat('th-TH', {
    style: 'currency',
    currency,
    maximumFractionDigits: 0,
  }).format(cents / 100);
}

onMounted(loadFeatures);
</script>

<template>
  <main class="admin-shop-page">
    <section class="page-hero">
      <div>
        <p class="eyebrow">Admin Shop</p>
        <h1>Shop Management</h1>
        <p>จัดการราคา feature, promotion, featured item, active status และลำดับการแสดงผลบนหน้า Shop</p>
      </div>

      <button type="button" class="primary-action" :disabled="isLoading" @click="loadFeatures">
        <RefreshCw :class="['h-4 w-4', isLoading ? 'animate-spin' : '']" />
        Refresh
      </button>
    </section>

    <section class="summary-grid" aria-label="Shop management summary">
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

    <section v-if="errorMessage" class="notice-panel">
      <CircleAlert class="h-5 w-5" />
      <span>{{ errorMessage }}</span>
    </section>

    <section class="toolbar">
      <label class="search-box">
        <Search class="h-4 w-4" />
        <input v-model="searchQuery" type="search" placeholder="Search feature, code, category..." />
      </label>
    </section>

    <section v-if="isLoading" class="notice-panel">
      <RefreshCw class="h-5 w-5 animate-spin" />
      <span>กำลังโหลดข้อมูลสินค้า</span>
    </section>

    <section class="feature-grid">
      <article v-for="feature in filteredFeatures" :key="feature.id" class="feature-card">
        <div class="card-topline">
          <div>
            <span class="code-label">{{ feature.code }}</span>
            <h2>{{ drafts[feature.id]?.name ?? feature.name }}</h2>
          </div>
          <span :class="['status-pill', drafts[feature.id]?.active ? 'online' : 'offline']">
            {{ drafts[feature.id]?.active ? 'Active' : 'Hidden' }}
          </span>
        </div>

        <div class="price-line">
          <strong>{{ formatMoney(feature.promotionPriceCents ?? feature.monthlyPriceCents, feature.currency) }}</strong>
          <span v-if="feature.promotionPriceCents">Promo from {{ formatMoney(feature.monthlyPriceCents, feature.currency) }}</span>
          <span v-else>Standard price</span>
        </div>

        <div v-if="featureDraft(feature.id)" class="form-grid">
          <label>
            Name
            <input v-model="featureDraft(feature.id)!.name" type="text" />
          </label>

          <label>
            Category
            <select v-model="featureDraft(feature.id)!.category">
              <option value="SHOP">Shop</option>
              <option value="PAYMENT">Payment</option>
              <option value="ROBLOX">Roblox</option>
              <option value="ENGAGEMENT">Engagement</option>
              <option value="RUNTIME">Runtime</option>
              <option value="ADMIN">Admin</option>
              <option value="AUTOMATION">Automation</option>
              <option value="SUPPORT">Support</option>
            </select>
          </label>

          <label class="full">
            Description
            <textarea v-model="featureDraft(feature.id)!.description" rows="3" />
          </label>

          <label>
            Price / month
            <input v-model.number="featureDraft(feature.id)!.monthlyPriceBaht" type="number" min="0" step="1" />
          </label>

          <label>
            Promo price
            <input v-model.number="featureDraft(feature.id)!.promotionPriceBaht" type="number" min="0" step="1" />
          </label>

          <label>
            Promo label
            <input v-model="featureDraft(feature.id)!.promotionLabel" type="text" placeholder="Launch price, Popular..." />
          </label>

          <label>
            Promo ends
            <input v-model="featureDraft(feature.id)!.promotionEndsAt" type="datetime-local" />
          </label>

          <label>
            Sort order
            <input v-model.number="featureDraft(feature.id)!.sortOrder" type="number" min="0" step="1" />
          </label>

          <div class="toggle-row">
            <label>
              <input v-model="featureDraft(feature.id)!.featured" type="checkbox" />
              Featured
            </label>
            <label>
              <input v-model="featureDraft(feature.id)!.active" type="checkbox" />
              Active
            </label>
          </div>
        </div>

        <button
          type="button"
          class="primary-action"
          :disabled="savingFeatureId === feature.id"
          @click="saveFeature(feature)"
        >
          <Save class="h-4 w-4" />
          Save pricing
        </button>
      </article>
    </section>
  </main>
</template>

<style scoped>
.admin-shop-page {
  width: min(100%, 1320px);
  margin: 0 auto;
  padding: 2rem 1rem 3rem;
  color: var(--color-text-primary);
}

.page-hero,
.summary-card,
.toolbar,
.notice-panel,
.feature-card {
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: color-mix(in srgb, var(--color-surface) 94%, transparent);
  box-shadow: var(--shadow-soft);
}

.page-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 1rem;
  min-height: 220px;
  padding: 1.5rem;
}

.eyebrow,
.summary-card span,
.code-label,
.form-grid label {
  color: var(--color-text-muted);
  font-size: 0.76rem;
  font-weight: 850;
  letter-spacing: 0;
  text-transform: uppercase;
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

.page-hero p {
  margin-top: 0.7rem;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1rem;
  margin-top: 1rem;
}

.summary-card {
  display: flex;
  min-height: 8rem;
  gap: 0.8rem;
  padding: 1rem;
}

.summary-card strong {
  display: block;
  margin-top: 0.2rem;
  font-size: 1.5rem;
}

.summary-card p,
.price-line span {
  color: var(--color-text-secondary);
}

.icon-box {
  display: grid;
  place-items: center;
  width: 2.5rem;
  height: 2.5rem;
  flex: 0 0 auto;
  border-radius: 8px;
  background: var(--color-surface-muted);
  color: var(--color-secondary);
}

.toolbar,
.notice-panel {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-top: 1rem;
  padding: 1rem;
}

.notice-panel {
  color: var(--color-error);
}

.search-box {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  width: min(100%, 34rem);
  min-height: 2.5rem;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 0 0.75rem;
  background: var(--color-surface);
  color: var(--color-text-muted);
}

.search-box input {
  width: 100%;
  min-width: 0;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--color-text-primary);
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
  margin-top: 1rem;
}

.feature-card {
  display: grid;
  gap: 1rem;
  padding: 1rem;
}

.card-topline,
.price-line,
.toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
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
  color: var(--color-success);
  background: color-mix(in srgb, var(--color-success) 14%, transparent);
}

.status-pill.offline {
  color: var(--color-text-muted);
}

.price-line {
  justify-content: flex-start;
}

.price-line strong {
  font-size: 1.45rem;
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

.form-grid .full {
  grid-column: 1 / -1;
}

input,
select,
textarea {
  min-width: 0;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-surface);
  color: var(--color-text-primary);
  font-weight: 750;
}

input,
select {
  min-height: 2.4rem;
  padding: 0 0.65rem;
}

textarea {
  resize: vertical;
  padding: 0.65rem;
  line-height: 1.5;
}

.toggle-row {
  justify-content: flex-start;
  align-items: end;
}

.toggle-row label {
  display: inline-flex;
  align-items: center;
  min-height: 2.4rem;
  gap: 0.45rem;
}

.toggle-row input {
  min-height: auto;
  accent-color: var(--color-secondary);
}

.primary-action {
  display: inline-flex;
  min-height: 2.45rem;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  border: 0;
  border-radius: 8px;
  padding: 0 1rem;
  background: var(--color-primary);
  color: var(--color-surface);
  font-weight: 850;
}

.primary-action:disabled {
  cursor: wait;
  opacity: 0.55;
}

@media (max-width: 1040px) {
  .summary-grid,
  .feature-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .page-hero {
    align-items: stretch;
    flex-direction: column;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
