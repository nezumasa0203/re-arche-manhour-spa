/**
 * AC-WH-13: 状態表示パターン
 * Given: 画面遷移直後
 * When: API データ取得中
 * Then: loading 状態が true → データ描画後 false。データ0件 → 空メッセージ表示
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import WorkHoursDataTable from '~/components/work-hours/WorkHoursDataTable.vue'
import type { WorkHoursRecord } from '~/types/api'

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

function mountDataTable() {
  return mount(WorkHoursDataTable, {
    props: {
      categories: [],
      isManager: false,
    },
    global: {
      plugins: [createPinia()],
      stubs: {
        'DataTable': {
          name: 'DataTable',
          template: `<div class="datatable" :class="{ loading: loading }">
            <slot></slot>
            <div v-if="!value || value.length === 0"><slot name="empty"></slot></div>
            <div v-for="row in value" :key="row.id" class="row">
              <slot name="body" :data="row"></slot>
            </div>
          </div>`,
          props: {
            value: Array,
            loading: { type: Boolean, default: false },
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
          template: '<div></div>',
          props: ['field', 'header', 'sortable', 'frozen', 'style', 'selectionMode'],
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

describe('AC-WH-13: 状態表示パターン', () => {
  beforeEach(() => {
    mockStore.records = []
    mockStore.loading = false
  })

  it('Given: loading=true → Then: loading クラスが付与される', () => {
    mockStore.loading = true
    const wrapper = mountDataTable()
    expect(wrapper.find('.datatable').classes()).toContain('loading')
  })

  it('Given: loading=false → Then: loading クラスなし', () => {
    mockStore.loading = false
    const wrapper = mountDataTable()
    expect(wrapper.find('.datatable').classes()).not.toContain('loading')
  })

  it('Given: データ0件 → Then: 空メッセージが表示される', () => {
    mockStore.records = []
    const wrapper = mountDataTable()
    expect(wrapper.text()).toContain('データがありません')
  })

  it('Given: データあり → Then: 行が表示される', () => {
    mockStore.records = [{
      id: 1,
      yearMonth: '2025-02',
      workDate: '2025-02-10',
      targetSubsystem: { subsystemNo: 'SUB001', subsystemName: '会計', systemNo: 'SYS01', systemName: '基幹' },
      causeSubsystem: { subsystemNo: 'SUB002', subsystemName: '人事', systemNo: 'SYS01', systemName: '基幹' },
      category: { categoryCode: '01', categoryName: '障害対応' },
      subject: 'テスト',
      hours: '03:00',
      tmrNo: '',
      workRequestNo: '',
      workRequesterName: '',
      status: '0',
      updatedAt: '2025-02-10T10:00:00Z',
    }]
    const wrapper = mountDataTable()
    expect(wrapper.findAll('.row').length).toBe(1)
  })
})
