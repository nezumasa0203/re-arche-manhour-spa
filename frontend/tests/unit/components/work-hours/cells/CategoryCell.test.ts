import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import CategoryCell from '~/components/work-hours/cells/CategoryCell.vue'

const mockUpdateField = vi.fn()

vi.mock('~/stores/workHours', () => ({
  useWorkHoursStore: () => ({
    updateField: mockUpdateField,
  }),
}))

const CATEGORIES = [
  { categoryCode: '01', categoryName: '障害対応' },
  { categoryCode: '02', categoryName: '保守' },
  { categoryCode: '03', categoryName: '開発' },
]

function mountCategoryCell(props: Record<string, unknown> = {}) {
  return mount(CategoryCell, {
    props: {
      recordId: 1,
      categoryCode: '01',
      categoryName: '障害対応',
      categories: CATEGORIES,
      editable: true,
      ...props,
    },
    global: {
      plugins: [createPinia()],
      stubs: {
        'Dropdown': {
          template: '<select :value="modelValue" data-testid="category-dropdown" @change="$emit(\'update:modelValue\', $event.target.value)"><option v-for="opt in options" :key="opt.value" :value="opt.value">{{ opt.label }}</option></select>',
          props: ['modelValue', 'options'],
        },
      },
    },
  })
}

describe('CategoryCell', () => {
  beforeEach(() => {
    mockUpdateField.mockReset()
  })

  // ─── 表示モード ──────────────────────────────────────────────

  describe('表示モード', () => {
    it('カテゴリ名が表示される', () => {
      const wrapper = mountCategoryCell()
      expect(wrapper.text()).toContain('障害対応')
    })

    it('空のとき空欄で表示される', () => {
      const wrapper = mountCategoryCell({ categoryCode: '', categoryName: '' })
      const display = wrapper.find('[data-testid="category-display"]')
      expect(display.text().trim()).toBe('')
    })
  })

  // ─── 編集可否 ─────────────────────────────────────────────

  describe('編集可否', () => {
    it('editable=true のときクリックで Dropdown 表示', async () => {
      const wrapper = mountCategoryCell({ editable: true })
      await wrapper.find('[data-testid="category-display"]').trigger('click')
      expect(wrapper.find('[data-testid="category-dropdown"]').exists()).toBe(true)
    })

    it('editable=false のときクリックしても Dropdown 非表示', async () => {
      const wrapper = mountCategoryCell({ editable: false })
      await wrapper.find('[data-testid="category-display"]').trigger('click')
      expect(wrapper.find('[data-testid="category-dropdown"]').exists()).toBe(false)
    })
  })

  // ─── PATCH API 呼出 ──────────────────────────────────────────

  describe('変更時 PATCH', () => {
    it('カテゴリ変更時に updateField が呼ばれる', async () => {
      const wrapper = mountCategoryCell({ recordId: 7 })
      await wrapper.find('[data-testid="category-display"]').trigger('click')
      const dropdown = wrapper.find('[data-testid="category-dropdown"]')
      await dropdown.setValue('02')
      await flushPromises()
      expect(mockUpdateField).toHaveBeenCalledWith(7, 'categoryCode', '02')
    })
  })
})
