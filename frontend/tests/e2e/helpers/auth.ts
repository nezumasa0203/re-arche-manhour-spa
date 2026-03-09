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

/** ACT-01: 報告担当者（一般権限） */
export const ACTOR_STAFF = 'ACT-01'

/** ACT-03: 全権管理者 */
export const ACTOR_ADMIN = 'ACT-03'

/** ACT-09: 外部契約者（canDelegate=true） */
export const ACTOR_DELEGATE = 'ACT-09'
