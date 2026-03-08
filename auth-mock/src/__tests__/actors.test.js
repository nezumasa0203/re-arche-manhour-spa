import { describe, it, expect } from 'vitest'
import { ACTORS, parseBits } from '../index.js'

// ============================================
// アクター定義テスト
// ============================================
describe('ACTORS definition', () => {
  // ------------------------------------------
  // 15 アクター存在確認
  // ------------------------------------------
  it('should have exactly 15 actors', () => {
    expect(ACTORS).toHaveLength(15)
  })

  it('should contain all actor IDs from ACT-01 through ACT-15', () => {
    const expectedIds = Array.from({ length: 15 }, (_, i) => {
      const num = String(i + 1).padStart(2, '0')
      return `ACT-${num}`
    })
    const actualIds = ACTORS.map((a) => a.id).sort()
    expect(actualIds).toEqual(expectedIds.sort())
  })

  // ------------------------------------------
  // 必須フィールド確認
  // ------------------------------------------
  const REQUIRED_FIELDS = [
    'id', 'name', 'jinjiMode', 'tab010', 'tab011', 'tab012',
    'dataAuthority', 'employmentType', 'orgCode', 'orgName',
    'staffRole', 'canDelegate',
  ]

  it.each(REQUIRED_FIELDS)('every actor should have the "%s" field', (field) => {
    for (const actor of ACTORS) {
      expect(actor).toHaveProperty(field)
    }
  })

  it('every actor should have all required fields defined (not undefined)', () => {
    for (const actor of ACTORS) {
      for (const field of REQUIRED_FIELDS) {
        expect(actor[field]).not.toBeUndefined()
      }
    }
  })

  // ------------------------------------------
  // Layer 1: JinjiMode 分類
  // ------------------------------------------
  describe('JinjiMode classification', () => {
    const JINJI_MODE_ACTORS = ['ACT-01', 'ACT-05', 'ACT-06', 'ACT-07', 'ACT-08', 'ACT-09']
    const KANRI_MODE_ACTORS = ['ACT-02', 'ACT-03', 'ACT-04', 'ACT-10', 'ACT-11', 'ACT-12', 'ACT-13', 'ACT-14', 'ACT-15']

    it.each(JINJI_MODE_ACTORS)('%s should have jinjiMode=true (人事モード)', (actorId) => {
      const actor = ACTORS.find((a) => a.id === actorId)
      expect(actor).toBeDefined()
      expect(actor.jinjiMode).toBe(true)
    })

    it.each(KANRI_MODE_ACTORS)('%s should have jinjiMode=false (管理モード)', (actorId) => {
      const actor = ACTORS.find((a) => a.id === actorId)
      expect(actor).toBeDefined()
      expect(actor.jinjiMode).toBe(false)
    })

    it('should have 6 jinji-mode actors and 9 kanri-mode actors', () => {
      const jinjiCount = ACTORS.filter((a) => a.jinjiMode).length
      const kanriCount = ACTORS.filter((a) => !a.jinjiMode).length
      expect(jinjiCount).toBe(6)
      expect(kanriCount).toBe(9)
    })
  })

  // ------------------------------------------
  // ACT-09 固有属性
  // ------------------------------------------
  describe('ACT-09 specific properties', () => {
    it('should have canDelegate=true', () => {
      const act09 = ACTORS.find((a) => a.id === 'ACT-09')
      expect(act09).toBeDefined()
      expect(act09.canDelegate).toBe(true)
    })

    it('should have employmentType=3 (外部契約者)', () => {
      const act09 = ACTORS.find((a) => a.id === 'ACT-09')
      expect(act09).toBeDefined()
      expect(act09.employmentType).toBe(3)
    })
  })
})

// ============================================
// parseBits テスト
// ============================================
describe('parseBits', () => {
  it('should parse "110000" correctly', () => {
    const result = parseBits('110000')
    expect(result).toEqual({
      bit0: true,
      bit1: true,
      bit2: false,
      bit3: false,
      bit4: false,
      bit5: false,
    })
  })

  it('should parse "001000" correctly', () => {
    const result = parseBits('001000')
    expect(result).toEqual({
      bit0: false,
      bit1: false,
      bit2: true,
      bit3: false,
      bit4: false,
      bit5: false,
    })
  })

  it('should parse "11" correctly (2-bit string)', () => {
    const result = parseBits('11')
    expect(result).toEqual({
      bit0: true,
      bit1: true,
    })
  })

  it('should parse "00" correctly (all zeros)', () => {
    const result = parseBits('00')
    expect(result).toEqual({
      bit0: false,
      bit1: false,
    })
  })

  it('should parse "10" correctly', () => {
    const result = parseBits('10')
    expect(result).toEqual({
      bit0: true,
      bit1: false,
    })
  })

  it('should parse "100000" correctly', () => {
    const result = parseBits('100000')
    expect(result).toEqual({
      bit0: true,
      bit1: false,
      bit2: false,
      bit3: false,
      bit4: false,
      bit5: false,
    })
  })

  it('should return an empty object for an empty string', () => {
    const result = parseBits('')
    expect(result).toEqual({})
  })
})
