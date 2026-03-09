<script setup lang="ts">
import { computed } from 'vue'
import { useWorkHoursStore } from '~/stores/workHours'
import type { WorkHoursRecord } from '~/types/api'
import StatusCell from '~/components/work-hours/cells/StatusCell.vue'
import DateCell from '~/components/work-hours/cells/DateCell.vue'
import SubsystemCell from '~/components/work-hours/cells/SubsystemCell.vue'
import CategoryCell from '~/components/work-hours/cells/CategoryCell.vue'
import SubjectCell from '~/components/work-hours/cells/SubjectCell.vue'
import HoursCell from '~/components/work-hours/cells/HoursCell.vue'
import TextCell from '~/components/work-hours/cells/TextCell.vue'

const props = defineProps<{
  categories: { categoryCode: string; categoryName: string }[]
  isManager: boolean
}>()

const store = useWorkHoursStore()

const selectedRecords = computed({
  get: () => store.records.filter(r => store.selectedIds.includes(r.id)),
  set: (val: WorkHoursRecord[]) => {
    store.selectedIds = val.map(r => r.id)
  },
})

function isEditable(record: WorkHoursRecord): boolean {
  return store.isEditable(record)
}

function rowClass(record: WorkHoursRecord): string {
  if (record.status === '1' || record.status === '2') {
    return 'cell-readonly'
  }
  return ''
}

function onSort(event: { sortField: string; sortOrder: number }) {
  const direction = event.sortOrder === -1 ? 'desc' : 'asc'
  store.sort = `${event.sortField}:${direction}`
  store.fetchRecords()
}

const sortField = computed(() => store.sort.split(':')[0])
const sortOrder = computed(() => store.sort.endsWith(':desc') ? -1 : 1)
</script>

<template>
  <div class="work-hours-datatable">
    <DataTable
      :value="store.records"
      :loading="store.loading"
      v-model:selection="selectedRecords"
      :sort-field="sortField"
      :sort-order="sortOrder"
      scrollable
      scroll-height="flex"
      :frozen-columns="4"
      data-key="id"
      :row-class="(data: WorkHoursRecord) => rowClass(data)"
      @sort="onSort"
    >
      <template #empty>
        <div class="empty-message">データがありません。[新規追加] ボタンで工数を登録してください</div>
      </template>

      <!-- 1: CHK -->
      <Column selection-mode="multiple" :style="{ width: '40px' }" frozen />

      <!-- 2: ステータス -->
      <Column field="status" header="ステータス" :sortable="true" :style="{ width: '80px' }" frozen>
        <template #body="{ data }">
          <StatusCell
            :record-id="data.id"
            :status="data.status"
            :is-manager="isManager"
          />
        </template>
      </Column>

      <!-- 3: 作業日 -->
      <Column field="workDate" header="作業日" :sortable="true" :style="{ width: '100px' }" frozen>
        <template #body="{ data }">
          <DateCell
            :record-id="data.id"
            :work-date="data.workDate"
            :editable="isEditable(data)"
            :year-month="store.yearMonth"
          />
        </template>
      </Column>

      <!-- 4: 保守担当所属 -->
      <Column field="department" header="保守担当所属" :sortable="true" :style="{ width: '120px' }" frozen>
        <template #body="{ data }">
          {{ data.department ?? '' }}
        </template>
      </Column>

      <!-- 5: 保守担当者名 -->
      <Column field="staffName" header="保守担当者名" :sortable="true" :style="{ width: '100px' }">
        <template #body="{ data }">
          {{ data.staffName ?? '' }}
        </template>
      </Column>

      <!-- 6: 対象SS No -->
      <Column field="targetSubsystem.subsystemNo" header="対象SS No" :sortable="true" :style="{ width: '70px' }">
        <template #body="{ data }">
          {{ data.targetSubsystem?.subsystemNo ?? '' }}
        </template>
      </Column>

      <!-- 7: 対象SS名 -->
      <Column field="targetSubsystem.subsystemName" header="対象SS名" :sortable="true" :style="{ width: '150px' }">
        <template #body="{ data }">
          <SubsystemCell
            :record-id="data.id"
            :subsystem-no="data.targetSubsystem?.subsystemNo ?? ''"
            :subsystem-name="data.targetSubsystem?.subsystemName ?? ''"
            :editable="isEditable(data)"
            mode="target"
          />
        </template>
      </Column>

      <!-- 8: 原因SS No -->
      <Column field="causeSubsystem.subsystemNo" header="原因SS No" :sortable="true" :style="{ width: '70px' }">
        <template #body="{ data }">
          {{ data.causeSubsystem?.subsystemNo ?? '' }}
        </template>
      </Column>

      <!-- 9: 原因SS名 -->
      <Column field="causeSubsystem.subsystemName" header="原因SS名" :sortable="true" :style="{ width: '150px' }">
        <template #body="{ data }">
          <SubsystemCell
            :record-id="data.id"
            :subsystem-no="data.causeSubsystem?.subsystemNo ?? ''"
            :subsystem-name="data.causeSubsystem?.subsystemName ?? ''"
            :editable="isEditable(data)"
            mode="cause"
          />
        </template>
      </Column>

      <!-- 10: 保守カテゴリ -->
      <Column field="category.code" header="保守カテゴリ" :sortable="true" :style="{ width: '160px' }">
        <template #body="{ data }">
          <CategoryCell
            :record-id="data.id"
            :category-code="data.category?.categoryCode ?? ''"
            :category-name="data.category?.categoryName ?? ''"
            :categories="categories"
            :editable="isEditable(data)"
          />
        </template>
      </Column>

      <!-- 11: 件名 -->
      <Column field="subject" header="件名" :sortable="true" :style="{ width: '220px' }">
        <template #body="{ data }">
          <SubjectCell
            :record-id="data.id"
            :subject="data.subject"
            :editable="isEditable(data)"
          />
        </template>
      </Column>

      <!-- 12: 工数 -->
      <Column field="hours" header="工数" :sortable="true" :style="{ width: '70px' }">
        <template #body="{ data }">
          <HoursCell
            :record-id="data.id"
            :hours="data.hours"
            :editable="isEditable(data)"
          />
        </template>
      </Column>

      <!-- 13: TMR番号 -->
      <Column field="tmrNo" header="TMR番号" :sortable="true" :style="{ width: '70px' }">
        <template #body="{ data }">
          <TextCell
            :record-id="data.id"
            field="tmrNo"
            :value="data.tmrNo"
            :editable="isEditable(data)"
          />
        </template>
      </Column>

      <!-- 14: 依頼書No -->
      <Column field="workRequestNo" header="依頼書No" :sortable="true" :style="{ width: '90px' }">
        <template #body="{ data }">
          <TextCell
            :record-id="data.id"
            field="workRequestNo"
            :value="data.workRequestNo"
            :editable="isEditable(data)"
          />
        </template>
      </Column>
    </DataTable>
  </div>
</template>

<style scoped>
.work-hours-datatable {
  flex: 1;
  overflow: hidden;
}

.empty-message {
  text-align: center;
  padding: 2rem;
  color: var(--text-color-secondary, #6c757d);
}

:deep(.cell-readonly) {
  background-color: var(--surface-100, #f8f9fa);
}
</style>
