import { describe, it, expect } from 'vitest'
import {
  validateRequired,
  validateHoursFormat,
  validate15MinUnit,
  validateByteLength,
  validateDateInMonth,
  validateFixedLength,
  validateCharType,
  validateWorkHoursRecord,
  parseHoursInput,
} from '~/composables/useValidation'

// ── T-007: 必須・形式チェック ──

describe('validateRequired', () => {
  it('空文字列はエラー', () => {
    const r = validateRequired('', '作業日')
    expect(r.valid).toBe(false)
    expect(r.code).toBe('CZ-126')
  })

  it('null はエラー', () => {
    expect(validateRequired(null, '作業日').valid).toBe(false)
  })

  it('undefined はエラー', () => {
    expect(validateRequired(undefined, '作業日').valid).toBe(false)
  })

  it('空白のみはエラー', () => {
    expect(validateRequired('   ', '作業日').valid).toBe(false)
  })

  it('値ありは OK', () => {
    expect(validateRequired('2025-04-01', '作業日').valid).toBe(true)
  })
})

describe('validateHoursFormat', () => {
  it('空文字列はエラー (必須)', () => {
    const r = validateHoursFormat('')
    expect(r.valid).toBe(false)
  })

  it('正規 HH:MM 形式は OK', () => {
    expect(validateHoursFormat('03:30').valid).toBe(true)
  })

  it('不正な文字列はエラー', () => {
    const r = validateHoursFormat('abc')
    expect(r.valid).toBe(false)
    expect(r.code).toBe('CZ-125')
  })

  it('0:00 はエラー (0より大きい必須)', () => {
    const r = validateHoursFormat('0:00')
    expect(r.valid).toBe(false)
    expect(r.code).toBe('CZ-129')
  })

  it('0:15 は OK (最小工数)', () => {
    expect(validateHoursFormat('0:15').valid).toBe(true)
  })
})

describe('parseHoursInput', () => {
  it.each([
    ['3', '03:00'],
    ['12', '12:00'],
    ['330', '03:30'],
    ['0330', '03:30'],
    ['3:30', '03:30'],
    ['03:30', '03:30'],
  ])('"%s" → "%s"', (input, expected) => {
    expect(parseHoursInput(input)).toBe(expected)
  })

  it('不正な入力は null を返す', () => {
    expect(parseHoursInput('abc')).toBeNull()
  })
})

describe('validate15MinUnit', () => {
  it.each(['00', '15', '30', '45'])('分=%s は OK', (min) => {
    expect(validate15MinUnit(`01:${min}`).valid).toBe(true)
  })

  it.each(['01', '10', '14', '20', '31', '44', '59'])('分=%s はエラー', (min) => {
    const r = validate15MinUnit(`01:${min}`)
    expect(r.valid).toBe(false)
    expect(r.code).toBe('CZ-147')
  })
})

describe('validateByteLength', () => {
  it('128バイト以内は OK', () => {
    expect(validateByteLength('a'.repeat(128), 128, '件名').valid).toBe(true)
  })

  it('128バイト超過はエラー', () => {
    const r = validateByteLength('a'.repeat(129), 128, '件名')
    expect(r.valid).toBe(false)
    expect(r.code).toBe('CZ-128')
  })

  it('全角64文字 = 128バイトは OK', () => {
    expect(validateByteLength('あ'.repeat(64), 128, '件名').valid).toBe(true)
  })

  it('全角65文字 = 130バイトはエラー', () => {
    expect(validateByteLength('あ'.repeat(65), 128, '件名').valid).toBe(false)
  })
})

// ── T-008: 日付・文字種・固定長チェック ──

describe('validateDateInMonth', () => {
  it('月初日は OK', () => {
    expect(validateDateInMonth('2025-02-01', '2025-02').valid).toBe(true)
  })

  it('月末日は OK', () => {
    expect(validateDateInMonth('2025-02-28', '2025-02').valid).toBe(true)
  })

  it('月外日付はエラー', () => {
    const r = validateDateInMonth('2025-03-01', '2025-02')
    expect(r.valid).toBe(false)
    expect(r.code).toBe('CZ-144')
  })

  it('前月日付はエラー', () => {
    const r = validateDateInMonth('2025-01-31', '2025-02')
    expect(r.valid).toBe(false)
    expect(r.code).toBe('CZ-144')
  })
})

describe('validateFixedLength', () => {
  it('空文字列は OK (任意入力)', () => {
    expect(validateFixedLength('', 7, '作業依頼書No').valid).toBe(true)
  })

  it('7文字ちょうどは OK', () => {
    expect(validateFixedLength('1234567', 7, '作業依頼書No').valid).toBe(true)
  })

  it.each([1, 2, 3, 4, 5, 6])('%d文字はエラー', (len) => {
    const r = validateFixedLength('1'.repeat(len), 7, '作業依頼書No')
    expect(r.valid).toBe(false)
    expect(r.code).toBe('CZ-137')
  })

  it('8文字はエラー', () => {
    const r = validateFixedLength('12345678', 7, '作業依頼書No')
    expect(r.valid).toBe(false)
    expect(r.code).toBe('CZ-137')
  })
})

describe('validateCharType', () => {
  it('半角数字のみ (mode=10111) で数字は OK', () => {
    expect(validateCharType('12345', '10111', 'TMR番号').valid).toBe(true)
  })

  it('半角数字のみ (mode=10111) で英字はエラー', () => {
    const r = validateCharType('123ab', '10111', 'TMR番号')
    expect(r.valid).toBe(false)
    expect(r.code).toBe('CZ-138')
  })

  it('制限なし (mode=00000) で日本語は OK', () => {
    expect(validateCharType('テスト件名', '00000', '件名').valid).toBe(true)
  })

  it('空文字列は OK', () => {
    expect(validateCharType('', '10111', 'TMR番号').valid).toBe(true)
  })
})

// ── T-009: 一括バリデーション ──

describe('validateWorkHoursRecord', () => {
  const validRecord = {
    workDate: '2025-04-01',
    targetSubsystemNo: 'SS001',
    causeSubsystemNo: 'SS002',
    categoryCode: 'CAT01',
    subject: 'テスト件名',
    hours: '03:00',
    tmrNo: '',
    workRequestNo: '',
    workRequesterName: '',
  }

  it('全フィールド正常値は OK', () => {
    const errors = validateWorkHoursRecord(validRecord, '2025-04')
    expect(errors).toHaveLength(0)
  })

  it('作業日が空でエラー (VR-001)', () => {
    const errors = validateWorkHoursRecord({ ...validRecord, workDate: '' }, '2025-04')
    expect(errors.some((e) => e.code === 'CZ-126' && e.field === 'workDate')).toBe(true)
  })

  it('対象サブシステムが空でエラー (VR-003)', () => {
    const errors = validateWorkHoursRecord({ ...validRecord, targetSubsystemNo: '' }, '2025-04')
    expect(errors.some((e) => e.code === 'CZ-126' && e.field === 'targetSubsystemNo')).toBe(true)
  })

  it('件名が空でエラー (VR-006)', () => {
    const errors = validateWorkHoursRecord({ ...validRecord, subject: '' }, '2025-04')
    expect(errors.some((e) => e.code === 'CZ-126' && e.field === 'subject')).toBe(true)
  })

  it('工数が不正形式でエラー (VR-009)', () => {
    const errors = validateWorkHoursRecord({ ...validRecord, hours: 'abc' }, '2025-04')
    expect(errors.some((e) => e.code === 'CZ-125')).toBe(true)
  })

  it('作業依頼書No が1〜6文字でエラー (VR-012)', () => {
    const errors = validateWorkHoursRecord({ ...validRecord, workRequestNo: '123' }, '2025-04')
    expect(errors.some((e) => e.code === 'CZ-137')).toBe(true)
  })

  it('複数エラーが同時検出される', () => {
    const errors = validateWorkHoursRecord(
      { ...validRecord, workDate: '', subject: '', hours: '' },
      '2025-04',
    )
    expect(errors.length).toBeGreaterThanOrEqual(3)
  })

  it('TMR番号が5文字以内の数字は OK (VR-011)', () => {
    const errors = validateWorkHoursRecord({ ...validRecord, tmrNo: '12345' }, '2025-04')
    expect(errors.some((e) => e.field === 'tmrNo')).toBe(false)
  })

  it('TMR番号が6文字以上はエラー (VR-011)', () => {
    const errors = validateWorkHoursRecord({ ...validRecord, tmrNo: '123456' }, '2025-04')
    expect(errors.some((e) => e.code === 'CZ-138')).toBe(true)
  })

  it('依頼者名が40文字以内は OK (VR-013)', () => {
    const errors = validateWorkHoursRecord({ ...validRecord, workRequesterName: 'a'.repeat(40) }, '2025-04')
    expect(errors.some((e) => e.field === 'workRequesterName')).toBe(false)
  })

  it('依頼者名が41文字以上はエラー (VR-013)', () => {
    const errors = validateWorkHoursRecord({ ...validRecord, workRequesterName: 'a'.repeat(41) }, '2025-04')
    expect(errors.some((e) => e.code === 'CZ-139')).toBe(true)
  })
})
