<template>
  <div class="flex items-center gap-1.5">
    <button
      v-for="option in options"
      :key="option.value"
      type="button"
      class="theme-option"
      :class="{ 'theme-option-active': themeStore.theme === option.value }"
      @click="setTheme(option.value)"
    >
      <component :is="option.icon" class="h-3.5 w-3.5" />
      <span class="sr-only">{{ option.label }}</span>
    </button>
  </div>
</template>

<script setup lang="ts">
  import { onMounted } from 'vue';
  import { useThemeStore } from '@/stores/themeStore';
  import { Sun, Moon, Monitor } from 'lucide-vue-next';

  type ThemeOption = 'light' | 'dark' | 'system';
  type ThemeOptionItem = {
    label: string;
    value: ThemeOption;
    icon: typeof Sun;
  };

  const themeStore = useThemeStore();

  const options: ThemeOptionItem[] = [
    { label: 'Light', value: 'light', icon: Sun },
    { label: 'Dark', value: 'dark', icon: Moon },
    { label: 'System', value: 'system', icon: Monitor },
  ];

  function setTheme(value: ThemeOption) {
    themeStore.setTheme(value);
  }

  onMounted(() => {
    themeStore.loadTheme();
  });
</script>

<style scoped>
  .theme-option {
    display: flex;
    min-width: 0;
    align-items: center;
    justify-content: center;
    border: 1px solid var(--color-border);
    border-radius: 0.375rem;
    height: 1.875rem;
    width: 1.875rem;
    padding: 0;
    color: var(--color-text-secondary);
    font-size: 0.75rem;
    font-weight: 600;
    transition:
      background-color 0.2s ease,
      border-color 0.2s ease,
      color 0.2s ease;
  }

  .theme-option:hover,
  .theme-option-active {
    background: var(--color-surface-muted);
    border-color: var(--color-primary);
    color: var(--color-primary);
  }
</style>
