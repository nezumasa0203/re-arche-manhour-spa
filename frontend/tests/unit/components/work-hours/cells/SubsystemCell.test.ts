import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import SubsystemCell from '~/components/work-hours/cells/SubsystemCell.vue'

const mockUpdateField = vi.fn()

vi.mock('~/stores/workHours', () => ({
  useWorkHoursStore: () => ({
    updateField: mockUpdateField,
  }),
}))

function mountSubsystemCell(props: Record<string, unknown> = {}) {
  return mount(SubsystemCell, {
    props: {
      recordId: 1,
      subsystemNo: 'SUB001',
      subsystemName: '会計モジュール',
      editable: true,
      mode: 'target',
      ...props,
    },
    global: {
      plugins: [createPinia()],
    },
  })
}

describe('SubsystemCell', () => {
  beforeEach(() => {
    mockUpdateField.mockReset()
  })

  // ─── 表示モード ──────────────────────────────────────────────

  describe('表示モード', () => {
    it('サブシステムNo + 名前が表示される', () => {
      const wrapper = mountSubsystemCell()
      expect(wrapper.text()).toContain('SUB001')
      expect(wrapper.text()).toContain('会計モジュール')
    })

    it('空のとき空欄で表示される', () => {
      const wrapper = mountSubsystemCell({ subsystemNo: '', subsystemName: '' })
      const display = wrapper.find('[data-testid="subsystem-display"]')
      expect(display.text().trim()).toBe('')
    })
  })

  // ─── 編集可否 ─────────────────────────────────────────────

  describe('編集可否', () => {
    it('editable=true のときクリックでダイアログ起動イベントが発火', async () => {
      const wrapper = mountSubsystemCell({ editable: true })
      await wrapper.find('[data-testid="subsystem-display"]').trigger('click')
      expect(wrapper.emitted('open-search')).toBeTruthy()
    })

    it('editable=false のときクリックしてもイベントなし', async () => {
      const wrapper = mountSubsystemCell({ editable: false })
      await wrapper.find('[data-testid="subsystem-display"]').trigger('click')
      expect(wrapper.emitted('open-search')).toBeFalsy()
    })
  })

  // ─── mode 切替 ──────────────────────────────────────────────

  describe('mode', () => {
    it('mode=target で open-search イベントに mode が含まれる', async () => {
      const wrapper = mountSubsystemCell({ mode: 'target', editable: true })
      await wrapper.find('[data-testid="subsystem-display"]').trigger('click')
      expect(wrapper.emitted('open-search')![0]).toEqual([{ recordId: 1, mode: 'target' }])
    })

    it('mode=cause で open-search イベントに mode が含まれる', async () => {
      const wrapper = mountSubsystemCell({ mode: 'cause', editable: true })
      await wrapper.find('[data-testid="subsystem-display"]').trigger('click')
      expect(wrapper.emitted('open-search')![0]).toEqual([{ recordId: 1, mode: 'cause' }])
    })
  })
})
