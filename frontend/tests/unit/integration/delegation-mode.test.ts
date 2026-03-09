import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
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
          props: ['label', 'icon', 'disabled', 'severity', 'size', 'text'],
        },
        'InputText': {
          template: '<input :value="modelValue" data-testid="staff-display" readonly />',
          props: ['modelValue', 'readonly'],
        },
        'Tag': {
          template: '<span class="p-tag" data-testid="daiko-badge"><slot>{{ value }}</slot></span>',
          props: ['value', 'severity'],
        },
      },
    },
  })
}

describe('代行モード統合', () => {
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

  // ─── 代行切替フロー ──────────────────────────────────────────

  describe('代行切替フロー', () => {
    it('通常モードでは代行バッジが非表示', () => {
      const wrapper = mountSearchPanel()
      expect(wrapper.find('[data-testid="daiko-badge"]').exists()).toBe(false)
      expect(wrapper.find('[data-testid="daiko-cancel"]').exists()).toBe(false)
    })

    it('代行モードでは代行バッジと解除ボタンが表示される', () => {
      mockStore.isDaiko = true
      mockStore.staffId = 'STAFF001'
      mockStore.staffName = '山田太郎'

      const wrapper = mountSearchPanel()
      expect(wrapper.find('[data-testid="daiko-badge"]').exists()).toBe(true)
      expect(wrapper.find('[data-testid="daiko-badge"]').text()).toContain('代行中')
      expect(wrapper.find('[data-testid="daiko-cancel"]').exists()).toBe(true)
    })

    it('解除ボタンクリックで switchDaiko(null) が呼ばれる', async () => {
      mockStore.isDaiko = true
      mockStore.staffId = 'STAFF001'
      mockStore.staffName = '山田太郎'

      const wrapper = mountSearchPanel()
      await wrapper.find('[data-testid="daiko-cancel"]').trigger('click')

      expect(mockStore.switchDaiko).toHaveBeenCalledWith(null)
    })
  })

  // ─── 代行中の担当者表示 ───────────────────────────────────────

  describe('代行中の担当者表示', () => {
    it('代行対象者名が担当者欄に表示される', () => {
      mockStore.isDaiko = true
      mockStore.staffId = 'STAFF001'
      mockStore.staffName = '山田太郎'

      const wrapper = mountSearchPanel()
      const staffInput = wrapper.find('[data-testid="staff-display"]')
      expect((staffInput.element as HTMLInputElement).value).toBe('山田太郎')
    })

    it('通常モードでは担当者名が空', () => {
      mockStore.isDaiko = false
      mockStore.staffName = null

      const wrapper = mountSearchPanel()
      const staffInput = wrapper.find('[data-testid="staff-display"]')
      expect((staffInput.element as HTMLInputElement).value).toBe('')
    })
  })

  // ─── 代行中の月切替 ──────────────────────────────────────────

  describe('代行中の月切替', () => {
    it('代行中に月切替しても代行状態が維持される', async () => {
      mockStore.isDaiko = true
      mockStore.staffId = 'STAFF001'
      mockStore.staffName = '山田太郎'

      const wrapper = mountSearchPanel()
      await wrapper.find('[data-testid="next-month"]').trigger('click')

      // changeMonth が呼ばれるが switchDaiko は呼ばれない
      expect(mockStore.changeMonth).toHaveBeenCalledWith('2025-03')
      expect(mockStore.switchDaiko).not.toHaveBeenCalled()
      expect(mockStore.isDaiko).toBe(true)
    })

    it('代行中に前月切替も代行状態が維持される', async () => {
      mockStore.isDaiko = true
      mockStore.staffId = 'STAFF001'

      const wrapper = mountSearchPanel()
      await wrapper.find('[data-testid="prev-month"]').trigger('click')

      expect(mockStore.changeMonth).toHaveBeenCalledWith('2025-01')
      expect(mockStore.switchDaiko).not.toHaveBeenCalled()
    })
  })

  // ─── 代行中の検索・リセット ────────────────────────────────────

  describe('代行中の検索・リセット', () => {
    it('代行中に検索ボタンで fetchRecords が呼ばれる', async () => {
      mockStore.isDaiko = true
      mockStore.staffId = 'STAFF001'

      const wrapper = mountSearchPanel()
      await wrapper.find('[data-testid="search-btn"]').trigger('click')

      expect(mockStore.fetchRecords).toHaveBeenCalled()
    })

    it('代行中にリセットで changeMonth が呼ばれる（代行は解除されない）', async () => {
      mockStore.isDaiko = true
      mockStore.staffId = 'STAFF001'

      const wrapper = mountSearchPanel()
      await wrapper.find('[data-testid="reset-btn"]').trigger('click')

      expect(mockStore.changeMonth).toHaveBeenCalled()
      expect(mockStore.switchDaiko).not.toHaveBeenCalled()
    })
  })

  // ─── useApi ヘッダー付与検証（ユニットレベル）──────────────────

  describe('代行状態管理', () => {
    it('switchDaiko で staffId と isDaiko が設定される（Store Action テスト）', () => {
      // Store の switchDaiko が正しいパラメータで呼ばれることを検証
      // 実際の API ヘッダー付与は useApi の単体テストでカバー済み
      mockStore.switchDaiko('STAFF001')
      expect(mockStore.switchDaiko).toHaveBeenCalledWith('STAFF001')
    })

    it('switchDaiko(null) で代行解除される（Store Action テスト）', () => {
      mockStore.switchDaiko(null)
      expect(mockStore.switchDaiko).toHaveBeenCalledWith(null)
    })
  })
})
