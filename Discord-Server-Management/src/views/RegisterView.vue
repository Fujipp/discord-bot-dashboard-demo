<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import {
  ArrowRight,
  Calendar,
  Chrome,
  Eye,
  EyeOff,
  Github,
  Lock,
  Mail,
  MessageCircle,
  User,
  UserPlus,
} from 'lucide-vue-next'
import { projectNavigation } from '@/config/navigation'
import {
  loginWithProvider as startSocialLogin,
  register,
  type SocialProvider as SocialProviderId,
} from '@/services/auth'
import { useAuthStore } from '@/stores/authStore'

type SocialProvider = {
  name: string
  id: SocialProviderId
  icon: typeof Github
  className: string
}

const router = useRouter()
const authStore = useAuthStore()
const email = ref('')
const username = ref('')
const password = ref('')
const showPassword = ref(false)
const age = ref<number | null>(null)
const isSubmitting = ref(false)
const errorMessage = ref('')

const socialProviders: SocialProvider[] = [
  { name: 'Discord', id: 'discord', icon: MessageCircle, className: 'discord' },
  { name: 'Google', id: 'google', icon: Chrome, className: 'google' },
  { name: 'GitHub', id: 'github', icon: Github, className: 'github' },
]

function registerWithProvider(provider: SocialProviderId) {
  startSocialLogin(provider)
}

async function submitRegister() {
  errorMessage.value = ''
  isSubmitting.value = true

  try {
    const response = await register({
      email: email.value,
      username: username.value,
      password: password.value,
      age: age.value,
    })

    authStore.setEmailSession(response)
    await router.push('/')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Registration failed'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <section class="auth-page">
    <div class="auth-shell">
      <aside class="auth-panel" aria-label="Create account intro">
        <RouterLink to="/" class="brand-lockup">
          <span class="brand-mark">F</span>
          <span>{{ projectNavigation.name }}</span>
        </RouterLink>

        <div class="panel-copy">
          <p class="eyebrow">Create your bot workspace</p>
          <h1>Start selling and managing Discord bot features.</h1>
          <p>
            Create an account, connect customer bots, and grow from one runtime into paid features,
            packs, and billing workflows.
          </p>
        </div>

        <div class="panel-stats" aria-label="Workspace highlights">
          <span>Runtime</span>
          <span>Features</span>
          <span>Billing</span>
        </div>
      </aside>

      <form class="auth-card" @submit.prevent="submitRegister">
        <div class="mobile-brand">
          <RouterLink to="/" class="brand-lockup">
            <span class="brand-mark">F</span>
            <span>{{ projectNavigation.name }}</span>
          </RouterLink>
        </div>

        <div class="card-heading">
          <UserPlus class="heading-icon" aria-hidden="true" />
          <div>
            <p class="eyebrow">Join {{ projectNavigation.name }}</p>
            <h2>Register</h2>
          </div>
        </div>

        <div class="social-grid" aria-label="Social register options">
          <button
            v-for="provider in socialProviders"
            :key="provider.name"
            type="button"
            class="social-button"
            :class="provider.className"
            @click="registerWithProvider(provider.id)"
          >
            <component :is="provider.icon" class="h-5 w-5" aria-hidden="true" />
            <span>{{ provider.name }}</span>
          </button>
        </div>

        <div class="divider">
          <span>or create with email</span>
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

        <div class="field-grid">
          <label class="field">
            <span>Username</span>
            <div class="input-wrap">
              <User class="field-icon" aria-hidden="true" />
              <input
                v-model="username"
                type="text"
                name="username"
                autocomplete="username"
                placeholder="fujipp"
                minlength="3"
                required
              />
            </div>
          </label>

          <label class="field">
            <span>Age</span>
            <div class="input-wrap">
              <Calendar class="field-icon" aria-hidden="true" />
              <input
                v-model.number="age"
                type="number"
                name="age"
                inputmode="numeric"
                placeholder="18"
                min="13"
                max="120"
                required
              />
            </div>
          </label>
        </div>

        <label class="field">
          <span>Password</span>
          <div class="input-wrap">
            <Lock class="field-icon" aria-hidden="true" />
            <input
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              name="password"
              autocomplete="new-password"
              placeholder="Create a strong password"
              minlength="8"
              required
            />
            <button
              type="button"
              class="password-toggle"
              :aria-label="showPassword ? 'Hide password' : 'Show password'"
              @click="showPassword = !showPassword"
            >
              <EyeOff v-if="showPassword" class="h-4 w-4" aria-hidden="true" />
              <Eye v-else class="h-4 w-4" aria-hidden="true" />
            </button>
          </div>
        </label>

        <label class="check-row terms">
          <input type="checkbox" required />
          <span>I agree to the terms and community management policy.</span>
        </label>

        <button type="submit" class="primary-button" :disabled="isSubmitting">
          <span>{{ isSubmitting ? 'Creating...' : 'Create account' }}</span>
          <ArrowRight class="h-5 w-5" aria-hidden="true" />
        </button>

        <p class="switch-copy">
          Already have an account?
          <RouterLink to="/login">Login</RouterLink>
        </p>
      </form>
    </div>
  </section>
</template>

<style scoped>
.auth-page {
  min-height: 100dvh;
  display: grid;
  place-items: center;
  padding: 1.25rem;
}

.auth-shell {
  width: min(100%, 66rem);
  min-height: min(44rem, calc(100dvh - 2.5rem));
  display: grid;
  grid-template-columns: minmax(18rem, 0.9fr) minmax(23rem, 1.1fr);
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-surface);
  box-shadow: var(--shadow-elevated);
}

.auth-panel {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 1.5rem;
  color: var(--color-surface);
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--color-primary) 36%, #020617), color-mix(in srgb, var(--color-surface-elevated) 58%, #020617)),
    linear-gradient(90deg, color-mix(in srgb, var(--color-secondary) 28%, transparent), transparent 50%);
}

.brand-lockup {
  display: inline-flex;
  width: fit-content;
  align-items: center;
  gap: 0.75rem;
  color: inherit;
  font-size: 0.95rem;
  font-weight: 800;
  text-decoration: none;
}

.brand-mark {
  width: 2.5rem;
  height: 2.5rem;
  display: grid;
  place-items: center;
  border: 1px solid currentColor;
  border-radius: 8px;
  font-size: 1.1rem;
}

.panel-copy {
  max-width: 28rem;
}

.eyebrow {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.75rem;
  font-weight: 800;
  letter-spacing: 0;
  text-transform: uppercase;
}

.auth-panel .eyebrow,
.auth-panel p {
  color: color-mix(in srgb, var(--color-surface) 78%, transparent);
}

.panel-copy h1 {
  margin: 0.85rem 0 1rem;
  color: var(--color-surface);
  font-size: clamp(2.15rem, 4vw, 3.75rem);
  line-height: 0.98;
  font-weight: 850;
  letter-spacing: 0;
}

.panel-copy p {
  margin: 0;
  font-size: 1rem;
  line-height: 1.7;
}

.panel-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.6rem;
}

.panel-stats span {
  min-height: 2.4rem;
  display: grid;
  place-items: center;
  border: 1px solid color-mix(in srgb, var(--color-surface) 22%, transparent);
  border-radius: 8px;
  color: color-mix(in srgb, var(--color-surface) 84%, transparent);
  font-size: 0.78rem;
  font-weight: 750;
}

.auth-card {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 0.9rem;
  padding: clamp(1.5rem, 3.8vw, 3rem);
  background: var(--color-surface);
}

.mobile-brand {
  display: none;
  color: var(--color-text-primary);
}

.card-heading {
  display: flex;
  align-items: center;
  gap: 0.85rem;
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
  font-size: 1.85rem;
  font-weight: 850;
}

.social-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.65rem;
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
    background-color 0.2s ease,
    color 0.2s ease;
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

.field-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(7rem, 9rem);
  gap: 0.85rem;
}

.field {
  display: grid;
  gap: 0.45rem;
  color: var(--color-text-secondary);
  font-size: 0.9rem;
  font-weight: 750;
}

.input-wrap {
  min-height: 3.2rem;
  display: flex;
  align-items: center;
  gap: 0.65rem;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 0 0.9rem;
  background: var(--color-surface-muted);
  color: var(--color-text-primary);
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.input-wrap:focus-within {
  border-color: var(--color-ring);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-ring) 16%, transparent);
}

.field-icon {
  width: 1.1rem;
  height: 1.1rem;
  flex: none;
  color: var(--color-text-muted);
}

.password-toggle {
  width: 2rem;
  height: 2rem;
  display: grid;
  flex: none;
  place-items: center;
  border: 0;
  border-radius: 8px;
  color: var(--color-text-muted);
  background: transparent;
  transition:
    background-color 0.2s ease,
    color 0.2s ease;
}

.password-toggle:hover {
  color: var(--color-primary);
  background: var(--color-surface);
}

input {
  width: 100%;
  min-width: 0;
  border: 0;
  outline: 0;
  color: var(--color-text-primary);
  background: transparent;
  font: inherit;
}

input::placeholder {
  color: var(--color-text-muted);
}

.check-row {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
}

.check-row input {
  width: 1rem;
  height: 1rem;
  flex: none;
  accent-color: var(--color-secondary);
}

.terms {
  align-items: flex-start;
  gap: 0.55rem;
  color: var(--color-text-secondary);
  font-size: 0.88rem;
  line-height: 1.5;
}

.terms input {
  margin-top: 0.2rem;
}

.switch-copy a {
  color: var(--color-secondary);
  font-weight: 800;
  text-decoration: none;
}

.switch-copy a:hover {
  text-decoration: underline;
}

.primary-button {
  border: 0;
  color: var(--color-surface);
  background: var(--color-primary);
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
  background: color-mix(in srgb, var(--color-error) 12%, transparent);
}

.switch-copy {
  margin: 0.25rem 0 0;
  color: var(--color-text-secondary);
  text-align: center;
}

@media (max-width: 860px) {
  .auth-page {
    display: block;
    min-height: 100dvh;
    padding: 0;
    background: var(--color-surface);
  }

  .auth-shell {
    min-height: 100dvh;
    display: block;
    border: 0;
    border-radius: 0;
    box-shadow: none;
  }

  .auth-panel {
    display: none;
  }

  .auth-card {
    min-height: 100dvh;
    justify-content: flex-start;
    padding: max(1.25rem, env(safe-area-inset-top)) 1.1rem max(1.25rem, env(safe-area-inset-bottom));
  }

  .mobile-brand {
    display: block;
    margin-bottom: 0.25rem;
  }
}

@media (max-width: 560px) {
  .social-grid,
  .field-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 420px) {
  .card-heading h2 {
    font-size: 1.6rem;
  }

  .social-button span {
    min-width: 0;
  }
}
</style>
