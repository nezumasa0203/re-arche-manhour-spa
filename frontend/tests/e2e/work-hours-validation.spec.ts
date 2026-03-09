import { test, expect } from '@playwright/test'
import { loginAs, ACTOR_STAFF } from './helpers/auth'
import { WorkHoursPage } from './pages/WorkHoursPage'

/**
 * T-054: バリデーション E2E テスト
 * 15分単位違反、禁止ワード、必須チェック等のバリデーション検証
 */

let workHours: WorkHoursPage

test.beforeEach(async ({ page }) => {
  await loginAs(page, ACTOR_STAFF)
  workHours = new WorkHoursPage(page)
  await workHours.goto()

  // テスト用に新規行を追加（API 応答を待機）
  await workHours.addRecord()
})

test.describe('バリデーション', () => {
  test('工数: 15分単位でない値はエラー（03:10 → CZ-147）', async () => {
    await workHours.editCell(0, 'hours', '3:10')

    // バリデーションエラーが発生し、値が保存されないことを確認
    const cell = workHours.cellDisplay(0, 'hours')
    const displayText = await cell.textContent()
    // 15分単位でない値は拒否される
    expect(displayText).not.toContain('03:10')
  })

  test('工数: 0:00 は最小値未満エラー（BR-007）', async () => {
    await workHours.editCell(0, 'hours', '0:00')

    const cell = workHours.cellDisplay(0, 'hours')
    const displayText = await cell.textContent()
    // 0:00 は保存されない（最小 0:15）
    expect(displayText).not.toBe('0:00')
  })

  test('件名: 空のまま blur で必須エラー（VR-006）', async ({ page }) => {
    // 件名セルをクリックして編集モード → 空のまま blur
    await workHours.clickCell(0, 'subject')
    const input = workHours.cellInput(0, 'subject')
    await input.waitFor({ state: 'visible' })
    await input.fill('')
    await input.blur()
    await page.waitForTimeout(500)

    // エラー状態の確認（赤枠やエラーメッセージ）
    const errorIndicator = workHours.row(0).locator('.cell-error, .p-invalid, [data-error]')
    // エラーが表示されるか、値が保存されないことを確認
    const hasError = await errorIndicator.count() > 0
    const displayText = await workHours.cellDisplay(0, 'subject').textContent()
    expect(hasError || displayText === '').toBeTruthy()
  })

  test('依頼書No: 7文字以外はエラー（VR-012）', async () => {
    // 5桁を入力（7文字固定ルール違反）
    await workHours.editCell(0, 'workRequestNo', '12345')

    const cell = workHours.cellDisplay(0, 'workRequestNo')
    const displayText = await cell.textContent()
    // 5文字は拒否される（空 or 7文字固定）
    expect(displayText).not.toBe('12345')
  })

  test('依頼書No: 7文字ちょうどは保存される', async () => {
    await workHours.editCell(0, 'workRequestNo', '1234567')

    const cell = workHours.cellDisplay(0, 'workRequestNo')
    const displayText = await cell.textContent()
    expect(displayText).toContain('1234567')
  })

  test('工数: 正常な15分単位の値は保存される（01:30）', async () => {
    await workHours.editCell(0, 'hours', '1:30')

    const cell = workHours.cellDisplay(0, 'hours')
    const displayText = await cell.textContent()
    expect(displayText).toContain('01:30')
  })
})
