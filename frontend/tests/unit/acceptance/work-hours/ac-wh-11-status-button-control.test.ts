/**
 * AC-WH-11: ステータス別ボタン制御
 * Given: STATUS_1（確定済）のレコードが表示されている
 * When: 画面を確認する
 * Then: 編集不可（isEditable=false）、差戻可能（canBatchRevert=true）
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import StatusFooter from '~/components/work-hours/StatusFooter.vue'

const mockStore = {
  records: [
    { id: 1, status: '1' },
    { id: 2, status: '1' },
  ],
  canBatchConfirm: false,
  canBatchRevert: true,
  statusCounts: { '0': 0, '1': 2, '2': 0 },
  message: null,
  batchConfirm: vi.fn(),
  batchRevert: vi.fn(),
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

function mountStatusFooter() {
  return mount(StatusFooter, {
    global: {
      plugins: [createPinia()],
      stubs: {
        'Button': {
          template: '<button :disabled="disabled" @click="$emit(\'click\')"><slot>{{ label }}</slot></button>',
          props: ['label', 'icon', 'disabled', 'severity', 'size'],
        },
        'Tag': {
          template: '<span class="p-tag">{{ value }}</span>',
          props: ['value', 'severity'],
        },
      },
    },
  })
}

describe('AC-WH-11: ステータス別ボタン制御', () => {
  beforeEach(() => {
    mockStore.canBatchConfirm = false
    mockStore.canBatchRevert = true
    mockStore.statusCounts = { '0': 0, '1': 2, '2': 0 }
    mockStore.batchConfirm.mockReset()
    mockStore.batchRevert.mockReset()
    mockConfirmByCode.mockReset().mockResolvedValue(true)
  })

  it('Given: STATUS_1 のみ（canBatchConfirm=false）→ 一括確定ボタンが非表示', () => {
    const wrapper = mountStatusFooter()
    const confirmBtn = wrapper.find('[data-testid="batch-confirm-btn"]')
    expect(confirmBtn.exists()).toBe(false)
  })

  it('Given: STATUS_1 あり → 差戻ボタンが有効', () => {
    const wrapper = mountStatusFooter()
    const revertBtn = wrapper.find('[data-testid="batch-revert-btn"]')
    expect(revertBtn.exists()).toBe(true)
    expect(revertBtn.attributes('disabled')).toBeUndefined()
  })

  it('When: 差戻ボタンクリック → 確認ダイアログ → batchRevert が呼ばれる', async () => {
    const wrapper = mountStatusFooter()
    const revertBtn = wrapper.find('[data-testid="batch-revert-btn"]')
    await revertBtn.trigger('click')
    await (await import('@vue/test-utils')).flushPromises()

    expect(mockStore.batchRevert).toHaveBeenCalled()
  })

  it('STATUS_1 の件数が表示される', () => {
    const wrapper = mountStatusFooter()
    expect(wrapper.text()).toContain('2')
  })
})
