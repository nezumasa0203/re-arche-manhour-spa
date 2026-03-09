import type { Locator, Page } from '@playwright/test'

/**
 * 工数入力画面 (FORM_010) の Page Object
 */
export class WorkHoursPage {
  constructor(readonly page: Page) {}

  // ─── ナビゲーション ───────────────────────────────────────────

  async goto() {
    await this.page.goto('/work-hours', { waitUntil: 'domcontentloaded' })
    // DataTable のヘッダーが描画されるまで待機
    await this.page.locator('.p-datatable-thead').waitFor({ state: 'visible', timeout: 15000 })
  }

  // ─── SearchPanel ──────────────────────────────────────────────

  get monthSelector(): Locator {
    return this.page.locator('[data-testid="month-selector"]')
  }

  get prevMonthBtn(): Locator {
    return this.page.locator('[data-testid="prev-month"]')
  }

  get nextMonthBtn(): Locator {
    return this.page.locator('[data-testid="next-month"]')
  }

  get searchBtn(): Locator {
    return this.page.locator('[data-testid="search-btn"]')
  }

  get resetBtn(): Locator {
    return this.page.locator('[data-testid="reset-btn"]')
  }

  get daikoBadge(): Locator {
    return this.page.locator('[data-testid="daiko-badge"]')
  }

  get daikoCancelBtn(): Locator {
    return this.page.locator('[data-testid="daiko-cancel"]')
  }

  get staffDisplay(): Locator {
    return this.page.locator('[data-testid="staff-display"]')
  }

  // ─── Toolbar ──────────────────────────────────────────────────

  get addBtn(): Locator {
    return this.page.locator('[data-testid="add-btn"]')
  }

  get copyBtn(): Locator {
    return this.page.locator('[data-testid="copy-btn"]')
  }

  get deleteBtn(): Locator {
    return this.page.locator('[data-testid="delete-btn"]')
  }

  get transferBtn(): Locator {
    return this.page.locator('[data-testid="transfer-btn"]')
  }

  get excelBtn(): Locator {
    return this.page.locator('[data-testid="excel-btn"]')
  }

  get pjSummaryBtn(): Locator {
    return this.page.locator('[data-testid="pj-summary-btn"]')
  }

  // ─── DataTable ────────────────────────────────────────────────

  get dataTable(): Locator {
    return this.page.locator('.work-hours-datatable')
  }

  /** データ行のみ（PrimeVue 空メッセージ行を除外） */
  get rows(): Locator {
    return this.page.locator('.p-datatable-tbody tr:not(.p-datatable-empty-message)')
  }

  row(index: number): Locator {
    return this.rows.nth(index)
  }

  /** 行追加して表示されるまで待機 */
  async addRecord(): Promise<void> {
    const responsePromise = this.page.waitForResponse(resp =>
      resp.url().includes('/work-hours') && resp.request().method() === 'POST' && resp.status() === 201,
    )
    await this.page.locator('[data-testid="add-btn"]').click()
    await responsePromise
    // 行が描画されるまで待機
    await this.rows.first().waitFor({ state: 'visible', timeout: 5000 })
  }

  get emptyMessage(): Locator {
    return this.page.locator('.empty-message')
  }

  get loadingOverlay(): Locator {
    return this.page.locator('.p-datatable-loading-overlay')
  }

  // ─── StatusFooter ─────────────────────────────────────────────

  get batchConfirmBtn(): Locator {
    return this.page.locator('[data-testid="batch-confirm-btn"]')
  }

  get batchRevertBtn(): Locator {
    return this.page.locator('[data-testid="batch-revert-btn"]')
  }

  get countStatus0(): Locator {
    return this.page.locator('[data-testid="count-status-0"]')
  }

  get countStatus1(): Locator {
    return this.page.locator('[data-testid="count-status-1"]')
  }

  get countStatus2(): Locator {
    return this.page.locator('[data-testid="count-status-2"]')
  }

  get footerMessage(): Locator {
    return this.page.locator('[data-testid="footer-message"]')
  }

  // ─── セル操作 ─────────────────────────────────────────────────

  /** 指定行のセルの表示要素を取得 */
  cellDisplay(rowIndex: number, field: string): Locator {
    return this.row(rowIndex).locator(`[data-testid="${field}-display"]`)
  }

  /** 指定行のセルの入力要素を取得 */
  cellInput(rowIndex: number, field: string): Locator {
    return this.row(rowIndex).locator(`[data-testid="${field}-input"]`)
  }

  /** セルをクリックして編集モードに入る（空セルでも高さ0で not visible になるため force 使用） */
  async clickCell(rowIndex: number, field: string): Promise<void> {
    const cell = this.cellDisplay(rowIndex, field)
    await cell.scrollIntoViewIfNeeded()
    await cell.click({ force: true })
  }

  /** セルを編集する（クリック→入力→blur→PATCH完了待機） */
  async editCell(rowIndex: number, field: string, value: string): Promise<void> {
    await this.clickCell(rowIndex, field)
    const input = this.cellInput(rowIndex, field)
    await input.waitFor({ state: 'visible' })
    await input.fill(value)
    // blur でPATCH APIが発火するので、レスポンスを待機
    const responsePromise = this.page.waitForResponse(
      resp => resp.url().includes('/work-hours/') && resp.request().method() === 'PATCH',
    )
    await input.blur()
    await responsePromise
    // Vue の反映待機
    await this.page.waitForTimeout(300)
  }

  /** 行のチェックボックスをクリック */
  async selectRow(rowIndex: number): Promise<void> {
    await this.row(rowIndex).locator('.p-checkbox, input[type="checkbox"]').first().click()
  }

  /** 行のステータスバッジテキストを取得 */
  async getRowStatus(rowIndex: number): Promise<string> {
    return await this.row(rowIndex).locator('[data-testid="status-badge"]').textContent() ?? ''
  }

  // ─── ダイアログ操作 ───────────────────────────────────────────

  /** PrimeVue ConfirmDialog の「はい」ボタンをクリック */
  async confirmDialogAccept(): Promise<void> {
    // PrimeVue 4 ConfirmDialog: クラス名またはテキストでボタンを検索
    const acceptBtn = this.page.locator(
      '.p-confirm-dialog-accept, .p-confirmdialog-accept-button',
    ).or(this.page.getByRole('button', { name: 'Yes' }))
    await acceptBtn.first().waitFor({ state: 'visible', timeout: 5000 })
    await acceptBtn.first().click()
  }

  /** PrimeVue ConfirmDialog の「いいえ」ボタンをクリック */
  async confirmDialogReject(): Promise<void> {
    const rejectBtn = this.page.locator(
      '.p-confirm-dialog-reject, .p-confirmdialog-reject-button',
    ).or(this.page.getByRole('button', { name: 'No' }))
    await rejectBtn.first().waitFor({ state: 'visible', timeout: 5000 })
    await rejectBtn.first().click()
  }

  /** TransferDialog が開いているか */
  get transferDialog(): Locator {
    return this.page.locator('[data-testid="transfer-dialog"]')
  }

  /** ProjectSummaryDialog が開いているか */
  get projectSummaryDialog(): Locator {
    return this.page.locator('[data-testid="project-summary-dialog"]')
  }

  // ─── 待機ヘルパー ─────────────────────────────────────────────

  /** レコードが表示されるまで待機 */
  async waitForRecords(): Promise<void> {
    await this.rows.first().waitFor({ state: 'visible', timeout: 10000 })
  }

  /** ページロード完了まで待機 */
  async waitForLoad(): Promise<void> {
    await this.page.locator('.p-datatable-thead').waitFor({ state: 'visible', timeout: 15000 })
  }

  /** API レスポンスを待機 */
  async waitForApiResponse(urlPattern: string | RegExp): Promise<void> {
    await this.page.waitForResponse(urlPattern)
  }
}
