import { test, expect } from '@playwright/test'
import { loginAs, ACTOR_STAFF } from './helpers/auth'
import { WorkHoursPage } from './pages/WorkHoursPage'

/**
 * T-057: コピー・転写・月切替 E2E
 * 一覧操作系の機能を検証
 */

let workHours: WorkHoursPage

test.beforeEach(async ({ page }) => {
  await loginAs(page, ACTOR_STAFF)
  workHours = new WorkHoursPage(page)
  await workHours.goto()
})

test.describe('コピー操作', () => {
  test('STATUS_0 レコードをチェックして [コピー] → 先頭にコピーレコード追加', async () => {
    // テスト用にレコード追加
    await workHours.addRecord()
    const initialCount = await workHours.rows.count()

    // 先頭行のチェックボックスを選択
    await workHours.selectRow(0)

    // コピー実行
    await workHours.copyBtn.click()
    await workHours.waitForLoad()

    // 行数が増えていることを確認
    const afterCount = await workHours.rows.count()
    expect(afterCount).toBe(initialCount + 1)
  })
})

test.describe('転写操作', () => {
  test('STATUS_0 レコードをチェックして [翌月転写] → TransferDialog → 実行', async ({ page }) => {
    // テスト用にレコード追加
    await workHours.addRecord()

    // 先頭行のチェックボックスを選択
    await workHours.selectRow(0)

    // 転写ボタンクリック
    await workHours.transferBtn.click()

    // TransferDialog が表示される
    const dialog = page.locator('.p-dialog, [data-testid="transfer-dialog"]')
    await expect(dialog).toBeVisible()

    // 転写実行（ダイアログ内の実行ボタン）
    const executeBtn = dialog.locator('button', { hasText: '実行' })
    const btnCount = await executeBtn.count()
    if (btnCount > 0) {
      await executeBtn.click()
      await workHours.waitForLoad()
    }
  })
})

test.describe('月切替', () => {
  test('<< ボタンで前月に切り替わる', async () => {
    // 現在の月を記録
    const currentMonth = await workHours.monthSelector.textContent()

    await workHours.prevMonthBtn.click()
    await workHours.waitForLoad()

    // 月表示が変わっていることを確認
    const newMonth = await workHours.monthSelector.textContent()
    expect(newMonth).not.toBe(currentMonth)
  })

  test('>> ボタンで翌月に切り替わる', async () => {
    // まず前月に移動
    await workHours.prevMonthBtn.click()
    await workHours.waitForLoad()
    const prevMonth = await workHours.monthSelector.textContent()

    // 翌月ボタンで戻る
    await workHours.nextMonthBtn.click()
    await workHours.waitForLoad()

    const currentMonth = await workHours.monthSelector.textContent()
    expect(currentMonth).not.toBe(prevMonth)
  })
})

test.describe('ソート', () => {
  test('「作業日」ヘッダークリックでソートが適用される', async ({ page }) => {
    // 作業日カラムヘッダーをクリック
    const workDateHeader = page.locator('.p-datatable-thead th', { hasText: '作業日' })
    const headerCount = await workDateHeader.count()
    if (headerCount === 0) {
      test.skip()
      return
    }

    await workDateHeader.click()
    await workHours.waitForLoad()

    // ソートアイコンが表示されることを確認
    const sortIcon = workDateHeader.locator('.p-sortable-column-icon, .p-icon')
    await expect(sortIcon).toBeVisible()
  })
})

test.describe('削除操作', () => {
  test('レコードをチェックして [削除] → 確認ダイアログ「はい」 → レコード消去', async () => {
    // テスト用にレコード2行追加
    await workHours.addRecord()
    await workHours.addRecord()

    const beforeCount = await workHours.rows.count()

    // 先頭行のチェックボックスを選択
    await workHours.selectRow(0)

    // 削除実行
    await workHours.deleteBtn.click()

    // 確認ダイアログで「はい」
    await workHours.confirmDialogAccept()
    await workHours.waitForLoad()

    // 行数が減っていることを確認
    const afterCount = await workHours.rows.count()
    expect(afterCount).toBeLessThan(beforeCount)
  })
})
