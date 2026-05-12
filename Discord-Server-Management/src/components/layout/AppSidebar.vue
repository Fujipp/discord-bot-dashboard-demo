<template>
  <header
    class="fixed left-0 top-0 z-50 flex h-16 w-full items-center justify-between border-b px-4 shadow-sm md:hidden"
    :style="{
      backgroundColor: 'var(--color-surface)',
      borderColor: 'var(--color-border)',
      color: 'var(--color-text-primary)',
    }"
  >
    <RouterLink
      to="/"
      class="text-xl font-bold transition hover:opacity-80"
      :style="{ color: 'var(--color-text-secondary)' }"
    >
      FUJIPP
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
      'fixed left-0 top-0 z-50 flex h-dvh w-72 max-w-[84vw] flex-col overflow-y-auto border-r px-5 py-5 shadow-xl transition-transform duration-200 md:translate-x-0',
      isMobileOpen ? 'translate-x-0' : '-translate-x-full md:translate-x-0',
    ]"
    :style="{
      backgroundColor: 'var(--color-surface)',
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
        FUJIPP
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

    <section
      class="mt-5 rounded-md border p-3"
      :style="{ borderColor: 'var(--color-border)', backgroundColor: 'var(--color-surface-elevated)' }"
    >
      <div class="flex items-center gap-3">
        <div
          v-if="!authStore.user?.avatarUrl"
          class="flex h-10 w-10 items-center justify-center rounded-full"
          :style="{ backgroundColor: 'var(--color-surface-muted)', color: 'var(--color-primary)' }"
        >
          <UserRound class="h-5 w-5" />
        </div>
        <img
          v-else
          :src="authStore.user.avatarUrl"
          alt=""
          class="h-10 w-10 rounded-full object-cover"
        />
        <div class="min-w-0">
          <p class="truncate text-sm font-semibold">
            {{ authStore.displayName }}
          </p>
          <p class="truncate text-xs" :style="{ color: 'var(--color-text-muted)' }">
            {{ authStore.displayEmail }}
          </p>
        </div>
      </div>

      <RouterLink
        v-if="!authStore.isAuthenticated"
        to="/login"
        class="mt-3 flex w-full items-center justify-center gap-2 rounded-md px-3 py-2 text-sm font-semibold transition hover:opacity-90"
        :style="{ backgroundColor: 'var(--color-primary)', color: 'var(--color-surface)' }"
        @click="emit('close-mobile')"
      >
        <LogIn class="h-4 w-4" />
        Login
      </RouterLink>

      <button
        v-else
        type="button"
        class="mt-3 flex w-full items-center justify-center gap-2 rounded-md px-3 py-2 text-sm font-semibold transition hover:opacity-90"
        :style="{ backgroundColor: 'var(--color-primary)', color: 'var(--color-surface)' }"
        @click="logout"
      >
        <LogOut class="h-4 w-4" />
        Logout
      </button>
    </section>

    <nav class="mt-5 flex flex-col gap-2">
      <RouterLink
        v-for="link in navigationItems"
        :key="link.path"
        :to="link.path"
        class="sidebar-link rounded-md px-3 py-3 text-sm font-medium transition"
        active-class="sidebar-link-active"
        @click="emit('close-mobile')"
      >
        {{ link.label }}
      </RouterLink>
    </nav>

    <div class="mt-5 border-t pt-4 md:mt-auto" :style="{ borderColor: 'var(--color-divider)' }">
      <ThemeSwitcher />
    </div>
  </aside>
</template>

<script setup lang="ts">
  import { onMounted } from 'vue';
  import { useRouter } from 'vue-router';
  import { LogIn, LogOut, Menu, UserRound, X } from 'lucide-vue-next';
  import { navigationItems } from '@/config/navigation';
  import { useAuthStore } from '@/stores/authStore';
  import ThemeSwitcher from './ThemeSwitcher.vue';

  defineProps<{ isMobileOpen: boolean }>();
  const emit = defineEmits<{
    (e: 'toggle-mobile'): void;
    (e: 'close-mobile'): void;
  }>();

  const router = useRouter();
  const authStore = useAuthStore();

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
    color: var(--color-text-secondary);
  }

  .sidebar-link:hover,
  .sidebar-link-active {
    background: var(--color-surface-muted);
    color: var(--color-primary);
  }
</style>
