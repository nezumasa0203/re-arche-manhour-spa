import { describe, it, expect, vi } from 'vitest'
import { ref, nextTick } from 'vue'
import { useCellValidation } from '~/composables/useCellValidation'
import type { ValidationResult } from '~/types/validation'

describe('useCellValidation', () => {
  // ─── 基本動作 ──────────────────────────────────────────

  describe('初期状態', () => {
    it('エラーなしの初期状態', () => {
      const { errors, hasError, getError } = useCellValidation()
      expect(errors.value).toEqual({})
      expect(hasError('workDate')).toBe(false)
      expect(getError('workDate')).toBeNull()
    })
  })

  // ─── setError / clearError ────────────────────────────

  describe('setError', () => {
    it('フィールドにエラーを設定できる', () => {
      const { setError, hasError, getError } = useCellValidation()

      setError('workDate', {
        valid: false,
        code: 'CZ-126',
        message: '作業日は必須入力です',
        field: 'workDate',
      })

      expect(hasError('workDate')).toBe(true)
      expect(getError('workDate')).toEqual(
        expect.objectContaining({
          code: 'CZ-126',
          message: '作業日は必須入力です',
        }),
      )
    })

    it('複数フィールドにエラーを設定できる', () => {
      const { setError, hasError } = useCellValidation()

      setError('workDate', { valid: false, code: 'CZ-126', message: 'エラー1', field: 'workDate' })
      setError('subject', { valid: false, code: 'CZ-128', message: 'エラー2', field: 'subject' })

      expect(hasError('workDate')).toBe(true)
      expect(hasError('subject')).toBe(true)
    })
  })

  describe('clearError', () => {
    it('特定フィールドのエラーをクリアできる', () => {
      const { setError, clearError, hasError } = useCellValidation()

      setError('workDate', { valid: false, code: 'CZ-126', message: 'エラー', field: 'workDate' })
      expect(hasError('workDate')).toBe(true)

      clearError('workDate')
      expect(hasError('workDate')).toBe(false)
    })

    it('存在しないフィールドのクリアはエラーにならない', () => {
      const { clearError } = useCellValidation()
      expect(() => clearError('nonExistent')).not.toThrow()
    })
  })

  describe('clearAll', () => {
    it('全エラーをクリアする', () => {
      const { setError, clearAll, hasError, errors } = useCellValidation()

      setError('workDate', { valid: false, code: 'CZ-126', message: 'エラー1', field: 'workDate' })
      setError('subject', { valid: false, code: 'CZ-128', message: 'エラー2', field: 'subject' })

      clearAll()

      expect(hasError('workDate')).toBe(false)
      expect(hasError('subject')).toBe(false)
      expect(errors.value).toEqual({})
    })
  })

  // ─── setErrors (一括設定) ─────────────────────────────

  describe('setErrors', () => {
    it('ValidationResult 配列からエラーを一括設定する', () => {
      const { setErrors, hasError, getError } = useCellValidation()

      const results: ValidationResult[] = [
        { valid: false, code: 'CZ-126', message: '作業日は必須入力です', field: 'workDate' },
        { valid: false, code: 'CZ-147', message: '工数は15分単位', field: 'hours' },
      ]

      setErrors(results)

      expect(hasError('workDate')).toBe(true)
      expect(hasError('hours')).toBe(true)
      expect(getError('workDate')?.code).toBe('CZ-126')
      expect(getError('hours')?.code).toBe('CZ-147')
    })

    it('valid: true の結果は無視される', () => {
      const { setErrors, hasError } = useCellValidation()

      setErrors([
        { valid: true },
        { valid: false, code: 'CZ-126', message: 'エラー', field: 'workDate' },
      ])

      expect(hasError('workDate')).toBe(true)
    })

    it('field が未指定の結果は無視される', () => {
      const { setErrors, errors } = useCellValidation()

      setErrors([
        { valid: false, code: 'CZ-300', message: 'システムエラー' },
      ])

      expect(Object.keys(errors.value).length).toBe(0)
    })

    it('既存エラーをクリアしてから設定する', () => {
      const { setError, setErrors, hasError } = useCellValidation()

      setError('subject', { valid: false, code: 'CZ-128', message: 'エラー', field: 'subject' })

      setErrors([
        { valid: false, code: 'CZ-126', message: 'エラー', field: 'workDate' },
      ])

      expect(hasError('workDate')).toBe(true)
      expect(hasError('subject')).toBe(false)
    })
  })

  // ─── errorCount ───────────────────────────────────────

  describe('errorCount', () => {
    it('エラー件数を返す', () => {
      const { setError, errorCount } = useCellValidation()

      expect(errorCount.value).toBe(0)

      setError('workDate', { valid: false, code: 'CZ-126', message: 'エラー', field: 'workDate' })
      expect(errorCount.value).toBe(1)

      setError('subject', { valid: false, code: 'CZ-128', message: 'エラー', field: 'subject' })
      expect(errorCount.value).toBe(2)
    })
  })

  // ─── getCellClass ─────────────────────────────────────

  describe('getCellClass', () => {
    it('エラーあり → cell-error クラスを返す', () => {
      const { setError, getCellClass } = useCellValidation()

      setError('workDate', { valid: false, code: 'CZ-126', message: 'エラー', field: 'workDate' })

      expect(getCellClass('workDate')).toContain('cell-error')
    })

    it('エラーなし → 空文字列を返す', () => {
      const { getCellClass } = useCellValidation()
      expect(getCellClass('workDate')).toBe('')
    })
  })

  // ─── getTooltip ───────────────────────────────────────

  describe('getTooltip', () => {
    it('エラーあり → エラーメッセージを返す', () => {
      const { setError, getTooltip } = useCellValidation()

      setError('workDate', {
        valid: false,
        code: 'CZ-126',
        message: '作業日は必須入力です',
        field: 'workDate',
      })

      expect(getTooltip('workDate')).toBe('作業日は必須入力です')
    })

    it('エラーなし → 空文字列を返す', () => {
      const { getTooltip } = useCellValidation()
      expect(getTooltip('workDate')).toBe('')
    })
  })
})
