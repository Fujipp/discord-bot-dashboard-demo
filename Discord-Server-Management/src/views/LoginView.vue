<script setup lang="ts">
import { ref } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { ArrowRight, Chrome, Github, Lock, Mail, MessageCircle, ShieldCheck } from 'lucide-vue-next';
import {
  login,
  loginWithProvider as startSocialLogin,
  type SocialProvider as SocialProviderId,
} from '@/services/auth';
import { useAuthStore } from '@/stores/authStore';

type SocialProvider = {
  name: string;
  id: SocialProviderId;
  icon: typeof Github;
  className: string;
};

const router = useRouter();
const authStore = useAuthStore();
const email = ref('');
const password = ref('');
const isSubmitting = ref(false);
const errorMessage = ref('');

const socialProviders: SocialProvider[] = [
  { name: 'Discord', id: 'discord', icon: MessageCircle, className: 'discord' },
  { name: 'Google', id: 'google', icon: Chrome, className: 'google' },
  { name: 'GitHub', id: 'github', icon: Github, className: 'github' },
];

function loginWithProvider(provider: SocialProviderId) {
  startSocialLogin(provider);
}

async function submitLogin() {
  errorMessage.value = '';
  isSubmitting.value = true;

  try {
    const response = await login({
      email: email.value,
      password: password.value,
    });

    authStore.setEmailSession(response);
    await router.push(String(router.currentRoute.value.query.redirect ?? '/'));
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Login failed';
  } finally {
    isSubmitting.value = false;
  }
}
</script>

<template>
  <section class="auth-page">
    <div class="auth-shell">
      <aside class="auth-copy" aria-label="Discord server management intro">
        <div class="brand-mark">F</div>
        <p class="eyebrow">Discord Server Management</p>
        <h1>Sign in to keep your community under control.</h1>
        <p class="lead">
          Manage server roles, moderation, member insights, and shop tools from one calm dashboard.
        </p>
      </aside>

      <form class="auth-card" @submit.prevent="submitLogin">
        <div class="card-heading">
          <ShieldCheck class="heading-icon" aria-hidden="true" />
          <div>
            <p class="eyebrow">Welcome back</p>
            <h2>Login</h2>
          </div>
        </div>

        <div class="social-grid" aria-label="Social login options">
          <button
            v-for="provider in socialProviders"
            :key="provider.name"
            type="button"
            class="social-button"
            :class="provider.className"
            @click="loginWithProvider(provider.id)"
          >
            <component :is="provider.icon" class="h-5 w-5" aria-hidden="true" />
            <span>{{ provider.name }}</span>
          </button>
        </div>

        <div class="divider">
          <span>or continue with email</span>
        </div>

        <p v-if="errorMessage" class="auth-alert error">
          {{ errorMessage }}
        </p>

        <label class="field">
          <span>Email</span>
          <div class="input-wrap">
            <Mail class="field-icon" aria-hidden="true" />
            <input
              v-model="email"
              type="email"
              name="email"
              autocomplete="email"
              placeholder="you@example.com"
              required
            />
          </div>
        </label>

        <label class="field">
          <span>Password</span>
          <div class="input-wrap">
            <Lock class="field-icon" aria-hidden="true" />
            <input
              v-model="password"
              type="password"
              name="password"
              autocomplete="current-password"
              placeholder="Enter your password"
              required
            />
          </div>
        </label>

        <div class="form-row">
          <label class="remember">
            <input type="checkbox" />
            <span>Remember me</span>
          </label>
          <a href="#" class="text-link">Forgot password?</a>
        </div>

        <button type="submit" class="primary-button" :disabled="isSubmitting">
          <span>{{ isSubmitting ? 'Logging in...' : 'Login' }}</span>
          <ArrowRight class="h-5 w-5" aria-hidden="true" />
        </button>

        <p class="switch-copy">
          New here?
          <RouterLink to="/register">Create an account</RouterLink>
        </p>
      </form>
    </div>
  </section>
</template>

<style scoped>
.auth-page {
  min-height: calc(100dvh - 4rem);
  display: grid;
  place-items: center;
  padding: 2rem 1rem;
}

.auth-shell {
  width: min(100%, 62rem);
  display: grid;
  grid-template-columns: 0.95fr 1.05fr;
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-surface);
  box-shadow: var(--shadow-elevated);
}

.auth-copy {
  display: flex;
  min-height: 34rem;
  flex-direction: column;
  justify-content: flex-end;
  padding: 2rem;
  color: #ffffff;
  background:
    linear-gradient(160deg, rgb(79 70 229 / 0.9), rgb(15 23 42 / 0.92)),
    radial-gradient(circle at 20% 20%, rgb(184 138 43 / 0.8), transparent 34%);
}

.brand-mark {
  width: 3rem;
  height: 3rem;
  display: grid;
  place-items: center;
  margin-bottom: auto;
  border: 1px solid rgb(255 255 255 / 0.34);
  border-radius: 8px;
  font-size: 1.35rem;
  font-weight: 800;
}

.eyebrow {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0;
  text-transform: uppercase;
}

.auth-copy .eyebrow,
.auth-copy .lead {
  color: rgb(255 255 255 / 0.76);
}

.auth-copy h1 {
  margin: 0.75rem 0 1rem;
  max-width: 25rem;
  font-size: clamp(2rem, 5vw, 3.5rem);
  line-height: 1;
  font-weight: 800;
  letter-spacing: 0;
}

.lead {
  margin: 0;
  max-width: 29rem;
  font-size: 1rem;
  line-height: 1.7;
}

.auth-card {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 2rem;
  background: var(--color-surface);
}

.card-heading {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  margin-bottom: 0.25rem;
}

.heading-icon {
  width: 2.75rem;
  height: 2.75rem;
  padding: 0.7rem;
  border-radius: 8px;
  color: var(--color-secondary);
  background: var(--color-surface-muted);
}

.card-heading h2 {
  margin: 0.2rem 0 0;
  color: var(--color-text-primary);
  font-size: 1.75rem;
  font-weight: 800;
}

.social-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
}

.social-button,
.primary-button {
  min-height: 3rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.55rem;
  border-radius: 8px;
  font-weight: 750;
  transition:
    transform 0.2s ease,
    border-color 0.2s ease,
    background-color 0.2s ease;
}

.social-button {
  border: 1px solid var(--color-border);
  color: var(--color-text-primary);
  background: var(--color-surface);
}

.social-button:hover,
.primary-button:hover {
  transform: translateY(-1px);
}

.social-button.discord {
  color: #5865f2;
}

.social-button.google {
  color: #1a73e8;
}

.social-button.github {
  color: var(--color-text-primary);
}

.divider {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  color: var(--color-text-muted);
  font-size: 0.8rem;
}

.divider::before,
.divider::after {
  content: '';
  height: 1px;
  flex: 1;
  background: var(--color-divider);
}

.field {
  display: grid;
  gap: 0.45rem;
  color: var(--color-text-secondary);
  font-size: 0.9rem;
  font-weight: 700;
}

.input-wrap {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  min-height: 3.2rem;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 0 0.9rem;
  background: var(--color-surface-muted);
  color: var(--color-text-primary);
}

.input-wrap:focus-within {
  border-color: var(--color-ring);
  box-shadow: 0 0 0 3px rgb(79 70 229 / 0.14);
}

.field-icon {
  width: 1.1rem;
  height: 1.1rem;
  flex: none;
  color: var(--color-text-muted);
}

input {
  width: 100%;
  border: 0;
  outline: 0;
  color: var(--color-text-primary);
  background: transparent;
  font: inherit;
}

input::placeholder {
  color: var(--color-text-muted);
}

.form-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  color: var(--color-text-secondary);
  font-size: 0.88rem;
}

.remember {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
}

.remember input {
  width: 1rem;
  height: 1rem;
  accent-color: var(--color-secondary);
}

.text-link,
.switch-copy a {
  color: var(--color-secondary);
  font-weight: 800;
  text-decoration: none;
}

.text-link:hover,
.switch-copy a:hover {
  text-decoration: underline;
}

.primary-button {
  border: 0;
  color: #ffffff;
  background: var(--color-secondary);
}

.primary-button:disabled {
  cursor: not-allowed;
  opacity: 0.7;
  transform: none;
}

.auth-alert {
  margin: 0;
  border-radius: 8px;
  padding: 0.8rem 0.9rem;
  font-size: 0.9rem;
  font-weight: 700;
}

.auth-alert.error {
  color: var(--color-error);
  background: rgb(220 38 38 / 0.1);
}

.switch-copy {
  margin: 0.25rem 0 0;
  color: var(--color-text-secondary);
  text-align: center;
}

@media (max-width: 900px) {
  .auth-shell {
    grid-template-columns: 1fr;
  }

  .auth-copy {
    min-height: auto;
  }
}

@media (max-width: 560px) {
  .auth-page {
    padding: 1rem;
  }

  .auth-copy,
  .auth-card {
    padding: 1.25rem;
  }

  .social-grid {
    grid-template-columns: 1fr;
  }

  .form-row {
    align-items: flex-start;
    flex-direction: column;
    gap: 0.75rem;
  }
}
</style>
