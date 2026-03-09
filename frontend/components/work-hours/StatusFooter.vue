<script setup lang="ts">
import { useWorkHoursStore } from '~/stores/workHours'
import { useConfirmAction } from '~/composables/useConfirmAction'

const store = useWorkHoursStore()
const { confirmByCode } = useConfirmAction()

async function onBatchConfirm() {
  const confirmed = await confirmByCode('CZ-505')
  if (!confirmed) return
  try {
    await store.batchConfirm()
  } catch {
    // エラーは store 側でメッセージ設定済み
  }
}

async function onBatchRevert() {
  const confirmed = await confirmByCode('CZ-518')
  if (!confirmed) return
  try {
    await store.batchRevert()
  } catch {
    // エラーは store 側でメッセージ設定済み
  }
}
</script>

<template>
  <div class="status-footer">
    <div class="status-footer__actions">
      <Button
        v-if="store.canBatchConfirm"
        data-testid="batch-confirm-btn"
        label="一括確認"
        icon="pi pi-check-circle"
        @click="onBatchConfirm"
      />
      <Button
        v-if="store.canBatchRevert"
        data-testid="batch-revert-btn"
        label="一括作成中"
        icon="pi pi-undo"
        severity="secondary"
        @click="onBatchRevert"
      />
    </div>

    <div class="status-footer__counts">
      <span
        data-testid="count-status-0"
        :class="{ 'text-danger': store.statusCounts['0'] > 0 }"
      >
        作成中: {{ store.statusCounts['0'] }}
      </span>
      <span data-testid="count-status-1">
        確認: {{ store.statusCounts['1'] }}
      </span>
      <span data-testid="count-status-2">
        確定: {{ store.statusCounts['2'] }}
      </span>
    </div>

    <div
      v-if="store.message"
      data-testid="footer-message"
      :class="['status-footer__message', `text-${store.message.type}`]"
    >
      {{ store.message.text }}
    </div>
  </div>
</template>

<style scoped>
.status-footer {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.5rem 1rem;
  border-top: 1px solid var(--surface-border, #dee2e6);
  flex-wrap: wrap;
}

.status-footer__actions {
  display: flex;
  gap: 0.5rem;
}

.status-footer__counts {
  display: flex;
  gap: 1rem;
  font-size: 0.9rem;
}

.status-footer__message {
  margin-left: auto;
  font-size: 0.9rem;
}

.text-danger { color: #e74c3c; font-weight: 600; }
.text-success { color: #27ae60; }
.text-error { color: #e74c3c; }
</style>
