import { useConfirm } from 'primevue/useconfirm'
import { resolveMessage } from '~/constants/messages'

/**
 * CZ 確認ダイアログ composable。
 *
 * PrimeVue ConfirmDialog をラップし、Promise<boolean> で結果を返す。
 * CZ-500〜799 の確認コードに対応。
 */
export function useConfirmAction() {
  const confirmService = useConfirm()

  /**
   * 確認ダイアログを表示する。
   *
   * @param code    CZ 確認コード (e.g., "CZ-506")
   * @param message 表示メッセージ
   * @returns OK → true, キャンセル → false
   */
  function confirm(code: string, message: string): Promise<boolean> {
    return new Promise((resolve) => {
      confirmService.require({
        message,
        header: '確認',
        accept: () => resolve(true),
        reject: () => resolve(false),
      })
    })
  }

  /**
   * CZ コードからメッセージを自動解決して確認ダイアログを表示する。
   *
   * @param code   CZ 確認コード (e.g., "CZ-506")
   * @param params メッセージパラメータ (optional)
   * @returns OK → true, キャンセル → false
   */
  function confirmByCode(code: string, params?: string[]): Promise<boolean> {
    const message = resolveMessage(code, params)
    return confirm(code, message)
  }

  return { confirm, confirmByCode }
}
