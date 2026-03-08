/**
 * バリデーション結果
 */
export interface ValidationResult {
  valid: boolean
  code?: string
  message?: string
  field?: string
}

/**
 * 工数レコード（バリデーション対象）
 */
export interface WorkHoursRecord {
  workDate?: string | null
  targetSubsystemNo?: string | null
  causeSubsystemNo?: string | null
  categoryCode?: string | null
  subject?: string | null
  hours?: string | null
  tmrNo?: string | null
  workRequestNo?: string | null
  workRequesterName?: string | null
}
