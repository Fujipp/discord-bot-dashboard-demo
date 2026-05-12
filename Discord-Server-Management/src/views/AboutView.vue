<script setup lang="ts">
import { Bot, CreditCard, ServerCog, ShieldCheck } from 'lucide-vue-next';

const highlights = [
  {
    title: 'Bot hosting',
    detail: 'จัดการ PM2 runtime, owner, ราคา และสถานะของบอทจากหน้า Admin',
    icon: ServerCog,
  },
  {
    title: 'Feature shop',
    detail: 'ขายเป็น feature เดี่ยวหรือ pack รายเดือนตามระบบจริงที่บอทใช้งาน',
    icon: CreditCard,
  },
  {
    title: 'Customer portal',
    detail: 'ลูกค้าดูบอท, feature ที่ใช้อยู่, billing และ health ได้จาก dashboard',
    icon: Bot,
  },
];
</script>

<template>
  <main class="about-page">
    <section class="page-hero">
      <div>
        <p class="eyebrow">About</p>
        <h1>Discord Bot Management</h1>
        <p>
          ระบบนี้ออกแบบเป็น customer portal สำหรับขายและดูแล Discord bot แบบรายเดือน
          ตั้งแต่ runtime บน VM, feature catalog, pack, billing ไปจนถึง admin operations
        </p>
      </div>
      <div class="hero-badge">
        <ShieldCheck class="h-6 w-6" />
        <span>Managed runtime</span>
      </div>
    </section>

    <section class="highlight-grid" aria-label="Project highlights">
      <article v-for="item in highlights" :key="item.title" class="highlight-card">
        <div class="icon-box">
          <component :is="item.icon" class="h-5 w-5" />
        </div>
        <h2>{{ item.title }}</h2>
        <p>{{ item.detail }}</p>
      </article>
    </section>
  </main>
</template>

<style scoped>
.about-page {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  width: min(100%, 1180px);
  margin: 0 auto;
  padding: 2rem 1rem 3rem;
  color: var(--color-text-primary);
}

.page-hero,
.highlight-card {
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: color-mix(in srgb, var(--color-surface) 94%, transparent);
  box-shadow: var(--shadow-soft);
}

.page-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 1.5rem;
  min-height: 220px;
  padding: 1.5rem;
}

.eyebrow {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.75rem;
  font-weight: 850;
  letter-spacing: 0;
  text-transform: uppercase;
}

h1,
h2,
p {
  margin: 0;
}

h1 {
  margin-top: 0.25rem;
  font-size: clamp(2.25rem, 6vw, 4rem);
  font-weight: 900;
  line-height: 0.98;
}

.page-hero p:last-child {
  max-width: 760px;
  margin-top: 0.75rem;
  color: var(--color-text-secondary);
  line-height: 1.65;
}

.hero-badge,
.icon-box {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: var(--color-surface-muted);
  color: var(--color-secondary);
}

.hero-badge {
  gap: 0.5rem;
  min-height: 2.75rem;
  padding: 0 1rem;
  font-weight: 850;
  white-space: nowrap;
}

.highlight-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1rem;
}

.highlight-card {
  min-height: 190px;
  padding: 1rem;
}

.icon-box {
  width: 2.6rem;
  height: 2.6rem;
}

.highlight-card h2 {
  margin-top: 1rem;
  font-size: 1.1rem;
  font-weight: 900;
}

.highlight-card p {
  margin-top: 0.5rem;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

@media (max-width: 860px) {
  .page-hero {
    align-items: stretch;
    flex-direction: column;
  }

  .highlight-grid {
    grid-template-columns: 1fr;
  }
}
</style>
