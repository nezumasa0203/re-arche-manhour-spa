import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useNotification } from '~/composables/useNotification'

// PrimeVue useToast モック
const mockAdd = vi.fn()
vi.mock('primevue/usetoast', () => ({
  useToast: () => ({ add: mockAdd }),
}))

describe('useNotification', () => {
  beforeEach(() => {
    mockAdd.mockClear()
  })

  // ─── CZ コードレンジ自動判定 ────────────────────────────

  describe('showByCode - コードレンジ自動判定', () => {
    it('CZ-000〜099 → success severity', () => {
      const { showByCode } = useNotification()
      showByCode('CZ-000', '成功完了')

      expect(mockAdd).toHaveBeenCalledWith(
        expect.objectContaining({
          severity: 'success',
          summary: '成功完了',
          life: 3000,
        }),
      )
    })

    it('CZ-001 → success', () => {
      const { showByCode } = useNotification()
      showByCode('CZ-001', '登録終了')

      expect(mockAdd).toHaveBeenCalledWith(
        expect.objectContaining({ severity: 'success' }),
      )
    })

    it('CZ-100〜299 → warn severity', () => {
      const { showByCode } = useNotification()
      showByCode('CZ-126', '作業日は必須入力です')

      expect(mockAdd).toHaveBeenCalledWith(
        expect.objectContaining({
          severity: 'warn',
          summary: '作業日は必須入力です',
          life: 5000,
        }),
      )
    })

    it('CZ-147 → warn', () => {
      const { showByCode } = useNotification()
      showByCode('CZ-147', '工数は15分単位で入力してください')

      expect(mockAdd).toHaveBeenCalledWith(
        expect.objectContaining({ severity: 'warn' }),
      )
    })

    it('CZ-300〜499 → error severity (sticky)', () => {
      const { showByCode } = useNotification()
      showByCode('CZ-300', 'システムエラー')

      expect(mockAdd).toHaveBeenCalledWith(
        expect.objectContaining({
          severity: 'error',
          summary: 'システムエラー',
        }),
      )
      // error は手動消去（life なし）
      expect(mockAdd.mock.calls[0][0].life).toBeUndefined()
    })

    it('CZ-800〜999 → info severity', () => {
      const { showByCode } = useNotification()
      showByCode('CZ-800', '対象を確認に戻しました')

      expect(mockAdd).toHaveBeenCalledWith(
        expect.objectContaining({
          severity: 'info',
          summary: '対象を確認に戻しました',
          life: 3000,
        }),
      )
    })

    it('未知のレンジ (CZ-600) → error severity', () => {
      const { showByCode } = useNotification()
      showByCode('CZ-600', '不明なエラー')

      expect(mockAdd).toHaveBeenCalledWith(
        expect.objectContaining({ severity: 'error' }),
      )
    })
  })

  // ─── 明示的 severity メソッド ────────────────────────────

  describe('success', () => {
    it('success severity + 3秒自動消去', () => {
      const { success } = useNotification()
      success('登録完了しました')

      expect(mockAdd).toHaveBeenCalledWith(
        expect.objectContaining({
          severity: 'success',
          summary: '登録完了しました',
          life: 3000,
        }),
      )
    })
  })

  describe('warn', () => {
    it('warn severity + 5秒自動消去', () => {
      const { warn } = useNotification()
      warn('入力エラーがあります')

      expect(mockAdd).toHaveBeenCalledWith(
        expect.objectContaining({
          severity: 'warn',
          summary: '入力エラーがあります',
          life: 5000,
        }),
      )
    })
  })

  describe('error', () => {
    it('error severity + 手動消去 (sticky)', () => {
      const { error } = useNotification()
      error('システムエラーが発生しました')

      expect(mockAdd).toHaveBeenCalledWith(
        expect.objectContaining({
          severity: 'error',
          summary: 'システムエラーが発生しました',
        }),
      )
      expect(mockAdd.mock.calls[0][0].life).toBeUndefined()
    })
  })

  describe('info', () => {
    it('info severity + 3秒自動消去', () => {
      const { info } = useNotification()
      info('処理が完了しました')

      expect(mockAdd).toHaveBeenCalledWith(
        expect.objectContaining({
          severity: 'info',
          summary: '処理が完了しました',
          life: 3000,
        }),
      )
    })
  })

  // ─── detail パラメータ ──────────────────────────────────

  describe('detail', () => {
    it('detail を渡せる', () => {
      const { success } = useNotification()
      success('登録完了', '3件のレコードを保存しました')

      expect(mockAdd).toHaveBeenCalledWith(
        expect.objectContaining({
          summary: '登録完了',
          detail: '3件のレコードを保存しました',
        }),
      )
    })
  })
})
