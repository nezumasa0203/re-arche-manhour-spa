import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import StatusFooter from '~/components/work-hours/StatusFooter.vue'

const mockStore = {
  canBatchConfirm: true,
  canBatchRevert: false,
  statusCounts: { '0': 3, '1': 5, '2': 10 },
  message: null as { type: string; text: string } | null,
  batchConfirm: vi.fn(),
  batchRevert: vi.fn(),
}

vi.mock('~/stores/workHours', () => ({
  useWorkHoursStore: () => mockStore,
}))

const mockConfirm = vi.fn().mockResolvedValue(true)
vi.mock('~/composables/useConfirmAction', () => ({
  useConfirmAction: () => ({
    confirm: mockConfirm,
    confirmByCode: mockConfirm,
  }),
}))

function mountStatusFooter() {
  return mount(StatusFooter, {
    global: {
      plugins: [createPinia()],
      stubs: {
        'Button': {
          template: '<button :disabled="disabled" @click="$emit(\'click\')"><slot>{{ label }}</slot></button>',
          props: ['label', 'icon', 'disabled', 'severity'],
        },
      },
    },
  })
}

describe('StatusFooter', () => {
  beforeEach(() => {
    mockStore.canBatchConfirm = true
    mockStore.canBatchRevert = false
    mockStore.statusCounts = { '0': 3, '1': 5, '2': 10 }
    mockStore.message = null
    mockStore.batchConfirm.mockReset()
    mockStore.batchRevert.mockReset()
    mockConfirm.mockReset().mockResolvedValue(true)
  })

  // ─── 一括確認ボタン ──────────────────────────────────────────

  describe('一括確認ボタン', () => {
    it('canBatchConfirm=true のとき表示される', () => {
      mockStore.canBatchConfirm = true
      const wrapper = mountStatusFooter()
      expect(wrapper.find('[data-testid="batch-confirm-btn"]').exists()).toBe(true)
    })

    it('canBatchConfirm=false のとき非表示', () => {
      mockStore.canBatchConfirm = false
      const wrapper = mountStatusFooter()
      expect(wrapper.find('[data-testid="batch-confirm-btn"]').exists()).toBe(false)
    })

    it('クリックで確認ダイアログ後に batchConfirm が呼ばれる', async () => {
      mockConfirm.mockResolvedValueOnce(true)
      const wrapper = mountStatusFooter()
      await wrapper.find('[data-testid="batch-confirm-btn"]').trigger('click')
      await flushPromises()
      expect(mockStore.batchConfirm).toHaveBeenCalled()
    })

    it('キャンセル時は batchConfirm しない', async () => {
      mockConfirm.mockImplementation(() => Promise.resolve(false))
      const wrapper = mountStatusFooter()
      await wrapper.find('[data-testid="batch-confirm-btn"]').trigger('click')
      await flushPromises()
      expect(mockStore.batchConfirm).not.toHaveBeenCalled()
    })
  })

  // ─── 一括作成中ボタン ────────────────────────────────────────

  describe('一括作成中ボタン', () => {
    it('canBatchRevert=true のとき表示される', () => {
      mockStore.canBatchRevert = true
      const wrapper = mountStatusFooter()
      expect(wrapper.find('[data-testid="batch-revert-btn"]').exists()).toBe(true)
    })

    it('canBatchRevert=false のとき非表示', () => {
      mockStore.canBatchRevert = false
      const wrapper = mountStatusFooter()
      expect(wrapper.find('[data-testid="batch-revert-btn"]').exists()).toBe(false)
    })
  })

  // ─── ステータス件数 ──────────────────────────────────────────

  describe('ステータス件数', () => {
    it('各ステータスの件数が表示される', () => {
      const wrapper = mountStatusFooter()
      expect(wrapper.find('[data-testid="count-status-0"]').text()).toContain('3')
      expect(wrapper.find('[data-testid="count-status-1"]').text()).toContain('5')
      expect(wrapper.find('[data-testid="count-status-2"]').text()).toContain('10')
    })

    it('STATUS_0 > 0 のとき赤文字クラスが付く', () => {
      mockStore.statusCounts = { '0': 2, '1': 0, '2': 0 }
      const wrapper = mountStatusFooter()
      expect(wrapper.find('[data-testid="count-status-0"]').classes()).toContain('text-danger')
    })

    it('STATUS_0 = 0 のとき赤文字クラスなし', () => {
      mockStore.statusCounts = { '0': 0, '1': 5, '2': 10 }
      const wrapper = mountStatusFooter()
      expect(wrapper.find('[data-testid="count-status-0"]').classes()).not.toContain('text-danger')
    })
  })

  // ─── メッセージエリア ────────────────────────────────────────

  describe('メッセージエリア', () => {
    it('message=null のときメッセージ非表示', () => {
      mockStore.message = null
      const wrapper = mountStatusFooter()
      expect(wrapper.find('[data-testid="footer-message"]').exists()).toBe(false)
    })

    it('成功メッセージが緑文字で表示される', () => {
      mockStore.message = { type: 'success', text: '保存しました' }
      const wrapper = mountStatusFooter()
      const msg = wrapper.find('[data-testid="footer-message"]')
      expect(msg.exists()).toBe(true)
      expect(msg.text()).toContain('保存しました')
      expect(msg.classes()).toContain('text-success')
    })

    it('エラーメッセージが赤文字で表示される', () => {
      mockStore.message = { type: 'error', text: 'エラーが発生しました' }
      const wrapper = mountStatusFooter()
      const msg = wrapper.find('[data-testid="footer-message"]')
      expect(msg.classes()).toContain('text-error')
    })
  })
})
