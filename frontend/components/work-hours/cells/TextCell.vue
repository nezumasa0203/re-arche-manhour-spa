<script setup lang="ts">
import { ref } from 'vue'
import { useWorkHoursStore } from '~/stores/workHours'
import { validateFixedLength } from '~/composables/useValidation'

const props = defineProps<{
  recordId: number
  field: 'tmrNo' | 'workRequestNo' | 'workRequesterName'
  value: string
  editable: boolean
}>()

const store = useWorkHoursStore()
const editing = ref(false)
const editValue = ref('')
const error = ref('')

function onCellClick() {
  if (!props.editable) return
  editValue.value = props.value
  error.value = ''
  editing.value = true
}

function validate(value: string): string | null {
  if (props.field === 'tmrNo') {
    if (value && value.length > 5) {
      return 'TMR番号は5文字以内で入力してください'
    }
  } else if (props.field === 'workRequestNo') {
    if (value) {
      const result = validateFixedLength(value, 7, '作業依頼書No')
      if (!result.valid) return result.message ?? ''
    }
  } else if (props.field === 'workRequesterName') {
    if (value && value.length > 40) {
      return '作業依頼者名は40文字以内で入力してください'
    }
  }
  return null
}

async function onBlur() {
  editing.value = false
  const value = editValue.value

  const errMsg = validate(value)
  if (errMsg) {
    error.value = errMsg
    return
  }

  error.value = ''
  if (value !== props.value) {
    await store.updateField(props.recordId, props.field, value)
  }
}
</script>

<template>
  <div class="text-cell" :class="{ 'cell-error': error }">
    <span
      v-if="!editing"
      :data-testid="`${field}-display`"
      :class="{ clickable: editable }"
      @click="onCellClick"
    >
      {{ value }}
    </span>
    <InputText
      v-else
      v-model="editValue"
      :data-testid="`${field}-input`"
      @blur="onBlur"
    />
    <span v-if="error" class="error-tooltip">{{ error }}</span>
  </div>
</template>

<style scoped>
.text-cell { position: relative; }
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
