<script setup lang="ts">
import { useWorkHoursStore } from '~/stores/workHours'
import { useConfirmAction } from '~/composables/useConfirmAction'

const store = useWorkHoursStore()
const { confirmByCode } = useConfirmAction()

const emit = defineEmits<{
  (e: 'open-transfer-dialog'): void
  (e: 'open-project-summary'): void
  (e: 'export-excel'): void
}>()

async function onAdd() {
  await store.createRecord()
}

async function onCopy() {
  await store.copyRecords([...store.selectedIds])
}

async function onDelete() {
  const confirmed = await confirmByCode('CZ-506', [String(store.selectedIds.length)])
  if (!confirmed) return
  await store.deleteRecords([...store.selectedIds])
}

function onTransfer() {
  emit('open-transfer-dialog')
}

function onProjectSummary() {
  emit('open-project-summary')
}

async function onExcel() {
  const confirmed = await confirmByCode('CZ-516')
  if (!confirmed) return
  emit('export-excel')
}
</script>

<template>
  <div class="toolbar">
    <div class="toolbar__left">
      <Button
        data-testid="add-btn"
        label="追加"
        icon="pi pi-plus"
        :disabled="!store.canAdd"
        @click="onAdd"
      />
      <Button
        data-testid="copy-btn"
        label="コピー"
        icon="pi pi-copy"
        severity="secondary"
        :disabled="!store.canCopy"
        @click="onCopy"
      />
      <Button
        data-testid="transfer-btn"
        label="翌月転写"
        icon="pi pi-arrow-right"
        severity="secondary"
        :disabled="!store.canCopy"
        @click="onTransfer"
      />
      <Button
        data-testid="delete-btn"
        label="削除"
        icon="pi pi-trash"
        severity="danger"
        :disabled="!store.canDelete"
        @click="onDelete"
      />
    </div>

    <div class="toolbar__right">
      <span data-testid="total-hours" class="toolbar__total">
        合計: {{ store.summary.monthlyTotal }}
      </span>
      <Button
        data-testid="project-summary-btn"
        label="PJ工数"
        icon="pi pi-chart-bar"
        severity="secondary"
        outlined
        @click="onProjectSummary"
      />
      <Button
        data-testid="excel-btn"
        label="Excel"
        icon="pi pi-file-excel"
        severity="success"
        outlined
        @click="onExcel"
      />
    </div>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.5rem 1rem;
  border-bottom: 1px solid var(--surface-border, #dee2e6);
  flex-wrap: wrap;
  gap: 0.5rem;
}

.toolbar__left,
.toolbar__right {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.toolbar__total {
  font-weight: 700;
  font-size: 1.1rem;
  padding: 0 0.75rem;
}
</style>
