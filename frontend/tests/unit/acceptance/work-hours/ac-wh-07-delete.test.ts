/**
 * AC-WH-07: 削除操作（US-012）
 * Given: STATUS_0 のレコードが選択されている
 * When: [削除] ボタンをクリックし確認ダイアログで「はい」を選択する
 * Then: deleteRecords が呼ばれ、レコードが DataTable から消える
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import Toolbar from '~/components/work-hours/Toolbar.vue'

const mockStore = {
  records: [{ id: 1, status: '0' }, { id: 2, status: '0' }],
  summary: { monthlyTotal: 6, dailyTotal: 3 },
  selectedIds: [1] as number[],
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
  canCopy: true,
  canDelete: true,
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

describe('AC-WH-07: 削除操作', () => {
  beforeEach(() => {
    mockStore.selectedIds = [1]
    mockStore.canDelete = true
    mockStore.deleteRecords.mockReset()
    mockConfirmByCode.mockReset().mockResolvedValue(true)
  })

  it('Given: レコードが選択されている → 削除ボタンが有効', () => {
    const wrapper = mountToolbar()
    const deleteBtn = wrapper.find('[data-testid="delete-btn"]')
    expect(deleteBtn.exists()).toBe(true)
    expect(deleteBtn.attributes('disabled')).toBeUndefined()
  })

  it('When: 削除クリック → 確認ダイアログ「はい」→ Then: deleteRecords が呼ばれる', async () => {
    const wrapper = mountToolbar()
    const deleteBtn = wrapper.find('[data-testid="delete-btn"]')
    await deleteBtn.trigger('click')
    await flushPromises()

    expect(mockConfirmByCode).toHaveBeenCalled()
    expect(mockStore.deleteRecords).toHaveBeenCalledWith([1])
  })

  it('確認ダイアログで「いいえ」→ deleteRecords は呼ばれない', async () => {
    mockConfirmByCode.mockImplementation(() => Promise.resolve(false))
    const wrapper = mountToolbar()
    const deleteBtn = wrapper.find('[data-testid="delete-btn"]')
    await deleteBtn.trigger('click')
    await flushPromises()

    expect(mockStore.deleteRecords).not.toHaveBeenCalled()
  })
})
