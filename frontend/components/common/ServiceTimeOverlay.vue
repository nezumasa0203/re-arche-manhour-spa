<script setup lang="ts">
/**
 * サービス時間外オーバーレイ (CZ-102)。
 *
 * 画面全体をマスクし、サービス提供時間外であることを表示する。
 * useApi のエラーインターセプターから CZ-102 受信時に自動表示。
 */

defineProps<{
  visible: boolean
  message?: string
}>()
</script>

<template>
  <div v-if="visible" class="service-time-overlay">
    <div class="service-time-overlay__content">
      <div class="service-time-overlay__icon">&#x1f6ab;</div>
      <h2 class="service-time-overlay__title">サービス提供時間外</h2>
      <p class="service-time-overlay__message">
        {{ message || 'サービス提供時間外（6:00〜23:30）のためデータ操作できません' }}
      </p>
      <p class="service-time-overlay__hours">
        サービス提供時間: 6:00 〜 23:30
      </p>
    </div>
  </div>
</template>

<style scoped>
.service-time-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(0, 0, 0, 0.6);
}

.service-time-overlay__content {
  background: white;
  border-radius: 8px;
  padding: 2rem 3rem;
  text-align: center;
  max-width: 480px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.3);
}

.service-time-overlay__icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.service-time-overlay__title {
  font-size: 1.5rem;
  font-weight: bold;
  margin-bottom: 0.5rem;
  color: #c0392b;
}

.service-time-overlay__message {
  font-size: 1rem;
  color: #333;
  margin-bottom: 1rem;
}

.service-time-overlay__hours {
  font-size: 0.875rem;
  color: #666;
}
</style>
