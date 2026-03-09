import { test, expect } from '@playwright/test'
import { loginAs, ACTOR_STAFF, ACTOR_DELEGATE } from './helpers/auth'
import { WorkHoursPage } from './pages/WorkHoursPage'

/**
 * T-056: 代行モード E2E
 * 代行モードの開始→操作→解除フローを検証
 *
 * NOTE: staff-selector（担当者選択 Dropdown）は未実装のため、
 * 代行操作系テストは skip。代行不可ユーザーの非表示チェックのみ実行。
 */

let workHours: WorkHoursPage

test.describe('代行モード', () => {
  test.describe('代行可能ユーザー（ACT-09）', () => {
    test.beforeEach(async ({ page }) => {
      await loginAs(page, ACTOR_DELEGATE)
      workHours = new WorkHoursPage(page)
      await workHours.goto()
      await workHours.waitForLoad()
    })

    // staff-selector 未実装のため skip
    test.skip('担当者選択UIが利用可能', async () => {
      const staffSelector = workHours.page.locator('[data-testid="staff-selector"]')
      await expect(staffSelector).toBeVisible()
    })

    test.skip('担当者を選択すると「代行中」バッジが表示され、対象者のデータが表示される', async ({ page }) => {
      const staffSelector = workHours.page.locator('[data-testid="staff-selector"]')
      await staffSelector.click()

      const staffOption = page.locator('.p-dropdown-item', { hasText: 'E00001' }).first()
      await staffOption.click()
      await workHours.waitForLoad()

      await expect(workHours.daikoBadge).toBeVisible()
    })

    test.skip('代行中にレコードを追加すると、対象担当者名義で追加される', async ({ page }) => {
      const staffSelector = workHours.page.locator('[data-testid="staff-selector"]')
      await staffSelector.click()
      const staffOption = page.locator('.p-dropdown-item', { hasText: 'E00001' }).first()
      await staffOption.click()
      await workHours.waitForLoad()

      await workHours.addRecord()

      const rowCount = await workHours.rows.count()
      expect(rowCount).toBeGreaterThan(0)
    })

    test.skip('代行解除で自分のデータ表示に戻る', async ({ page }) => {
      const staffSelector = workHours.page.locator('[data-testid="staff-selector"]')
      await staffSelector.click()
      const staffOption = page.locator('.p-dropdown-item', { hasText: 'E00001' }).first()
      await staffOption.click()
      await workHours.waitForLoad()

      await workHours.daikoCancelBtn.click()
      await workHours.waitForLoad()

      await expect(workHours.daikoBadge).not.toBeVisible()
    })
  })

  test.describe('代行不可ユーザー（ACT-01）', () => {
    test('担当者選択UIが非表示', async ({ page }) => {
      await loginAs(page, ACTOR_STAFF)
      workHours = new WorkHoursPage(page)
      await workHours.goto()
      await workHours.waitForLoad()

      const staffSelector = workHours.page.locator('[data-testid="staff-selector"]')
      await expect(staffSelector).not.toBeVisible()
    })
  })
})
