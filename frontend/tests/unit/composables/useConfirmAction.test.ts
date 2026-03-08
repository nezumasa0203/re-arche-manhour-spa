import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useConfirmAction } from '~/composables/useConfirmAction'

// PrimeVue useConfirm モック
const mockRequire = vi.fn()
vi.mock('primevue/useconfirm', () => ({
  useConfirm: () => ({ require: mockRequire }),
}))

describe('useConfirmAction', () => {
  beforeEach(() => {
    mockRequire.mockClear()
  })

  // ─── 基本動作 ──────────────────────────────────────────

  describe('confirm', () => {
    it('確認ダイアログを表示する', () => {
      const { confirm } = useConfirmAction()
      confirm('CZ-506', '選択したレコードを削除します。よろしいですか？')

      expect(mockRequire).toHaveBeenCalledTimes(1)
      expect(mockRequire).toHaveBeenCalledWith(
        expect.objectContaining({
          message: '選択したレコードを削除します。よろしいですか？',
        }),
      )
    })

    it('Promise<boolean> を返す', () => {
      const { confirm } = useConfirmAction()
      const result = confirm('CZ-505', 'テスト')
      expect(result).toBeInstanceOf(Promise)
    })

    it('OK クリック → true を返す', async () => {
      mockRequire.mockImplementation((options: { accept: () => void }) => {
        options.accept()
      })

      const { confirm } = useConfirmAction()
      const result = await confirm('CZ-506', '削除しますか？')
      expect(result).toBe(true)
    })

    it('キャンセル → false を返す', async () => {
      mockRequire.mockImplementation((options: { reject: () => void }) => {
        options.reject()
      })

      const { confirm } = useConfirmAction()
      const result = await confirm('CZ-506', '削除しますか？')
      expect(result).toBe(false)
    })
  })

  // ─── CZ 確認コードテスト ───────────────────────────────

  describe('CZ 確認コード', () => {
    it.each([
      ['CZ-505', '「作成中」を全て「確認」に変更します。よろしいですか？'],
      ['CZ-506', '選択したレコードを削除します。よろしいですか？'],
      ['CZ-507', '選択レコードを「確認」に戻します。よろしいですか？'],
      ['CZ-508', '選択レコードを「承認」に変更します。よろしいですか？'],
      ['CZ-509', '「記入可能」に戻します。よろしいですか？'],
      ['CZ-510', '「登録確認」に変更します。よろしいですか？'],
      ['CZ-511', '「データ承認」に変更します。よろしいですか？'],
      ['CZ-518', '「確認」を全て「作成中」に変更します。よろしいですか？'],
    ])('%s のメッセージで確認ダイアログが表示される', (code, message) => {
      const { confirm } = useConfirmAction()
      confirm(code, message)

      expect(mockRequire).toHaveBeenCalledWith(
        expect.objectContaining({ message }),
      )
    })
  })

  // ─── confirmByCode (メッセージカタログ連携) ─────────────

  describe('confirmByCode', () => {
    it('CZ コードからメッセージを解決して表示する', () => {
      const { confirmByCode } = useConfirmAction()
      confirmByCode('CZ-506')

      expect(mockRequire).toHaveBeenCalledWith(
        expect.objectContaining({
          message: '選択したレコードを削除します。よろしいですか？',
        }),
      )
    })

    it('CZ-500 のメッセージを解決する', () => {
      const { confirmByCode } = useConfirmAction()
      confirmByCode('CZ-500')

      expect(mockRequire).toHaveBeenCalledWith(
        expect.objectContaining({
          message: 'システムを終了します。よろしいですか？',
        }),
      )
    })

    it('OK → true を返す', async () => {
      mockRequire.mockImplementation((options: { accept: () => void }) => {
        options.accept()
      })

      const { confirmByCode } = useConfirmAction()
      const result = await confirmByCode('CZ-506')
      expect(result).toBe(true)
    })

    it('キャンセル → false を返す', async () => {
      mockRequire.mockImplementation((options: { reject: () => void }) => {
        options.reject()
      })

      const { confirmByCode } = useConfirmAction()
      const result = await confirmByCode('CZ-506')
      expect(result).toBe(false)
    })
  })
})
