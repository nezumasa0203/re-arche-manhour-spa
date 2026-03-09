/**
 * AC-WH-04: 一括確定（US-015）
 * Given: STATUS_0 のレコードが3件あり、全て必須項目入力済み
 * When: [一括確定] をクリックし、CZ-505 確認ダイアログで「はい」を選択する
 * Then: batchConfirm が呼ばれ、StatusFooter が更新される
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import StatusFooter from '~/components/work-hours/StatusFooter.vue'

const mockStore = {
  records: [
    { id: 1, status: '0' },
    { id: 2, status: '0' },
    { id: 3, status: '0' },
  ],
  canBatchConfirm: true,
  canBatchRevert: false,
  statusCounts: { '0': 3, '1': 0, '2': 0 },
  message: null as { type: string; text: string } | null,
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

describe('AC-WH-04: 一括確定', () => {
  beforeEach(() => {
    mockStore.canBatchConfirm = true
    mockStore.canBatchRevert = false
    mockStore.statusCounts = { '0': 3, '1': 0, '2': 0 }
    mockStore.message = null
    mockStore.batchConfirm.mockReset()
    mockStore.batchRevert.mockReset()
    mockConfirmByCode.mockReset().mockResolvedValue(true)
  })

  it('Given: STATUS_0 が3件 → 一括確定ボタンが有効', () => {
    const wrapper = mountStatusFooter()
    const confirmBtn = wrapper.find('[data-testid="batch-confirm-btn"]')
    expect(confirmBtn.exists()).toBe(true)
    expect(confirmBtn.attributes('disabled')).toBeUndefined()
  })

  it('When: 一括確定クリック → 確認ダイアログ → Then: batchConfirm が呼ばれる', async () => {
    const wrapper = mountStatusFooter()
    const confirmBtn = wrapper.find('[data-testid="batch-confirm-btn"]')
    await confirmBtn.trigger('click')
    await flushPromises()

    expect(mockConfirmByCode).toHaveBeenCalled()
    expect(mockStore.batchConfirm).toHaveBeenCalled()
  })

  it('確認ダイアログで「いいえ」→ batchConfirm は呼ばれない', async () => {
    mockConfirmByCode.mockResolvedValue(false)
    const wrapper = mountStatusFooter()
    const confirmBtn = wrapper.find('[data-testid="batch-confirm-btn"]')
    await confirmBtn.trigger('click')
    await flushPromises()

    expect(mockStore.batchConfirm).not.toHaveBeenCalled()
  })

  it('ステータス件数が表示される', () => {
    const wrapper = mountStatusFooter()
    expect(wrapper.text()).toContain('3')
  })
})
