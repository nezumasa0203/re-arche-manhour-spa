import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import SearchPanel from '~/components/work-hours/SearchPanel.vue'

// --- Store mock ---
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
          template: '<select :value="modelValue" @change="$emit(\'update:modelValue\', $event.target.value)"><option v-for="opt in options" :key="opt.value" :value="opt.value">{{ opt.label }}</option></select>',
          props: ['modelValue', 'options'],
        },
        'Button': {
          template: '<button :disabled="disabled" @click="$emit(\'click\')"><slot>{{ label }}</slot></button>',
          props: ['label', 'icon', 'disabled', 'severity'],
        },
        'InputText': {
          template: '<input :value="modelValue" readonly />',
          props: ['modelValue', 'readonly'],
        },
        'Tag': {
          template: '<span class="p-tag"><slot>{{ value }}</slot></span>',
          props: ['value', 'severity'],
        },
      },
    },
  })
}

describe('SearchPanel', () => {
  beforeEach(() => {
    mockStore.yearMonth = '2025-02'
    mockStore.staffId = null
    mockStore.staffName = null
    mockStore.isDaiko = false
    mockStore.loading = false
    mockStore.changeMonth.mockReset()
    mockStore.switchDaiko.mockReset()
    mockStore.fetchRecords.mockReset()
  })

  // ─── 年月 Dropdown ─────────────────────────────────────────

  describe('年月 Dropdown', () => {
    it('年月ドロップダウンが表示される', () => {
      const wrapper = mountSearchPanel()
      const dropdown = wrapper.find('[data-testid="month-selector"]')
      expect(dropdown.exists()).toBe(true)
    })

    it('±12ヶ月（計25件）のオプションが生成される', () => {
      const wrapper = mountSearchPanel()
      const options = wrapper.findAll('[data-testid="month-selector"] option')
      expect(options.length).toBe(25)
    })

    it('現在の yearMonth が選択されている', () => {
      const wrapper = mountSearchPanel()
      const select = wrapper.find('[data-testid="month-selector"]')
      expect((select.element as HTMLSelectElement).value).toBe('2025-02')
    })

    it('年月変更時に changeMonth が呼ばれる', async () => {
      const wrapper = mountSearchPanel()
      const select = wrapper.find('[data-testid="month-selector"]')
      await select.setValue('2025-03')
      expect(mockStore.changeMonth).toHaveBeenCalledWith('2025-03')
    })
  })

  // ─── << / >> ボタン ─────────────────────────────────────────

  describe('前月/翌月ボタン', () => {
    it('<< ボタンで前月に切替', async () => {
      const wrapper = mountSearchPanel()
      const prevBtn = wrapper.find('[data-testid="prev-month"]')
      await prevBtn.trigger('click')
      expect(mockStore.changeMonth).toHaveBeenCalledWith('2025-01')
    })

    it('>> ボタンで翌月に切替', async () => {
      const wrapper = mountSearchPanel()
      const nextBtn = wrapper.find('[data-testid="next-month"]')
      await nextBtn.trigger('click')
      expect(mockStore.changeMonth).toHaveBeenCalledWith('2025-03')
    })
  })

  // ─── リセットボタン ──────────────────────────────────────────

  describe('リセットボタン', () => {
    it('リセットボタンが表示される', () => {
      const wrapper = mountSearchPanel()
      const resetBtn = wrapper.find('[data-testid="reset-btn"]')
      expect(resetBtn.exists()).toBe(true)
    })

    it('リセットで初期値に復元し fetchRecords を呼ぶ', async () => {
      mockStore.yearMonth = '2025-05'
      mockStore.isDaiko = true
      mockStore.staffId = 'STAFF001'

      const wrapper = mountSearchPanel()
      const resetBtn = wrapper.find('[data-testid="reset-btn"]')
      await resetBtn.trigger('click')

      // changeMonth が現在月で呼ばれる
      const now = new Date()
      const currentMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
      expect(mockStore.changeMonth).toHaveBeenCalledWith(currentMonth)
    })
  })

  // ─── 代行モード表示 ─────────────────────────────────────────

  describe('代行モード', () => {
    it('isDaiko=false のとき代行バッジは非表示', () => {
      mockStore.isDaiko = false
      const wrapper = mountSearchPanel()
      expect(wrapper.find('[data-testid="daiko-badge"]').exists()).toBe(false)
    })

    it('isDaiko=true のとき「代行中」バッジが表示される', () => {
      mockStore.isDaiko = true
      mockStore.staffName = '山田太郎'
      const wrapper = mountSearchPanel()
      const badge = wrapper.find('[data-testid="daiko-badge"]')
      expect(badge.exists()).toBe(true)
      expect(badge.text()).toContain('代行中')
    })

    it('isDaiko=true のとき解除ボタンが表示される', () => {
      mockStore.isDaiko = true
      mockStore.staffName = '山田太郎'
      const wrapper = mountSearchPanel()
      const cancelBtn = wrapper.find('[data-testid="daiko-cancel"]')
      expect(cancelBtn.exists()).toBe(true)
    })

    it('解除ボタンで switchDaiko(null) が呼ばれる', async () => {
      mockStore.isDaiko = true
      mockStore.staffName = '山田太郎'
      const wrapper = mountSearchPanel()
      const cancelBtn = wrapper.find('[data-testid="daiko-cancel"]')
      await cancelBtn.trigger('click')
      expect(mockStore.switchDaiko).toHaveBeenCalledWith(null)
    })
  })

  // ─── 検索ボタン ─────────────────────────────────────────────

  describe('検索ボタン', () => {
    it('検索ボタンで fetchRecords が呼ばれる', async () => {
      const wrapper = mountSearchPanel()
      const searchBtn = wrapper.find('[data-testid="search-btn"]')
      await searchBtn.trigger('click')
      expect(mockStore.fetchRecords).toHaveBeenCalled()
    })
  })

  // ─── 担当者表示 ─────────────────────────────────────────────

  describe('担当者表示', () => {
    it('担当者名が表示される', () => {
      mockStore.staffName = '山田太郎'
      const wrapper = mountSearchPanel()
      const staffInput = wrapper.find('[data-testid="staff-display"]')
      expect(staffInput.exists()).toBe(true)
    })
  })
})
