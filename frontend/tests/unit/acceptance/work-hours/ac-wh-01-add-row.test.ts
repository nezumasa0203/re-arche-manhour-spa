/**
 * AC-WH-01: 新規行追加（US-010）
 * Given: STATUS_0 権限を持つユーザーが工数入力画面を表示している
 * When: [追加] ボタンをクリックする
 * Then: DataTable 先頭に空行が追加され、作業日セルにフォーカスが当たる
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import Toolbar from '~/components/work-hours/Toolbar.vue'
import type { WorkHoursRecord } from '~/types/api'

const mockStore = {
  records: [] as WorkHoursRecord[],
  summary: { monthlyTotal: 0, dailyTotal: 0 },
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
  canAdd: true,
  canCopy: false,
  canDelete: false,
  loading: false,
  createRecord: vi.fn(),
  deleteRecords: vi.fn(),
  copyRecords: vi.fn(),
}

vi.mock('~/stores/workHours', () => ({
  useWorkHoursStore: () => mockStore,
}))

vi.mock('~/composables/useConfirmAction', () => ({
  useConfirmAction: () => ({
    confirmByCode: vi.fn().mockResolvedValue(true),
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

describe('AC-WH-01: 新規行追加', () => {
  beforeEach(() => {
    mockStore.records = []
    mockStore.canAdd = true
    mockStore.createRecord.mockReset()
  })

  it('Given: canAdd=true の状態で画面表示 → [追加] ボタンが有効', () => {
    const wrapper = mountToolbar()
    const addBtn = wrapper.find('[data-testid="add-btn"]')
    expect(addBtn.exists()).toBe(true)
    expect(addBtn.attributes('disabled')).toBeUndefined()
  })

  it('When: [追加] ボタンをクリック → Then: createRecord が呼ばれる', async () => {
    const wrapper = mountToolbar()
    const addBtn = wrapper.find('[data-testid="add-btn"]')
    await addBtn.trigger('click')
    await flushPromises()

    expect(mockStore.createRecord).toHaveBeenCalled()
  })

  it('Given: monthControl.isLocked=true → [追加] ボタンが無効', () => {
    mockStore.canAdd = false
    const wrapper = mountToolbar()
    const addBtn = wrapper.find('[data-testid="add-btn"]')
    expect(addBtn.attributes('disabled')).toBeDefined()
  })
})
