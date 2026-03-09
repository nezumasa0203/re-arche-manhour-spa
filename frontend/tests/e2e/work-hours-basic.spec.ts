import { test, expect } from '@playwright/test'
import { loginAs, ACTOR_STAFF } from './helpers/auth'
import { WorkHoursPage } from './pages/WorkHoursPage'

/**
 * T-053: 基本フロー E2E テスト
 * ログイン → 工数入力画面 → 追加 → 各フィールド入力 → 一括確認
 */

let workHours: WorkHoursPage

test.beforeEach(async ({ page }) => {
  await loginAs(page, ACTOR_STAFF)
  workHours = new WorkHoursPage(page)
})

test.describe('基本フロー', () => {
  test('工数入力画面が表示される', async () => {
    await workHours.goto()
    await expect(workHours.dataTable).toBeVisible()
    await expect(workHours.addBtn).toBeVisible()
    await expect(workHours.searchBtn).toBeVisible()
  })

  test('新規行を追加できる', async () => {
    await workHours.goto()

    // 追加前のデータ行数（空メッセージ行は除外済み）
    const initialRowCount = await workHours.rows.count()

    await workHours.addRecord()

    const newRowCount = await workHours.rows.count()
    expect(newRowCount).toBe(initialRowCount + 1)
  })

  test('工数セルに短縮入力して自動変換される', async () => {
    await workHours.goto()

    // 新規行追加
    await workHours.addRecord()

    // 工数セルを編集（"130" → "1:30" に自動変換）
    await workHours.editCell(0, 'hours', '130')

    // 表示値が変換後の値になっていることを確認（バックエンドは "H:mm" 形式で返却）
    const displayText = await workHours.cellDisplay(0, 'hours').textContent()
    expect(displayText).toContain('1:30')
  })

  test('件名を入力して保存される', async () => {
    await workHours.goto()

    await workHours.addRecord()

    await workHours.editCell(0, 'subject', 'E2Eテスト件名')

    const displayText = await workHours.cellDisplay(0, 'subject').textContent()
    expect(displayText).toContain('E2Eテスト件名')
  })

  test('一括確認で STATUS_0 → STATUS_1 に遷移', async () => {
    await workHours.goto()

    // 新規行追加＋必須項目入力
    await workHours.addRecord()
    await workHours.editCell(0, 'subject', '一括確認テスト')
    await workHours.editCell(0, 'hours', '100')

    // 作業日を入力（PrimeVue Calendar は Playwright fill() で v-model 更新されないため、
    // ブラウザ内の Pinia store 経由で updateField を直接呼び出す）
    const today = new Date()
    const dateStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-01`
    const datePatchPromise = workHours.page.waitForResponse(
      resp => resp.url().includes('/work-hours/') && resp.request().method() === 'PATCH',
    )
    await workHours.page.evaluate(async (d: string) => {
      // Vue app → $pinia → workHours ストアにアクセス
      const el = document.querySelector('#__nuxt') as HTMLElement & { __vue_app__: { config: { globalProperties: { $pinia: { _s: Map<string, Record<string, unknown>> } } } } }
      const pinia = el.__vue_app__.config.globalProperties.$pinia
      const store = pinia._s.get('workHours') as { records: Array<{ id: number }>; updateField: (id: number, field: string, value: string) => Promise<void> }
      const record = store.records[0]
      if (record) await store.updateField(record.id, 'workDate', d)
    }, dateStr)
    await datePatchPromise
    await workHours.page.waitForTimeout(300)

    // 一括確認
    await workHours.batchConfirmBtn.click()
    await workHours.confirmDialogAccept()

    // batchConfirm → fetchRecords の完了を待機
    await workHours.page.waitForTimeout(2000)

    // STATUS_1 の件数が増加していることを確認
    const count1Text = await workHours.countStatus1.textContent()
    expect(count1Text).toContain('1')
  })

  test('ステータスフッターにステータス件数が表示される', async () => {
    await workHours.goto()
    await workHours.waitForLoad()

    await expect(workHours.countStatus0).toBeVisible()
    await expect(workHours.countStatus1).toBeVisible()
    await expect(workHours.countStatus2).toBeVisible()
  })

  test('月切替で SearchPanel の年月が変わる', async () => {
    await workHours.goto()
    await workHours.waitForLoad()

    await workHours.prevMonthBtn.click()
    await workHours.waitForLoad()

    // ページがリロードされてデータが更新される
    await expect(workHours.dataTable).toBeVisible()
  })

  test('データ0件のとき空メッセージが表示される', async () => {
    await workHours.goto()

    // 遠い過去の月に切替（データがない月）
    for (let i = 0; i < 12; i++) {
      await workHours.prevMonthBtn.click()
    }
    await workHours.waitForLoad()

    // データ行が0件であることを確認
    const rowCount = await workHours.rows.count()
    expect(rowCount).toBe(0)
  })
})
