import { defineStore } from 'pinia';
import { ref } from 'vue';

type ThemeOption = 'light' | 'dark' | 'system';
const themeOptions: ThemeOption[] = ['light', 'dark', 'system'];

export const useThemeStore = defineStore('theme', () => {
  const theme = ref<ThemeOption>('system');
  let mediaQuery: MediaQueryList | null = null;

  const applyTheme = () => {
    if (typeof window === 'undefined') return;

    const root = document.documentElement;
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;

    const isDark =
      theme.value === 'system' ? prefersDark : theme.value === 'dark';

    root.classList.toggle('dark', isDark);
    root.setAttribute('data-theme', theme.value);
    root.style.colorScheme = isDark ? 'dark' : 'light';
  };

  const setTheme = (value: ThemeOption) => {
    theme.value = value;
    localStorage.setItem('theme', value);
    applyTheme();
  };

  const loadTheme = () => {
    const saved = localStorage.getItem('theme') as ThemeOption | null;
    theme.value = saved && themeOptions.includes(saved) ? saved : 'system';
    applyTheme();
    listenToSystemTheme();
  };

  const handleSystemChange = () => {
    if (theme.value === 'system') applyTheme();
  };

  const listenToSystemTheme = () => {
    if (typeof window === 'undefined' || mediaQuery) return;

    mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
    mediaQuery.addEventListener('change', handleSystemChange);
  };

  return { theme, setTheme, loadTheme, applyTheme };
});
