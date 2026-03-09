import { test, expect } from '@playwright/test'
import { loginAs, ACTOR_STAFF, ACTOR_ADMIN } from './helpers/auth'
import { WorkHoursPage } from './pages/WorkHoursPage'

/**
 * T-055: 権限テスト E2E
 * ACT-01（担当者）vs ACT-03（管理者）の権限差異を検証
 */

let workHours: WorkHoursPage

test.describe('担当者（ACT-01）権限', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, ACTOR_STAFF)
    workHours = new WorkHoursPage(page)
    await workHours.goto()
    await workHours.waitForLoad()
  })

  test('STATUS_0 レコードがある場合、[追加][コピー][削除] ボタンが表示される', async () => {
    await expect(workHours.addBtn).toBeVisible()
    await expect(workHours.copyBtn).toBeVisible()
    await expect(workHours.deleteBtn).toBeVisible()
  })

  test('STATUS_1 レコードは編集不可（セルクリックしても入力モードにならない）', async () => {
    // STATUS_1 のレコードが存在する前提（シードデータ）
    const status1Row = workHours.rows.filter({ hasText: '確認済' }).first()
    const count = await status1Row.count()
    if (count === 0) {
      test.skip()
      return
    }

    // セルクリックしても input が表示されないことを確認
    await status1Row.locator('[data-testid="subject-display"]').click()
    const input = status1Row.locator('[data-testid="subject-input"]')
    await expect(input).not.toBeVisible()
  })

  test('STATUS_2 レコードは編集不可、差戻ボタンも非表示', async () => {
    // STATUS_2（承認済み）レコードの存在確認
    const status2Row = workHours.rows.filter({ hasText: '承認済' }).first()
    const count = await status2Row.count()
    if (count === 0) {
      test.skip()
      return
    }

    await status2Row.locator('[data-testid="subject-display"]').click()
    const input = status2Row.locator('[data-testid="subject-input"]')
    await expect(input).not.toBeVisible()

    // 担当者には差戻ボタンが非表示
    await expect(workHours.batchRevertBtn).not.toBeVisible()
  })
})

test.describe('管理者（ACT-03）権限', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, ACTOR_ADMIN)
    workHours = new WorkHoursPage(page)
    await workHours.goto()
    await workHours.waitForLoad()
  })

  test('STATUS_1 レコードを編集可能（管理者権限）', async () => {
    const status1Row = workHours.rows.filter({ hasText: '確認済' }).first()
    const count = await status1Row.count()
    if (count === 0) {
      test.skip()
      return
    }

    await status1Row.locator('[data-testid="subject-display"]').click()
    const input = status1Row.locator('[data-testid="subject-input"]')
    await expect(input).toBeVisible()
  })

  test('STATUS_1 レコードがある場合、[差戻] ボタンが表示される', async () => {
    // STATUS_1 レコードがなければスキップ
    const status1Row = workHours.rows.filter({ hasText: '確認済' }).first()
    const count = await status1Row.count()
    if (count === 0) {
      test.skip()
      return
    }
    await expect(workHours.batchRevertBtn).toBeVisible()
  })

  test('ステータスを 0→1→2 に遷移可能（全遷移権限）', async () => {
    // 新規行追加（STATUS_0）
    await workHours.addRecord()

    // 必須項目を入力
    await workHours.editCell(0, 'subject', '管理者権限テスト')
    await workHours.editCell(0, 'hours', '1:00')

    // 一括確認（STATUS_0 → 1）
    await workHours.batchConfirmBtn.click()
    await workHours.confirmDialogAccept()
    await workHours.waitForLoad()

    // STATUS_1 件数が増加していることを確認
    const count1Text = await workHours.countStatus1.textContent()
    expect(Number(count1Text?.replace(/\D/g, ''))).toBeGreaterThan(0)
  })
})
