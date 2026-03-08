import { useToast } from 'primevue/usetoast'

/**
 * CZ 通知 composable。
 *
 * PrimeVue Toast をラップし、CZ エラーコードレンジから
 * severity と自動消去タイマーを自動判定する。
 *
 * コードレンジ:
 *   CZ-000〜099 → success (3秒)
 *   CZ-100〜299 → warn (5秒)
 *   CZ-300〜499 → error (手動消去)
 *   CZ-800〜999 → info (3秒)
 */
export function useNotification() {
  const toast = useToast()

  function resolveSeverity(code: string): 'success' | 'info' | 'warn' | 'error' {
    const num = parseInt(code.replace('CZ-', ''), 10)
    if (num >= 0 && num <= 99) return 'success'
    if (num >= 100 && num <= 299) return 'warn'
    if (num >= 300 && num <= 499) return 'error'
    if (num >= 800 && num <= 999) return 'info'
    return 'error'
  }

  function resolveLife(severity: string): number | undefined {
    if (severity === 'success') return 3000
    if (severity === 'warn') return 5000
    if (severity === 'info') return 3000
    return undefined // error は手動消去
  }

  function showByCode(code: string, message: string, detail?: string) {
    const severity = resolveSeverity(code)
    const life = resolveLife(severity)
    toast.add({
      severity,
      summary: message,
      ...(detail ? { detail } : {}),
      ...(life != null ? { life } : {}),
    })
  }

  function success(message: string, detail?: string) {
    toast.add({
      severity: 'success',
      summary: message,
      ...(detail ? { detail } : {}),
      life: 3000,
    })
  }

  function warn(message: string, detail?: string) {
    toast.add({
      severity: 'warn',
      summary: message,
      ...(detail ? { detail } : {}),
      life: 5000,
    })
  }

  function error(message: string, detail?: string) {
    toast.add({
      severity: 'error',
      summary: message,
      ...(detail ? { detail } : {}),
    })
  }

  function info(message: string, detail?: string) {
    toast.add({
      severity: 'info',
      summary: message,
      ...(detail ? { detail } : {}),
      life: 3000,
    })
  }

  return { showByCode, success, warn, error, info }
}
