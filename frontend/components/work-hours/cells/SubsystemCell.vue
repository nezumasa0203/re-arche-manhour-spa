<script setup lang="ts">
const props = defineProps<{
  recordId: number
  subsystemNo: string
  subsystemName: string
  editable: boolean
  mode: 'target' | 'cause'
}>()

const emit = defineEmits<{
  (e: 'open-search', payload: { recordId: number; mode: 'target' | 'cause' }): void
}>()

function onCellClick() {
  if (!props.editable) return
  emit('open-search', { recordId: props.recordId, mode: props.mode })
}
</script>

<template>
  <div class="subsystem-cell">
    <span
      data-testid="subsystem-display"
      :class="{ clickable: editable }"
      @click="onCellClick"
    >
      {{ subsystemNo }} {{ subsystemName }}
    </span>
  </div>
</template>

<style scoped>
.clickable { cursor: pointer; }
</style>
