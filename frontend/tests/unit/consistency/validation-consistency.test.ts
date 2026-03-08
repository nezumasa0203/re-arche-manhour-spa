import { describe, it, expect } from 'vitest'
import { calculateByteLength } from '~/utils/byteLength'

/**
 * T-032: バイト長計算の FE/BE 一致テスト (Frontend 側)
 *
 * Backend ByteLengthCalculator と同一テストケースで結果を検証する。
 * Backend テスト実行時に同一パターンで比較すること。
 */
describe('FE/BE 一致 - バイト長計算', () => {
  // 代表パターン 20 件
  const testCases: [string, number][] = [
    // 半角英数字
    ['', 0],
    ['a', 1],
    ['abc', 3],
    ['Hello World', 11],
    ['1234567890', 10],

    // 全角文字
    ['あ', 2],
    ['あいう', 6],
    ['漢字テスト', 10],
    ['工数入力', 8],

    // 半角カタカナ
    ['ｱ', 2],
    ['ｱｲｳ', 6],
    ['ﾃｽﾄ', 6],

    // 混在
    ['aあｱ', 5],
    ['Hello世界', 9],
    ['abc123あいう', 12],
    ['TMR12345', 8],

    // 境界値 (128バイト)
    ['a'.repeat(128), 128],
    ['a'.repeat(129), 129],
    ['あ'.repeat(64), 128],
    ['あ'.repeat(65), 130],
  ]

  it.each(testCases)(
    'calculateByteLength("%s") → %d',
    (input, expected) => {
      expect(calculateByteLength(input)).toBe(expected)
    },
  )

  // 特殊ケース
  it('スペースは1バイト', () => {
    expect(calculateByteLength(' ')).toBe(1)
  })

  it('全角スペースは2バイト', () => {
    expect(calculateByteLength('\u3000')).toBe(2)
  })

  it('改行は1バイト', () => {
    expect(calculateByteLength('\n')).toBe(1)
  })

  it('タブは1バイト', () => {
    expect(calculateByteLength('\t')).toBe(1)
  })
})
