import type { ValidationResult, WorkHoursRecord } from '~/types/validation'
import { resolveMessage } from '~/constants/messages'
import { calculateByteLength } from '~/utils/byteLength'

/**
 * 必須チェック
 */
export function validateRequired(
  value: string | null | undefined,
  fieldName: string,
): ValidationResult {
  if (value == null || value.trim() === '') {
    return { valid: false, code: 'CZ-126', message: resolveMessage('CZ-126', [fieldName]), field: fieldName }
  }
  return { valid: true }
}

/**
 * HH:MM 入力値を正規化する。
 * 不正な入力は null を返す。
 */
export function parseHoursInput(input: string): string | null {
  const trimmed = input.trim()
  if (trimmed === '') return null

  // コロン付き
  if (trimmed.includes(':')) {
    const [h, m] = trimmed.split(':')
    const hh = parseInt(h, 10)
    const mm = parseInt(m, 10)
    if (isNaN(hh) || isNaN(mm)) return null
    return `${String(hh).padStart(2, '0')}:${String(mm).padStart(2, '0')}`
  }

  // 数字のみ
  if (!/^\d+$/.test(trimmed)) return null

  if (trimmed.length <= 2) {
    // 1-2桁: HH → HH:00
    return `${trimmed.padStart(2, '0')}:00`
  }
  if (trimmed.length === 3) {
    // 3桁: H:MM
    const h = trimmed.slice(0, 1)
    const m = trimmed.slice(1)
    return `${h.padStart(2, '0')}:${m}`
  }
  if (trimmed.length === 4) {
    // 4桁: HH:MM
    const h = trimmed.slice(0, 2)
    const m = trimmed.slice(2)
    return `${h}:${m}`
  }

  return null
}

/**
 * HH:MM 形式 + 必須 + 最小工数チェック
 */
export function validateHoursFormat(value: string): ValidationResult {
  if (value == null || value.trim() === '') {
    return { valid: false, code: 'CZ-126', message: resolveMessage('CZ-126', ['工数']), field: 'hours' }
  }

  const parsed = parseHoursInput(value)
  if (!parsed) {
    return { valid: false, code: 'CZ-125', message: resolveMessage('CZ-125', ['工数']), field: 'hours' }
  }

  const [h, m] = parsed.split(':').map(Number)
  const totalMinutes = h * 60 + m

  if (totalMinutes === 0) {
    return { valid: false, code: 'CZ-129', message: resolveMessage('CZ-129'), field: 'hours' }
  }

  if (m % 15 !== 0) {
    return { valid: false, code: 'CZ-147', message: resolveMessage('CZ-147'), field: 'hours' }
  }

  return { valid: true }
}

/**
 * 15分単位チェック
 */
export function validate15MinUnit(value: string): ValidationResult {
  if (!value.includes(':')) return { valid: true }
  const m = parseInt(value.split(':')[1], 10)
  if (m % 15 !== 0) {
    return { valid: false, code: 'CZ-147', message: resolveMessage('CZ-147'), field: 'hours' }
  }
  return { valid: true }
}

/**
 * バイト長上限チェック
 */
export function validateByteLength(
  value: string,
  maxBytes: number,
  fieldName: string,
): ValidationResult {
  if (calculateByteLength(value) > maxBytes) {
    return { valid: false, code: 'CZ-128', message: resolveMessage('CZ-128', [fieldName]), field: fieldName }
  }
  return { valid: true }
}

/**
 * 月内日付チェック (VR-002)
 */
export function validateDateInMonth(date: string, yearMonth: string): ValidationResult {
  if (!date || !yearMonth) return { valid: true }
  const dateYm = date.substring(0, 7) // "YYYY-MM"
  if (dateYm !== yearMonth) {
    return { valid: false, code: 'CZ-144', message: resolveMessage('CZ-144'), field: 'workDate' }
  }
  return { valid: true }
}

/**
 * 固定長チェック (VR-012: 空 or 7文字固定)
 */
export function validateFixedLength(
  value: string,
  length: number,
  fieldName: string,
): ValidationResult {
  if (value === '') return { valid: true }
  if (value.length !== length) {
    return { valid: false, code: 'CZ-137', message: resolveMessage('CZ-137'), field: fieldName }
  }
  return { valid: true }
}

/**
 * 文字種チェック (VR-011)
 * mode: 5桁制御文字列
 *   mode[0]: 半角英字 (0=許可, 1=禁止)
 *   mode[1]: 半角数字 (0=許可, 1=禁止)
 *   mode[2]: 半角カタカナ (0=許可, 1=禁止)
 *   mode[3]: 特殊文字 (0=制限, 1=全許可, 2=禁止)
 *   mode[4]: 全角文字 (0=許可, 1=禁止)
 */
export function validateCharType(
  value: string,
  mode: string,
  fieldName: string,
): ValidationResult {
  if (!value) return { valid: true }

  for (const char of value) {
    const code = char.codePointAt(0)!

    // 半角英字
    if ((code >= 0x41 && code <= 0x5a) || (code >= 0x61 && code <= 0x7a)) {
      if (mode[0] === '1') {
        return { valid: false, code: 'CZ-138', message: resolveMessage('CZ-138'), field: fieldName }
      }
      continue
    }

    // 半角数字
    if (code >= 0x30 && code <= 0x39) {
      if (mode[1] === '1') {
        return { valid: false, code: 'CZ-138', message: resolveMessage('CZ-138'), field: fieldName }
      }
      continue
    }

    // 半角カタカナ
    if (code >= 0xff61 && code <= 0xff9f) {
      if (mode[2] === '1') {
        return { valid: false, code: 'CZ-138', message: resolveMessage('CZ-138'), field: fieldName }
      }
      continue
    }

    // 全角文字 (U+0080以上、半角カタカナ除外)
    if (code >= 0x80) {
      if (mode[4] === '1') {
        return { valid: false, code: 'CZ-138', message: resolveMessage('CZ-138'), field: fieldName }
      }
      continue
    }

    // 半角記号 (上記以外の ASCII)
    if (mode[3] === '2') {
      return { valid: false, code: 'CZ-138', message: resolveMessage('CZ-138'), field: fieldName }
    }
  }

  return { valid: true }
}

/**
 * 工数レコード一括バリデーション (VR-001〜VR-013, Layer 1)
 */
export function validateWorkHoursRecord(
  record: WorkHoursRecord,
  yearMonth: string,
): ValidationResult[] {
  const errors: ValidationResult[] = []

  // VR-001: 作業日 必須
  const workDateReq = validateRequired(record.workDate, '作業日')
  if (!workDateReq.valid) {
    errors.push({ ...workDateReq, field: 'workDate' })
  } else if (record.workDate) {
    // VR-002: 月内日付
    const dateInMonth = validateDateInMonth(record.workDate, yearMonth)
    if (!dateInMonth.valid) errors.push(dateInMonth)
  }

  // VR-003: 対象サブシステム 必須
  const targetReq = validateRequired(record.targetSubsystemNo, '対象サブシステム')
  if (!targetReq.valid) errors.push({ ...targetReq, field: 'targetSubsystemNo' })

  // VR-004: 原因サブシステム 必須
  const causeReq = validateRequired(record.causeSubsystemNo, '原因サブシステム')
  if (!causeReq.valid) errors.push({ ...causeReq, field: 'causeSubsystemNo' })

  // VR-005: 保守カテゴリ 必須
  const catReq = validateRequired(record.categoryCode, '保守カテゴリ')
  if (!catReq.valid) errors.push({ ...catReq, field: 'categoryCode' })

  // VR-006: 件名 必須 + 128バイト
  const subjectReq = validateRequired(record.subject, '件名')
  if (!subjectReq.valid) {
    errors.push({ ...subjectReq, field: 'subject' })
  } else if (record.subject) {
    const subjectLen = validateByteLength(record.subject, 128, '件名')
    if (!subjectLen.valid) errors.push({ ...subjectLen, field: 'subject' })
  }

  // VR-008/VR-009: 工数 必須 + 形式 + 15分単位
  if (record.hours != null) {
    const hoursResult = validateHoursFormat(record.hours)
    if (!hoursResult.valid) errors.push(hoursResult)
  } else {
    errors.push({ valid: false, code: 'CZ-126', message: resolveMessage('CZ-126', ['工数']), field: 'hours' })
  }

  // VR-011: TMR番号 (任意、5文字以内、半角数字)
  if (record.tmrNo) {
    if (record.tmrNo.length > 5) {
      errors.push({ valid: false, code: 'CZ-138', message: resolveMessage('CZ-138'), field: 'tmrNo' })
    } else {
      const charResult = validateCharType(record.tmrNo, '10111', 'TMR番号')
      if (!charResult.valid) errors.push({ ...charResult, field: 'tmrNo' })
    }
  }

  // VR-012: 作業依頼書No (空 or 7文字固定)
  if (record.workRequestNo) {
    const fixedResult = validateFixedLength(record.workRequestNo, 7, '作業依頼書No')
    if (!fixedResult.valid) errors.push({ ...fixedResult, field: 'workRequestNo' })
  }

  // VR-013: 作業依頼者名 (40文字以内)
  if (record.workRequesterName && record.workRequesterName.length > 40) {
    errors.push({ valid: false, code: 'CZ-139', message: resolveMessage('CZ-139'), field: 'workRequesterName' })
  }

  return errors
}
