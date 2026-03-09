<script setup lang="ts">
import { ref } from 'vue'
import { useWorkHoursStore } from '~/stores/workHours'
import { validateRequired, validateByteLength } from '~/composables/useValidation'

const props = defineProps<{
  recordId: number
  subject: string
  editable: boolean
}>()

const store = useWorkHoursStore()
const editing = ref(false)
const editValue = ref('')
const error = ref('')

function onCellClick() {
  if (!props.editable) return
  editValue.value = props.subject
  error.value = ''
  editing.value = true
}

async function onBlur() {
  editing.value = false
  // 改行コード自動除去
  const value = editValue.value.replace(/[\r\n]/g, '')

  // VR-006: 必須チェック
  const reqResult = validateRequired(value, '件名')
  if (!reqResult.valid) {
    error.value = reqResult.message ?? ''
    return
  }

  // VR-006: 128バイト以内
  const lenResult = validateByteLength(value, 128, '件名')
  if (!lenResult.valid) {
    error.value = lenResult.message ?? ''
    return
  }

  error.value = ''
  if (value !== props.subject) {
    await store.updateField(props.recordId, 'subject', value)
  }
}
</script>

<template>
  <div class="subject-cell" :class="{ 'cell-error': error }">
    <span
      v-if="!editing"
      data-testid="subject-display"
      :class="['subject-truncate', { clickable: editable }]"
      @click="onCellClick"
    >
      {{ subject }}
    </span>
    <InputText
      v-else
      v-model="editValue"
      data-testid="subject-input"
      :maxlength="128"
      @blur="onBlur"
    />
    <span v-if="error" class="error-tooltip">{{ error }}</span>
  </div>
</template>

<style scoped>
.subject-cell { position: relative; }
.subject-truncate {
  display: inline-block;
  max-width: 220px;
  min-height: 1.2em;
  min-width: 2em;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.clickable { cursor: pointer; }
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
