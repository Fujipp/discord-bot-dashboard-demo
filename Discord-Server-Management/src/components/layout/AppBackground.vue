<template>
  <div class="app-background" aria-hidden="true" />
</template>

<style scoped>
.app-background {
  position: fixed;
  inset: 0;
  z-index: -1;
  width: 100%;
  height: 100%;
  pointer-events: none;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--color-surface-muted) 28%, transparent), transparent 36rem),
    linear-gradient(90deg, color-mix(in srgb, var(--color-border) 22%, transparent) 1px, transparent 1px),
    linear-gradient(180deg, color-mix(in srgb, var(--color-border) 18%, transparent) 1px, transparent 1px),
    var(--color-background);
  background-size: auto, 72px 72px, 72px 72px, auto;
  transition: background 0.35s ease;
}
</style>
