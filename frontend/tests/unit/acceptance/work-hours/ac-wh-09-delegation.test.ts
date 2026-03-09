/**
 * AC-WH-09: 代行モード（US-014）
 * Given: canDelegate=true でログインしている
 * When: 代行先を正社員 A に設定する
 * Then: 正社員 A の工数データが表示され、以降の操作は正社員 A 名義で記録される
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
          template: '<select></select>',
          props: ['modelValue', 'options'],
        },
        'Button': {
          template: '<button @click="$emit(\'click\')"><slot>{{ label }}</slot></button>',
          props: ['label', 'icon', 'severity', 'size', 'text'],
        },
        'InputText': {
          template: '<input :value="modelValue" data-testid="staff-display" readonly />',
          props: ['modelValue', 'readonly'],
        },
        'Tag': {
          template: '<span data-testid="daiko-badge">{{ value }}</span>',
          props: ['value', 'severity'],
        },
      },
    },
  })
}

describe('AC-WH-09: 代行モード', () => {
  beforeEach(() => {
    mockStore.staffId = null
    mockStore.staffName = null
    mockStore.isDaiko = false
    mockStore.switchDaiko.mockReset()
  })

  it('Given: 代行先を設定済み → Then: 代行対象者名が表示される', () => {
    mockStore.isDaiko = true
    mockStore.staffId = 'STAFF001'
    mockStore.staffName = '正社員A'

    const wrapper = mountSearchPanel()
    const staffInput = wrapper.find('[data-testid="staff-display"]')
    expect((staffInput.element as HTMLInputElement).value).toBe('正社員A')
  })

  it('Given: 代行モード中 → Then: 「代行中」バッジが表示される', () => {
    mockStore.isDaiko = true
    mockStore.staffName = '正社員A'

    const wrapper = mountSearchPanel()
    const badge = wrapper.find('[data-testid="daiko-badge"]')
    expect(badge.exists()).toBe(true)
    expect(badge.text()).toContain('代行中')
  })

  it('When: 代行解除ボタン → Then: switchDaiko(null) が呼ばれる', async () => {
    mockStore.isDaiko = true
    mockStore.staffName = '正社員A'

    const wrapper = mountSearchPanel()
    await wrapper.find('[data-testid="daiko-cancel"]').trigger('click')
    expect(mockStore.switchDaiko).toHaveBeenCalledWith(null)
  })

  it('Given: 通常モード → Then: 代行バッジは表示されない', () => {
    mockStore.isDaiko = false
    const wrapper = mountSearchPanel()
    expect(wrapper.find('[data-testid="daiko-badge"]').exists()).toBe(false)
  })
})
