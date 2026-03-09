/**
 * AC-WH-08: 月切替（US-013）
 * Given: 2025年02月のデータが表示されている
 * When: MonthSelector で 2025年01月を選択する
 * Then: changeMonth が呼ばれ、2025年01月のデータが取得される
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import SearchPanel from '~/components/work-hours/SearchPanel.vue'

const mockStore = {
  yearMonth: '2025-02',
  staffId: null as string | null,
  staffName: null as string | null,
  isDaiko: false,
  loading: false,
  changeMonth: vi.fn(),
  switchDaiko: vi.fn(),
  fetchRecords: vi.fn(),
}

vi.mock('~/stores/workHours', () => ({
  useWorkHoursStore: () => mockStore,
}))

function mountSearchPanel() {
  return mount(SearchPanel, {
    global: {
      plugins: [createPinia()],
      stubs: {
        'Dropdown': {
          template: '<select :value="modelValue" data-testid="month-selector" @change="$emit(\'update:modelValue\', $event.target.value)"><option v-for="opt in options" :key="opt.value" :value="opt.value">{{ opt.label }}</option></select>',
          props: ['modelValue', 'options'],
        },
        'Button': {
          template: '<button @click="$emit(\'click\')"><slot>{{ label }}</slot></button>',
          props: ['label', 'icon', 'severity', 'size', 'text'],
        },
        'InputText': {
          template: '<input :value="modelValue" readonly />',
          props: ['modelValue', 'readonly'],
        },
        'Tag': {
          template: '<span></span>',
          props: ['value', 'severity'],
        },
      },
    },
  })
}

describe('AC-WH-08: 月切替', () => {
  beforeEach(() => {
    mockStore.yearMonth = '2025-02'
    mockStore.changeMonth.mockReset()
    mockStore.fetchRecords.mockReset()
  })

  it('Given: 2025年02月表示中 → When: 前月ボタン → Then: 2025-01 で changeMonth', async () => {
    const wrapper = mountSearchPanel()
    await wrapper.find('[data-testid="prev-month"]').trigger('click')
    expect(mockStore.changeMonth).toHaveBeenCalledWith('2025-01')
  })

  it('Given: 2025年02月表示中 → When: 翌月ボタン → Then: 2025-03 で changeMonth', async () => {
    const wrapper = mountSearchPanel()
    await wrapper.find('[data-testid="next-month"]').trigger('click')
    expect(mockStore.changeMonth).toHaveBeenCalledWith('2025-03')
  })

  it('When: Dropdown で月を選択 → Then: changeMonth が呼ばれる', async () => {
    const wrapper = mountSearchPanel()
    const select = wrapper.find('[data-testid="month-selector"]')
    await select.setValue('2025-01')
    expect(mockStore.changeMonth).toHaveBeenCalledWith('2025-01')
  })
})
