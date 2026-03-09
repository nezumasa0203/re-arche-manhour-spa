# Feature 04: E2E テスト計画

## 1. 前提条件

### 1.1 実行環境

```
docker compose up  →  全サービス起動
  frontend  : http://localhost:3000  (Nuxt.js 3)
  backend   : http://localhost:8080  (Spring Boot)
  auth-mock : http://localhost:8180  (Express JWT)
  db        : localhost:5432         (PostgreSQL 16)
  redis     : localhost:6379         (Redis 7)
```

### 1.2 Playwright 設定

```
baseURL    : http://localhost:3000
testDir    : ./tests/e2e
browsers   : Chromium (headless)
retries    : 0 (local) / 2 (CI)
trace      : on-first-retry
```

### 1.3 テストデータ（DB シード: `db/init/02-seed.sql`）

| テーブル | レコード | 内容 |
|---------|---------|------|
| mcz02_hosyu_kategori | 5件 | 障害対応/機能追加/改善/運用保守/その他 |
| mav01_sys | 4件 | 基幹/人事給与/財務会計/WorkSys |
| mav03_subsys | 4件 | 受注管理/在庫管理/勤怠管理/仕訳入力 |
| tcz01_hosyu_kousuu | 3件 | E00001(STATUS_0,1), E00002(STATUS_2) |
| mcz21_kanri_taisyo | 2件 | E00001→E00002, E00003→E00002（代行関係）|
| mcz04_ctrl | 6件 | 月次コントロール（2026-02, 2025-01, 2024-12）|

---

## 2. 共通設計

### 2.1 認証ヘルパー（`tests/e2e/helpers/auth.ts`）

auth-mock の `POST /api/switch` を使い、テスト用トークンを取得してブラウザの cookie にセット。

```typescript
async function loginAs(page: Page, actorId: string): Promise<void> {
  // 1. auth-mock からトークン取得
  const response = await fetch('http://localhost:8180/api/switch', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ actorId }),
  })
  const { token } = await response.json()

  // 2. ブラウザの cookie にトークンをセット
  await page.context().addCookies([{
    name: 'cz-auth-token',
    value: token,
    domain: 'localhost',
    path: '/',
  }])
}
```

**使用アクター:**

| アクター | 用途 | 権限概要 |
|---------|------|---------|
| ACT-01 | 担当者テスト | 報告担当者, canDelegate=false, STATUS_0↔1 のみ |
| ACT-03 | 管理者テスト | 全権管理者, STATUS_0/1/2 すべて操作可 |
| ACT-09 | 代行テスト | 外部契約者, canDelegate=true |

### 2.2 Page Object（`tests/e2e/pages/WorkHoursPage.ts`）

```typescript
class WorkHoursPage {
  constructor(private page: Page) {}

  // ─── ナビゲーション ───
  async goto() { await this.page.goto('/work-hours') }

  // ─── SearchPanel ───
  get monthSelector()  { return this.page.locator('[data-testid="month-selector"]') }
  get prevMonthBtn()   { return this.page.locator('[data-testid="prev-month"]') }
  get nextMonthBtn()   { return this.page.locator('[data-testid="next-month"]') }
  get searchBtn()      { return this.page.locator('[data-testid="search-btn"]') }
  get resetBtn()       { return this.page.locator('[data-testid="reset-btn"]') }
  get daikoBadge()     { return this.page.locator('[data-testid="daiko-badge"]') }
  get daikoCancelBtn() { return this.page.locator('[data-testid="daiko-cancel"]') }

  // ─── Toolbar ───
  get addBtn()      { return this.page.locator('[data-testid="add-btn"]') }
  get copyBtn()     { return this.page.locator('[data-testid="copy-btn"]') }
  get deleteBtn()   { return this.page.locator('[data-testid="delete-btn"]') }
  get transferBtn() { return this.page.locator('[data-testid="transfer-btn"]') }
  get excelBtn()    { return this.page.locator('[data-testid="excel-btn"]') }
  get pjSummaryBtn(){ return this.page.locator('[data-testid="pj-summary-btn"]') }

  // ─── DataTable ───
  get dataTable()   { return this.page.locator('.work-hours-datatable') }
  get rows()        { return this.page.locator('.p-datatable-tbody tr') }
  row(index: number){ return this.rows.nth(index) }
  get emptyMessage(){ return this.page.locator('.empty-message') }

  // ─── StatusFooter ───
  get batchConfirmBtn() { return this.page.locator('[data-testid="batch-confirm-btn"]') }
  get batchRevertBtn()  { return this.page.locator('[data-testid="batch-revert-btn"]') }
  get countStatus0()    { return this.page.locator('[data-testid="count-status-0"]') }
  get countStatus1()    { return this.page.locator('[data-testid="count-status-1"]') }
  get countStatus2()    { return this.page.locator('[data-testid="count-status-2"]') }

  // ─── セル操作ヘルパー ───
  async clickCell(rowIndex: number, field: string) {
    await this.row(rowIndex).locator(`[data-testid="${field}-display"]`).click()
  }
  async editCell(rowIndex: number, field: string, value: string) {
    await this.clickCell(rowIndex, field)
    const input = this.row(rowIndex).locator(`[data-testid="${field}-input"]`)
    await input.fill(value)
    await input.blur()
  }

  // ─── 確認ダイアログ ───
  async confirmDialog()  { await this.page.locator('.p-confirmdialog .p-confirm-dialog-accept').click() }
  async cancelDialog()   { await this.page.locator('.p-confirmdialog .p-confirm-dialog-reject').click() }

  // ─── 待機ヘルパー ───
  async waitForLoad()    { await this.page.waitForLoadState('networkidle') }
  async waitForRecords() { await this.rows.first().waitFor({ state: 'visible', timeout: 10000 }) }
}
```

### 2.3 データリセット戦略

E2E テスト間のデータ独立性を保つため、以下のいずれかを採用：

**案A: テスト前に DB リセット（推奨）**
```typescript
test.beforeEach(async () => {
  // backend の /api/test/reset エンドポイント（dev 限定）
  // or docker compose exec db psql で seed 再実行
})
```

**案B: テストごとに作成→削除**
```typescript
// テスト内で createRecord → テスト → deleteRecords
```

**→ 初期実装は案B を採用**（バックエンド側に test/reset エンドポイントがないため）

---

## 3. テストパターン詳細

### T-053: 基本フロー E2E（`work-hours-basic.spec.ts`）

**目的:** 新規追加→編集→一括確認のメインフローを検証

| # | Given | When | Then | セレクター |
|---|-------|------|------|-----------|
| 1 | ACT-01 でログイン, 工数入力画面表示 | 画面遷移 | DataTable が表示される | `.work-hours-datatable` |
| 2 | 画面表示中 | [追加] ボタンクリック | 先頭に空行追加（STATUS_0） | `[data-testid="add-btn"]` → `.p-datatable-tbody tr` |
| 3 | 空行追加済み | 作業日セルをクリック→日付選択 | 作業日が設定される | `[data-testid="workDate-display"]` |
| 4 | 作業日設定済み | カテゴリを選択 | カテゴリが設定される | `[data-testid="category-display"]` |
| 5 | カテゴリ設定済み | 件名を入力（"E2Eテスト"） | 件名が保存される | `[data-testid="subject-display"]` |
| 6 | 件名入力済み | 工数を入力（"130"→"01:30"） | 自動変換+保存 | `[data-testid="hours-display"]` |
| 7 | 全必須項目入力済み | [一括確認] → 確認ダイアログ「はい」 | STATUS_0→1 に遷移 | `[data-testid="batch-confirm-btn"]` |
| 8 | ステータス確認後 | ステータスフッター確認 | STATUS_1 件数が増加 | `[data-testid="count-status-1"]` |

---

### T-054: バリデーション E2E（`work-hours-validation.spec.ts`）

**目的:** フロントエンドバリデーション＋バックエンドバリデーションの検証

| # | Given | When | Then | エラーコード |
|---|-------|------|------|------------|
| 1 | 空行追加済み | 工数に "03:10" を入力 | 15分単位エラー表示 | CZ-147 |
| 2 | 空行追加済み | 工数に "0" を入力 | 最小値エラー（0:15未満） | BR-007 |
| 3 | 空行追加済み | 件名に "カ層" を含む文字列入力 | 禁止語句エラー | CZ-141 |
| 4 | 空行追加済み | 件名を空のまま blur | 必須エラー | VR-006 |
| 5 | 空行追加済み | 依頼書No に "12345"（5桁）を入力 | 7文字固定エラー | VR-012 |
| 6 | エラー表示中 | エラーセルを確認 | 赤枠 + ツールチップ | `.cell-error` |

---

### T-055: 権限テスト E2E（`work-hours-permissions.spec.ts`）

**目的:** ACT-01（担当者）vs ACT-03（管理者）の権限差異を検証

| # | アクター | Given | When | Then |
|---|---------|-------|------|------|
| 1 | ACT-01 | STATUS_0 レコードあり | 画面表示 | [追加][コピー][削除] ボタン表示 |
| 2 | ACT-01 | STATUS_1 レコードあり | セルクリック | 編集不可（isEditable=false） |
| 3 | ACT-01 | STATUS_2 レコードあり | 画面確認 | 編集不可、差戻不可 |
| 4 | ACT-03 | STATUS_1 レコードあり | セルクリック | 編集可能（管理者権限） |
| 5 | ACT-03 | STATUS_1 レコードあり | 画面確認 | [差戻] ボタン表示 |
| 6 | ACT-03 | 画面表示中 | ステータスを 0→1→2 遷移 | 全遷移可能 |

---

### T-056: 代行モード E2E（`work-hours-delegation.spec.ts`）

**目的:** 代行モードの開始→操作→解除フローを検証

| # | アクター | Given | When | Then |
|---|---------|-------|------|------|
| 1 | ACT-09 | canDelegate=true でログイン | 画面表示 | 担当者選択UI が利用可能 |
| 2 | ACT-09 | 通常モード | 担当者「E00001（山田太郎）」を選択 | 「代行中」バッジ表示、山田太郎のデータ表示 |
| 3 | ACT-09 | 代行中 | [追加] → レコード追加 | レコードが E00001 名義で追加される |
| 4 | ACT-09 | 代行中 | [解除] ボタンクリック | 代行バッジ消去、自分のデータ表示 |
| 5 | ACT-01 | canDelegate=false | 画面表示 | 担当者選択UI が非表示 |

---

### T-057: コピー・転写・月切替 E2E（`work-hours-operations.spec.ts`）

**目的:** 一覧操作系の機能を検証

| # | Given | When | Then |
|---|-------|------|------|
| 1 | STATUS_0 レコード1件チェック | [コピー] ボタンクリック | 先頭にコピーレコード（STATUS_0）追加 |
| 2 | STATUS_0 レコード1件チェック | [翌月転写] → TransferDialog → 月選択 → 実行 | 転写成功メッセージ |
| 3 | 2025年02月表示中 | << ボタンクリック | 2025年01月データに切替 |
| 4 | 2025年01月表示中 | >> ボタンクリック | 2025年02月データに戻る |
| 5 | DataTable 表示中 | 「作業日」ヘッダークリック | ソート適用（asc→desc） |
| 6 | レコード2件チェック | [削除] → 確認「はい」 | 選択レコードが消去 |

---

## 4. ファイル構成

```
frontend/tests/e2e/
├── helpers/
│   └── auth.ts              # loginAs(), アクター切替
├── pages/
│   └── WorkHoursPage.ts     # Page Object
├── work-hours-basic.spec.ts      # T-053: 基本フロー
├── work-hours-validation.spec.ts # T-054: バリデーション
├── work-hours-permissions.spec.ts# T-055: 権限テスト
├── work-hours-delegation.spec.ts # T-056: 代行モード
└── work-hours-operations.spec.ts # T-057: コピー・転写・月切替
```

---

## 5. 実行手順

```bash
# 1. 全サービス起動
docker compose up -d
docker compose exec db pg_isready -U postgres -d cz_migration_dev

# 2. Playwright ブラウザインストール（初回のみ）
cd frontend && npx playwright install chromium

# 3. E2E テスト実行
cd frontend
npx playwright test tests/e2e/                          # 全テスト
npx playwright test tests/e2e/work-hours-basic.spec.ts  # 特定ファイル
npx playwright test tests/e2e/ -g "工数入力画面"          # 特定テスト名

# 4. テスト結果をブラウザで確認（HTMLレポート）
npx playwright show-report --host 0.0.0.0 --port 9323
```

### 5.1 HTMLレポートの見方（Codespaces 環境）

テスト実行後、以下の手順でブラウザからテスト結果を確認できます。

1. `npx playwright show-report --host 0.0.0.0 --port 9323` を実行
2. VS Code の **ポートタブ**（左下パネル → 「ポート」）を開く
3. ポート **9323** の行にある **地球アイコン（ブラウザで開く）** をクリック

**HTMLレポートに含まれるもの:**

| 項目 | 内容 |
|------|------|
| テスト一覧 | Pass/Fail の結果一覧 |
| スクリーンショット | 各テストのステップごとの画面キャプチャ |
| 動画 | テスト全体の録画（webm 形式） |
| Trace | タイムライン形式で DOM スナップショット・ネットワーク通信・コンソールログを確認可能 |

**Playwright 設定（`playwright.config.ts`）:**

```
trace      : 'on'    ← 全テストで Trace を記録
screenshot : 'on'    ← 全テストでスクリーンショットを記録
video      : 'on'    ← 全テストで動画を記録
```

> CI 環境ではディスク・メモリ節約のため `'on-first-retry'` / `'only-on-failure'` に戻すことを推奨。

---

## 6. リスクと制約

| リスク | 影響 | 対策 |
|-------|------|------|
| バックエンド API 未完成 | テスト Red | API モックで代替、バックエンド完成後に Green |
| テストデータ汚染 | テスト間干渉 | テストごとに create→delete パターン |
| Codespaces のブラウザ制約 | Playwright 実行不可 | `--headless` + Xvfb |
| サービス起動時間 | CI 遅延 | `webServer` でヘルスチェック待機 |

---

## 7. Phase 12 実装順序

1. `helpers/auth.ts` — 認証ヘルパー
2. `pages/WorkHoursPage.ts` — Page Object
3. `work-hours-basic.spec.ts` (T-053) — メインフロー
4. `work-hours-validation.spec.ts` (T-054) — バリデーション
5. `work-hours-permissions.spec.ts` (T-055) — 権限
6. `work-hours-delegation.spec.ts` (T-056) — 代行
7. `work-hours-operations.spec.ts` (T-057) — 操作系
