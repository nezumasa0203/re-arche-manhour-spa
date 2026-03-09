<script setup lang="ts">
import { computed } from 'vue'
import { useWorkHoursStore } from '~/stores/workHours'

const store = useWorkHoursStore()

// ±12ヶ月のオプション生成（計25件）
const monthOptions = computed(() => {
  const options: { label: string; value: string }[] = []
  const [baseYear, baseMonth] = store.yearMonth.split('-').map(Number)
  for (let offset = -12; offset <= 12; offset++) {
    const d = new Date(baseYear, baseMonth - 1 + offset, 1)
    const y = d.getFullYear()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const value = `${y}-${m}`
    const label = `${y}年${d.getMonth() + 1}月`
    options.push({ label, value })
  }
  return options
})

function currentYearMonth(): string {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
}

function prevMonth(): string {
  const [y, m] = store.yearMonth.split('-').map(Number)
  const d = new Date(y, m - 2, 1)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}

function nextMonth(): string {
  const [y, m] = store.yearMonth.split('-').map(Number)
  const d = new Date(y, m, 1)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}

function onMonthChange(value: string) {
  store.changeMonth(value)
}

function onPrevMonth() {
  store.changeMonth(prevMonth())
}

function onNextMonth() {
  store.changeMonth(nextMonth())
}

function onReset() {
  store.changeMonth(currentYearMonth())
}

function onSearch() {
  store.fetchRecords()
}

function onCancelDaiko() {
  store.switchDaiko(null)
}
</script>

<template>
  <div class="search-panel">
    <div class="search-panel__row">
      <!-- 担当者表示 -->
      <div class="search-panel__field">
        <label>担当者</label>
        <InputText
          data-testid="staff-display"
          :model-value="store.staffName ?? ''"
          readonly
        />
        <template v-if="store.isDaiko">
          <Tag
            data-testid="daiko-badge"
            value="代行中"
            severity="warning"
          />
          <Button
            data-testid="daiko-cancel"
            label="解除"
            severity="secondary"
            size="small"
            @click="onCancelDaiko"
          />
        </template>
      </div>

      <!-- 年月セレクター -->
      <div class="search-panel__field">
        <label>年月</label>
        <Button
          data-testid="prev-month"
          icon="pi pi-angle-double-left"
          severity="secondary"
          text
          @click="onPrevMonth"
        />
        <Dropdown
          data-testid="month-selector"
          :model-value="store.yearMonth"
          :options="monthOptions"
          option-label="label"
          option-value="value"
          @update:model-value="onMonthChange"
        />
        <Button
          data-testid="next-month"
          icon="pi pi-angle-double-right"
          severity="secondary"
          text
          @click="onNextMonth"
        />
      </div>

      <!-- 検索・リセット -->
      <div class="search-panel__actions">
        <Button
          data-testid="search-btn"
          label="検索"
          icon="pi pi-search"
          @click="onSearch"
        />
        <Button
          data-testid="reset-btn"
          label="リセット"
          icon="pi pi-refresh"
          severity="secondary"
          @click="onReset"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.search-panel {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--surface-border, #dee2e6);
}

.search-panel__row {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.search-panel__field {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.search-panel__field label {
  font-weight: 600;
  white-space: nowrap;
}

.search-panel__actions {
  display: flex;
  gap: 0.5rem;
  margin-left: auto;
}
</style>
