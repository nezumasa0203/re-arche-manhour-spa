/**
 * API レスポンス型定義
 *
 * Backend Java Record DTO に1:1対応する TypeScript interface。
 * BigDecimal → number, LocalDateTime → string (ISO-8601)。
 */

// ---------------------------------------------------------------------------
// 共通
// ---------------------------------------------------------------------------

export interface ApiError {
  code: string
  message: string
  field?: string | null
  params?: string[] | null
  recordId?: number | null
}

export interface MonthValue {
  yearMonth: string
  value: number
}

// ---------------------------------------------------------------------------
// 工数入力（WorkHours）
// ---------------------------------------------------------------------------

export interface SubsystemInfo {
  subsystemNo: string
  subsystemName: string
  systemNo: string
  systemName: string
}

export interface CategoryInfo {
  categoryCode: string
  categoryName: string
}

export interface WorkHoursSummary {
  monthlyTotal: number
  dailyTotal: number
}

export interface WorkHoursPermissions {
  canCreate: boolean
  canEdit: boolean
  canDelete: boolean
  canConfirm: boolean
  canRevert: boolean
  canCopy: boolean
  canTransfer: boolean
}

export interface MonthControl {
  yearMonth: string
  status: string
  isLocked: boolean
}

export interface WorkHoursRecord {
  id: number
  yearMonth: string
  workDate: string
  targetSubsystem: SubsystemInfo
  causeSubsystem: SubsystemInfo
  category: CategoryInfo
  subject: string
  hours: string
  tmrNo: string
  workRequestNo: string
  workRequesterName: string
  status: string
  updatedAt: string
}

export interface WorkHoursListResponse {
  records: WorkHoursRecord[]
  summary: WorkHoursSummary
  permissions: WorkHoursPermissions
  monthControl: MonthControl
}

export interface WorkHoursUpdateResponse {
  id: number
  field: string
  oldValue: string
  newValue: string
  summary: WorkHoursSummary
}

// ---------------------------------------------------------------------------
// 作業状況（WorkStatus）
// ---------------------------------------------------------------------------

export interface WorkStatusRecord {
  staffId: string
  staffName: string
  organizationCode: string
  organizationName: string
  yearMonth: string
  status: string
  totalHours: number
  recordCount: number
}

export interface MonthlyControlInfo {
  yearMonth: string
  organizationCode: string
  status: string
  isConfirmed: boolean
  isAggregated: boolean
}

export interface WorkStatusPermissions {
  canApprove: boolean
  canRevert: boolean
  canMonthlyConfirm: boolean
  canMonthlyAggregate: boolean
  canMonthlyUnconfirm: boolean
}

export interface WorkStatusListResponse {
  records: WorkStatusRecord[]
  monthlyControl: MonthlyControlInfo
  permissions: WorkStatusPermissions
}

// ---------------------------------------------------------------------------
// 半期推移（HalfTrends）
// ---------------------------------------------------------------------------

export interface HalfTrendsRow {
  key: string
  label: string
  months: MonthValue[]
  total: number
}

export interface DrilldownContext {
  yearHalf: string
  systemNo: string
  subsystemNo: string
  categoryCode: string
}

export interface HalfTrendsResponse {
  rows: HalfTrendsRow[]
  drilldown: DrilldownContext
}

// ---------------------------------------------------------------------------
// 月別内訳（MonthlyBreakdown）
// ---------------------------------------------------------------------------

export interface BreakdownRow {
  key: string
  label: string
  months: MonthValue[]
  total: number
}

export interface BreakdownContext {
  yearMonth: string
  systemNo: string
  subsystemNo: string
  categoryCode: string
}

export interface MonthlyBreakdownResponse {
  rows: BreakdownRow[]
  context: BreakdownContext
}

// ---------------------------------------------------------------------------
// マスタ（Master）
// ---------------------------------------------------------------------------

export interface MasterListResponse<T = unknown> {
  items: T[]
  totalCount: number
  page: number
  pageSize: number
}

// ---------------------------------------------------------------------------
// マイシステム（MySystem）
// ---------------------------------------------------------------------------

export interface MySystemItem {
  systemNo: string
  systemName: string
  subsystemCount: number
}

export interface MySystemListResponse {
  systems: MySystemItem[]
}

// ---------------------------------------------------------------------------
// 代行（Delegation）
// ---------------------------------------------------------------------------

export interface DelegationResponse {
  delegationStaffId: string | null
  delegationStaffName: string | null
  isDaiko: boolean
}
