import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useRecordError } from '~/composables/useRecordError'

describe('useRecordError', () => {
  beforeEach(() => {
    // DOM をリセット
    document.body.innerHTML = ''
  })

  // ─── 基本動作 ──────────────────────────────────────────

  describe('初期状態', () => {
    it('エラー行なしの初期状態', () => {
      const { errorRecordIds, hasRecordError } = useRecordError()
      expect(errorRecordIds.value).toEqual([])
      expect(hasRecordError(1)).toBe(false)
    })
  })

  // ─── setRecordErrors / clearRecordErrors ──────────────

  describe('setRecordErrors', () => {
    it('エラー行 ID を設定できる', () => {
      const { setRecordErrors, hasRecordError } = useRecordError()

      setRecordErrors([1, 5, 10])

      expect(hasRecordError(1)).toBe(true)
      expect(hasRecordError(5)).toBe(true)
      expect(hasRecordError(10)).toBe(true)
      expect(hasRecordError(2)).toBe(false)
    })
  })

  describe('clearRecordErrors', () => {
    it('全エラー行をクリアする', () => {
      const { setRecordErrors, clearRecordErrors, hasRecordError, errorRecordIds } = useRecordError()

      setRecordErrors([1, 5])
      clearRecordErrors()

      expect(hasRecordError(1)).toBe(false)
      expect(errorRecordIds.value).toEqual([])
    })
  })

  // ─── getRowClass ──────────────────────────────────────

  describe('getRowClass', () => {
    it('エラー行 → row-error クラスを返す', () => {
      const { setRecordErrors, getRowClass } = useRecordError()

      setRecordErrors([1])

      expect(getRowClass(1)).toContain('row-error')
    })

    it('正常行 → 空文字列を返す', () => {
      const { getRowClass } = useRecordError()
      expect(getRowClass(1)).toBe('')
    })
  })

  // ─── scrollToRecord ───────────────────────────────────

  describe('scrollToRecord', () => {
    it('対象行にスクロールする', () => {
      const row = document.createElement('tr')
      row.setAttribute('data-record-id', '5')
      row.scrollIntoView = vi.fn()
      document.body.appendChild(row)

      const { scrollToRecord } = useRecordError()
      scrollToRecord(5)

      expect(row.scrollIntoView).toHaveBeenCalledWith({
        behavior: 'smooth',
        block: 'center',
      })
    })

    it('対象行が存在しない場合はエラーにならない', () => {
      const { scrollToRecord } = useRecordError()
      expect(() => scrollToRecord(999)).not.toThrow()
    })

    it('スクロール後にハイライトクラスが付与される', () => {
      const row = document.createElement('tr')
      row.setAttribute('data-record-id', '3')
      row.scrollIntoView = vi.fn()
      document.body.appendChild(row)

      const { scrollToRecord } = useRecordError()
      scrollToRecord(3)

      expect(row.classList.contains('row-error-highlight')).toBe(true)
    })
  })

  // ─── API エラーレスポンスからの処理 ────────────────────

  describe('handleApiErrors', () => {
    it('recordId 付きエラーから行 ID を抽出して設定する', () => {
      const { handleApiErrors, hasRecordError } = useRecordError()

      handleApiErrors([
        { code: 'CZ-126', message: 'エラー1', recordId: 1 },
        { code: 'CZ-147', message: 'エラー2', recordId: 5 },
      ])

      expect(hasRecordError(1)).toBe(true)
      expect(hasRecordError(5)).toBe(true)
    })

    it('recordId がないエラーは無視する', () => {
      const { handleApiErrors, errorRecordIds } = useRecordError()

      handleApiErrors([
        { code: 'CZ-300', message: 'システムエラー' },
      ])

      expect(errorRecordIds.value).toEqual([])
    })

    it('重複する recordId は1つにまとめる', () => {
      const { handleApiErrors, errorRecordIds } = useRecordError()

      handleApiErrors([
        { code: 'CZ-126', message: 'エラー1', recordId: 1 },
        { code: 'CZ-147', message: 'エラー2', recordId: 1 },
      ])

      expect(errorRecordIds.value).toEqual([1])
    })
  })
})
