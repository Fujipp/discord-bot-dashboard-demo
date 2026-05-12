<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import { AppNavbar, AppSidebar, AppBackground } from '@/components/layout'
import { useThemeStore } from '@/stores/themeStore'

type NavigationMode = 'navbar' | 'sidebar'

const navigationMode = ref<NavigationMode>('sidebar')
const isMobileSidebarOpen = ref(false)
const themeStore = useThemeStore()
const route = useRoute()

const isAuthPage = computed(() => route.name === 'login' || route.name === 'register')

function toggleMobileSidebar() {
  isMobileSidebarOpen.value = !isMobileSidebarOpen.value
}

function closeMobileSidebar() {
  isMobileSidebarOpen.value = false
}

onMounted(() => {
  themeStore.loadTheme()
})
</script>

<template>
  <AppNavbar v-if="!isAuthPage && navigationMode === 'navbar'" />
  <AppSidebar
    v-else-if="!isAuthPage"
    :is-mobile-open="isMobileSidebarOpen"
    @toggle-mobile="toggleMobileSidebar"
    @close-mobile="closeMobileSidebar"
  />
  <AppBackground />

  <main :class="isAuthPage ? '' : navigationMode === 'sidebar' ? 'pt-16 md:pl-72 md:pt-0' : 'pt-16'">
    <RouterView />
  </main>
</template>

<style scoped>
</style>
