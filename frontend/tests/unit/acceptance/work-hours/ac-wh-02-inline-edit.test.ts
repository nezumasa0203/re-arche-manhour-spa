/**
 * AC-WH-02: インライン編集（US-011）
 * Given: DataTable に STATUS_0 のレコードが表示されている
 * When: 工数セルをクリックして "130" を入力し、フォーカスアウトする
 * Then: 値が "01:30" に自動変換され、PATCH API が送信されて保存される
 */
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
      hours: '00:00',
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

describe('AC-WH-02: インライン編集', () => {
  beforeEach(() => {
    mockUpdateField.mockReset()
  })

  it('Given: STATUS_0 レコードの工数セル → クリックで編集モードになる', async () => {
    const wrapper = mountHoursCell({ editable: true })
    await wrapper.find('[data-testid="hours-display"]').trigger('click')
    expect(wrapper.find('[data-testid="hours-input"]').exists()).toBe(true)
  })

  it('When: "130" を入力してフォーカスアウト → Then: "01:30" に変換され PATCH が呼ばれる', async () => {
    const wrapper = mountHoursCell({ recordId: 1, hours: '00:00', editable: true })
    await wrapper.find('[data-testid="hours-display"]').trigger('click')
    const input = wrapper.find('[data-testid="hours-input"]')
    await input.setValue('130')
    await input.trigger('blur')
    await flushPromises()

    expect(mockUpdateField).toHaveBeenCalledWith(1, 'hours', '01:30')
  })

  it('editable=false のとき編集モードにならない', async () => {
    const wrapper = mountHoursCell({ editable: false })
    await wrapper.find('[data-testid="hours-display"]').trigger('click')
    expect(wrapper.find('[data-testid="hours-input"]').exists()).toBe(false)
  })
})
