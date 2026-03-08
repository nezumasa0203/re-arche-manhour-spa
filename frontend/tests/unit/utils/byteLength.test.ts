import { describe, it, expect } from 'vitest'
import { calculateByteLength } from '~/utils/byteLength'

describe('calculateByteLength', () => {
  describe('半角文字 (1バイト)', () => {
    it('半角英字', () => {
      expect(calculateByteLength('abc')).toBe(3)
    })

    it('半角数字', () => {
      expect(calculateByteLength('123')).toBe(3)
    })

    it('半角記号', () => {
      expect(calculateByteLength('!@#')).toBe(3)
    })

    it('半角スペース', () => {
      expect(calculateByteLength(' ')).toBe(1)
    })
  })

  describe('全角文字 (2バイト)', () => {
    it('ひらがな', () => {
      expect(calculateByteLength('あいう')).toBe(6)
    })

    it('カタカナ（全角）', () => {
      expect(calculateByteLength('アイウ')).toBe(6)
    })

    it('漢字', () => {
      expect(calculateByteLength('漢字')).toBe(4)
    })

    it('全角英数字', () => {
      expect(calculateByteLength('ＡＢＣ')).toBe(6)
    })
  })

  describe('半角カタカナ (2バイト)', () => {
    it('半角カタカナ', () => {
      expect(calculateByteLength('ｱｲｳ')).toBe(6)
    })

    it('半角カタカナ濁点', () => {
      expect(calculateByteLength('ｶﾞ')).toBe(4) // ｶ(2) + ﾞ(2)
    })
  })

  describe('混在文字列', () => {
    it('半角英字 + 全角 + 半角カタカナ', () => {
      expect(calculateByteLength('aあｱ')).toBe(5) // 1 + 2 + 2
    })

    it('数字 + 漢字', () => {
      expect(calculateByteLength('1漢')).toBe(3) // 1 + 2
    })
  })

  describe('空文字・境界値', () => {
    it('空文字列は0バイト', () => {
      expect(calculateByteLength('')).toBe(0)
    })

    it('128バイトちょうど（半角128文字）', () => {
      expect(calculateByteLength('a'.repeat(128))).toBe(128)
    })

    it('128バイトちょうど（全角64文字）', () => {
      expect(calculateByteLength('あ'.repeat(64))).toBe(128)
    })

    it('129バイト超過', () => {
      expect(calculateByteLength('a'.repeat(128) + 'b')).toBe(129)
    })
  })
})
