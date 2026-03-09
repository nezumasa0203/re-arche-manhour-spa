import { defineStore } from 'pinia'
import { useApi } from '~/composables/useApi'
import type {
  WorkHoursRecord,
  WorkHoursSummary,
  WorkHoursPermissions,
  MonthControl,
  WorkHoursListResponse,
  DelegationResponse,
} from '~/types/api'

export interface EditingCell {
  recordId: number
  field: string
}

export interface StatusMessage {
  type: 'success' | 'error' | 'info'
  code: string
  text: string
}

function currentYearMonth(): string {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
}

const defaultPermissions: WorkHoursPermissions = {
  canCreate: false,
  canEdit: false,
  canDelete: false,
  canConfirm: false,
  canRevert: false,
  canCopy: false,
  canTransfer: false,
}

export const useWorkHoursStore = defineStore('workHours', {
  state: () => ({
    records: [] as WorkHoursRecord[],
    summary: { monthlyTotal: 0, dailyTotal: 0 } as WorkHoursSummary,
    yearMonth: currentYearMonth(),
    staffId: null as string | null,
    staffName: null as string | null,
    isDaiko: false,
    sort: 'workDate:asc',
    monthControl: { yearMonth: '', status: '', isLocked: false } as MonthControl,
    permissions: { ...defaultPermissions } as WorkHoursPermissions,
    statusMatrix: {} as Record<string, Record<string, number>>,
    selectedIds: [] as number[],
    editingCell: null as EditingCell | null,
    loading: false,
    message: null as StatusMessage | null,
  }),

  getters: {
    canAdd(state): boolean {
      return state.permissions.canCreate && !state.monthControl.isLocked
    },

    canCopy(state): boolean {
      return state.permissions.canCopy && state.selectedIds.length > 0
    },

    canDelete(state): boolean {
      return state.permissions.canDelete && state.selectedIds.length > 0
    },

    canBatchConfirm(state): boolean {
      return state.permissions.canConfirm && state.records.some(r => r.status === '0')
    },

    canBatchRevert(state): boolean {
      return state.permissions.canRevert && state.records.some(r => r.status === '1')
    },

    statusCounts(state): Record<string, number> {
      const counts: Record<string, number> = { '0': 0, '1': 0, '2': 0 }
      for (const r of state.records) {
        if (r.status in counts) {
          counts[r.status]++
        }
      }
      return counts
    },
  },

  actions: {
    isEditable(record: WorkHoursRecord): boolean {
      if (record.status === '9') return false
      if (record.status === '0') return true
      return this.permissions.canEdit
    },

    async fetchRecords(): Promise<void> {
      this.loading = true
      try {
        const api = useApi()
        const response = await api.get<WorkHoursListResponse>('/work-hours', {
          yearMonth: this.yearMonth,
          staffId: this.staffId,
          sort: this.sort,
        })
        this.records = response.records
        this.summary = response.summary
        this.monthControl = response.monthControl
        this.permissions = response.permissions
        this.selectedIds = []
      } catch {
        // API エラー時はメッセージを表示（records はリセット）
        this.records = []
        this.message = { type: 'error', code: '', text: 'データの取得に失敗しました' }
      } finally {
        this.loading = false
      }
    },

    async createRecord(): Promise<void> {
      this.loading = true
      try {
        const api = useApi()
        const newRecord = await api.post<WorkHoursRecord>('/work-hours', {
          yearMonth: this.yearMonth,
        })
        this.records.unshift(newRecord)
      } finally {
        this.loading = false
      }
    },

    async updateField(id: number, field: string, value: string): Promise<void> {
      const record = this.records.find(r => r.id === id)
      if (!record) return

      const originalValue = (record as Record<string, unknown>)[field]
      const api = useApi()

      try {
        const response = await api.patch<WorkHoursRecord>(`/work-hours/${id}`, {
          field,
          value,
          updatedAt: record.updatedAt,
        })
        // Update record with server response
        const idx = this.records.findIndex(r => r.id === id)
        if (idx !== -1) {
          this.records[idx] = response
        }
      } catch (error: unknown) {
        const err = error as { statusCode?: number; data?: Record<string, unknown> }
        if (err.statusCode === 409 && err.data) {
          // Conflict: update to server's current value
          ;(record as Record<string, unknown>)[field] = err.data.currentValue
          if (err.data.updatedAt) {
            record.updatedAt = err.data.updatedAt as string
          }
        } else if (err.statusCode === 400) {
          // Validation error: restore original value
          ;(record as Record<string, unknown>)[field] = originalValue
        } else {
          ;(record as Record<string, unknown>)[field] = originalValue
          throw error
        }
      }
    },

    async deleteRecords(ids: number[]): Promise<void> {
      const api = useApi()
      const response = await api.del<{ deletedIds: number[]; summary: WorkHoursSummary }>('/work-hours', { ids })
      this.records = this.records.filter(r => !response.deletedIds.includes(r.id))
      this.summary = response.summary
      this.selectedIds = []
    },

    async copyRecords(ids: number[]): Promise<void> {
      const api = useApi()
      const response = await api.post<{ records: WorkHoursRecord[] }>('/work-hours/copy', { ids })
      this.records.unshift(...response.records)
    },

    async transferNextMonth(ids: number[], months: string[]): Promise<void> {
      const api = useApi()
      await api.post('/work-hours/transfer-next-month', { ids, months })
    },

    async batchConfirm(): Promise<void> {
      const api = useApi()
      await api.post('/work-hours/batch-confirm', {
        yearMonth: this.yearMonth,
      })
      await this.fetchRecords()
    },

    async batchRevert(): Promise<void> {
      const api = useApi()
      await api.post('/work-hours/batch-revert', {
        yearMonth: this.yearMonth,
      })
      await this.fetchRecords()
    },

    async changeMonth(yearMonth: string): Promise<void> {
      this.yearMonth = yearMonth
      this.selectedIds = []
      this.editingCell = null
      await this.fetchRecords()
    },

    async switchDaiko(targetStaffId: string | null): Promise<void> {
      const api = useApi()
      const response = await api.post<DelegationResponse>('/delegation/switch', {
        targetStaffId,
      })
      this.staffId = response.delegationStaffId
      this.staffName = response.delegationStaffName ?? null
      this.isDaiko = response.isDaiko
      await this.fetchRecords()
    },
  },
})
