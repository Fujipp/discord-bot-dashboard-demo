import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { getCurrentUser, type AuthResponse, type AuthUser } from '@/services/auth';

const AUTH_STORAGE_KEY = 'discord_management_auth_session';

export type AuthSession = {
  accessToken: string;
  user: AuthUser;
  provider: 'email' | 'discord' | 'google' | 'github';
};

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(null);
  const user = ref<AuthUser | null>(null);
  const provider = ref<AuthSession['provider']>('email');

  const isAuthenticated = computed(() => Boolean(accessToken.value && user.value));
  const displayName = computed(() => user.value?.username ?? 'Guest User');
  const displayEmail = computed(() => user.value?.email ?? 'Login to manage your account');

  function loadSession() {
    const rawSession = localStorage.getItem(AUTH_STORAGE_KEY);
    if (!rawSession) {
      return;
    }

    try {
      const session = JSON.parse(rawSession) as AuthSession;
      accessToken.value = session.accessToken;
      user.value = session.user;
      provider.value = session.provider;
    } catch {
      clearSession();
    }
  }

  async function refreshUser() {
    if (!accessToken.value) {
      clearSession();
      return;
    }

    try {
      user.value = await getCurrentUser(accessToken.value);
      persistSession();
    } catch {
      clearSession();
    }
  }

  function setEmailSession(response: AuthResponse) {
    setSession({
      accessToken: response.accessToken,
      user: response.user,
      provider: 'email',
    });
  }

  function setOAuthSession(session: AuthSession) {
    setSession(session);
  }

  function setSession(session: AuthSession) {
    accessToken.value = session.accessToken;
    user.value = session.user;
    provider.value = session.provider;
    persistSession();
  }

  function persistSession() {
    if (!accessToken.value || !user.value) {
      return;
    }

    localStorage.setItem(
      AUTH_STORAGE_KEY,
      JSON.stringify({
        accessToken: accessToken.value,
        user: user.value,
        provider: provider.value,
      }),
    );
  }

  function clearSession() {
    accessToken.value = null;
    user.value = null;
    provider.value = 'email';
    localStorage.removeItem(AUTH_STORAGE_KEY);
  }

  return {
    accessToken,
    user,
    provider,
    isAuthenticated,
    displayName,
    displayEmail,
    loadSession,
    refreshUser,
    setEmailSession,
    setOAuthSession,
    clearSession,
  };
});
