<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { ShieldCheck, UserRound } from 'lucide-vue-next';
import { useAuthStore } from '@/stores/authStore';

const authStore = useAuthStore();

const tokenPreview = computed(() => {
  if (!authStore.accessToken) {
    return 'No token';
  }

  return `${authStore.accessToken.slice(0, 16)}...`;
});

onMounted(() => {
  authStore.loadSession();
});
</script>

<template>
  <main class="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
    <section class="dashboard-panel">
      <div class="flex items-center gap-3">
        <div class="icon-box">
          <UserRound class="h-5 w-5" />
        </div>
        <div>
          <p class="eyebrow">Signed in as</p>
          <h1>{{ authStore.displayName }}</h1>
        </div>
      </div>

      <div class="details-grid">
        <div>
          <span>Email</span>
          <strong>{{ authStore.displayEmail }}</strong>
        </div>
        <div>
          <span>Role</span>
          <strong>{{ authStore.user?.role ?? 'USER' }}</strong>
        </div>
        <div>
          <span>Provider</span>
          <strong>{{ authStore.provider }}</strong>
        </div>
        <div>
          <span>Token</span>
          <strong>{{ tokenPreview }}</strong>
        </div>
      </div>

      <div class="status-line">
        <ShieldCheck class="h-4 w-4" />
        <span>Navigation guard is active for this dashboard.</span>
      </div>
    </section>
  </main>
</template>

<style scoped>
.dashboard-panel {
  display: grid;
  gap: 1.5rem;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 1.5rem;
  background: var(--color-surface);
  color: var(--color-text-primary);
  box-shadow: var(--shadow-soft);
}

.icon-box {
  width: 2.75rem;
  height: 2.75rem;
  display: grid;
  place-items: center;
  border-radius: 8px;
  color: var(--color-secondary);
  background: var(--color-surface-muted);
}

.eyebrow {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.75rem;
  font-weight: 800;
  text-transform: uppercase;
}

h1 {
  margin: 0.15rem 0 0;
  font-size: 1.6rem;
  font-weight: 850;
}

.details-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.85rem;
}

.details-grid div {
  display: grid;
  gap: 0.25rem;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 1rem;
  background: var(--color-surface-muted);
}

.details-grid span {
  color: var(--color-text-muted);
  font-size: 0.78rem;
  font-weight: 800;
  text-transform: uppercase;
}

.details-grid strong {
  min-width: 0;
  overflow-wrap: anywhere;
}

.status-line {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  color: var(--color-success);
  font-weight: 800;
}

@media (max-width: 900px) {
  .details-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .details-grid {
    grid-template-columns: 1fr;
  }
}
</style>
