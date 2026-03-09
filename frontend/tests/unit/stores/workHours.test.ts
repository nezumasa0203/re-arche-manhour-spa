import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

// --- Mocks ---

const mockGet = vi.fn()
const mockPost = vi.fn()
const mockPatch = vi.fn()
const mockDel = vi.fn()
const mockGetBlob = vi.fn()

vi.mock('~/composables/useApi', () => ({
  useApi: () => ({
    get: mockGet,
    post: mockPost,
    patch: mockPatch,
    del: mockDel,
    getBlob: mockGetBlob,
  }),
}))

// Auto-imports stub
vi.stubGlobal('ref', (val: unknown) => {
  const r = { value: val }
  return r
})
vi.stubGlobal('computed', (fn: () => unknown) => {
  return { get value() { return fn() } }
})

vi.mock('#app', () => ({
  useRuntimeConfig: () => ({ public: { apiBase: 'http://localhost:8080' } }),
}))

import { useWorkHoursStore } from '~/stores/workHours'
import type {
  WorkHoursRecord,
  WorkHoursSummary,
  WorkHoursPermissions,
  MonthControl,
  WorkHoursListResponse,
} from '~/types/api'

// --- Test Helpers ---

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

function makePermissions(overrides: Partial<WorkHoursPermissions> = {}): WorkHoursPermissions {
  return {
    canCreate: true,
    canEdit: true,
    canDelete: true,
    canConfirm: true,
    canRevert: true,
    canCopy: true,
    canTransfer: true,
    ...overrides,
  }
}

function makeMonthControl(overrides: Partial<MonthControl> = {}): MonthControl {
  return {
    yearMonth: '2025-02',
    status: 'OPEN',
    isLocked: false,
    ...overrides,
  }
}

function makeSummary(overrides: Partial<WorkHoursSummary> = {}): WorkHoursSummary {
  return {
    monthlyTotal: 120,
    dailyTotal: 8,
    ...overrides,
  }
}

function makeListResponse(overrides: Partial<WorkHoursListResponse> = {}): WorkHoursListResponse {
  return {
    records: [makeRecord()],
    summary: makeSummary(),
    permissions: makePermissions(),
    monthControl: makeMonthControl(),
    ...overrides,
  }
}

// =============================================================================
// T-001: State / Getters Tests
// =============================================================================

describe('workHours Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    mockGet.mockReset()
    mockPost.mockReset()
    mockPatch.mockReset()
    mockDel.mockReset()
    mockGetBlob.mockReset()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  // ─── T-001: State 初期値 ──────────────────────────────────────

  describe('initial state', () => {
    it('has empty records array', () => {
      const store = useWorkHoursStore()
      expect(store.records).toEqual([])
    })

    it('has zero summary', () => {
      const store = useWorkHoursStore()
      expect(store.summary).toEqual({ monthlyTotal: 0, dailyTotal: 0 })
    })

    it('has current yearMonth as default', () => {
      const store = useWorkHoursStore()
      const now = new Date()
      const expected = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
      expect(store.yearMonth).toBe(expected)
    })

    it('has null staffId', () => {
      const store = useWorkHoursStore()
      expect(store.staffId).toBeNull()
    })

    it('has isDaiko false', () => {
      const store = useWorkHoursStore()
      expect(store.isDaiko).toBe(false)
    })

    it('has default sort', () => {
      const store = useWorkHoursStore()
      expect(store.sort).toBe('workDate:asc')
    })

    it('has empty selectedIds', () => {
      const store = useWorkHoursStore()
      expect(store.selectedIds).toEqual([])
    })

    it('has null editingCell', () => {
      const store = useWorkHoursStore()
      expect(store.editingCell).toBeNull()
    })

    it('has loading false', () => {
      const store = useWorkHoursStore()
      expect(store.loading).toBe(false)
    })

    it('has null message', () => {
      const store = useWorkHoursStore()
      expect(store.message).toBeNull()
    })

    it('has default monthControl', () => {
      const store = useWorkHoursStore()
      expect(store.monthControl).toEqual({ yearMonth: '', status: '', isLocked: false })
    })

    it('has default permissions', () => {
      const store = useWorkHoursStore()
      expect(store.permissions.canCreate).toBe(false)
    })
  })

  // ─── T-001: Getters ──────────────────────────────────────────

  describe('canAdd getter', () => {
    it('returns true when permissions.canCreate is true and not locked', () => {
      const store = useWorkHoursStore()
      store.permissions = makePermissions({ canCreate: true })
      store.monthControl = makeMonthControl({ isLocked: false })
      expect(store.canAdd).toBe(true)
    })

    it('returns false when permissions.canCreate is false', () => {
      const store = useWorkHoursStore()
      store.permissions = makePermissions({ canCreate: false })
      expect(store.canAdd).toBe(false)
    })

    it('returns false when month is locked', () => {
      const store = useWorkHoursStore()
      store.permissions = makePermissions({ canCreate: true })
      store.monthControl = makeMonthControl({ isLocked: true })
      expect(store.canAdd).toBe(false)
    })
  })

  describe('canCopy getter', () => {
    it('returns true when permissions.canCopy and selectedIds not empty', () => {
      const store = useWorkHoursStore()
      store.permissions = makePermissions({ canCopy: true })
      store.selectedIds = [1, 2]
      expect(store.canCopy).toBe(true)
    })

    it('returns false when selectedIds is empty', () => {
      const store = useWorkHoursStore()
      store.permissions = makePermissions({ canCopy: true })
      store.selectedIds = []
      expect(store.canCopy).toBe(false)
    })

    it('returns false when permissions.canCopy is false', () => {
      const store = useWorkHoursStore()
      store.permissions = makePermissions({ canCopy: false })
      store.selectedIds = [1]
      expect(store.canCopy).toBe(false)
    })
  })

  describe('canDelete getter', () => {
    it('returns true when permissions.canDelete and selectedIds not empty', () => {
      const store = useWorkHoursStore()
      store.permissions = makePermissions({ canDelete: true })
      store.selectedIds = [1]
      expect(store.canDelete).toBe(true)
    })

    it('returns false when selectedIds is empty', () => {
      const store = useWorkHoursStore()
      store.permissions = makePermissions({ canDelete: true })
      store.selectedIds = []
      expect(store.canDelete).toBe(false)
    })
  })

  describe('canBatchConfirm getter', () => {
    it('returns true when canConfirm and STATUS_0 records exist', () => {
      const store = useWorkHoursStore()
      store.permissions = makePermissions({ canConfirm: true })
      store.records = [makeRecord({ status: '0' })]
      expect(store.canBatchConfirm).toBe(true)
    })

    it('returns false when no STATUS_0 records', () => {
      const store = useWorkHoursStore()
      store.permissions = makePermissions({ canConfirm: true })
      store.records = [makeRecord({ status: '1' })]
      expect(store.canBatchConfirm).toBe(false)
    })

    it('returns false when canConfirm is false', () => {
      const store = useWorkHoursStore()
      store.permissions = makePermissions({ canConfirm: false })
      store.records = [makeRecord({ status: '0' })]
      expect(store.canBatchConfirm).toBe(false)
    })
  })

  describe('canBatchRevert getter', () => {
    it('returns true when canRevert and STATUS_1 records exist', () => {
      const store = useWorkHoursStore()
      store.permissions = makePermissions({ canRevert: true })
      store.records = [makeRecord({ status: '1' })]
      expect(store.canBatchRevert).toBe(true)
    })

    it('returns false when no STATUS_1 records', () => {
      const store = useWorkHoursStore()
      store.permissions = makePermissions({ canRevert: true })
      store.records = [makeRecord({ status: '0' })]
      expect(store.canBatchRevert).toBe(false)
    })
  })

  describe('isEditable', () => {
    it('returns true for STATUS_0 records', () => {
      const store = useWorkHoursStore()
      store.permissions = makePermissions({ canEdit: true })
      const record = makeRecord({ status: '0' })
      expect(store.isEditable(record)).toBe(true)
    })

    it('returns false for STATUS_1 records when canEdit is false', () => {
      const store = useWorkHoursStore()
      store.permissions = makePermissions({ canEdit: false })
      const record = makeRecord({ status: '1' })
      expect(store.isEditable(record)).toBe(false)
    })

    it('returns true for STATUS_1 records when canEdit is true (manager)', () => {
      const store = useWorkHoursStore()
      store.permissions = makePermissions({ canEdit: true })
      const record = makeRecord({ status: '1' })
      expect(store.isEditable(record)).toBe(true)
    })

    it('returns false for STATUS_2 records', () => {
      const store = useWorkHoursStore()
      store.permissions = makePermissions({ canEdit: false })
      const record = makeRecord({ status: '2' })
      expect(store.isEditable(record)).toBe(false)
    })

    it('returns false for STATUS_9 records', () => {
      const store = useWorkHoursStore()
      store.permissions = makePermissions({ canEdit: true })
      const record = makeRecord({ status: '9' })
      expect(store.isEditable(record)).toBe(false)
    })
  })

  describe('statusCounts getter', () => {
    it('counts records by status', () => {
      const store = useWorkHoursStore()
      store.records = [
        makeRecord({ id: 1, status: '0' }),
        makeRecord({ id: 2, status: '0' }),
        makeRecord({ id: 3, status: '1' }),
        makeRecord({ id: 4, status: '2' }),
        makeRecord({ id: 5, status: '2' }),
        makeRecord({ id: 6, status: '2' }),
      ]
      expect(store.statusCounts).toEqual({ '0': 2, '1': 1, '2': 3 })
    })

    it('returns zero counts for empty records', () => {
      const store = useWorkHoursStore()
      expect(store.statusCounts).toEqual({ '0': 0, '1': 0, '2': 0 })
    })
  })

  // ─── T-002: fetchRecords Action ─────────────────────────────

  describe('fetchRecords', () => {
    it('fetches records and updates state', async () => {
      const store = useWorkHoursStore()
      const response = makeListResponse()
      mockGet.mockResolvedValueOnce(response)

      await store.fetchRecords()

      expect(mockGet).toHaveBeenCalledWith('/work-hours', {
        yearMonth: store.yearMonth,
        staffId: null,
        sort: 'workDate:asc',
      })
      expect(store.records).toEqual(response.records)
      expect(store.summary).toEqual(response.summary)
      expect(store.monthControl).toEqual(response.monthControl)
      expect(store.permissions).toEqual(response.permissions)
    })

    it('sets loading true during fetch, false after', async () => {
      const store = useWorkHoursStore()
      mockGet.mockImplementation(() => {
        expect(store.loading).toBe(true)
        return Promise.resolve(makeListResponse())
      })

      await store.fetchRecords()
      expect(store.loading).toBe(false)
    })

    it('sets loading false on error and shows error message', async () => {
      const store = useWorkHoursStore()
      mockGet.mockRejectedValueOnce(new Error('network error'))

      await store.fetchRecords()
      expect(store.loading).toBe(false)
      expect(store.records).toEqual([])
      expect(store.message).toEqual({ type: 'error', code: '', text: 'データの取得に失敗しました' })
    })

    it('handles empty records response', async () => {
      const store = useWorkHoursStore()
      mockGet.mockResolvedValueOnce(makeListResponse({ records: [] }))

      await store.fetchRecords()
      expect(store.records).toEqual([])
    })

    it('clears selectedIds on fetch', async () => {
      const store = useWorkHoursStore()
      store.selectedIds = [1, 2, 3]
      mockGet.mockResolvedValueOnce(makeListResponse())

      await store.fetchRecords()
      expect(store.selectedIds).toEqual([])
    })

    it('includes staffId when in daiko mode', async () => {
      const store = useWorkHoursStore()
      store.staffId = 'STAFF001'
      store.isDaiko = true
      mockGet.mockResolvedValueOnce(makeListResponse())

      await store.fetchRecords()
      expect(mockGet).toHaveBeenCalledWith('/work-hours', expect.objectContaining({
        staffId: 'STAFF001',
      }))
    })
  })

  // ─── T-003: createRecord Action ─────────────────────────────

  describe('createRecord', () => {
    it('creates a new record via POST and prepends to records', async () => {
      const store = useWorkHoursStore()
      store.records = [makeRecord({ id: 10 })]
      const newRecord = makeRecord({ id: 99, status: '0', subject: '' })
      mockPost.mockResolvedValueOnce(newRecord)

      await store.createRecord()

      expect(mockPost).toHaveBeenCalledWith('/work-hours', {
        yearMonth: store.yearMonth,
      })
      expect(store.records[0].id).toBe(99)
      expect(store.records).toHaveLength(2)
    })

    it('sets loading during creation', async () => {
      const store = useWorkHoursStore()
      mockPost.mockImplementation(() => {
        expect(store.loading).toBe(true)
        return Promise.resolve(makeRecord({ id: 50 }))
      })

      await store.createRecord()
      expect(store.loading).toBe(false)
    })
  })

  // ─── T-004: updateField Action ──────────────────────────────

  describe('updateField', () => {
    it('updates a field via PATCH and updates record in state', async () => {
      const store = useWorkHoursStore()
      const record = makeRecord({ id: 1, subject: '旧件名' })
      store.records = [record]

      mockPatch.mockResolvedValueOnce({
        id: 1,
        field: 'subject',
        oldValue: '旧件名',
        newValue: '新件名',
        summary: makeSummary({ monthlyTotal: 130 }),
      })

      await store.updateField(1, 'subject', '新件名')

      expect(mockPatch).toHaveBeenCalledWith('/work-hours/1', {
        field: 'subject',
        value: '新件名',
        updatedAt: record.updatedAt,
      })
      expect(store.records[0].subject).toBe('新件名')
      expect(store.summary.monthlyTotal).toBe(130)
    })

    it('restores original value on 409 Conflict', async () => {
      const store = useWorkHoursStore()
      store.records = [makeRecord({ id: 1, hours: '03:00', updatedAt: '2025-02-10T10:00:00Z' })]

      const conflictError = {
        statusCode: 409,
        data: {
          code: 'CZ-101',
          message: '別ユーザーにより更新されました',
          currentValue: '05:00',
          updatedAt: '2025-02-10T12:00:00Z',
        },
      }
      mockPatch.mockRejectedValueOnce(conflictError)

      await store.updateField(1, 'hours', '04:00')

      // Should update to server's current value
      expect(store.records[0].hours).toBe('05:00')
      expect(store.records[0].updatedAt).toBe('2025-02-10T12:00:00Z')
    })

    it('restores original value on 400 validation error', async () => {
      const store = useWorkHoursStore()
      store.records = [makeRecord({ id: 1, hours: '03:00' })]

      const validationError = {
        statusCode: 400,
        data: { code: 'CZ-125', message: '工数はHH:MM形式で入力してください', field: 'hours' },
      }
      mockPatch.mockRejectedValueOnce(validationError)

      await store.updateField(1, 'hours', 'invalid')

      // Should keep original value
      expect(store.records[0].hours).toBe('03:00')
    })

    it('updates summary from response', async () => {
      const store = useWorkHoursStore()
      store.records = [makeRecord({ id: 1 })]
      store.summary = makeSummary({ monthlyTotal: 100 })

      mockPatch.mockResolvedValueOnce({
        id: 1,
        field: 'hours',
        oldValue: '03:00',
        newValue: '05:00',
        summary: makeSummary({ monthlyTotal: 102 }),
      })

      await store.updateField(1, 'hours', '05:00')
      expect(store.summary.monthlyTotal).toBe(102)
    })
  })

  // ─── T-005: Batch Operations ────────────────────────────────

  describe('deleteRecords', () => {
    it('deletes selected records via DELETE and removes from state', async () => {
      const store = useWorkHoursStore()
      store.records = [
        makeRecord({ id: 1 }),
        makeRecord({ id: 2 }),
        makeRecord({ id: 3 }),
      ]
      mockDel.mockResolvedValueOnce({ deletedIds: [1, 3], summary: makeSummary({ monthlyTotal: 50 }) })

      await store.deleteRecords([1, 3])

      expect(mockDel).toHaveBeenCalledWith('/work-hours', { ids: [1, 3] })
      expect(store.records).toHaveLength(1)
      expect(store.records[0].id).toBe(2)
      expect(store.summary.monthlyTotal).toBe(50)
    })

    it('clears selectedIds after delete', async () => {
      const store = useWorkHoursStore()
      store.records = [makeRecord({ id: 1 })]
      store.selectedIds = [1]
      mockDel.mockResolvedValueOnce({ deletedIds: [1], summary: makeSummary() })

      await store.deleteRecords([1])
      expect(store.selectedIds).toEqual([])
    })
  })

  describe('copyRecords', () => {
    it('copies selected records via POST and prepends new records', async () => {
      const store = useWorkHoursStore()
      const original = makeRecord({ id: 1 })
      store.records = [original]
      const copied = makeRecord({ id: 100, status: '0' })
      mockPost.mockResolvedValueOnce({ records: [copied] })

      await store.copyRecords([1])

      expect(mockPost).toHaveBeenCalledWith('/work-hours/copy', { ids: [1] })
      expect(store.records).toHaveLength(2)
      expect(store.records[0].id).toBe(100)
    })
  })

  describe('transferNextMonth', () => {
    it('transfers records to next months', async () => {
      const store = useWorkHoursStore()
      store.records = [makeRecord({ id: 1 })]
      mockPost.mockResolvedValueOnce({ transferredCount: 2 })

      await store.transferNextMonth([1], ['2025-03', '2025-04'])

      expect(mockPost).toHaveBeenCalledWith('/work-hours/transfer-next-month', {
        ids: [1],
        months: ['2025-03', '2025-04'],
      })
    })
  })

  describe('batchConfirm', () => {
    it('confirms all STATUS_0 records and refreshes', async () => {
      const store = useWorkHoursStore()
      store.records = [makeRecord({ id: 1, status: '0' })]
      mockPost.mockResolvedValueOnce({ confirmedCount: 1 })
      mockGet.mockResolvedValueOnce(makeListResponse({
        records: [makeRecord({ id: 1, status: '1' })],
      }))

      await store.batchConfirm()

      expect(mockPost).toHaveBeenCalledWith('/work-hours/batch-confirm', {
        yearMonth: store.yearMonth,
      })
      // Should trigger fetchRecords after confirm
      expect(mockGet).toHaveBeenCalled()
    })

    it('handles validation errors with recordIds', async () => {
      const store = useWorkHoursStore()
      store.records = [makeRecord({ id: 1, status: '0' })]

      const error = {
        statusCode: 400,
        data: {
          errors: [
            { code: 'CZ-126', message: '件名は必須入力です', field: 'subject', recordId: 1 },
          ],
        },
      }
      mockPost.mockRejectedValueOnce(error)

      await expect(store.batchConfirm()).rejects.toEqual(error)
    })
  })

  describe('batchRevert', () => {
    it('reverts all STATUS_1 records and refreshes', async () => {
      const store = useWorkHoursStore()
      store.records = [makeRecord({ id: 1, status: '1' })]
      mockPost.mockResolvedValueOnce({ revertedCount: 1 })
      mockGet.mockResolvedValueOnce(makeListResponse({
        records: [makeRecord({ id: 1, status: '0' })],
      }))

      await store.batchRevert()

      expect(mockPost).toHaveBeenCalledWith('/work-hours/batch-revert', {
        yearMonth: store.yearMonth,
      })
      expect(mockGet).toHaveBeenCalled()
    })
  })

  // ─── T-006: changeMonth / switchDaiko ───────────────────────

  describe('changeMonth', () => {
    it('updates yearMonth and fetches records', async () => {
      const store = useWorkHoursStore()
      mockGet.mockResolvedValueOnce(makeListResponse())

      await store.changeMonth('2025-03')

      expect(store.yearMonth).toBe('2025-03')
      expect(mockGet).toHaveBeenCalledWith('/work-hours', expect.objectContaining({
        yearMonth: '2025-03',
      }))
    })

    it('clears selectedIds on month change', async () => {
      const store = useWorkHoursStore()
      store.selectedIds = [1, 2]
      mockGet.mockResolvedValueOnce(makeListResponse())

      await store.changeMonth('2025-03')
      expect(store.selectedIds).toEqual([])
    })

    it('clears editingCell on month change', async () => {
      const store = useWorkHoursStore()
      store.editingCell = { recordId: 1, field: 'hours' }
      mockGet.mockResolvedValueOnce(makeListResponse())

      await store.changeMonth('2025-03')
      expect(store.editingCell).toBeNull()
    })
  })

  describe('switchDaiko', () => {
    it('switches to daiko mode and fetches records', async () => {
      const store = useWorkHoursStore()
      mockPost.mockResolvedValueOnce({
        delegationStaffId: 'STAFF001',
        delegationStaffName: '山田太郎',
        isDaiko: true,
      })
      mockGet.mockResolvedValueOnce(makeListResponse())

      await store.switchDaiko('STAFF001')

      expect(mockPost).toHaveBeenCalledWith('/delegation/switch', {
        targetStaffId: 'STAFF001',
      })
      expect(store.staffId).toBe('STAFF001')
      expect(store.staffName).toBe('山田太郎')
      expect(store.isDaiko).toBe(true)
      expect(mockGet).toHaveBeenCalled()
    })

    it('cancels daiko mode with null staffId', async () => {
      const store = useWorkHoursStore()
      store.staffId = 'STAFF001'
      store.staffName = '山田太郎'
      store.isDaiko = true

      mockPost.mockResolvedValueOnce({
        delegationStaffId: null,
        delegationStaffName: null,
        isDaiko: false,
      })
      mockGet.mockResolvedValueOnce(makeListResponse())

      await store.switchDaiko(null)

      expect(mockPost).toHaveBeenCalledWith('/delegation/switch', {
        targetStaffId: null,
      })
      expect(store.staffId).toBeNull()
      expect(store.staffName).toBeNull()
      expect(store.isDaiko).toBe(false)
    })
  })
})
