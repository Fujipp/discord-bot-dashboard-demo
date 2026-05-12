<template>
  <transition name="fade">
    <header
      :class="[
        'fixed top-0 left-0 w-full z-50 transition-colors duration-300 h-13',
        isScrolled ? 'bg-[var(--color-surface)] shadow' : 'bg-transparent',
      ]"
    >
      <div
        class="container mx-auto relative flex items-center justify-between px-4 py-3"
      >
        <div class="flex items-center space-x-3">
          <RouterLink
            to="/"
            class="text-xl font-bold hover:opacity-80 transition"
            :style="{ color: 'var(--color-text-secondary)' }"
          >
            FUJIPP
          </RouterLink>
        </div>

        <nav
          class="hidden md:flex space-x-6 items-center absolute left-1/2 transform -translate-x-1/2"
        >
          <RouterLink
            v-for="link in navigationItems"
            :key="link.path"
            :to="link.path"
            class="nav-link relative font-medium"
            active-class="font-bold"
          >
            {{ link.label }}
          </RouterLink>
        </nav>

        <div class="h-6 w-6" />
      </div>
    </header>
  </transition>
</template>

<script setup lang="ts">
  import { ref, onMounted, onUnmounted } from 'vue';
  import { navigationItems } from '@/config/navigation';

  const isScrolled = ref(false);

  const handleScroll = () => {
    isScrolled.value = window.scrollY > 10;
  };

  onMounted(() => {
    window.addEventListener('scroll', handleScroll);
  });
  onUnmounted(() => {
    window.removeEventListener('scroll', handleScroll);
  });
</script>

<style scoped>
  .fade-enter-active,
  .fade-leave-active {
    transition: opacity 0.3s ease;
  }
  .fade-enter-from,
  .fade-leave-to {
    opacity: 0;
  }

  .nav-link {
    color: var(--color-text-secondary);
    position: relative;
    transition: color 0.3s ease;
  }
  .nav-link:hover {
    color: var(--color-primary);
    cursor: pointer;
  }
  .nav-link::after {
    content: '';
    position: absolute;
    bottom: -2px;
    left: 50%;
    transform: translateX(-50%) scaleX(0);
    transform-origin: center;
    height: 2px;
    width: 100%;
    background-color: var(--color-primary);
    transition:
      transform 0.3s ease,
      background-color 0.3s ease;
  }
  .nav-link:hover::after {
    transform: translateX(-50%) scaleX(1);
  }
  .router-link-active {
    font-weight: bold;
  }
</style>
