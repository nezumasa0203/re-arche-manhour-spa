<script setup lang="ts">
import { ref } from 'vue'
import { useWorkHoursStore } from '~/stores/workHours'
import { parseHoursInput, validateHoursFormat } from '~/composables/useValidation'

const props = defineProps<{
  recordId: number
  hours: string
  editable: boolean
}>()

const store = useWorkHoursStore()
const editing = ref(false)
const editValue = ref('')
const error = ref('')

function onCellClick() {
  if (!props.editable) return
  editValue.value = props.hours
  error.value = ''
  editing.value = true
}

async function onBlur() {
  editing.value = false
  const raw = editValue.value.trim()

  // バリデーション
  const result = validateHoursFormat(raw)
  if (!result.valid) {
    error.value = result.message ?? ''
    return
  }

  // 自動変換
  const parsed = parseHoursInput(raw)!
  error.value = ''

  if (parsed !== props.hours) {
    await store.updateField(props.recordId, 'hours', parsed)
  }
}
</script>

<template>
  <div class="hours-cell" :class="{ 'cell-error': error }">
    <span
      v-if="!editing"
      data-testid="hours-display"
      :class="{ clickable: editable }"
      @click="onCellClick"
    >
      {{ hours }}
    </span>
    <InputText
      v-else
      v-model="editValue"
      data-testid="hours-input"
      placeholder="HH:MM"
      @blur="onBlur"
    />
    <span v-if="error" class="error-tooltip">{{ error }}</span>
  </div>
</template>

<style scoped>
.hours-cell { position: relative; }
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
