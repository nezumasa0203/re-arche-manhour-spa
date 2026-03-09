import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

// --- Mocks ---

const mockFetch = vi.fn()
vi.stubGlobal('$fetch', mockFetch)

// useRuntimeConfig mock
vi.mock('#app', () => ({
  useRuntimeConfig: () => ({
    public: { apiBase: 'http://localhost:8080' },
  }),
}))

// useAuth mock
const mockAuth = {
  isAuthenticated: { value: true },
  user: { value: { userId: 'U001', delegationStaffId: null } },
}
vi.mock('~/composables/useAuth', () => ({
  useAuth: () => mockAuth,
}))

// useAuthStore mock
const mockToken = { value: 'test-jwt-token' }
vi.mock('~/stores/auth', () => ({
  useAuthStore: () => ({ token: mockToken }),
}))

// useCookie mock
vi.mock('#app/composables/cookie', () => ({
  useCookie: () => mockToken,
}))
vi.stubGlobal('useCookie', () => mockToken)

import { useApi } from '~/composables/useApi'

describe('useApi', () => {
  beforeEach(() => {
    mockFetch.mockReset()
    mockAuth.isAuthenticated = { value: true }
    mockAuth.user = { value: { userId: 'U001', delegationStaffId: null } }
    mockToken.value = 'test-jwt-token'
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  // ─── GET ──────────────────────────────────────────────────

  describe('get', () => {
    it('sends GET request with Authorization header', async () => {
      mockFetch.mockResolvedValueOnce({ records: [] })
      const { get } = useApi()

      await get('/work-hours')

      expect(mockFetch).toHaveBeenCalledWith(
        '/api/work-hours',
        expect.objectContaining({
          method: 'GET',
          headers: expect.objectContaining({
            Authorization: 'Bearer test-jwt-token',
          }),
        }),
      )
    })

    it('passes query params', async () => {
      mockFetch.mockResolvedValueOnce({ records: [] })
      const { get } = useApi()

      await get('/work-hours', { yearMonth: '202602' })

      expect(mockFetch).toHaveBeenCalledWith(
        '/api/work-hours',
        expect.objectContaining({
          params: { yearMonth: '202602' },
        }),
      )
    })
  })

  // ─── POST ─────────────────────────────────────────────────

  describe('post', () => {
    it('sends POST request with JSON body', async () => {
      mockFetch.mockResolvedValueOnce({ id: 1 })
      const { post } = useApi()

      await post('/work-hours', { workDate: '2026-02-01' })

      expect(mockFetch).toHaveBeenCalledWith(
        '/api/work-hours',
        expect.objectContaining({
          method: 'POST',
          body: { workDate: '2026-02-01' },
        }),
      )
    })
  })

  // ─── PATCH ────────────────────────────────────────────────

  describe('patch', () => {
    it('sends PATCH request', async () => {
      mockFetch.mockResolvedValueOnce({ id: 1, field: 'hours' })
      const { patch } = useApi()

      await patch('/work-hours/1', { hours: '02:30' })

      expect(mockFetch).toHaveBeenCalledWith(
        '/api/work-hours/1',
        expect.objectContaining({
          method: 'PATCH',
          body: { hours: '02:30' },
        }),
      )
    })
  })

  // ─── DELETE ───────────────────────────────────────────────

  describe('del', () => {
    it('sends DELETE request', async () => {
      mockFetch.mockResolvedValueOnce(undefined)
      const { del } = useApi()

      await del('/work-hours/1')

      expect(mockFetch).toHaveBeenCalledWith(
        '/api/work-hours/1',
        expect.objectContaining({
          method: 'DELETE',
        }),
      )
    })
  })

  // ─── getBlob (Excel export) ───────────────────────────────

  describe('getBlob', () => {
    it('sends GET request with blob responseType', async () => {
      const blobData = new Blob(['test'], { type: 'application/octet-stream' })
      mockFetch.mockResolvedValueOnce(blobData)
      const { getBlob } = useApi()

      const result = await getBlob('/work-hours/export/excel')

      expect(mockFetch).toHaveBeenCalledWith(
        '/api/work-hours/export/excel',
        expect.objectContaining({
          method: 'GET',
          responseType: 'blob',
        }),
      )
      expect(result).toBe(blobData)
    })
  })

  // ─── JWT 自動付与 ─────────────────────────────────────────

  describe('JWT auto-attach', () => {
    it('includes Bearer token from cookie', async () => {
      mockFetch.mockResolvedValueOnce({})
      const { get } = useApi()
      await get('/auth/me')

      const callArgs = mockFetch.mock.calls[0]
      expect(callArgs[1].headers.Authorization).toBe('Bearer test-jwt-token')
    })

    it('omits Authorization when no token', async () => {
      mockToken.value = null
      mockFetch.mockResolvedValueOnce({})
      const { get } = useApi()
      await get('/auth/me')

      const callArgs = mockFetch.mock.calls[0]
      expect(callArgs[1].headers.Authorization).toBeUndefined()
    })
  })

  // ─── 代行ヘッダー ─────────────────────────────────────────

  describe('delegation header', () => {
    it('includes X-Delegation-Staff-Id when delegation active', async () => {
      mockAuth.user = { value: { userId: 'U001', delegationStaffId: 'D002' } }
      mockFetch.mockResolvedValueOnce({})
      const { get } = useApi()
      await get('/work-hours')

      const callArgs = mockFetch.mock.calls[0]
      expect(callArgs[1].headers['X-Delegation-Staff-Id']).toBe('D002')
    })

    it('omits delegation header when not delegating', async () => {
      mockAuth.user = { value: { userId: 'U001', delegationStaffId: null } }
      mockFetch.mockResolvedValueOnce({})
      const { get } = useApi()
      await get('/work-hours')

      const callArgs = mockFetch.mock.calls[0]
      expect(callArgs[1].headers['X-Delegation-Staff-Id']).toBeUndefined()
    })
  })

  // ─── エラーインターセプター ──────────────────────────────

  describe('error interceptor', () => {
    it('throws on 401 with login redirect hint', async () => {
      mockFetch.mockRejectedValueOnce({
        response: { status: 401 },
        data: { code: 'CZ-401', message: 'Unauthorized' },
      })
      const { get } = useApi()

      await expect(get('/work-hours')).rejects.toMatchObject({
        response: { status: 401 },
      })
    })

    it('throws on 409 conflict', async () => {
      mockFetch.mockRejectedValueOnce({
        response: { status: 409 },
        data: { code: 'CZ-101', message: 'Optimistic lock' },
      })
      const { get } = useApi()

      await expect(get('/work-hours')).rejects.toMatchObject({
        response: { status: 409 },
      })
    })

    it('throws on 500 server error', async () => {
      mockFetch.mockRejectedValueOnce({
        response: { status: 500 },
        data: { code: 'CZ-999', message: 'Internal error' },
      })
      const { get } = useApi()

      await expect(get('/work-hours')).rejects.toMatchObject({
        response: { status: 500 },
      })
    })
  })

  // ─── リトライ ─────────────────────────────────────────────

  describe('retry', () => {
    it('retries on network error up to 3 times', async () => {
      const networkError = new TypeError('Failed to fetch')
      mockFetch
        .mockRejectedValueOnce(networkError)
        .mockRejectedValueOnce(networkError)
        .mockResolvedValueOnce({ ok: true })

      const { get } = useApi()
      const result = await get('/work-hours')

      expect(mockFetch).toHaveBeenCalledTimes(3)
      expect(result).toEqual({ ok: true })
    })

    it('gives up after 3 network failures', async () => {
      const networkError = new TypeError('Failed to fetch')
      mockFetch
        .mockRejectedValueOnce(networkError)
        .mockRejectedValueOnce(networkError)
        .mockRejectedValueOnce(networkError)

      const { get } = useApi()

      await expect(get('/work-hours')).rejects.toThrow('Failed to fetch')
      expect(mockFetch).toHaveBeenCalledTimes(3)
    })

    it('does not retry on HTTP errors (non-network)', async () => {
      mockFetch.mockRejectedValueOnce({
        response: { status: 400 },
        data: { code: 'CZ-100', message: 'Bad request' },
      })

      const { get } = useApi()
      await expect(get('/work-hours')).rejects.toMatchObject({
        response: { status: 400 },
      })
      expect(mockFetch).toHaveBeenCalledTimes(1)
    })
  })
})
