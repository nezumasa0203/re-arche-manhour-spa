# 工数状況一覧 (FORM_020): SPA 画面仕様

## 概要

管理モードユーザーが配下担当者の工数報告状況を把握し、
月次確認・集約・承認ワークフローを実行する画面。
現行 MPA の左右フレーム分割（head_l/r + body_l/r）+ 手動 JS 同期を、
Nuxt.js 3 SPA の PrimeVue DataTable + CSS `position:sticky` 固定列に移行する。

**対応ユーザーストーリー**: US-020〜US-027（8件）
**対応分析ドキュメント**:
- `analysis/03_user_stories.md` セクション 1.2
- `analysis/04_screen_transition.md` — SCR-020
- `analysis/05_gap_analysis.md` — GAP-F20-01〜08

**画面 URL**: `/work-status`

**主要アクター**: ACT-02（報告管理者）、ACT-04（管理モードユーザー）、ACT-05（人事モードユーザー）

---

## 1. ページレイアウト

```
┌──────────────────────────────────────────────────────────────────┐
│ [AppHeader] 保有資源管理 | ユーザー名 | ヘルプ                      │
├────────┬─────────────────────────────────────────────────────────┤
│ [Side  │ [WorkStatusPage]                                        │
│  Nav]  │ ┌──────────────────────────────────────────────────────┐│
│        │ │ [SearchPanel]                                        ││
│ 工数入力│ │ 年月:[2025/02 ▼] [<<][>>]                            ││
│▶工数管理│ │ 組織:[IT推進部 ▼][検索] 担当者:[____] [検索]           ││
│ 分析   │ │ ☐作成中も表示   [リセット]                            ││
│ 設定   │ ├──────────────────────────────────────────────────────┤│
│        │ │ [MonthlyControlBar]                                  ││
│        │ │ 月次ステータス: ■確認済   [未確認][確認][集約]          ││
│        │ ├──────────────────────────────────────────────────────┤│
│        │ │ [Toolbar]                                            ││
│        │ │ [承認][戻す][Excel]      全 150件 (1/3ページ)          ││
│        │ │ [全選択/解除]                                         ││
│        │ ├──────────────────────────────────────────────────────┤│
│        │ │ [WorkStatusDataTable]                                ││
│        │ │ ☐|STS|所属    |担当者 |日付 |対象SS|原因SS|件名|工数  ││
│        │ │ ☐|確認|開発1課|鈴木  |02/25|SS001|SS002|...|03:30 ││
│        │ │ ☐|確定|開発2課|田中  |02/24|SS003|SS003|...|02:00 ││
│        │ │  (工数セルクリック → インライン編集)                    ││
│        │ ├──────────────────────────────────────────────────────┤│
│        │ │ [Pagination]                                         ││
│        │ │ [<<] [<] 1 2 3 [>] [>>]  表示件数:[50 ▼]             ││
│        │ ├──────────────────────────────────────────────────────┤│
│        │ │ [MessageArea]                                        ││
│        │ │ [操作結果メッセージ]                                   ││
│        │ └──────────────────────────────────────────────────────┘│
└────────┴─────────────────────────────────────────────────────────┘
```

---

## 2. コンポーネント構成

```
pages/work-status.vue (WorkStatusPage)
├── components/work-status/SearchPanel.vue
├── components/work-status/MonthlyControlBar.vue
├── components/work-status/Toolbar.vue
├── components/work-status/WorkStatusDataTable.vue
│   ├── (PrimeVue DataTable + Column)
│   ├── components/work-status/cells/StatusBadge.vue
│   └── components/work-status/cells/HoursEditCell.vue
├── components/work-status/Pagination.vue (PrimeVue Paginator)
├── components/common/OrganizationSearchDialog.vue (組織選択)
└── components/common/StaffSearchDialog.vue (担当者選択)
```

---

## 3. 状態管理 (Pinia Store)

### 3.1 `stores/workStatus.ts`

```typescript
interface WorkStatusState {
  // データ
  records: WorkStatusRecord[]

  // 検索条件
  yearMonth: string             // "2025-02"
  organizationCode: string | null
  organizationName: string | null
  staffId: string | null
  staffName: string | null
  statusFilter: number[]        // デフォルト [1, 2] (確認+確定)
  includeStatus0: boolean       // 作成中も表示するか (デフォルト: false)
  sort: string                  // "staffName:asc,workDate:asc"

  // ページネーション
  page: number                  // 現在ページ (1-based)
  pageSize: number              // 1ページ表示件数 (デフォルト: 50)
  totalCount: number            // 全件数

  // 月次制御
  monthlyControl: MonthlyControl

  // 権限
  permissions: WorkStatusPermissions

  // UI 状態
  selectedIds: number[]         // チェックボックス選択
  loading: boolean
  message: StatusMessage | null
}
```

**UI 状態表示**:
- `loading: true` → DataTable に PrimeVue `loading` プロパティ適用（スピナーオーバーレイ表示）。Toolbar ボタンは disabled
- `records` が空配列 → テーブル中央に「該当するレコードがありません」を表示。承認/戻し/Excel ボタンは disabled
- 初回ページ表示時: 現在月 + ログインユーザーの組織で自動 fetch

```typescript
interface WorkStatusRecord {
  id: number
  status: number                // 0=作成中, 1=確認, 2=確定
  statusLabel: string
  statusColor: string
  workDate: string              // "2025-02-25"
  staffId: string
  staffName: string
  department: string
  targetSubsystem: SubsystemInfo
  causeSubsystem: SubsystemInfo
  category: CategoryInfo
  subject: string
  hours: string                 // "03:30"
  hoursMinutes: number          // 210
  tmrNo: string | null
  workRequestNo: string | null
  workRequesterName: string | null
  registrantId: string
  registrantName: string
  updatedAt: string
}

interface MonthlyControl {
  yearMonth: string
  getsujiKakutei: boolean       // 月次確認フラグ (gjkt_flg)
  dataSyuukei: boolean          // データ集約フラグ (data_sk_flg)
  statusLabel: string           // "未確認" / "確認" / "集約"
  statusColor: string           // "#FBFBB6" / "#BDEAAD" / "#9DBDFE"
}

interface WorkStatusPermissions {
  canApprove: boolean           // レコード承認可否
  canRevert: boolean            // 承認取消可否
  canConfirm: boolean           // 月次確認可否 (tab012.bit0)
  canAggregate: boolean         // 月次集約可否 (tab012.bit1)
  canUnconfirm: boolean         // 月次未確認戻し可否
  canEditHours: boolean         // インライン工数編集可否
  canExport: boolean            // Excel出力可否
}
```

### 3.2 主要 Actions

| Action | API | US |
|--------|-----|-----|
| `fetchRecords()` | `GET /work-status` | US-020 |
| `updateHours(id, value)` | `PATCH /work-status/{id}/hours` | US-026 |
| `approveRecords(ids)` | `POST /work-status/approve` | US-024 |
| `revertRecords(ids)` | `POST /work-status/revert` | US-025 |
| `monthlyConfirm()` | `POST /work-status/monthly-confirm` | US-022 |
| `monthlyAggregate()` | `POST /work-status/monthly-aggregate` | US-023 |
| `monthlyUnconfirm()` | `POST /work-status/monthly-unconfirm` | US-021 |
| `changeMonth(yearMonth)` | `GET /work-status` (月切替) | US-020 |
| `changePage(page)` | `GET /work-status` (ページ切替) | US-027 |
| `exportExcel()` | `GET /work-status/export/excel` | - |

### 3.3 主要 Getters

| Getter | 用途 |
|--------|------|
| `filteredStatusFilter` | `includeStatus0` に応じたステータスフィルタ配列 |
| `canApproveSelected` | 選択レコードに STATUS_1 が含まれるか |
| `canRevertSelected` | 選択レコードに STATUS_2 が含まれるか |
| `monthlyStatusLevel` | 月次制御レベル (0=未確認, 1=確認, 2=集約) |
| `isHoursEditable(record)` | レコードの工数がインライン編集可能か |
| `totalPages` | 全ページ数 (`Math.ceil(totalCount / pageSize)`) |

---

## 4. コンポーネント詳細仕様

### 4.1 SearchPanel.vue — 検索パネル

| 要素 | コンポーネント | 動作 | US |
|------|-------------|------|-----|
| 年月 | PrimeVue Dropdown | ±12ヶ月のオプション。変更で `changeMonth()` | US-020 |
| << / >> | PrimeVue Button | 前月/翌月切替 | US-020 |
| 組織 | PrimeVue InputText (readonly) + OrganizationSearchDialog | クリックで組織選択モーダル | US-020 |
| 担当者 | PrimeVue InputText + StaffSearchDialog | クリックで担当者選択モーダル | US-020 |
| 作成中も表示 | PrimeVue Checkbox | チェック時 `statusFilter` に 0 を追加して再検索 | US-020 |
| 検索 | PrimeVue Button | `fetchRecords()` 実行 | - |
| リセット | PrimeVue Button | 検索条件を初期値に戻す | - |

**デフォルト表示**: STATUS_1（確認）と STATUS_2（確定）のみ。
STATUS_0（作成中）は「作成中も表示」チェックで追加可能。

**組織の初期値**: ログインユーザーの `organizationCode` から
`dataAuthority.ref` の範囲で参照可能な組織を自動設定。

### 4.2 MonthlyControlBar.vue — 月次制御バー

月次制御フラグ（MCZ04CTRLMST）の現在状態と遷移操作を提供する。

#### 月次ステータス表示

| 状態 | gjkt_flg | data_sk_flg | ラベル | 色 | 意味 |
|------|:---:|:---:|--------|-----|------|
| 未確認 | 0 | 0 | 未確認 | `#FBFBB6` (黄) | 入力受付中 |
| 確認済 | 1 | 0 | 確認 | `#BDEAAD` (緑) | 入力締切、承認可能 |
| 集約済 | 1 | 1 | 集約 | `#9DBDFE` (青) | 集計対象確定 |

#### ボタン定義

| ボタン | 表示条件 | 有効条件 | 確認ダイアログ | 処理 | US |
|--------|---------|---------|-------------|------|-----|
| 未確認 | `canUnconfirm` | `monthlyStatusLevel >= 1` | CZ-509 | `monthlyUnconfirm()` | US-021 |
| 確認 | `canConfirm` (tab012.bit0) | `monthlyStatusLevel === 0` | CZ-510 | `monthlyConfirm()` | US-022 |
| 集約 | `canAggregate` (tab012.bit1) | `monthlyStatusLevel === 1` | CZ-511 | `monthlyAggregate()` | US-023 |

**ボタンの色とスタイル**:
```
[未確認] — 背景: #FBFBB6 (黄)、アウトライン: canInputPeriod 権限時のみ表示
[確認]   — 背景: #BDEAAD (緑)、アウトライン: canInputPeriod 権限時のみ表示
[集約]   — 背景: #9DBDFE (青)、アウトライン: canAggregate 権限時のみ表示
```

**状態遷移図**:
```
未確認 (00) ──[確認]──→ 確認済 (10) ──[集約]──→ 集約済 (11)
    ↑                       │                      │
    └──────[未確認]──────────┘                      │
    ↑                                              │
    └──────────────[未確認]─────────────────────────┘
```

### 4.3 Toolbar.vue — ツールバー

| ボタン | 表示条件 | 有効条件 | 確認ダイアログ | US |
|--------|---------|---------|-------------|-----|
| 承認 | ステータスマトリクス `statusUpdate === 1` | `selectedIds` に STATUS_1 レコードあり | CZ-508 | US-024 |
| 戻す | ステータスマトリクス `statusUpdate === 1` | `selectedIds` に STATUS_2 レコードあり | CZ-507 | US-025 |

> **混在ステータス選択時の動作**: 承認ボタンクリック時、Frontend は selectedIds から STATUS_1 のみ抽出して送信する。戻しボタンクリック時は STATUS_2 のみ抽出して送信する。対象ステータス以外のレコードは自動的に除外される。
| 全選択 | 常時 | レコード件数 > 0 | - | - |
| 全解除 | 常時 | `selectedIds.length > 0` | - | - |
| Excel | 常時 | レコード件数 > 0 | CZ-516 | - |

**承認/戻しの権限判定**:
```
ステータスキー = monthControl から算出 (sts + ins + sum の3桁)
  sts: 対象レコードの STATUS (0/1/2)
  ins: monthlyControl.getsujiKakutei ? 1 : 0
  sum: monthlyControl.dataSyuukei ? 1 : 0
系列 = 管理者系列 (man) 固定 (FORM_020 は管理モード画面)
StatusMatrixResolver.resolve(ステータスキー, false)
  → statusUpdate: 1/0/9
  → 1: 有効, 0: グレーアウト, 9: 非表示
```

### 4.4 WorkStatusDataTable.vue — メインテーブル

PrimeVue `DataTable` を使用。ヘッダー固定 + 左側固定列 + 水平スクロール。
現行の左右フレーム分割を CSS `position:sticky` に置換 (GAP-F20-02)。

#### 列定義（15列）

| # | 列 | field | 幅 | ソート | 固定 | 編集 |
|---|-----|-------|-----|:---:|:---:|------|
| 1 | CHK | - | 40px | - | O | Checkbox |
| 2 | ステータス | `status` | 80px | O | O | StatusBadge (表示のみ) |
| 3 | 保守担当所属 | `department` | 140px | O | O | 表示のみ |
| 4 | 保守担当者名 | `staffName` | 100px | O | O | 表示のみ |
| 5 | 作業日 | `workDate` | 100px | O | - | 表示のみ |
| 6 | 対象SS No | `targetSubsystem.subsystemNo` | 70px | O | - | 表示のみ |
| 7 | 対象SS名 | `targetSubsystem.subsystemName` | 150px | O | - | 表示のみ |
| 8 | 原因SS No | `causeSubsystem.subsystemNo` | 70px | O | - | 表示のみ |
| 9 | 原因SS名 | `causeSubsystem.subsystemName` | 150px | O | - | 表示のみ |
| 10 | 保守カテゴリ | `category.name` | 160px | O | - | 表示のみ |
| 11 | 件名 | `subject` | 220px | O | - | 表示のみ |
| 12 | 工数 | `hours` | 70px | O | - | HoursEditCell |
| 13 | TMR番号 | `tmrNo` | 70px | O | - | 表示のみ |
| 14 | 依頼書No | `workRequestNo` | 90px | O | - | 表示のみ |
| 15 | 依頼者名 | `workRequesterName` | 100px | O | - | 表示のみ |

**FORM_010 との差異**:
- FORM_010 は担当者自身のレコードを CRUD する画面（全フィールド編集可）
- FORM_020 は管理者が配下担当者の一覧を閲覧・承認する画面（工数のみ編集可）
- FORM_020 では担当者所属・担当者名が表示列として追加される

#### ステータスバッジ色分け

| STATUS | ラベル | 背景色 | 文字色 |
|--------|--------|--------|--------|
| 0 | 作成中 | `#FBFBB6` (黄) | `#000` |
| 1 | 確認 | `#BDEAAD` (緑) | `#000` |
| 2 | 確定 | `#9DBDFE` (青) | `#000` |

#### 行スタイル

- STATUS_0: 黄色バッジ。工数セルは編集不可（`StatusMatrixResolver.resolve(statusKey, false).update === 0` の場合）
- STATUS_1: 緑色バッジ。工数セルは編集可能（`StatusMatrixResolver.resolve(statusKey, false).update === 1`）
- STATUS_2: 青色バッジ。工数セルは編集可能（`StatusMatrixResolver.resolve(statusKey, false).update === 1`）

#### 固定列の実装 (GAP-F20-02)

```css
/* CHK, ステータス, 所属, 担当者名を左固定 */
:deep(.p-datatable-frozen-column) {
  position: sticky;
  z-index: 1;
  background: var(--surface-card);
}
```

PrimeVue DataTable の `frozen` プロパティで列 1〜4 を固定。
残りの列は水平スクロールで表示。

#### ソート

- 列ヘッダークリックで昇順/降順トグル
- デフォルト: 担当者名昇順 → 作業日昇順
- API に `sort` パラメータとして送信

### 4.5 HoursEditCell.vue — 工数インライン編集 (US-026)

FORM_020 で唯一のインライン編集可能セル。

```
表示モード: "03:30" (太字で表示)
  - 編集可能な場合: ホバー時に鉛筆アイコン + カーソル変化
  - 編集不可の場合: 通常カーソル
編集モード: PrimeVue InputText (placeholder="HH:MM", width: 60px)
入力補助:
  - 短縮入力の自動変換 (BR-006):
    - 1桁: "3" → "03:00"、2桁: "12" → "12:00"、3桁: "330" → "03:30"、4桁: "1230" → "12:30"
    - 範囲外（XX>24 or YY>59）: バリデーションエラー (VR-009)
  - 空値 → バリデーションエラー（管理者の工数編集は既存値の変更のみ。空にはできない）
バリデーション:
  - HH:MM 形式 (CZ-125)
  - 15分単位: 00/15/30/45 (CZ-147)
  - 最大 24:00 (CZ-146)
  - 最小 0:15 (CZ-129)
変更時: PATCH /work-status/{id}/hours { value: "03:30", updatedAt: "..." }
```

**編集可否の判定**:
```typescript
function isHoursEditable(record: WorkStatusRecord): boolean {
  // 管理者系列のステータスマトリクスで判定
  const statusKey = buildStatusKey(record.status, monthlyControl)
  const matrix = StatusMatrixResolver.resolve(statusKey, false) // man 系列
  return matrix.update === 1
}

// buildStatusKey: sts(0/1/2) + ins(gjkt_flg) + sum(data_sk_flg)
// 例: STATUS_1, 確認済, 未集約 → "110"
function buildStatusKey(
  status: number,
  ctrl: MonthlyControl
): string {
  const sts = status === 0 ? '0' : status === 1 ? '1' : '2'
  const ins = ctrl.getsujiKakutei ? '1' : '0'
  const sum = ctrl.dataSyuukei ? '1' : '0'
  return `${sts}${ins}${sum}`
}
```

### 4.6 Pagination.vue — ページネーション (US-027)

PrimeVue `Paginator` コンポーネントを使用。

| 要素 | 動作 |
|------|------|
| << (最初) | `changePage(1)` |
| < (前) | `changePage(page - 1)` |
| ページ番号 | `changePage(n)` |
| > (次) | `changePage(page + 1)` |
| >> (最後) | `changePage(totalPages)` |
| 表示件数 | PrimeVue Dropdown: [20, 50, 100, 200] 件。変更で `pageSize` 更新 + 再検索 |

**表示情報**: `全 {totalCount} 件中 {from}-{to} 件表示 ({page}/{totalPages} ページ)`

デフォルトページサイズ: 50 件（現行 200 件/ページから変更、GAP-F20-03）。
ページサイズ変更は `pageSize` を更新し `page=1` にリセットして `fetchRecords()` 再実行。

---

## 5. インタラクション仕様

### 5.1 一覧表示 (US-020)

```
1. ユーザー: サイドナビから「工数管理」を選択
2. Frontend: WorkStatusPage マウント
   → yearMonth = 現在月、organizationCode = ログインユーザーの組織
   → statusFilter = [1, 2] (確認+確定)
   → fetchRecords()
3. Frontend: GET /work-status?yearMonth=2025-02&organizationCode=100210
                               &statusFilter=1,2&page=1&pageSize=50
4. Backend: 組織スコープフィルタ適用 + ステータスフィルタ + ページネーション
5. Frontend: records / monthlyControl / permissions を state にセット
   → DataTable レンダリング（ステータスカラーコード付き）
   → MonthlyControlBar 表示（現在の月次ステータス）
   → Pagination 表示（全件数 / ページ数）
```

### 5.2 月次確認 (US-022)

```
1. ユーザー: MonthlyControlBar の [確認] ボタンクリック
2. Frontend: 確認ダイアログ表示
   "月次データを確認状態にします。入力が制限されます。よろしいですか？" (CZ-510)
3. ユーザー: [OK]
4. Frontend: POST /work-status/monthly-confirm
             { yearMonth: "2025-02", organizationCode: "100210" }
5. Backend:
   - 権限チェック: canInputPeriod() === true
   - MCZ04CTRLMST: SELECT FOR UPDATE (排他制御)
   - gjkt_flg = '1', data_sk_flg = '0' に更新
6. Frontend: monthlyControl 更新 → MonthlyControlBar の色が黄→緑に変化
   → fetchRecords() で一覧リフレッシュ
```

### 5.3 月次集約 (US-023)

```
1. ユーザー: MonthlyControlBar の [集約] ボタンクリック
2. Frontend: 確認ダイアログ表示
   "月次データを集約します。半期推移・月別内訳の集計対象になります。
    よろしいですか？" (CZ-511)
3. ユーザー: [OK]
4. Frontend: POST /work-status/monthly-aggregate
             { yearMonth: "2025-02", organizationCode: "100210" }
5. Backend:
   - 権限チェック: canAggregate() === true
   - MCZ04CTRLMST: SELECT FOR UPDATE
   - gjkt_flg = '1', data_sk_flg = '1' に更新
6. Frontend: monthlyControl 更新 → MonthlyControlBar の色が緑→青に変化
   → fetchRecords() で一覧リフレッシュ
```

### 5.4 月次未確認戻し (US-021)

```
1. ユーザー: MonthlyControlBar の [未確認] ボタンクリック
2. Frontend: 確認ダイアログ表示
   "確認状態を解除し、データ編集を再開可能にします。
    よろしいですか？" (CZ-509)
3. ユーザー: [OK]
4. Frontend: POST /work-status/monthly-unconfirm
             { yearMonth: "2025-02", organizationCode: "100210" }
5. Backend:
   - 権限チェック: canInputPeriod() === true
   - MCZ04CTRLMST: SELECT FOR UPDATE
   - gjkt_flg = '0', data_sk_flg = '0' に更新（確認済・集約済いずれの状態からも未確認に戻す）
6. Frontend: monthlyControl 更新 → MonthlyControlBar の色が緑/青→黄に変化
   → fetchRecords() で一覧リフレッシュ
```

### 5.5 レコード承認 (US-024)

```
1. ユーザー: チェックボックスで STATUS_1 レコードを1件以上選択
2. ユーザー: [承認] ボタンクリック
3. Frontend: 確認ダイアログ表示
   "選択した {n} 件のレコードを承認（確定）します。
    よろしいですか？" (CZ-508)
4. ユーザー: [OK]
5. Frontend: POST /work-status/approve { ids: [12345, 12346] }
6. Backend:
   - 権限チェック: ステータスマトリクス statusUpdate === 1
   - 各レコード: STATUS_1 → STATUS_2 に更新
   - STATUS_1 以外のレコードが含まれる場合: エラー CZ-109
7. Frontend:
   - 成功: 該当行のステータスを「確定」(青) に更新
   - メッセージ: "{n} 件を承認しました" (CZ-801)
   - selectedIds クリア
```

### 5.6 承認取消（戻す）(US-025)

```
1. ユーザー: チェックボックスで STATUS_2 レコードを1件以上選択
2. ユーザー: [戻す] ボタンクリック
3. Frontend: 確認ダイアログ表示
   "選択した {n} 件のレコードを確認状態に戻します。
    よろしいですか？" (CZ-507)
4. ユーザー: [OK]
5. Frontend: POST /work-status/revert { ids: [12345, 12346] }
6. Backend:
   - 権限チェック: ステータスマトリクス statusUpdate === 1
   - 各レコード: STATUS_2 → STATUS_1 に更新
   - STATUS_2 以外のレコードが含まれる場合: エラー CZ-110
7. Frontend:
   - 成功: 該当行のステータスを「確認」(緑) に更新
   - メッセージ: "{n} 件を確認状態に戻しました" (CZ-800)
   - selectedIds クリア
```

### 5.7 インライン工数編集 (US-026)

```
1. ユーザー: 工数セルをクリック
2. Frontend: isHoursEditable(record) 判定
   → false: 何もしない
   → true: 3 へ
3. Frontend: セルを編集モードに遷移 (InputText 表示)
4. ユーザー: 工数値を入力
5. ユーザー: フォーカスアウト or Enter
6. Frontend: フロントバリデーション
   → エラー: セルに赤枠 + ツールチップ、元の値に復元
   → 成功: 7 へ
7. Frontend: PATCH /work-status/{id}/hours
             { value: "04:00", updatedAt: "2025-02-25T10:30:00" }
8. Backend: バリデーション + 楽観的ロック + 保存
   → エラー: エラーレスポンス → セルにエラー表示
   → 成功: { data: { id, oldValue, newValue } }
9. Frontend: セル値更新
```

### 5.8 ページネーション (US-027)

```
1. ユーザー: ページ番号クリック or [<][>][<<][>>] クリック
2. Frontend: changePage(newPage)
   → page = newPage
   → fetchRecords() (検索条件は保持、page のみ変更)
3. Frontend: records 更新 → DataTable リレンダリング
   → selectedIds クリア（ページ跨ぎで選択解除）
```

---

## 6. 権限によるUI制御

### 6.1 アクター別 UI 差異

| UI 要素 | ACT-02 報告管理 | ACT-03 全権管理 | ACT-04 管理モード | ACT-10 全社スタッフ | ACT-13 局スタッフ |
|---------|:---:|:---:|:---:|:---:|:---:|
| 画面アクセス | O | O | O | O | O |
| 組織検索範囲 | 配下のみ | 全組織 | 配下のみ | 全組織 | 局配下 |
| 月次確認ボタン | canInputPeriod | O | canInputPeriod | - | - |
| 月次集約ボタン | - | O | canAggregate | - | - |
| 承認/戻し | O (配下のみ) | O | O (配下のみ) | - | - |
| インライン工数編集 | O (man系列) | O | O (man系列) | - | - |
| Excel出力 | O | O | O | O | O |

### 6.2 管理者系列ステータスマトリクス（FORM_020 固有）

FORM_020 は管理モード画面のため、常に管理者系列（man）を使用。
`statusUpdate` と `statusView` が FORM_020 固有の操作。

| 操作 | 000 | 010 | 011 | 100 | 110 | 111 | 200 | 210 | 211 | 900 | 910 | 911 |
|------|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| 状況更新 | 0 | 0 | 0 | **1** | **1** | **1** | **1** | **1** | **1** | 9 | 9 | 9 |
| 状況参照 | **1** | **1** | **1** | **1** | **1** | **1** | **1** | **1** | **1** | 9 | 9 | 9 |

- `statusUpdate === 1`: 承認・戻し・インライン工数編集が可能
- `statusUpdate === 0`: 閲覧のみ（STATUS_0 かつ未確認時）
- `statusView === 9`: 非表示（STATUS_9xx 系、通常発生しない）

### 6.3 データアクセススコープ

FORM_020 の検索範囲は `dataAuthority.ref` による組織スコープフィルタで制限
（`OrganizationScopeResolver` で解決。spec #2 セクション 2.4 の 7 階層レベルに準拠）:

| dataAuthority.ref | 参照可能範囲 |
|-------------------|------------|
| ZENSYA | 全社全組織 |
| EIGYOSHO | ログインユーザーの営業所配下 |
| HONBU | ログインユーザーの本部配下 |
| KYOKU | ログインユーザーの局配下 |
| SHITSU | ログインユーザーの室配下 |
| BU | ログインユーザーの部配下 |
| KA | ログインユーザーの課のみ |

> FORM_020 は管理者向け画面のため、`dataAuthority.ref` が KA レベルのユーザーは
> 実質的に自課の担当者のみ表示される。参照専用アクター (ACT-05 等) は
> `dataAuthority.upd` = null のため承認・編集操作は不可。

---

## 7. エラーハンドリング

### 7.1 月次制御の排他制御エラー

```
POST /work-status/monthly-confirm → 409 Conflict
原因: 別の管理者が同時に月次操作を実行
Frontend:
  1. PrimeVue Toast で「他の管理者が月次操作を実行中です。
     しばらく待ってから再試行してください」表示
  2. monthlyControl を最新に更新
```

### 7.2 承認対象ステータス不整合

```
POST /work-status/approve → 400 + CZ-109
原因: 選択レコードに STATUS_1 以外が含まれる
Frontend:
  1. Toast で「承認対象でないレコードが含まれています」表示
  2. fetchRecords() で最新状態に更新
  3. selectedIds クリア
```

### 7.3 同時編集競合 (CZ-101)

```
PATCH /work-status/{id}/hours → 409 Conflict
Frontend:
  1. Toast で「別ユーザーにより更新されました」表示
  2. セル値をレスポンスの最新値で上書き
  3. fetchRecords() でリフレッシュ
```

### 7.4 権限不足

```
POST /work-status/monthly-aggregate → 403 + CZ-106
原因: canAggregate() === false のユーザーが集約操作を試行
Frontend:
  1. Toast で「この操作を実行する権限がありません」表示
```

### 7.5 ネットワークエラー

```
Frontend:
  1. Toast で「通信エラーが発生しました。再試行してください」
  2. 編集中のセルを元の値に復元
```

---

## 8. レスポンシブ対応

| ブレークポイント | レイアウト |
|-------------|----------|
| >= 1280px | サイドナビ + フルテーブル（全15列表示、固定列4列） |
| 960-1279px | サイドナビ折りたたみ + テーブル水平スクロール（固定列4列） |
| < 960px | サイドナビ非表示 + テーブル水平スクロール（固定列2列: CHK + ステータス） |

固定列（スクロール時も表示）: CHK, ステータス, 保守担当所属, 保守担当者名

---

## 9. GAP 対応マッピング

| GAP ID | 区分 | 本 spec での対応 |
|--------|------|----------------|
| GAP-F20-01 | KEEP | セクション 4.2 MonthlyControlBar — 月次確認→集約の3段階遷移 |
| GAP-F20-02 | IMPROVE | セクション 4.4 固定列の実装 — CSS sticky + PrimeVue frozen |
| GAP-F20-03 | IMPROVE | セクション 4.6 Pagination — 件数設定可能 (20/50/100/200) |
| GAP-F20-04 | KEEP | セクション 5.2〜5.4 — CZ-509/510/511 確認ダイアログ |
| GAP-F20-05 | IMPROVE | セクション 4.5 HoursEditCell — HH:MM 入力マスク + リアルタイムバリデーション |
| GAP-F20-06 | KEEP | セクション 4.3 Toolbar — チェックボックス + 承認/戻し一括操作 |
| GAP-F20-07 | ADD/P2 | セクション 4.1 SearchPanel — ステータス別フィルタ (作成中も表示チェック) |
| GAP-F20-08 | ADD/P2 | P2 スコープ: PrimeVue DataTable の列リサイズ (`resizableColumns`) で対応予定 |

---

## 10. テスト要件

### 10.1 コンポーネント単体テスト (Vitest)

| コンポーネント | テスト内容 |
|-------------|----------|
| SearchPanel | 年月切替 → changeMonth 呼出、組織選択 → フィルタ適用 |
| MonthlyControlBar | 3段階ステータス表示、ボタン表示条件 (canInputPeriod/canAggregate)、色の切替 |
| Toolbar | 承認/戻しボタン有効条件、選択レコードのステータス判定 |
| WorkStatusDataTable | 15列レンダリング、固定列動作、ステータスカラーコード |
| HoursEditCell | HH:MM パース、15分単位バリデーション、自動変換、編集可否判定 |
| Pagination | ページ番号生成、表示件数変更、境界値 (1ページ / 最終ページ) |
| StatusBadge | STATUS_0/1/2 の色分け表示 |

### 10.2 Pinia Store テスト (Vitest)

| テスト | 内容 |
|--------|------|
| fetchRecords | API モック → records / monthlyControl / permissions 更新確認 |
| monthlyConfirm | API 呼出 → monthlyControl フラグ更新 |
| monthlyAggregate | API 呼出 → monthlyControl フラグ更新 |
| monthlyUnconfirm | API 呼出 → monthlyControl リセット |
| approveRecords | 対象レコードの STATUS_1 → STATUS_2 更新確認 |
| revertRecords | 対象レコードの STATUS_2 → STATUS_1 更新確認 |
| updateHours | 楽観的更新 → API 成功/失敗時の state 確認 |
| changePage | page 更新 → fetchRecords 再呼出、selectedIds クリア |
| isHoursEditable | ステータスマトリクス 12状態 × man系列の全パターン |

### 10.3 E2E テスト (Playwright)

| シナリオ | 内容 |
|---------|------|
| 基本フロー | 管理者ログイン → 工数状況一覧 → 検索 → ステータスカラーコード確認 |
| 月次確認フロー | [確認] → CZ-510 確認 → ステータスバー色変化 → 一覧更新 |
| 月次集約フロー | [集約] → CZ-511 確認 → ステータスバー色変化 |
| 未確認戻し | [未確認] → CZ-509 確認 → ステータスバー色リセット |
| 承認フロー | STATUS_1 レコード選択 → [承認] → CZ-508 確認 → ステータス青に変化 |
| 戻しフロー | STATUS_2 レコード選択 → [戻す] → CZ-507 確認 → ステータス緑に変化 |
| インライン編集 | 工数セルクリック → "04:00" 入力 → Enter → 保存確認 |
| バリデーション | 工数 "04:10" 入力 → 15分単位エラー表示 |
| ページネーション | 次ページ → データ更新確認 → 表示件数変更 → 1ページ目リセット |
| 権限テスト | ACT-04(管理) vs ACT-10(スタッフ) で承認ボタン表示差異確認 |
| 排他制御 | 2ユーザー同時月次操作 → 409 Conflict → エラーメッセージ |
