/**
 * AC-WH-03: 短縮入力変換
 * Given: HoursCell が編集モードである
 * When: "8" を入力してフォーカスアウトする
 * Then: "08:00" に自動変換される
 * 追加パターン: "3"→"03:00", "12"→"12:00", "330"→"03:30", "1230"→"12:30"
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

async function inputAndBlur(wrapper: ReturnType<typeof mountHoursCell>, value: string) {
  await wrapper.find('[data-testid="hours-display"]').trigger('click')
  const input = wrapper.find('[data-testid="hours-input"]')
  await input.setValue(value)
  await input.trigger('blur')
  await flushPromises()
}

describe('AC-WH-03: 短縮入力変換', () => {
  beforeEach(() => {
    mockUpdateField.mockReset()
  })

  it.each([
    { input: '8', expected: '08:00' },
    { input: '3', expected: '03:00' },
    { input: '12', expected: '12:00' },
    { input: '330', expected: '03:30' },
    { input: '1230', expected: '12:30' },
    { input: '130', expected: '01:30' },
    { input: '3:30', expected: '03:30' },
    { input: '12:30', expected: '12:30' },
  ])('入力 "$input" → "$expected" に自動変換', async ({ input, expected }) => {
    const wrapper = mountHoursCell({ recordId: 1, hours: '00:00' })
    await inputAndBlur(wrapper, input)
    expect(mockUpdateField).toHaveBeenCalledWith(1, 'hours', expected)
  })
})
