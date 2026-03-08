import { reactive, computed } from 'vue'
import type { ValidationResult } from '~/types/validation'

/**
 * セルバリデーション composable。
 *
 * フィールド単位でバリデーションエラーを管理し、
 * セル赤枠 + ツールチップ表示のためのクラス名・メッセージを提供する。
 */
export function useCellValidation() {
  const errors = reactive<Record<string, ValidationResult>>({})

  function setError(field: string, result: ValidationResult) {
    errors[field] = result
  }

  function clearError(field: string) {
    delete errors[field]
  }

  function clearAll() {
    Object.keys(errors).forEach((key) => delete errors[key])
  }

  /**
   * ValidationResult 配列からエラーを一括設定する。
   * 既存エラーはクリアされる。
   */
  function setErrors(results: ValidationResult[]) {
    clearAll()
    for (const result of results) {
      if (!result.valid && result.field) {
        errors[result.field] = result
      }
    }
  }

  function hasError(field: string): boolean {
    return field in errors
  }

  function getError(field: string): ValidationResult | null {
    return errors[field] ?? null
  }

  const errorCount = computed(() => Object.keys(errors).length)

  function getCellClass(field: string): string {
    return hasError(field) ? 'cell-error' : ''
  }

  function getTooltip(field: string): string {
    return errors[field]?.message ?? ''
  }

  return {
    errors: computed(() => errors),
    errorCount,
    setError,
    clearError,
    clearAll,
    setErrors,
    hasError,
    getError,
    getCellClass,
    getTooltip,
  }
}
