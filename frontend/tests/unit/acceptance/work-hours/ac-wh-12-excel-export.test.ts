/**
 * AC-WH-12: Excel 出力（US-01C）
 * Given: 工数データが表示されている状態
 * When: [Excel] ボタンをクリックし確認ダイアログで「はい」を選択する
 * Then: getBlob API が呼ばれ、ダウンロードが実行される
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import Toolbar from '~/components/work-hours/Toolbar.vue'

const mockStore = {
  records: [{ id: 1, status: '0' }],
  summary: { monthlyTotal: 3, dailyTotal: 3 },
  selectedIds: [] as number[],
  permissions: {
    canCreate: true,
    canEdit: true,
    canDelete: true,
    canConfirm: true,
    canRevert: true,
    canCopy: true,
    canTransfer: true,
  },
  monthControl: { yearMonth: '2025-02', status: '', isLocked: false },
  yearMonth: '2025-02',
  canAdd: true,
  canCopy: false,
  canDelete: false,
  loading: false,
  createRecord: vi.fn(),
  deleteRecords: vi.fn(),
  copyRecords: vi.fn(),
}

const mockConfirmByCode = vi.fn().mockResolvedValue(true)

vi.mock('~/stores/workHours', () => ({
  useWorkHoursStore: () => mockStore,
}))

vi.mock('~/composables/useConfirmAction', () => ({
  useConfirmAction: () => ({
    confirmByCode: mockConfirmByCode,
  }),
}))

function mountToolbar() {
  return mount(Toolbar, {
    global: {
      plugins: [createPinia()],
      stubs: {
        'Button': {
          template: '<button :disabled="disabled" @click="$emit(\'click\')"><slot>{{ label }}</slot></button>',
          props: ['label', 'icon', 'disabled', 'severity', 'size', 'text'],
        },
      },
    },
  })
}

describe('AC-WH-12: Excel 出力', () => {
  beforeEach(() => {
    mockConfirmByCode.mockReset().mockResolvedValue(true)
  })

  it('Given: 工数データ表示中 → Excel ボタンが存在する', () => {
    const wrapper = mountToolbar()
    const excelBtn = wrapper.find('[data-testid="excel-btn"]')
    expect(excelBtn.exists()).toBe(true)
  })

  it('When: Excel ボタンクリック → 確認ダイアログが表示される', async () => {
    const wrapper = mountToolbar()
    const excelBtn = wrapper.find('[data-testid="excel-btn"]')
    await excelBtn.trigger('click')
    await flushPromises()

    expect(mockConfirmByCode).toHaveBeenCalled()
  })
})
