import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import SubjectCell from '~/components/work-hours/cells/SubjectCell.vue'

const mockUpdateField = vi.fn()

vi.mock('~/stores/workHours', () => ({
  useWorkHoursStore: () => ({
    updateField: mockUpdateField,
  }),
}))

function mountSubjectCell(props: Record<string, unknown> = {}) {
  return mount(SubjectCell, {
    props: {
      recordId: 1,
      subject: 'テスト件名',
      editable: true,
      ...props,
    },
    global: {
      plugins: [createPinia()],
      stubs: {
        'InputText': {
          template: '<input :value="modelValue" data-testid="subject-input" @input="$emit(\'update:modelValue\', $event.target.value)" @blur="$emit(\'blur\')" />',
          props: ['modelValue', 'maxlength'],
        },
      },
    },
  })
}

describe('SubjectCell', () => {
  beforeEach(() => {
    mockUpdateField.mockReset()
  })

  // ─── 表示モード ──────────────────────────────────────────────

  describe('表示モード', () => {
    it('件名テキストが表示される', () => {
      const wrapper = mountSubjectCell({ subject: 'テスト件名です' })
      expect(wrapper.text()).toContain('テスト件名です')
    })

    it('長い件名は30文字で折り返し表示される', () => {
      const longSubject = 'あ'.repeat(40)
      const wrapper = mountSubjectCell({ subject: longSubject })
      const display = wrapper.find('[data-testid="subject-display"]')
      expect(display.classes()).toContain('subject-truncate')
    })
  })

  // ─── 編集可否 ─────────────────────────────────────────────

  describe('編集可否', () => {
    it('editable=true のときクリックで InputText 表示', async () => {
      const wrapper = mountSubjectCell({ editable: true })
      await wrapper.find('[data-testid="subject-display"]').trigger('click')
      expect(wrapper.find('[data-testid="subject-input"]').exists()).toBe(true)
    })

    it('editable=false のときクリックしても InputText 非表示', async () => {
      const wrapper = mountSubjectCell({ editable: false })
      await wrapper.find('[data-testid="subject-display"]').trigger('click')
      expect(wrapper.find('[data-testid="subject-input"]').exists()).toBe(false)
    })
  })

  // ─── バリデーション ──────────────────────────────────────────

  describe('バリデーション', () => {
    it('空値でエラー（VR-006 必須）', async () => {
      const wrapper = mountSubjectCell({ subject: '' })
      await wrapper.find('[data-testid="subject-display"]').trigger('click')
      const input = wrapper.find('[data-testid="subject-input"]')
      await input.setValue('')
      await input.trigger('blur')
      await flushPromises()
      expect(wrapper.find('.cell-error').exists()).toBe(true)
    })

    it('128バイト超過でエラー（VR-006）', async () => {
      const wrapper = mountSubjectCell()
      await wrapper.find('[data-testid="subject-display"]').trigger('click')
      const input = wrapper.find('[data-testid="subject-input"]')
      // 全角65文字 = 130バイト > 128
      await input.setValue('あ'.repeat(65))
      await input.trigger('blur')
      await flushPromises()
      expect(wrapper.find('.cell-error').exists()).toBe(true)
    })

    it('改行コードが自動除去される', async () => {
      const wrapper = mountSubjectCell({ subject: '元の件名' })
      await wrapper.find('[data-testid="subject-display"]').trigger('click')
      const input = wrapper.find('[data-testid="subject-input"]')
      await input.setValue('テスト\n件名')
      await input.trigger('blur')
      await flushPromises()
      expect(mockUpdateField).toHaveBeenCalledWith(1, 'subject', 'テスト件名')
    })
  })

  // ─── PATCH API 呼出 ──────────────────────────────────────────

  describe('変更時 PATCH', () => {
    it('件名変更時に updateField が呼ばれる', async () => {
      const wrapper = mountSubjectCell({ recordId: 3 })
      await wrapper.find('[data-testid="subject-display"]').trigger('click')
      const input = wrapper.find('[data-testid="subject-input"]')
      await input.setValue('新しい件名')
      await input.trigger('blur')
      await flushPromises()
      expect(mockUpdateField).toHaveBeenCalledWith(3, 'subject', '新しい件名')
    })
  })
})
