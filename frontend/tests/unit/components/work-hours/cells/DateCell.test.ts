import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import DateCell from '~/components/work-hours/cells/DateCell.vue'

const mockUpdateField = vi.fn()

vi.mock('~/stores/workHours', () => ({
  useWorkHoursStore: () => ({
    updateField: mockUpdateField,
    yearMonth: '2025-02',
  }),
}))

function mountDateCell(props: Record<string, unknown> = {}) {
  return mount(DateCell, {
    props: {
      recordId: 1,
      workDate: '2025-02-10',
      editable: true,
      yearMonth: '2025-02',
      ...props,
    },
    global: {
      plugins: [createPinia()],
      stubs: {
        'Calendar': {
          template: '<input :value="modelValue" data-testid="date-input" @change="$emit(\'update:modelValue\', $event.target.value)" @blur="$emit(\'blur\')" />',
          props: ['modelValue', 'dateFormat'],
        },
      },
    },
  })
}

describe('DateCell', () => {
  beforeEach(() => {
    mockUpdateField.mockReset()
  })

  // ─── 表示モード ──────────────────────────────────────────────

  describe('表示モード', () => {
    it('MM/DD 短縮表示される', () => {
      const wrapper = mountDateCell({ workDate: '2025-02-25' })
      expect(wrapper.text()).toContain('02/25')
    })

    it('別の日付でも正しく表示される', () => {
      const wrapper = mountDateCell({ workDate: '2025-12-01' })
      expect(wrapper.text()).toContain('12/01')
    })
  })

  // ─── 編集可否 ─────────────────────────────────────────────

  describe('編集可否', () => {
    it('editable=true のときクリックで編集モードに遷移', async () => {
      const wrapper = mountDateCell({ editable: true })
      await wrapper.find('[data-testid="date-display"]').trigger('click')
      expect(wrapper.find('[data-testid="date-input"]').exists()).toBe(true)
    })

    it('editable=false のときクリックしても編集モードにならない', async () => {
      const wrapper = mountDateCell({ editable: false })
      await wrapper.find('[data-testid="date-display"]').trigger('click')
      expect(wrapper.find('[data-testid="date-input"]').exists()).toBe(false)
    })
  })

  // ─── バリデーション ──────────────────────────────────────────

  describe('バリデーション', () => {
    it('空値でエラー表示（VR-001）', async () => {
      const wrapper = mountDateCell({ workDate: '' })
      await wrapper.find('[data-testid="date-display"]').trigger('click')
      const input = wrapper.find('[data-testid="date-input"]')
      await input.setValue('')
      await input.trigger('blur')
      await flushPromises()
      expect(wrapper.find('.cell-error').exists()).toBe(true)
    })
  })

  // ─── PATCH API 呼出 ──────────────────────────────────────────

  describe('変更時 PATCH', () => {
    it('日付変更時に updateField が呼ばれる', async () => {
      const wrapper = mountDateCell({ recordId: 5 })
      await wrapper.find('[data-testid="date-display"]').trigger('click')
      const input = wrapper.find('[data-testid="date-input"]')
      await input.setValue('2025-02-20')
      await input.trigger('blur')
      await flushPromises()
      expect(mockUpdateField).toHaveBeenCalledWith(5, 'workDate', '2025-02-20')
    })
  })
})
