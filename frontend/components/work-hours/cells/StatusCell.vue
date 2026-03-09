<script setup lang="ts">
import { ref, computed } from 'vue'
import { useWorkHoursStore } from '~/stores/workHours'

const props = defineProps<{
  recordId: number
  status: string
  isManager?: boolean
}>()

const store = useWorkHoursStore()
const editing = ref(false)

const STATUS_LABELS: Record<string, string> = {
  '0': '作成中',
  '1': '確認',
  '2': '確定',
  '9': '非表示',
}

const label = computed(() => STATUS_LABELS[props.status] ?? props.status)

const statusOptions = computed(() => {
  if (props.isManager) {
    return [
      { label: '作成中', value: '0' },
      { label: '確認', value: '1' },
      { label: '確定', value: '2' },
    ]
  }
  // 担当者系列: 0↔1 のみ
  return [
    { label: '作成中', value: '0' },
    { label: '確認', value: '1' },
  ]
})

function onBadgeClick() {
  if (props.status === '9') return
  editing.value = true
}

async function onStatusChange(newValue: string) {
  editing.value = false
  if (newValue !== props.status) {
    await store.updateField(props.recordId, 'status', newValue)
  }
}
</script>

<template>
  <div class="status-cell">
    <span
      v-if="!editing"
      data-testid="status-badge"
      :class="['status-badge', `status-${status}`]"
      @click="onBadgeClick"
    >
      {{ label }}
    </span>
    <Dropdown
      v-else
      data-testid="status-dropdown"
      :model-value="status"
      :options="statusOptions"
      option-label="label"
      option-value="value"
      @update:model-value="onStatusChange"
    />
  </div>
</template>

<style scoped>
.status-badge {
  display: inline-block;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  text-align: center;
  min-width: 3.5rem;
}

.status-0 { background: #FBFBB6; color: #000; }
.status-1 { background: #BDEAAD; color: #000; }
.status-2 { background: #9DBDFE; color: #000; }
.status-9 { background: #5D5D5D; color: #FFF; cursor: default; }
</style>
