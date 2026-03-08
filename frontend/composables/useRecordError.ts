import { ref, computed } from 'vue'

interface ApiError {
  code: string
  message: string
  recordId?: number
}

/**
 * 行エラーハイライト composable。
 *
 * API エラーレスポンスの recordId から行を特定し、
 * ハイライト + スクロール + フォーカスを行う。
 */
export function useRecordError() {
  const errorRecordIds = ref<number[]>([])

  function setRecordErrors(ids: number[]) {
    errorRecordIds.value = [...ids]
  }

  function clearRecordErrors() {
    errorRecordIds.value = []
  }

  function hasRecordError(recordId: number): boolean {
    return errorRecordIds.value.includes(recordId)
  }

  function getRowClass(recordId: number): string {
    return hasRecordError(recordId) ? 'row-error' : ''
  }

  /**
   * 対象行にスクロールしてハイライトアニメーションを適用する。
   */
  function scrollToRecord(recordId: number) {
    const row = document.querySelector(`[data-record-id="${recordId}"]`)
    if (!row) return

    row.scrollIntoView({ behavior: 'smooth', block: 'center' })
    row.classList.add('row-error-highlight')
  }

  /**
   * API エラーレスポンスから recordId を抽出してエラー行を設定する。
   * 最初のエラー行にスクロールする。
   */
  function handleApiErrors(errors: ApiError[]) {
    const ids = [...new Set(
      errors
        .filter((e) => e.recordId != null)
        .map((e) => e.recordId!),
    )]

    setRecordErrors(ids)

    if (ids.length > 0) {
      scrollToRecord(ids[0])
    }
  }

  return {
    errorRecordIds: computed(() => errorRecordIds.value),
    setRecordErrors,
    clearRecordErrors,
    hasRecordError,
    getRowClass,
    scrollToRecord,
    handleApiErrors,
  }
}
