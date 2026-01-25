<script setup lang="ts">
interface Props {
  count?: number
  type?: 'grid' | 'list'
}

withDefaults(defineProps<Props>(), {
  count: 8,
  type: 'grid'
})
</script>

<template>
  <div v-if="type === 'grid'" class="skeleton-grid">
    <div
      v-for="i in count"
      :key="i"
      class="skeleton-card"
      :style="{ animationDelay: `${i * 100}ms` }"
    >
      <div class="skeleton-icon skeleton" />
      <div class="skeleton-name skeleton" />
      <div class="skeleton-meta skeleton" />
    </div>
  </div>

  <div v-else class="skeleton-list">
    <div
      v-for="i in count"
      :key="i"
      class="skeleton-row"
      :style="{ animationDelay: `${i * 50}ms` }"
    >
      <div class="skeleton-row-icon skeleton" />
      <div class="skeleton-row-content">
        <div class="skeleton-row-name skeleton" />
        <div class="skeleton-row-meta skeleton" />
      </div>
      <div class="skeleton-row-size skeleton" />
      <div class="skeleton-row-date skeleton" />
    </div>
  </div>
</template>

<style scoped lang="scss">
@keyframes skeletonPulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.4;
  }
}

.skeleton {
  background: linear-gradient(
    90deg,
    var(--color-border-light) 25%,
    var(--color-surface) 50%,
    var(--color-border-light) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite, skeletonPulse 2s infinite;
}

// Grid skeleton
.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: var(--space-md);
  padding: var(--space-md);
}

.skeleton-card {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: var(--space-md);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-sm);
  opacity: 0;
  animation: fadeIn 0.3s ease forwards;
}

.skeleton-icon {
  width: 64px;
  height: 64px;
  border-radius: var(--radius-md);
}

.skeleton-name {
  width: 80%;
  height: 16px;
  border-radius: var(--radius-sm);
}

.skeleton-meta {
  width: 50%;
  height: 12px;
  border-radius: var(--radius-sm);
}

// List skeleton
.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
  padding: var(--space-sm);
}

.skeleton-row {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-sm) var(--space-md);
  background: var(--card-bg);
  border-radius: var(--radius-md);
  opacity: 0;
  animation: fadeIn 0.3s ease forwards;
}

.skeleton-row-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-sm);
  flex-shrink: 0;
}

.skeleton-row-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}

.skeleton-row-name {
  width: 60%;
  height: 16px;
  border-radius: var(--radius-sm);
}

.skeleton-row-meta {
  width: 30%;
  height: 12px;
  border-radius: var(--radius-sm);
}

.skeleton-row-size {
  width: 60px;
  height: 14px;
  border-radius: var(--radius-sm);
  flex-shrink: 0;
}

.skeleton-row-date {
  width: 100px;
  height: 14px;
  border-radius: var(--radius-sm);
  flex-shrink: 0;
}

@keyframes fadeIn {
  to {
    opacity: 1;
  }
}

@keyframes shimmer {
  0% {
    background-position: -200% 0;
  }
  100% {
    background-position: 200% 0;
  }
}
</style>
