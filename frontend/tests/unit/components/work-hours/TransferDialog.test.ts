import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import TransferDialog from '~/components/work-hours/TransferDialog.vue'

const mockTransferNextMonth = vi.fn()

vi.mock('~/stores/workHours', () => ({
  useWorkHoursStore: () => ({
    yearMonth: '2025-02',
    transferNextMonth: mockTransferNextMonth,
  }),
}))

function mountTransferDialog(props: Record<string, unknown> = {}) {
  return mount(TransferDialog, {
    props: {
      visible: true,
      selectedIds: [1, 2, 3],
      ...props,
    },
    global: {
      plugins: [createPinia()],
      stubs: {
        'Dialog': {
          template: '<div v-if="visible" class="dialog"><slot></slot><slot name="footer"></slot></div>',
          props: ['visible', 'header', 'modal'],
        },
        'Checkbox': {
          template: '<input type="checkbox" :checked="modelValue" @change="$emit(\'update:modelValue\', $event.target.checked)" />',
          props: ['modelValue', 'binary'],
        },
        'Button': {
          template: '<button :disabled="disabled" @click="$emit(\'click\')">{{ label }}</button>',
          props: ['label', 'disabled', 'severity'],
        },
      },
    },
  })
}

describe('TransferDialog', () => {
  beforeEach(() => {
    mockTransferNextMonth.mockReset()
  })

  describe('表示', () => {
    it('visible=true のときダイアログが表示される', () => {
      const wrapper = mountTransferDialog({ visible: true })
      expect(wrapper.find('.dialog').exists()).toBe(true)
    })

    it('visible=false のときダイアログが非表示', () => {
      const wrapper = mountTransferDialog({ visible: false })
      expect(wrapper.find('.dialog').exists()).toBe(false)
    })

    it('選択レコード件数が表示される', () => {
      const wrapper = mountTransferDialog({ selectedIds: [1, 2, 3] })
      expect(wrapper.text()).toContain('3')
    })

    it('転写先月チェックボックスが表示される（最大12ヶ月）', () => {
      const wrapper = mountTransferDialog()
      const checkboxes = wrapper.findAll('input[type="checkbox"]')
      expect(checkboxes.length).toBe(12)
    })

    it('カテゴリ年度不存在の注意書きが表示される', () => {
      const wrapper = mountTransferDialog()
      expect(wrapper.text()).toContain('カテゴリ')
    })
  })

  describe('転写実行', () => {
    it('転写先月を選択して実行すると transferNextMonth が呼ばれる', async () => {
      const wrapper = mountTransferDialog()
      // 最初のチェックボックスをチェック
      const checkboxes = wrapper.findAll('input[type="checkbox"]')
      await checkboxes[0].setValue(true)

      const execBtn = wrapper.find('[data-testid="transfer-exec-btn"]')
      await execBtn.trigger('click')
      await flushPromises()

      expect(mockTransferNextMonth).toHaveBeenCalledWith(
        [1, 2, 3],
        expect.arrayContaining([expect.stringMatching(/^\d{4}-\d{2}$/)])
      )
    })

    it('選択なしのとき転写ボタンが無効', () => {
      const wrapper = mountTransferDialog()
      const execBtn = wrapper.find('[data-testid="transfer-exec-btn"]')
      expect(execBtn.attributes('disabled')).toBeDefined()
    })
  })
})
