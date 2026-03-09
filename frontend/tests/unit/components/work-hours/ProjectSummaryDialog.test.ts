import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import ProjectSummaryDialog from '~/components/work-hours/ProjectSummaryDialog.vue'

const mockGet = vi.fn()

vi.mock('~/composables/useApi', () => ({
  useApi: () => ({
    get: mockGet,
  }),
}))

vi.mock('~/stores/workHours', () => ({
  useWorkHoursStore: () => ({
    yearMonth: '2025-02',
  }),
}))

function mountDialog(props: Record<string, unknown> = {}) {
  return mount(ProjectSummaryDialog, {
    props: {
      visible: true,
      ...props,
    },
    global: {
      plugins: [createPinia()],
      stubs: {
        'Dialog': {
          template: '<div v-if="visible" class="dialog"><slot></slot><slot name="footer"></slot></div>',
          props: ['visible', 'header', 'modal', 'style'],
        },
        'DataTable': {
          template: '<table><tr v-for="row in value" :key="row.key"><td>{{ row.systemName }}</td><td>{{ row.subsystemName }}</td><td>{{ row.hours }}</td></tr></table>',
          props: ['value'],
        },
        'Column': { template: '<col />', props: ['field', 'header'] },
        'Button': {
          template: '<button @click="$emit(\'click\')">{{ label }}</button>',
          props: ['label'],
        },
      },
    },
  })
}

describe('ProjectSummaryDialog', () => {
  beforeEach(() => {
    mockGet.mockReset()
  })

  describe('表示', () => {
    it('visible=true のときダイアログが表示される', () => {
      mockGet.mockResolvedValueOnce({ rows: [] })
      const wrapper = mountDialog({ visible: true })
      expect(wrapper.find('.dialog').exists()).toBe(true)
    })

    it('visible=false のときダイアログが非表示', () => {
      const wrapper = mountDialog({ visible: false })
      expect(wrapper.find('.dialog').exists()).toBe(false)
    })
  })

  describe('データ取得', () => {
    it('表示時に API からプロジェクト別工数を取得する', async () => {
      mockGet.mockResolvedValueOnce({
        rows: [
          { key: '1', systemName: '基幹', subsystemName: '会計', hours: '40:00' },
          { key: '2', systemName: '基幹', subsystemName: '人事', hours: '32:00' },
        ],
      })
      const wrapper = mountDialog()
      await flushPromises()

      expect(mockGet).toHaveBeenCalledWith(
        '/work-hours/project-summary',
        expect.objectContaining({ yearMonth: '2025-02' })
      )
    })

    it('取得データがテーブルに表示される', async () => {
      mockGet.mockResolvedValueOnce({
        rows: [
          { key: '1', systemName: '基幹', subsystemName: '会計', hours: '40:00' },
        ],
      })
      const wrapper = mountDialog()
      await flushPromises()

      expect(wrapper.text()).toContain('基幹')
      expect(wrapper.text()).toContain('会計')
      expect(wrapper.text()).toContain('40:00')
    })
  })

  describe('閉じるボタン', () => {
    it('閉じるボタンで close イベントが発火', async () => {
      mockGet.mockResolvedValueOnce({ rows: [] })
      const wrapper = mountDialog()
      await flushPromises()

      const closeBtn = wrapper.find('button')
      await closeBtn.trigger('click')
      expect(wrapper.emitted('update:visible')).toBeTruthy()
    })
  })
})
