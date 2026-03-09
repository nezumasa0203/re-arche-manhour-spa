import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import TextCell from '~/components/work-hours/cells/TextCell.vue'

const mockUpdateField = vi.fn()

vi.mock('~/stores/workHours', () => ({
  useWorkHoursStore: () => ({
    updateField: mockUpdateField,
  }),
}))

function mountTextCell(props: Record<string, unknown> = {}) {
  const field = (props.field ?? 'tmrNo') as string
  return mount(TextCell, {
    props: {
      recordId: 1,
      field: 'tmrNo',
      value: '',
      editable: true,
      ...props,
    },
    global: {
      plugins: [createPinia()],
      stubs: {
        'InputText': {
          template: `<input :value="modelValue" data-testid="${field}-input" @input="$emit('update:modelValue', $event.target.value)" @blur="$emit('blur')" />`,
          props: ['modelValue', 'maxlength'],
        },
      },
    },
  })
}

/** field に応じた display セレクタ */
function displaySelector(field: string): string {
  return `[data-testid="${field}-display"]`
}

/** field に応じた input セレクタ */
function inputSelector(field: string): string {
  return `[data-testid="${field}-input"]`
}

describe('TextCell', () => {
  beforeEach(() => {
    mockUpdateField.mockReset()
  })

  // ─── 表示モード ──────────────────────────────────────────────

  describe('表示モード', () => {
    it('値が表示される', () => {
      const wrapper = mountTextCell({ value: '12345' })
      expect(wrapper.text()).toContain('12345')
    })

    it('空値は空欄で表示される', () => {
      const wrapper = mountTextCell({ value: '' })
      const display = wrapper.find(displaySelector('tmrNo'))
      expect(display.text().trim()).toBe('')
    })
  })

  // ─── 編集可否 ─────────────────────────────────────────────

  describe('編集可否', () => {
    it('editable=true のときクリックで InputText 表示', async () => {
      const wrapper = mountTextCell({ editable: true })
      await wrapper.find(displaySelector('tmrNo')).trigger('click')
      expect(wrapper.find(inputSelector('tmrNo')).exists()).toBe(true)
    })

    it('editable=false のときクリックしても InputText 非表示', async () => {
      const wrapper = mountTextCell({ editable: false })
      await wrapper.find(displaySelector('tmrNo')).trigger('click')
      expect(wrapper.find(inputSelector('tmrNo')).exists()).toBe(false)
    })
  })

  // ─── TMR番号バリデーション（VR-011）───────────────────────

  describe('TMR番号（field=tmrNo）', () => {
    it('5文字以内の半角数字は有効', async () => {
      const wrapper = mountTextCell({ field: 'tmrNo', recordId: 1 })
      await wrapper.find(displaySelector('tmrNo')).trigger('click')
      const input = wrapper.find(inputSelector('tmrNo'))
      await input.setValue('12345')
      await input.trigger('blur')
      await flushPromises()
      expect(mockUpdateField).toHaveBeenCalledWith(1, 'tmrNo', '12345')
    })

    it('6文字以上はエラー', async () => {
      const wrapper = mountTextCell({ field: 'tmrNo' })
      await wrapper.find(displaySelector('tmrNo')).trigger('click')
      const input = wrapper.find(inputSelector('tmrNo'))
      await input.setValue('123456')
      await input.trigger('blur')
      await flushPromises()
      expect(wrapper.find('.cell-error').exists()).toBe(true)
      expect(mockUpdateField).not.toHaveBeenCalled()
    })

    it('空値は許可（任意項目）', async () => {
      const wrapper = mountTextCell({ field: 'tmrNo', value: '123', recordId: 1 })
      await wrapper.find(displaySelector('tmrNo')).trigger('click')
      const input = wrapper.find(inputSelector('tmrNo'))
      await input.setValue('')
      await input.trigger('blur')
      await flushPromises()
      expect(wrapper.find('.cell-error').exists()).toBe(false)
      expect(mockUpdateField).toHaveBeenCalledWith(1, 'tmrNo', '')
    })
  })

  // ─── 依頼書Noバリデーション（VR-012）─────────────────────

  describe('依頼書No（field=workRequestNo）', () => {
    it('空値は許可', async () => {
      const wrapper = mountTextCell({ field: 'workRequestNo', recordId: 1 })
      await wrapper.find(displaySelector('workRequestNo')).trigger('click')
      const input = wrapper.find(inputSelector('workRequestNo'))
      await input.setValue('')
      await input.trigger('blur')
      await flushPromises()
      expect(wrapper.find('.cell-error').exists()).toBe(false)
    })

    it('7文字固定は有効', async () => {
      const wrapper = mountTextCell({ field: 'workRequestNo', recordId: 1 })
      await wrapper.find(displaySelector('workRequestNo')).trigger('click')
      const input = wrapper.find(inputSelector('workRequestNo'))
      await input.setValue('1234567')
      await input.trigger('blur')
      await flushPromises()
      expect(mockUpdateField).toHaveBeenCalledWith(1, 'workRequestNo', '1234567')
    })

    it('1〜6文字はエラー（CZ-137）', async () => {
      const wrapper = mountTextCell({ field: 'workRequestNo' })
      await wrapper.find(displaySelector('workRequestNo')).trigger('click')
      const input = wrapper.find(inputSelector('workRequestNo'))
      await input.setValue('123')
      await input.trigger('blur')
      await flushPromises()
      expect(wrapper.find('.cell-error').exists()).toBe(true)
      expect(mockUpdateField).not.toHaveBeenCalled()
    })
  })

  // ─── 依頼者名バリデーション（VR-013）─────────────────────

  describe('依頼者名（field=workRequesterName）', () => {
    it('40文字以内は有効', async () => {
      const wrapper = mountTextCell({ field: 'workRequesterName', recordId: 1 })
      await wrapper.find(displaySelector('workRequesterName')).trigger('click')
      const input = wrapper.find(inputSelector('workRequesterName'))
      await input.setValue('山田太郎')
      await input.trigger('blur')
      await flushPromises()
      expect(mockUpdateField).toHaveBeenCalledWith(1, 'workRequesterName', '山田太郎')
    })

    it('41文字以上はエラー', async () => {
      const wrapper = mountTextCell({ field: 'workRequesterName' })
      await wrapper.find(displaySelector('workRequesterName')).trigger('click')
      const input = wrapper.find(inputSelector('workRequesterName'))
      await input.setValue('あ'.repeat(41))
      await input.trigger('blur')
      await flushPromises()
      expect(wrapper.find('.cell-error').exists()).toBe(true)
      expect(mockUpdateField).not.toHaveBeenCalled()
    })
  })

  // ─── PATCH API 呼出 ──────────────────────────────────────────

  describe('変更時 PATCH', () => {
    it('値が変わらない場合は PATCH しない', async () => {
      const wrapper = mountTextCell({ value: 'abc', field: 'tmrNo' })
      await wrapper.find(displaySelector('tmrNo')).trigger('click')
      const input = wrapper.find(inputSelector('tmrNo'))
      await input.setValue('abc')
      await input.trigger('blur')
      await flushPromises()
      expect(mockUpdateField).not.toHaveBeenCalled()
    })
  })
})
