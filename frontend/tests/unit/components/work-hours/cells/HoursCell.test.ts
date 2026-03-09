import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import HoursCell from '~/components/work-hours/cells/HoursCell.vue'

const mockUpdateField = vi.fn()

vi.mock('~/stores/workHours', () => ({
  useWorkHoursStore: () => ({
    updateField: mockUpdateField,
  }),
}))

function mountHoursCell(props: Record<string, unknown> = {}) {
  return mount(HoursCell, {
    props: {
      recordId: 1,
      hours: '03:30',
      editable: true,
      ...props,
    },
    global: {
      plugins: [createPinia()],
      stubs: {
        'InputText': {
          template: '<input :value="modelValue" data-testid="hours-input" @input="$emit(\'update:modelValue\', $event.target.value)" @blur="$emit(\'blur\')" />',
          props: ['modelValue', 'placeholder'],
        },
      },
    },
  })
}

describe('HoursCell', () => {
  beforeEach(() => {
    mockUpdateField.mockReset()
  })

  // ─── 表示モード ──────────────────────────────────────────────

  describe('表示モード', () => {
    it('HH:MM 形式で表示される', () => {
      const wrapper = mountHoursCell({ hours: '03:30' })
      expect(wrapper.text()).toContain('03:30')
    })

    it('空値は空欄で表示される', () => {
      const wrapper = mountHoursCell({ hours: '' })
      const display = wrapper.find('[data-testid="hours-display"]')
      expect(display.text().trim()).toBe('')
    })
  })

  // ─── 編集可否 ─────────────────────────────────────────────

  describe('編集可否', () => {
    it('editable=true のときクリックで InputText 表示', async () => {
      const wrapper = mountHoursCell({ editable: true })
      await wrapper.find('[data-testid="hours-display"]').trigger('click')
      expect(wrapper.find('[data-testid="hours-input"]').exists()).toBe(true)
    })

    it('editable=false のときクリックしても InputText 非表示', async () => {
      const wrapper = mountHoursCell({ editable: false })
      await wrapper.find('[data-testid="hours-display"]').trigger('click')
      expect(wrapper.find('[data-testid="hours-input"]').exists()).toBe(false)
    })
  })

  // ─── 自動変換 ────────────────────────────────────────────────

  describe('自動変換', () => {
    it('1桁 "3" → "03:00" に変換して PATCH', async () => {
      const wrapper = mountHoursCell({ recordId: 1 })
      await wrapper.find('[data-testid="hours-display"]').trigger('click')
      const input = wrapper.find('[data-testid="hours-input"]')
      await input.setValue('3')
      await input.trigger('blur')
      await flushPromises()
      expect(mockUpdateField).toHaveBeenCalledWith(1, 'hours', '03:00')
    })

    it('2桁 "12" → "12:00" に変換して PATCH', async () => {
      const wrapper = mountHoursCell({ recordId: 1 })
      await wrapper.find('[data-testid="hours-display"]').trigger('click')
      const input = wrapper.find('[data-testid="hours-input"]')
      await input.setValue('12')
      await input.trigger('blur')
      await flushPromises()
      expect(mockUpdateField).toHaveBeenCalledWith(1, 'hours', '12:00')
    })

    it('3桁 "330" → "03:30" に変換して PATCH', async () => {
      const wrapper = mountHoursCell({ recordId: 1, hours: '01:00' })
      await wrapper.find('[data-testid="hours-display"]').trigger('click')
      const input = wrapper.find('[data-testid="hours-input"]')
      await input.setValue('330')
      await input.trigger('blur')
      await flushPromises()
      expect(mockUpdateField).toHaveBeenCalledWith(1, 'hours', '03:30')
    })

    it('4桁 "1230" → "12:30" に変換して PATCH', async () => {
      const wrapper = mountHoursCell({ recordId: 1 })
      await wrapper.find('[data-testid="hours-display"]').trigger('click')
      const input = wrapper.find('[data-testid="hours-input"]')
      await input.setValue('1230')
      await input.trigger('blur')
      await flushPromises()
      expect(mockUpdateField).toHaveBeenCalledWith(1, 'hours', '12:30')
    })

    it('コロン付き "3:30" → "03:30" に変換して PATCH', async () => {
      const wrapper = mountHoursCell({ recordId: 1, hours: '01:00' })
      await wrapper.find('[data-testid="hours-display"]').trigger('click')
      const input = wrapper.find('[data-testid="hours-input"]')
      await input.setValue('3:30')
      await input.trigger('blur')
      await flushPromises()
      expect(mockUpdateField).toHaveBeenCalledWith(1, 'hours', '03:30')
    })
  })

  // ─── バリデーション ──────────────────────────────────────────

  describe('バリデーション', () => {
    it('空値でエラー（VR-008 必須）', async () => {
      const wrapper = mountHoursCell()
      await wrapper.find('[data-testid="hours-display"]').trigger('click')
      const input = wrapper.find('[data-testid="hours-input"]')
      await input.setValue('')
      await input.trigger('blur')
      await flushPromises()
      expect(wrapper.find('.cell-error').exists()).toBe(true)
      expect(mockUpdateField).not.toHaveBeenCalled()
    })

    it('15分単位でない値でエラー（VR-009）', async () => {
      const wrapper = mountHoursCell()
      await wrapper.find('[data-testid="hours-display"]').trigger('click')
      const input = wrapper.find('[data-testid="hours-input"]')
      await input.setValue('3:10')
      await input.trigger('blur')
      await flushPromises()
      expect(wrapper.find('.cell-error').exists()).toBe(true)
      expect(mockUpdateField).not.toHaveBeenCalled()
    })

    it('不正形式でエラー', async () => {
      const wrapper = mountHoursCell()
      await wrapper.find('[data-testid="hours-display"]').trigger('click')
      const input = wrapper.find('[data-testid="hours-input"]')
      await input.setValue('abc')
      await input.trigger('blur')
      await flushPromises()
      expect(wrapper.find('.cell-error').exists()).toBe(true)
      expect(mockUpdateField).not.toHaveBeenCalled()
    })
  })

  // ─── PATCH API 呼出 ──────────────────────────────────────────

  describe('変更時 PATCH', () => {
    it('有効な工数変更で updateField が呼ばれる', async () => {
      const wrapper = mountHoursCell({ recordId: 10 })
      await wrapper.find('[data-testid="hours-display"]').trigger('click')
      const input = wrapper.find('[data-testid="hours-input"]')
      await input.setValue('05:00')
      await input.trigger('blur')
      await flushPromises()
      expect(mockUpdateField).toHaveBeenCalledWith(10, 'hours', '05:00')
    })

    it('値が変わらない場合は PATCH しない', async () => {
      const wrapper = mountHoursCell({ hours: '03:30' })
      await wrapper.find('[data-testid="hours-display"]').trigger('click')
      const input = wrapper.find('[data-testid="hours-input"]')
      await input.setValue('03:30')
      await input.trigger('blur')
      await flushPromises()
      expect(mockUpdateField).not.toHaveBeenCalled()
    })
  })
})
