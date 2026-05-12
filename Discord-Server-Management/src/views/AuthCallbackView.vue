<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import { CheckCircle2, XCircle } from 'lucide-vue-next';
import { useAuthStore, type AuthSession } from '@/stores/authStore';
import type { AuthUser, SocialProvider } from '@/services/auth';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const status = computed(() => String(route.query.status ?? 'error'));
const provider = computed(() => String(route.query.provider ?? 'social'));
const message = computed(() => normalizeOAuthError(String(route.query.message ?? 'OAuth login failed')));
const isSuccess = computed(() => status.value === 'success');

onMounted(() => {
  if (!isSuccess.value) {
    return;
  }

  authStore.setOAuthSession(createOAuthSession());

  window.setTimeout(() => {
    router.replace('/');
  }, 900);
});

function createOAuthSession(): AuthSession {
  return {
    accessToken: readQuery('token') ?? crypto.randomUUID(),
    provider: isSocialProvider(provider.value) ? provider.value : 'email',
    user: createOAuthUser(),
  };
}

function createOAuthUser(): AuthUser {
  const statusValue = readQuery('userStatus');

  return {
    id: Number(route.query.id ?? 0),
    email: readQuery('email') ?? '',
    username: readQuery('username') ?? 'user',
    age: readQuery('age') ? Number(readQuery('age')) : null,
    avatarUrl: readQuery('avatarUrl'),
    role: readQuery('role') === 'ADMIN' ? 'ADMIN' : 'USER',
    status: statusValue === 'BANNED' ? 'BANNED' : statusValue === 'DISABLED' ? 'DISABLED' : 'ACTIVE',
    emailVerified: readQuery('emailVerified') === 'true',
    createdAt: null,
    updatedAt: null,
  };
}

function readQuery(key: string) {
  const value = route.query[key];
  return typeof value === 'string' && value ? value : null;
}

function isSocialProvider(value: string): value is SocialProvider {
  return ['discord', 'google', 'github'].includes(value);
}

function normalizeOAuthError(value: string) {
  if (value.includes('invalid_client')) {
    return 'OAuth client is invalid. Please check the Client ID and Client Secret in Backend/.env, then restart the backend.';
  }

  if (value.includes('invalid_token_response')) {
    return 'OAuth provider rejected the token request. Check the provider credentials and callback URL.';
  }

  return value;
}
</script>

<template>
  <section class="callback-page">
    <div class="callback-card">
      <CheckCircle2 v-if="isSuccess" class="status-icon success" aria-hidden="true" />
      <XCircle v-else class="status-icon error" aria-hidden="true" />

      <h1>{{ isSuccess ? 'Login successful' : 'Login failed' }}</h1>
      <p>
        {{
          isSuccess
            ? `Connected with ${provider}. Redirecting to dashboard.`
            : message
        }}
      </p>

      <RouterLink :to="isSuccess ? '/' : '/login'" class="primary-link">
        {{ isSuccess ? 'Go to dashboard' : 'Back to login' }}
      </RouterLink>
    </div>
  </section>
</template>

<style scoped>
.callback-page {
  min-height: 100dvh;
  display: grid;
  place-items: center;
  padding: 1rem;
}

.callback-card {
  width: min(100%, 28rem);
  display: grid;
  justify-items: center;
  gap: 1rem;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 2rem;
  background: var(--color-surface);
  box-shadow: var(--shadow-elevated);
  text-align: center;
}

.status-icon {
  width: 3rem;
  height: 3rem;
}

.success {
  color: var(--color-success);
}

.error {
  color: var(--color-error);
}

h1 {
  margin: 0;
  color: var(--color-text-primary);
  font-size: 1.75rem;
  font-weight: 800;
}

p {
  margin: 0;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.primary-link {
  display: inline-flex;
  min-height: 2.75rem;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  padding: 0 1rem;
  color: var(--color-surface);
  background: var(--color-primary);
  font-weight: 800;
  text-decoration: none;
}
</style>
