import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import Toolbar from '~/components/work-hours/Toolbar.vue'

// --- Store mock ---
const mockStore = {
  canAdd: true,
  canCopy: false,
  canDelete: false,
  selectedIds: [] as number[],
  summary: { monthlyTotal: 120.5, dailyTotal: 8 },
  permissions: {
    canCreate: true,
    canEdit: true,
    canDelete: true,
    canConfirm: true,
    canRevert: true,
    canCopy: true,
    canTransfer: true,
  },
  monthControl: { yearMonth: '2025-02', status: 'OPEN', isLocked: false },
  createRecord: vi.fn(),
  deleteRecords: vi.fn(),
  copyRecords: vi.fn(),
}

vi.mock('~/stores/workHours', () => ({
  useWorkHoursStore: () => mockStore,
}))

// Confirm mock
const mockConfirm = vi.fn().mockResolvedValue(true)
vi.mock('~/composables/useConfirmAction', () => ({
  useConfirmAction: () => ({
    confirm: mockConfirm,
    confirmByCode: mockConfirm,
  }),
}))

function mountToolbar() {
  return mount(Toolbar, {
    global: {
      plugins: [createPinia()],
      stubs: {
        'Button': {
          template: '<button :disabled="disabled" :class="{ hidden: !visible }" @click="$emit(\'click\')"><slot>{{ label }}</slot></button>',
          props: ['label', 'icon', 'disabled', 'severity', 'visible'],
        },
      },
    },
  })
}

describe('Toolbar', () => {
  beforeEach(() => {
    mockStore.canAdd = true
    mockStore.canCopy = false
    mockStore.canDelete = false
    mockStore.selectedIds = []
    mockStore.summary = { monthlyTotal: 120.5, dailyTotal: 8 }
    mockStore.permissions = {
      canCreate: true,
      canEdit: true,
      canDelete: true,
      canConfirm: true,
      canRevert: true,
      canCopy: true,
      canTransfer: true,
    }
    mockStore.monthControl = { yearMonth: '2025-02', status: 'OPEN', isLocked: false }
    mockStore.createRecord.mockReset()
    mockStore.deleteRecords.mockReset()
    mockStore.copyRecords.mockReset()
    mockConfirm.mockReset().mockResolvedValue(true)
  })

  // ─── 追加ボタン ──────────────────────────────────────────────

  describe('追加ボタン', () => {
    it('canAdd=true のとき追加ボタンが有効', () => {
      mockStore.canAdd = true
      const wrapper = mountToolbar()
      const addBtn = wrapper.find('[data-testid="add-btn"]')
      expect(addBtn.exists()).toBe(true)
      expect(addBtn.attributes('disabled')).toBeUndefined()
    })

    it('canAdd=false のとき追加ボタンが無効', () => {
      mockStore.canAdd = false
      const wrapper = mountToolbar()
      const addBtn = wrapper.find('[data-testid="add-btn"]')
      expect(addBtn.attributes('disabled')).toBeDefined()
    })

    it('追加ボタンクリックで createRecord が呼ばれる', async () => {
      mockStore.canAdd = true
      const wrapper = mountToolbar()
      const addBtn = wrapper.find('[data-testid="add-btn"]')
      await addBtn.trigger('click')
      expect(mockStore.createRecord).toHaveBeenCalled()
    })
  })

  // ─── コピーボタン ────────────────────────────────────────────

  describe('コピーボタン', () => {
    it('canCopy=false のとき無効', () => {
      mockStore.canCopy = false
      const wrapper = mountToolbar()
      const copyBtn = wrapper.find('[data-testid="copy-btn"]')
      expect(copyBtn.attributes('disabled')).toBeDefined()
    })

    it('canCopy=true（選択あり）のとき有効', () => {
      mockStore.canCopy = true
      mockStore.selectedIds = [1, 2]
      const wrapper = mountToolbar()
      const copyBtn = wrapper.find('[data-testid="copy-btn"]')
      expect(copyBtn.attributes('disabled')).toBeUndefined()
    })

    it('コピーボタンクリックで copyRecords が呼ばれる', async () => {
      mockStore.canCopy = true
      mockStore.selectedIds = [1, 2]
      const wrapper = mountToolbar()
      const copyBtn = wrapper.find('[data-testid="copy-btn"]')
      await copyBtn.trigger('click')
      expect(mockStore.copyRecords).toHaveBeenCalledWith([1, 2])
    })
  })

  // ─── 削除ボタン ──────────────────────────────────────────────

  describe('削除ボタン', () => {
    it('canDelete=false のとき無効', () => {
      mockStore.canDelete = false
      const wrapper = mountToolbar()
      const deleteBtn = wrapper.find('[data-testid="delete-btn"]')
      expect(deleteBtn.attributes('disabled')).toBeDefined()
    })

    it('canDelete=true（選択あり）のとき有効', () => {
      mockStore.canDelete = true
      mockStore.selectedIds = [1]
      const wrapper = mountToolbar()
      const deleteBtn = wrapper.find('[data-testid="delete-btn"]')
      expect(deleteBtn.attributes('disabled')).toBeUndefined()
    })

    it('削除ボタンクリックで確認ダイアログ後に deleteRecords が呼ばれる', async () => {
      mockStore.canDelete = true
      mockStore.selectedIds = [1, 3]
      mockConfirm.mockResolvedValueOnce(true)
      const wrapper = mountToolbar()
      const deleteBtn = wrapper.find('[data-testid="delete-btn"]')
      await deleteBtn.trigger('click')
      await flushPromises()
      expect(mockStore.deleteRecords).toHaveBeenCalledWith([1, 3])
    })

    it('確認ダイアログでキャンセル時は削除しない', async () => {
      mockStore.canDelete = true
      mockStore.selectedIds = [1]
      mockConfirm.mockImplementation(() => Promise.resolve(false))
      const wrapper = mountToolbar()
      const deleteBtn = wrapper.find('[data-testid="delete-btn"]')
      await deleteBtn.trigger('click')
      await flushPromises()
      expect(mockStore.deleteRecords).not.toHaveBeenCalled()
    })
  })

  // ─── 翌月転写ボタン ─────────────────────────────────────────

  describe('翌月転写ボタン', () => {
    it('canCopy=false のとき無効', () => {
      mockStore.canCopy = false
      const wrapper = mountToolbar()
      const transferBtn = wrapper.find('[data-testid="transfer-btn"]')
      expect(transferBtn.attributes('disabled')).toBeDefined()
    })

    it('canCopy=true（選択あり）のとき有効', () => {
      mockStore.canCopy = true
      mockStore.selectedIds = [1]
      const wrapper = mountToolbar()
      const transferBtn = wrapper.find('[data-testid="transfer-btn"]')
      expect(transferBtn.attributes('disabled')).toBeUndefined()
    })
  })

  // ─── 合計工数表示 ────────────────────────────────────────────

  describe('合計工数表示', () => {
    it('合計工数が表示される', () => {
      mockStore.summary = { monthlyTotal: 120.5, dailyTotal: 8 }
      const wrapper = mountToolbar()
      const total = wrapper.find('[data-testid="total-hours"]')
      expect(total.exists()).toBe(true)
      expect(total.text()).toContain('120.5')
    })

    it('合計が0の場合も表示される', () => {
      mockStore.summary = { monthlyTotal: 0, dailyTotal: 0 }
      const wrapper = mountToolbar()
      const total = wrapper.find('[data-testid="total-hours"]')
      expect(total.text()).toContain('0')
    })
  })

  // ─── PJ工数 / Excel ボタン ──────────────────────────────────

  describe('PJ工数/Excel ボタン', () => {
    it('PJ工数ボタンが常時表示される', () => {
      const wrapper = mountToolbar()
      const pjBtn = wrapper.find('[data-testid="project-summary-btn"]')
      expect(pjBtn.exists()).toBe(true)
    })

    it('Excel ボタンが常時表示される', () => {
      const wrapper = mountToolbar()
      const excelBtn = wrapper.find('[data-testid="excel-btn"]')
      expect(excelBtn.exists()).toBe(true)
    })
  })
})
