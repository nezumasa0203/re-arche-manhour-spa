import { useAuth } from '~/composables/useAuth'

const API_PREFIX = '/api'
const MAX_RETRIES = 3
const RETRY_BASE_MS = 200

function isNetworkError(error: unknown): boolean {
  return error instanceof TypeError && /fetch|network/i.test(error.message)
}

function sleep(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms))
}

export function useApi() {
  const auth = useAuth()

  function buildHeaders(): Record<string, string> {
    const headers: Record<string, string> = {}

    const token = useCookie('cz-auth-token').value
    if (token) {
      headers.Authorization = `Bearer ${token}`
    }

    const delegationStaffId = auth.user.value?.delegationStaffId
    if (delegationStaffId) {
      headers['X-Delegation-Staff-Id'] = delegationStaffId
    }

    return headers
  }

  async function request<T>(
    path: string,
    options: Record<string, unknown> = {},
  ): Promise<T> {
    const url = `${API_PREFIX}${path}`
    const headers = { ...buildHeaders(), ...(options.headers as Record<string, string> ?? {}) }
    const fetchOptions = { ...options, headers }

    let lastError: unknown
    for (let attempt = 0; attempt < MAX_RETRIES; attempt++) {
      try {
        return await $fetch<T>(url, fetchOptions)
      } catch (error) {
        lastError = error
        if (!isNetworkError(error)) {
          throw error
        }
        if (attempt < MAX_RETRIES - 1) {
          await sleep(RETRY_BASE_MS * Math.pow(2, attempt))
        }
      }
    }
    throw lastError
  }

  function get<T = unknown>(path: string, params?: Record<string, unknown>): Promise<T> {
    return request<T>(path, { method: 'GET', params })
  }

  function post<T = unknown>(path: string, body?: unknown): Promise<T> {
    return request<T>(path, { method: 'POST', body })
  }

  function patch<T = unknown>(path: string, body?: unknown): Promise<T> {
    return request<T>(path, { method: 'PATCH', body })
  }

  function del<T = void>(path: string): Promise<T> {
    return request<T>(path, { method: 'DELETE' })
  }

  function getBlob(path: string, params?: Record<string, unknown>): Promise<Blob> {
    return request<Blob>(path, { method: 'GET', params, responseType: 'blob' })
  }

  return { get, post, patch, del, getBlob }
}
