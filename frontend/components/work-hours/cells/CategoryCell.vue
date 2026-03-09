<script setup lang="ts">
import { ref } from 'vue'
import { useWorkHoursStore } from '~/stores/workHours'

const props = defineProps<{
  recordId: number
  categoryCode: string
  categoryName: string
  categories: { categoryCode: string; categoryName: string }[]
  editable: boolean
}>()

const store = useWorkHoursStore()
const editing = ref(false)

const dropdownOptions = props.categories.map(c => ({
  label: c.categoryName,
  value: c.categoryCode,
}))

function onCellClick() {
  if (!props.editable) return
  editing.value = true
}

async function onCategoryChange(value: string) {
  editing.value = false
  if (value !== props.categoryCode) {
    await store.updateField(props.recordId, 'categoryCode', value)
  }
}
</script>

<template>
  <div class="category-cell">
    <span
      v-if="!editing"
      data-testid="category-display"
      :class="{ clickable: editable }"
      @click="onCellClick"
    >
      {{ categoryName }}
    </span>
    <Dropdown
      v-else
      data-testid="category-dropdown"
      :model-value="categoryCode"
      :options="dropdownOptions"
      option-label="label"
      option-value="value"
      @update:model-value="onCategoryChange"
    />
  </div>
</template>

<style scoped>
.clickable { cursor: pointer; }
</style>
