<template>
  <header
    class="fixed left-0 top-0 z-50 flex h-16 w-full items-center justify-between border-b px-4 shadow-sm backdrop-blur md:hidden"
    :style="{
      backgroundColor: 'color-mix(in srgb, var(--color-surface) 92%, transparent)',
      borderColor: 'var(--color-border)',
      color: 'var(--color-text-primary)',
    }"
  >
    <RouterLink
      to="/"
      class="text-xl font-bold transition hover:opacity-80"
      :style="{ color: 'var(--color-text-secondary)' }"
    >
      {{ projectNavigation.name }}
    </RouterLink>

    <button
      type="button"
      class="rounded-md p-2 transition hover:bg-[var(--color-surface-muted)]"
      aria-label="Open sidebar"
      @click="emit('toggle-mobile')"
    >
      <Menu class="h-5 w-5" />
    </button>
  </header>

  <button
    v-if="isMobileOpen"
    type="button"
    class="fixed inset-0 z-50 bg-[var(--color-overlay)] md:hidden"
    aria-label="Close sidebar"
    @click="emit('close-mobile')"
  />

  <aside
    :class="[
      'fixed left-0 top-0 z-50 flex h-dvh w-72 max-w-[84vw] flex-col overflow-y-auto border-r px-5 py-5 shadow-xl backdrop-blur transition-transform duration-200 md:translate-x-0',
      isMobileOpen ? 'translate-x-0' : '-translate-x-full md:translate-x-0',
    ]"
    :style="{
      backgroundColor: 'color-mix(in srgb, var(--color-surface) 94%, transparent)',
      borderColor: 'var(--color-border)',
      color: 'var(--color-text-primary)',
    }"
  >
    <div class="flex items-center justify-between">
      <RouterLink
        to="/"
        class="text-xl font-bold transition hover:opacity-80"
        :style="{ color: 'var(--color-text-secondary)' }"
        @click="emit('close-mobile')"
      >
        {{ projectNavigation.name }}
      </RouterLink>

      <button
        type="button"
        class="rounded-md p-2 transition hover:bg-[var(--color-surface-muted)] md:hidden"
        aria-label="Close sidebar"
        @click="emit('close-mobile')"
      >
        <X class="h-5 w-5" />
      </button>
    </div>

    <nav class="mt-6 flex flex-1 flex-col gap-2">
      <RouterLink
        v-for="link in visibleNavigationItems"
        :key="link.path"
        :to="link.path"
        class="sidebar-link rounded-md px-3 py-3 text-sm font-medium transition"
        active-class="sidebar-link-active"
        @click="emit('close-mobile')"
      >
        {{ link.label }}
      </RouterLink>
    </nav>

    <div class="mt-5 flex flex-col gap-3 border-t pt-4" :style="{ borderColor: 'var(--color-divider)' }">
      <ThemeSwitcher />

      <section
        class="account-panel flex items-center gap-3 rounded-md px-2 py-2 transition"
        :style="{ color: 'var(--color-text-primary)' }"
      >
        <div
          v-if="!authStore.user?.avatarUrl"
          class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full"
          :style="{ backgroundColor: 'var(--color-surface-muted)', color: 'var(--color-primary)' }"
        >
          <UserRound class="h-4 w-4" />
        </div>
        <img
          v-else
          :src="authStore.user.avatarUrl"
          alt=""
          class="h-9 w-9 shrink-0 rounded-full object-cover"
        />

        <div class="min-w-0 flex-1">
          <p class="truncate text-sm font-semibold">
            {{ authStore.displayName }}
          </p>
          <p class="truncate text-xs" :style="{ color: 'var(--color-text-muted)' }">
            {{ authStore.displayEmail }}
          </p>
        </div>

        <RouterLink
          v-if="!authStore.isAuthenticated"
          to="/login"
          class="account-action"
          aria-label="Login"
          @click="emit('close-mobile')"
        >
          <LogIn class="h-4 w-4" />
        </RouterLink>

        <button
          v-else
          type="button"
          class="account-action"
          aria-label="Logout"
          @click="logout"
        >
          <LogOut class="h-4 w-4" />
        </button>
      </section>
    </div>
  </aside>
</template>

<script setup lang="ts">
  import { computed, onMounted } from 'vue';
  import { useRouter } from 'vue-router';
  import { LogIn, LogOut, Menu, UserRound, X } from 'lucide-vue-next';
  import { navigationItems, projectNavigation } from '@/config/navigation';
  import { useAuthStore } from '@/stores/authStore';
  import ThemeSwitcher from './ThemeSwitcher.vue';

  defineProps<{ isMobileOpen: boolean }>();
  const emit = defineEmits<{
    (e: 'toggle-mobile'): void;
    (e: 'close-mobile'): void;
  }>();

  const router = useRouter();
  const authStore = useAuthStore();
  const visibleNavigationItems = computed(() =>
    navigationItems.filter((item) => !item.adminOnly || authStore.user?.role === 'ADMIN'),
  );

  onMounted(() => {
    authStore.loadSession();
  });

  async function logout() {
    authStore.clearSession();
    emit('close-mobile');
    await router.push('/login');
  }

</script>

<style scoped>
  .sidebar-link {
    display: flex;
    align-items: center;
    min-height: 2.7rem;
    color: var(--color-text-secondary);
    border: 1px solid transparent;
  }

  .sidebar-link:hover,
  .sidebar-link-active {
    border-color: var(--color-border);
    background: var(--color-surface-muted);
    color: var(--color-primary);
  }

  .account-panel:hover {
    background: var(--color-surface-muted);
  }

  .account-action {
    display: flex;
    height: 2rem;
    width: 2rem;
    flex-shrink: 0;
    align-items: center;
    justify-content: center;
    border-radius: 0.375rem;
    color: var(--color-text-secondary);
    transition:
      background-color 0.2s ease,
      color 0.2s ease;
  }

  .account-action:hover {
    background: var(--color-surface-elevated);
    color: var(--color-primary);
  }
</style>
