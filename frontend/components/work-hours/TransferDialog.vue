<script setup lang="ts">
import { ref, computed } from 'vue'
import { useWorkHoursStore } from '~/stores/workHours'

const props = defineProps<{
  visible: boolean
  selectedIds: number[]
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'transferred'): void
}>()

const store = useWorkHoursStore()

// 現在月+1 から最大12ヶ月先の月リスト
const monthOptions = computed(() => {
  const [y, m] = store.yearMonth.split('-').map(Number)
  const options: { label: string; value: string }[] = []
  for (let i = 1; i <= 12; i++) {
    const d = new Date(y, m - 1 + i, 1)
    const yy = d.getFullYear()
    const mm = String(d.getMonth() + 1).padStart(2, '0')
    options.push({
      label: `${yy}年${d.getMonth() + 1}月`,
      value: `${yy}-${mm}`,
    })
  }
  return options
})

const selectedMonths = ref<string[]>([])

const canExecute = computed(() => selectedMonths.value.length > 0)

function isMonthSelected(month: string): boolean {
  return selectedMonths.value.includes(month)
}

function toggleMonth(month: string, checked: boolean) {
  if (checked) {
    selectedMonths.value.push(month)
  } else {
    selectedMonths.value = selectedMonths.value.filter(m => m !== month)
  }
}

async function onExecute() {
  if (!canExecute.value) return
  await store.transferNextMonth(props.selectedIds, [...selectedMonths.value])
  emit('transferred')
  emit('update:visible', false)
}

function onCancel() {
  emit('update:visible', false)
}
</script>

<template>
  <Dialog
    :visible="visible"
    header="翌月以降へ転写"
    :modal="true"
    @update:visible="$emit('update:visible', $event)"
  >
    <p>選択レコード: {{ selectedIds.length }}件</p>

    <div class="transfer-months">
      <div v-for="opt in monthOptions" :key="opt.value" class="transfer-month-item">
        <Checkbox
          :model-value="isMonthSelected(opt.value)"
          :binary="true"
          @update:model-value="toggleMonth(opt.value, $event as boolean)"
        />
        <label>{{ opt.label }}</label>
      </div>
    </div>

    <p class="transfer-note">
      ※ カテゴリが対象年度に存在しない場合はカテゴリが空白になります
    </p>

    <template #footer>
      <Button
        data-testid="transfer-exec-btn"
        label="転写実行"
        :disabled="!canExecute"
        @click="onExecute"
      />
      <Button
        label="キャンセル"
        severity="secondary"
        @click="onCancel"
      />
    </template>
  </Dialog>
</template>

<style scoped>
.transfer-months {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin: 1rem 0;
}

.transfer-month-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.transfer-note {
  font-size: 0.85rem;
  color: var(--text-color-secondary, #6c757d);
}
</style>
