<script setup lang="ts">
import { ref, watch } from 'vue'
import { useWorkHoursStore } from '~/stores/workHours'
import { useApi } from '~/composables/useApi'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
}>()

const store = useWorkHoursStore()
const api = useApi()

interface ProjectRow {
  key: string
  systemName: string
  subsystemName: string
  hours: string
}

const rows = ref<ProjectRow[]>([])
const loading = ref(false)

watch(() => props.visible, async (val) => {
  if (val) {
    loading.value = true
    try {
      const response = await api.get<{ rows: ProjectRow[] }>(
        '/work-hours/project-summary',
        { yearMonth: store.yearMonth }
      )
      rows.value = response.rows
    } finally {
      loading.value = false
    }
  }
}, { immediate: true })

function onClose() {
  emit('update:visible', false)
}
</script>

<template>
  <Dialog
    :visible="visible"
    header="プロジェクト別工数"
    :modal="true"
    :style="{ width: '600px' }"
    @update:visible="$emit('update:visible', $event)"
  >
    <p>対象月: {{ store.yearMonth }}</p>

    <DataTable :value="rows">
      <Column field="systemName" header="システム" />
      <Column field="subsystemName" header="SS名" />
      <Column field="hours" header="工数" />
    </DataTable>

    <template #footer>
      <Button label="閉じる" @click="onClose" />
    </template>
  </Dialog>
</template>
