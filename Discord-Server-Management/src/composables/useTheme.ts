import { computed, onMounted } from 'vue';
import { useThemeStore } from '@/stores/themeStore';

type ThemeOption = 'light' | 'dark' | 'system';

export function useTheme() {
  const themeStore = useThemeStore();
  const theme = computed(() => themeStore.theme);

  const setTheme = (value: ThemeOption) => {
    themeStore.setTheme(value);
  };

  const toggleTheme = () => {
    setTheme(theme.value === 'dark' ? 'light' : 'dark');
  };

  onMounted(() => {
    themeStore.loadTheme();
  });

  return {
    theme,
    setTheme,
    toggleTheme,
  };
}
