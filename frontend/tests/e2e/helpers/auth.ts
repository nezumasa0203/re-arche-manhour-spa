import type { Page } from '@playwright/test'

const AUTH_MOCK_URL = process.env.AUTH_MOCK_URL || 'http://localhost:8180'

/**
 * auth-mock からトークンを取得し、ブラウザの cookie にセットしてログイン状態にする
 */
export async function loginAs(page: Page, actorId: string): Promise<void> {
  const response = await fetch(`${AUTH_MOCK_URL}/api/switch`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ actorId }),
  })

  if (!response.ok) {
    throw new Error(`Failed to login as ${actorId}: ${response.status}`)
  }

  const { token } = (await response.json()) as { token: string }

  // Playwright context cookie + ブラウザ document.cookie の両方にセット
  // （Nuxt useCookie() が document.cookie を参照するため）
  await page.context().addCookies([
    {
      name: 'cz-auth-token',
      value: token,
      domain: 'localhost',
      path: '/',
    },
  ])

  // ベースURL上で document.cookie にもセット（Nuxt useCookie() が参照するため）
  const baseURL = process.env.E2E_BASE_URL || 'http://localhost:3000'
  await page.goto(baseURL, { waitUntil: 'commit' })
  await page.evaluate((t: string) => {
    document.cookie = `cz-auth-token=${t}; path=/`
  }, token)
}

/**
 * 指定月のテストレコード（STATUS_0）を全削除する。
 * テスト間のデータ干渉を防ぐためのクリーンアップ用。
 */
export async function cleanupTestRecords(actorId: string, yearMonth: string): Promise<void> {
  const BACKEND_URL = process.env.E2E_BACKEND_URL || 'http://localhost:8080'
  const resp = await fetch(`${AUTH_MOCK_URL}/api/switch`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ actorId }),
  })
  if (!resp.ok) return
  const { token } = (await resp.json()) as { token: string }
  const headers = { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` }

  // GET で全レコード取得
  const listResp = await fetch(`${BACKEND_URL}/api/work-hours?staffId=${actorId}&yearMonth=${yearMonth}`, { headers })
  if (!listResp.ok) return
  const data = (await listResp.json()) as { records: Array<{ id: number; status: string }> }
  const draftIds = data.records.filter(r => r.status === '0').map(r => r.id)

  // STATUS_0 のレコードを個別に DELETE
  for (const id of draftIds) {
    await fetch(`${BACKEND_URL}/api/work-hours/${id}`, { method: 'DELETE', headers })
  }
}

/** ACT-01: 報告担当者（一般権限） */
export const ACTOR_STAFF = 'ACT-01'

/** ACT-03: 全権管理者 */
export const ACTOR_ADMIN = 'ACT-03'

/** ACT-09: 外部契約者（canDelegate=true） */
export const ACTOR_DELEGATE = 'ACT-09'
