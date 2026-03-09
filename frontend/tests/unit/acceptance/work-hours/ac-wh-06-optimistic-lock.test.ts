/**
 * AC-WH-06: 楽観的ロック競合（US-01B）
 * Given: 同一レコード（version=1）を表示している
 * When: PATCH API が 409 Conflict を返す
 * Then: 元の値が保持されないが、サーバーの最新値で上書きされる
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useWorkHoursStore } from '~/stores/workHours'

const mockApi = {
  get: vi.fn(),
  post: vi.fn(),
  patch: vi.fn(),
  del: vi.fn(),
  getBlob: vi.fn(),
}

vi.mock('~/composables/useApi', () => ({
  useApi: () => mockApi,
}))

describe('AC-WH-06: 楽観的ロック競合', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    mockApi.get.mockReset()
    mockApi.post.mockReset()
    mockApi.patch.mockReset()
  })

  it('Given: レコードを表示 → When: 409 Conflict → Then: サーバーの最新値で上書き', async () => {
    const store = useWorkHoursStore()

    // Given: レコードがある
    store.records = [{
      id: 1,
      yearMonth: '2025-02',
      workDate: '2025-02-10',
      targetSubsystem: { subsystemNo: 'SUB001', subsystemName: '会計', systemNo: 'SYS01', systemName: '基幹' },
      causeSubsystem: { subsystemNo: 'SUB002', subsystemName: '人事', systemNo: 'SYS01', systemName: '基幹' },
      category: { categoryCode: '01', categoryName: '障害対応' },
      subject: '元の件名',
      hours: '03:00',
      tmrNo: '',
      workRequestNo: '',
      workRequesterName: '',
      status: '0',
      updatedAt: '2025-02-10T10:00:00Z',
    }]

    // When: PATCH が 409 Conflict を返す
    const conflictError = {
      statusCode: 409,
      data: {
        currentValue: 'サーバーの最新値',
        updatedAt: '2025-02-10T11:00:00Z',
      },
    }
    mockApi.patch.mockRejectedValue(conflictError)

    await store.updateField(1, 'subject', '新しい件名')

    // Then: サーバーの最新値で上書きされ、updatedAt も更新される
    expect(store.records[0].subject).toBe('サーバーの最新値')
    expect(store.records[0].updatedAt).toBe('2025-02-10T11:00:00Z')
  })

  it('Given: 正常な PATCH → Then: 新しい値が反映される', async () => {
    const store = useWorkHoursStore()

    store.records = [{
      id: 1,
      yearMonth: '2025-02',
      workDate: '2025-02-10',
      targetSubsystem: { subsystemNo: 'SUB001', subsystemName: '会計', systemNo: 'SYS01', systemName: '基幹' },
      causeSubsystem: { subsystemNo: 'SUB002', subsystemName: '人事', systemNo: 'SYS01', systemName: '基幹' },
      category: { categoryCode: '01', categoryName: '障害対応' },
      subject: '元の件名',
      hours: '03:00',
      tmrNo: '',
      workRequestNo: '',
      workRequesterName: '',
      status: '0',
      updatedAt: '2025-02-10T10:00:00Z',
    }]

    mockApi.patch.mockResolvedValue({
      newValue: '更新後の件名',
      summary: { monthlyTotal: 3, dailyTotal: 3 },
    })

    await store.updateField(1, 'subject', '更新後の件名')

    expect(store.records[0].subject).toBe('更新後の件名')
  })
})
