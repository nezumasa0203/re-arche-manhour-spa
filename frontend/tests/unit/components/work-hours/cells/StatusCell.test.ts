import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import StatusCell from '~/components/work-hours/cells/StatusCell.vue'

const mockUpdateField = vi.fn()

vi.mock('~/stores/workHours', () => ({
  useWorkHoursStore: () => ({
    updateField: mockUpdateField,
    permissions: { canEdit: false },
  }),
}))

function mountStatusCell(props: Record<string, unknown> = {}) {
  return mount(StatusCell, {
    props: {
      recordId: 1,
      status: '0',
      isManager: false,
      ...props,
    },
    global: {
      plugins: [createPinia()],
      stubs: {
        'Dropdown': {
          template: '<select :value="modelValue" @change="$emit(\'update:modelValue\', $event.target.value)"><option v-for="opt in options" :key="opt.value" :value="opt.value">{{ opt.label }}</option></select>',
          props: ['modelValue', 'options'],
        },
      },
    },
  })
}

describe('StatusCell', () => {
  beforeEach(() => {
    mockUpdateField.mockReset()
  })

  // ─── 表示モード（色付きバッジ） ─────────────────────────────

  describe('表示モード', () => {
    it('STATUS_0 で「作成中」ラベルが表示される', () => {
      const wrapper = mountStatusCell({ status: '0' })
      expect(wrapper.text()).toContain('作成中')
    })

    it('STATUS_1 で「確認」ラベルが表示される', () => {
      const wrapper = mountStatusCell({ status: '1' })
      expect(wrapper.text()).toContain('確認')
    })

    it('STATUS_2 で「確定」ラベルが表示される', () => {
      const wrapper = mountStatusCell({ status: '2' })
      expect(wrapper.text()).toContain('確定')
    })

    it('STATUS_9 で「非表示」ラベルが表示される', () => {
      const wrapper = mountStatusCell({ status: '9' })
      expect(wrapper.text()).toContain('非表示')
    })

    it('STATUS_0 で黄色背景クラスが適用される', () => {
      const wrapper = mountStatusCell({ status: '0' })
      const badge = wrapper.find('[data-testid="status-badge"]')
      expect(badge.classes()).toContain('status-0')
    })

    it('STATUS_1 で緑背景クラスが適用される', () => {
      const wrapper = mountStatusCell({ status: '1' })
      const badge = wrapper.find('[data-testid="status-badge"]')
      expect(badge.classes()).toContain('status-1')
    })

    it('STATUS_2 で青背景クラスが適用される', () => {
      const wrapper = mountStatusCell({ status: '2' })
      const badge = wrapper.find('[data-testid="status-badge"]')
      expect(badge.classes()).toContain('status-2')
    })

    it('STATUS_9 で灰背景クラスが適用される', () => {
      const wrapper = mountStatusCell({ status: '9' })
      const badge = wrapper.find('[data-testid="status-badge"]')
      expect(badge.classes()).toContain('status-9')
    })
  })

  // ─── 編集モード ──────────────────────────────────────────────

  describe('編集モード', () => {
    it('セルクリックで編集モードに遷移する', async () => {
      const wrapper = mountStatusCell({ status: '0' })
      await wrapper.find('[data-testid="status-badge"]').trigger('click')
      expect(wrapper.find('[data-testid="status-dropdown"]').exists()).toBe(true)
    })

    it('常時編集可（isEditable とは独立）', async () => {
      const wrapper = mountStatusCell({ status: '2' })
      await wrapper.find('[data-testid="status-badge"]').trigger('click')
      expect(wrapper.find('[data-testid="status-dropdown"]').exists()).toBe(true)
    })

    it('STATUS_9 は編集不可', async () => {
      const wrapper = mountStatusCell({ status: '9' })
      await wrapper.find('[data-testid="status-badge"]').trigger('click')
      expect(wrapper.find('[data-testid="status-dropdown"]').exists()).toBe(false)
    })
  })

  // ─── ステータス遷移ルール ─────────────────────────────────

  describe('担当者系列（isManager=false）', () => {
    it('STATUS_0 のとき選択肢は 0, 1 のみ', async () => {
      const wrapper = mountStatusCell({ status: '0', isManager: false })
      await wrapper.find('[data-testid="status-badge"]').trigger('click')
      const options = wrapper.findAll('[data-testid="status-dropdown"] option')
      const values = options.map(o => o.attributes('value'))
      expect(values).toContain('0')
      expect(values).toContain('1')
      expect(values).not.toContain('2')
    })

    it('STATUS_1 のとき選択肢は 0, 1 のみ', async () => {
      const wrapper = mountStatusCell({ status: '1', isManager: false })
      await wrapper.find('[data-testid="status-badge"]').trigger('click')
      const options = wrapper.findAll('[data-testid="status-dropdown"] option')
      const values = options.map(o => o.attributes('value'))
      expect(values).toContain('0')
      expect(values).toContain('1')
      expect(values).not.toContain('2')
    })
  })

  describe('管理者系列（isManager=true）', () => {
    it('STATUS_0 のとき全遷移可（0, 1, 2）', async () => {
      const wrapper = mountStatusCell({ status: '0', isManager: true })
      await wrapper.find('[data-testid="status-badge"]').trigger('click')
      const options = wrapper.findAll('[data-testid="status-dropdown"] option')
      const values = options.map(o => o.attributes('value'))
      expect(values).toContain('0')
      expect(values).toContain('1')
      expect(values).toContain('2')
    })

    it('STATUS_2 のとき全遷移可（0, 1, 2）', async () => {
      const wrapper = mountStatusCell({ status: '2', isManager: true })
      await wrapper.find('[data-testid="status-badge"]').trigger('click')
      const options = wrapper.findAll('[data-testid="status-dropdown"] option')
      const values = options.map(o => o.attributes('value'))
      expect(values).toContain('0')
      expect(values).toContain('1')
      expect(values).toContain('2')
    })
  })

  // ─── PATCH API 呼出 ──────────────────────────────────────────

  describe('変更時 PATCH', () => {
    it('ステータス変更時に updateField が呼ばれる', async () => {
      const wrapper = mountStatusCell({ recordId: 42, status: '0' })
      await wrapper.find('[data-testid="status-badge"]').trigger('click')
      const dropdown = wrapper.find('[data-testid="status-dropdown"]')
      await dropdown.setValue('1')
      await flushPromises()
      expect(mockUpdateField).toHaveBeenCalledWith(42, 'status', '1')
    })
  })
})
