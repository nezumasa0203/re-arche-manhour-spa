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

function mountDataTable(props: Record<string, unknown> = {}) {
  return mount(WorkHoursDataTable, {
    props: {
      categories: [
        { categoryCode: '01', categoryName: '障害対応' },
        { categoryCode: '02', categoryName: '保守' },
      ],
      isManager: false,
      ...props,
    },
    global: {
      plugins: [createPinia()],
      stubs: {
        'DataTable': {
          name: 'DataTable',
          template: `<div class="datatable" :class="{ loading: loading }">
            <slot></slot>
            <div v-if="!value || value.length === 0"><slot name="empty"></slot></div>
            <div v-for="row in value" :key="row.id" class="row" :data-record-id="row.id">
              <slot name="body" :data="row"></slot>
            </div>
          </div>`,
          props: ['value', 'loading', 'selection', 'sortField', 'sortOrder', 'scrollable', 'frozenColumns', 'dataKey', 'rowClass'],
          emits: ['update:selection', 'sort'],
        },
        'Column': {
          template: '<div class="column"><slot name="body" :data="{}"></slot></div>',
          props: ['field', 'header', 'sortable', 'frozen', 'style'],
        },
        'Checkbox': {
          template: '<input type="checkbox" data-testid="row-checkbox" />',
          props: ['modelValue', 'binary'],
        },
        'StatusCell': { template: '<span class="status-cell-stub"></span>', props: ['recordId', 'status', 'isManager'] },
        'DateCell': { template: '<span class="date-cell-stub"></span>', props: ['recordId', 'workDate', 'editable', 'yearMonth'] },
        'SubsystemCell': { template: '<span class="subsystem-cell-stub"></span>', props: ['recordId', 'subsystemNo', 'subsystemName', 'editable', 'mode'] },
        'CategoryCell': { template: '<span class="category-cell-stub"></span>', props: ['recordId', 'categoryCode', 'categoryName', 'categories', 'editable'] },
        'SubjectCell': { template: '<span class="subject-cell-stub"></span>', props: ['recordId', 'subject', 'editable'] },
        'HoursCell': { template: '<span class="hours-cell-stub"></span>', props: ['recordId', 'hours', 'editable'] },
        'TextCell': { template: '<span class="text-cell-stub"></span>', props: ['recordId', 'field', 'value', 'editable'] },
      },
    },
  })
}

describe('WorkHoursDataTable', () => {
  beforeEach(() => {
    mockStore.records = [
      makeRecord({ id: 1, status: '0' }),
      makeRecord({ id: 2, status: '1' }),
      makeRecord({ id: 3, status: '2' }),
    ]
    mockStore.loading = false
    mockStore.selectedIds = []
    mockStore.sort = 'workDate:asc'
    mockStore.isEditable.mockReset().mockReturnValue(true)
    mockStore.updateField.mockReset()
    mockStore.fetchRecords.mockReset()
  })

  // ─── レンダリング ────────────────────────────────────────────

  describe('レンダリング', () => {
    it('DataTable が表示される', () => {
      const wrapper = mountDataTable()
      expect(wrapper.find('.datatable').exists()).toBe(true)
    })

    it('DataTable コンポーネントが存在する', () => {
      const wrapper = mountDataTable()
      const dataTable = wrapper.findComponent({ name: 'DataTable' })
      expect(dataTable.exists()).toBe(true)
    })

    it('レコードが表示される', () => {
      const wrapper = mountDataTable()
      const rows = wrapper.findAll('.row')
      expect(rows.length).toBe(3)
    })
  })

  // ─── ローディング ────────────────────────────────────────────

  describe('ローディング', () => {
    it('loading=true のとき loading クラスが付与される', () => {
      mockStore.loading = true
      const wrapper = mountDataTable()
      expect(wrapper.find('.datatable').classes()).toContain('loading')
    })

    it('loading=false のとき loading クラスなし', () => {
      mockStore.loading = false
      const wrapper = mountDataTable()
      expect(wrapper.find('.datatable').classes()).not.toContain('loading')
    })
  })

  // ─── 空データ ─────────────────────────────────────────────

  describe('空データ', () => {
    it('レコード0件のとき空メッセージが表示される', () => {
      mockStore.records = []
      const wrapper = mountDataTable()
      expect(wrapper.text()).toContain('データがありません')
    })
  })

  // ─── ソート ──────────────────────────────────────────────────

  describe('ソート', () => {
    it('デフォルトのソートが workDate:asc', () => {
      mockStore.sort = 'workDate:asc'
      const wrapper = mountDataTable()
      const dataTable = wrapper.findComponent({ name: 'DataTable' })
      expect(dataTable.props('sortField')).toBe('workDate')
      expect(dataTable.props('sortOrder')).toBe(1)
    })

    it('desc ソートの場合 sortOrder が -1', () => {
      mockStore.sort = 'hours:desc'
      const wrapper = mountDataTable()
      const dataTable = wrapper.findComponent({ name: 'DataTable' })
      expect(dataTable.props('sortField')).toBe('hours')
      expect(dataTable.props('sortOrder')).toBe(-1)
    })
  })

  // ─── 行スタイル ──────────────────────────────────────────────

  describe('行スタイル', () => {
    it('STATUS_0 行は通常スタイル', () => {
      const wrapper = mountDataTable()
      const rows = wrapper.findAll('.row')
      // 最初の行は STATUS_0
      expect(rows[0].classes()).not.toContain('cell-readonly')
    })
  })

  // ─── セルコンポーネント統合 ──────────────────────────────────

  describe('セルコンポーネント', () => {
    it('StatusCell スタブが存在する', () => {
      const wrapper = mountDataTable()
      expect(wrapper.findAll('.status-cell-stub').length).toBeGreaterThan(0)
    })

    it('DateCell スタブが存在する', () => {
      const wrapper = mountDataTable()
      expect(wrapper.findAll('.date-cell-stub').length).toBeGreaterThan(0)
    })

    it('HoursCell スタブが存在する', () => {
      const wrapper = mountDataTable()
      expect(wrapper.findAll('.hours-cell-stub').length).toBeGreaterThan(0)
    })
  })
})
