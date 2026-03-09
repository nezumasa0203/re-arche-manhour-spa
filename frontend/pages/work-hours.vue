<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useWorkHoursStore } from '~/stores/workHours'
import { useAuth } from '~/composables/useAuth'

const store = useWorkHoursStore()
const auth = useAuth()

const isManager = computed(() => auth.canManage.value || auth.canFullAccess.value)

onMounted(() => {
  // auth store から staffId / staffName を初期化
  if (auth.user.value && !store.staffId) {
    store.staffId = auth.user.value.userId
    store.staffName = auth.user.value.userName
  }
  store.fetchRecords()
})
</script>

<template>
  <div class="work-hours-page">
    <WorkHoursSearchPanel />
    <WorkHoursToolbar />
    <WorkHoursDataTable
      :categories="[]"
      :is-manager="isManager"
    />
    <WorkHoursStatusFooter />
    <WorkHoursTransferDialog
      :visible="false"
      :selected-ids="store.selectedIds"
    />
    <WorkHoursProjectSummaryDialog :visible="false" />
  </div>
</template>

<style scoped>
.work-hours-page {
  display: flex;
  flex-direction: column;
  height: 100%;
}
</style>
