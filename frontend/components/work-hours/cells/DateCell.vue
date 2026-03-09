<script setup lang="ts">
import { ref, computed } from 'vue'
import { useWorkHoursStore } from '~/stores/workHours'
import { validateRequired, validateDateInMonth } from '~/composables/useValidation'

const props = defineProps<{
  recordId: number
  workDate: string
  editable: boolean
  yearMonth: string
}>()

const store = useWorkHoursStore()
const editing = ref(false)
const editValue = ref('')
const error = ref('')

const displayDate = computed(() => {
  if (!props.workDate) return ''
  const parts = props.workDate.split('-')
  return `${parts[1]}/${parts[2]}`
})

function onCellClick() {
  if (!props.editable) return
  editValue.value = props.workDate
  error.value = ''
  editing.value = true
}

async function onBlur() {
  if (!editing.value) return
  editing.value = false
  const value = editValue.value

  // VR-001: 必須チェック
  const reqResult = validateRequired(value, '作業日')
  if (!reqResult.valid) {
    error.value = reqResult.message ?? ''
    return
  }

  // VR-002: 月内範囲チェック
  const dateResult = validateDateInMonth(value, props.yearMonth)
  if (!dateResult.valid) {
    error.value = dateResult.message ?? ''
    return
  }

  error.value = ''
  if (value !== props.workDate) {
    await store.updateField(props.recordId, 'workDate', value)
  }
}
</script>

<template>
  <div class="date-cell" :class="{ 'cell-error': error }">
    <span
      v-if="!editing"
      data-testid="date-display"
      :class="{ clickable: editable }"
      @click="onCellClick"
    >
      {{ displayDate }}
    </span>
    <Calendar
      v-else
      v-model="editValue"
      data-testid="date-input"
      date-format="yy-mm-dd"
      @blur="onBlur"
      @hide="onBlur"
      @date-select="onBlur"
    />
    <span v-if="error" class="error-tooltip">{{ error }}</span>
  </div>
</template>

<style scoped>
.date-cell { position: relative; }
.clickable { cursor: pointer; min-height: 1.2em; display: inline-block; min-width: 2em; }
.cell-error { outline: 2px solid #e74c3c; }
.error-tooltip {
  position: absolute;
  bottom: -1.5rem;
  left: 0;
  font-size: 0.75rem;
  color: #e74c3c;
  white-space: nowrap;
}
</style>
