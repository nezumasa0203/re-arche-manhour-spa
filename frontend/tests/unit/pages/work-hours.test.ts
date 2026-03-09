import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import WorkHoursPage from '~/pages/work-hours.vue'

const mockStore = {
  records: [],
  loading: false,
  summary: { monthlyTotal: 0, dailyTotal: 0 },
  yearMonth: '2025-02',
  selectedIds: [],
  permissions: {
    canCreate: true,
    canEdit: true,
    canDelete: true,
    canConfirm: true,
    canRevert: true,
    canCopy: true,
    canTransfer: true,
  },
  monthControl: { yearMonth: '2025-02', status: 'OPEN', isLocked: false },
  canAdd: true,
  canCopy: false,
  canDelete: false,
  canBatchConfirm: false,
  canBatchRevert: false,
  statusCounts: { '0': 0, '1': 0, '2': 0 },
  message: null,
  sort: 'workDate:asc',
  staffId: null,
  staffName: null,
  isDaiko: false,
  editingCell: null,
  isEditable: vi.fn().mockReturnValue(true),
  fetchRecords: vi.fn().mockResolvedValue(undefined),
  createRecord: vi.fn(),
  updateField: vi.fn(),
  deleteRecords: vi.fn(),
  copyRecords: vi.fn(),
  changeMonth: vi.fn(),
  switchDaiko: vi.fn(),
  batchConfirm: vi.fn(),
  batchRevert: vi.fn(),
}

vi.mock('~/stores/workHours', () => ({
  useWorkHoursStore: () => mockStore,
}))

vi.mock('~/composables/useConfirmAction', () => ({
  useConfirmAction: () => ({
    confirm: vi.fn().mockResolvedValue(true),
    confirmByCode: vi.fn().mockResolvedValue(true),
  }),
}))

vi.mock('~/composables/useApi', () => ({
  useApi: () => ({
    get: vi.fn().mockResolvedValue({ rows: [] }),
    post: vi.fn(),
    patch: vi.fn(),
    del: vi.fn(),
    getBlob: vi.fn(),
  }),
}))

vi.mock('~/composables/useAuth', () => ({
  useAuth: () => ({
    user: { value: { userId: 'ACT-01', userName: '報告担当者' } },
    isAuthenticated: { value: true },
    canManage: { value: false },
    canFullAccess: { value: false },
  }),
}))

function mountPage() {
  return mount(WorkHoursPage, {
    global: {
      plugins: [createPinia()],
      stubs: {
        WorkHoursSearchPanel: { template: '<div data-testid="search-panel"></div>' },
        WorkHoursToolbar: { template: '<div data-testid="toolbar"></div>' },
        WorkHoursDataTable: { template: '<div data-testid="datatable"></div>' },
        WorkHoursStatusFooter: { template: '<div data-testid="status-footer"></div>' },
        WorkHoursTransferDialog: { template: '<div data-testid="transfer-dialog"></div>' },
        WorkHoursProjectSummaryDialog: { template: '<div data-testid="project-dialog"></div>' },
      },
    },
  })
}

describe('WorkHoursPage', () => {
  beforeEach(() => {
    mockStore.loading = false
    mockStore.records = []
    mockStore.fetchRecords.mockReset().mockResolvedValue(undefined)
  })

  // ─── 初期化 ──────────────────────────────────────────────────

  describe('初期化', () => {
    it('onMounted で fetchRecords が呼ばれる', async () => {
      mountPage()
      await flushPromises()
      expect(mockStore.fetchRecords).toHaveBeenCalled()
    })
  })

  // ─── コンポーネント統合 ──────────────────────────────────────

  describe('コンポーネント統合', () => {
    it('SearchPanel が表示される', () => {
      const wrapper = mountPage()
      expect(wrapper.find('[data-testid="search-panel"]').exists()).toBe(true)
    })

    it('Toolbar が表示される', () => {
      const wrapper = mountPage()
      expect(wrapper.find('[data-testid="toolbar"]').exists()).toBe(true)
    })

    it('DataTable が表示される', () => {
      const wrapper = mountPage()
      expect(wrapper.find('[data-testid="datatable"]').exists()).toBe(true)
    })

    it('StatusFooter が表示される', () => {
      const wrapper = mountPage()
      expect(wrapper.find('[data-testid="status-footer"]').exists()).toBe(true)
    })
  })
})
