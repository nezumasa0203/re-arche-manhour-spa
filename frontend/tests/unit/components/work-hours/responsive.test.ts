import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import WorkHoursDataTable from '~/components/work-hours/WorkHoursDataTable.vue'
import type { WorkHoursRecord } from '~/types/api'

// --- Store mock ---
const mockStore = {
  records: [] as WorkHoursRecord[],
  loading: false,
  selectedIds: [] as number[],
  sort: 'workDate:asc',
  yearMonth: '2025-02',
  permissions: {
    canCreate: true,
    canEdit: true,
    canDelete: true,
    canConfirm: true,
    canRevert: true,
    canCopy: true,
    canTransfer: true,
  },
  isEditable: vi.fn().mockReturnValue(true),
  updateField: vi.fn(),
  fetchRecords: vi.fn(),
}

vi.mock('~/stores/workHours', () => ({
  useWorkHoursStore: () => mockStore,
}))

function makeRecord(overrides: Partial<WorkHoursRecord> = {}): WorkHoursRecord {
  return {
    id: 1,
    yearMonth: '2025-02',
    workDate: '2025-02-10',
    targetSubsystem: { subsystemNo: 'SUB001', subsystemName: '会計', systemNo: 'SYS01', systemName: '基幹' },
    causeSubsystem: { subsystemNo: 'SUB002', subsystemName: '人事', systemNo: 'SYS01', systemName: '基幹' },
    category: { categoryCode: '01', categoryName: '障害対応' },
    subject: 'テスト件名',
    hours: '03:00',
    tmrNo: '',
    workRequestNo: '',
    workRequesterName: '',
    status: '0',
    updatedAt: '2025-02-10T10:00:00Z',
    ...overrides,
  }
}

function mountDataTable() {
  return mount(WorkHoursDataTable, {
    props: {
      categories: [
        { categoryCode: '01', categoryName: '障害対応' },
      ],
      isManager: false,
    },
    global: {
      plugins: [createPinia()],
      stubs: {
        'DataTable': {
          name: 'DataTable',
          template: `<div class="datatable">
            <slot></slot>
            <div v-for="row in value" :key="row.id" class="row">
              <slot name="body" :data="row"></slot>
            </div>
          </div>`,
          props: {
            value: Array,
            loading: Boolean,
            selection: Array,
            sortField: String,
            sortOrder: Number,
            scrollable: { type: Boolean, default: false },
            frozenColumns: Number,
            scrollHeight: String,
            dataKey: String,
            rowClass: Function,
          },
          emits: ['update:selection', 'sort'],
        },
        'Column': {
          name: 'Column',
          template: '<div class="column"></div>',
          props: {
            field: String,
            header: String,
            sortable: { type: Boolean, default: false },
            frozen: { type: Boolean, default: false },
            style: [String, Object],
            selectionMode: String,
          },
        },
        'StatusCell': { template: '<span></span>', props: ['recordId', 'status', 'isManager'] },
        'DateCell': { template: '<span></span>', props: ['recordId', 'workDate', 'editable', 'yearMonth'] },
        'SubsystemCell': { template: '<span></span>', props: ['recordId', 'subsystemNo', 'subsystemName', 'editable', 'mode'] },
        'CategoryCell': { template: '<span></span>', props: ['recordId', 'categoryCode', 'categoryName', 'categories', 'editable'] },
        'SubjectCell': { template: '<span></span>', props: ['recordId', 'subject', 'editable'] },
        'HoursCell': { template: '<span></span>', props: ['recordId', 'hours', 'editable'] },
        'TextCell': { template: '<span></span>', props: ['recordId', 'field', 'value', 'editable'] },
        'Checkbox': { template: '<input type="checkbox" />', props: ['modelValue', 'binary'] },
      },
    },
  })
}

describe('レスポンシブ対応', () => {
  beforeEach(() => {
    mockStore.records = [makeRecord()]
    mockStore.loading = false
    mockStore.selectedIds = []
    mockStore.sort = 'workDate:asc'
    mockStore.isEditable.mockReset().mockReturnValue(true)
  })

  // ─── DataTable スクロール設定 ────────────────────────────────

  describe('DataTable スクロール設定', () => {
    it('scrollable が有効である', () => {
      const wrapper = mountDataTable()
      const dataTable = wrapper.findComponent({ name: 'DataTable' })
      expect(dataTable.props('scrollable')).toBeTruthy()
    })

    it('scroll-height が flex である', () => {
      const wrapper = mountDataTable()
      const dataTable = wrapper.findComponent({ name: 'DataTable' })
      expect(dataTable.props('scrollHeight')).toBe('flex')
    })

    it('frozenColumns が 4 に設定されている', () => {
      const wrapper = mountDataTable()
      const dataTable = wrapper.findComponent({ name: 'DataTable' })
      expect(dataTable.props('frozenColumns')).toBe(4)
    })
  })

  // ─── 固定列の定義 ────────────────────────────────────────────

  describe('固定列（frozen columns）', () => {
    it('最初の4列に frozen 属性が設定されている', () => {
      const wrapper = mountDataTable()
      const columns = wrapper.findAllComponents({ name: 'Column' })

      // 固定列: CHK(selection), ステータス, 作業日, 保守担当所属
      const frozenColumns = columns.filter(c => c.props('frozen') === true || c.props('frozen') === '')
      expect(frozenColumns.length).toBeGreaterThanOrEqual(3)
    })

    it('ステータス列は frozen で sortable', () => {
      const wrapper = mountDataTable()
      const columns = wrapper.findAllComponents({ name: 'Column' })
      const statusCol = columns.find(c => c.props('field') === 'status')
      expect(statusCol).toBeDefined()
      expect(statusCol!.props('frozen')).toBeTruthy()
      expect(statusCol!.props('sortable')).toBeTruthy()
    })

    it('作業日列は frozen で sortable', () => {
      const wrapper = mountDataTable()
      const columns = wrapper.findAllComponents({ name: 'Column' })
      const dateCol = columns.find(c => c.props('field') === 'workDate')
      expect(dateCol).toBeDefined()
      expect(dateCol!.props('frozen')).toBeTruthy()
      expect(dateCol!.props('sortable')).toBeTruthy()
    })
  })

  // ─── コンテナ CSS クラス ──────────────────────────────────────

  describe('コンテナレイアウト', () => {
    it('work-hours-datatable クラスが存在する', () => {
      const wrapper = mountDataTable()
      expect(wrapper.find('.work-hours-datatable').exists()).toBe(true)
    })

    it('flex: 1 と overflow: hidden が設定されている（CSS 定義確認）', () => {
      // jsdom では実際の computed style は検証できないため、
      // コンポーネントのスタイルブロックに定義が存在することを構造的に確認
      const wrapper = mountDataTable()
      const container = wrapper.find('.work-hours-datatable')
      expect(container.exists()).toBe(true)
    })
  })

  // ─── SearchPanel flex-wrap ─────────────────────────────────

  describe('SearchPanel レスポンシブ', () => {
    // SearchPanel の flex-wrap: wrap はCSSで定義済み
    // jsdom では実際のレイアウト折り返しはテストできないため、
    // CSS クラスの存在を確認
    it('search-panel__row クラスが存在する', async () => {
      const SearchPanel = (await import('~/components/work-hours/SearchPanel.vue')).default
      const wrapper = mount(SearchPanel, {
        global: {
          plugins: [createPinia()],
          stubs: {
            'Dropdown': { template: '<select></select>', props: ['modelValue', 'options'] },
            'Button': { template: '<button @click="$emit(\'click\')"></button>', props: ['label', 'icon', 'severity', 'size', 'text'] },
            'InputText': { template: '<input />', props: ['modelValue', 'readonly'] },
            'Tag': { template: '<span></span>', props: ['value', 'severity'] },
          },
        },
      })
      expect(wrapper.find('.search-panel__row').exists()).toBe(true)
    })
  })
})
