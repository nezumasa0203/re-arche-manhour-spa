# 工数入力画面 (FORM_010): SPA 画面仕様

## 概要

保守担当者が日々の保守工数を入力・管理する画面。
現行 MPA の FRAMESET 4 フレーム + TdMask Ajax インライン編集を、
Nuxt.js 3 SPA の単一ページ + PrimeVue DataTable インライン編集に移行する。

**対応ユーザーストーリー**: US-010〜US-01D（13件）
**対応分析ドキュメント**:
- `analysis/03_user_stories.md` セクション 1.1
- `analysis/04_screen_transition.md` セクション 5.1 (SCR-010)

**画面 URL**: `/work-hours`

---

## 仕様分類サマリー

詳細は [consistency-check.md セクション D](./consistency-check.md) を参照。

| 分類 | 件数 | 主要項目 |
|------|:----:|----------|
| 既存踏襲（KEEP） | 12 | 15分単位入力、ステータス遷移（0→1→2）、12状態マトリクス、禁止語句12語、バイト長計算、確認ダイアログ |
| 改善（IMPROVE） | 9 | TdMask→PrimeVue インライン編集、空行追加→即座追加、画面リロード→リアクティブ更新 |
| 新規追加（ADD） | 9 | Pinia Store、StatusFooter、MonthSelector、ショートカットキー、楽観的ロック UI |
| 廃止（REMOVE） | 6 | FRAMESET 4フレーム構成、TdMask.js、ActionDispatcher、InsertList Unit/Action、hidden iframe Excel |

### 移行元ソースコード参照

| 機能 | 移行元ファイル |
|------|---------------|
| 入力画面メイン | `czConsv/WEB-INF/src/jp/co/isid/cz/integ/unit/InsertListJspBean.java` |
| 入力チェック | `czConsv/WEB-INF/src/jp/co/isid/cz/integ/proc/InsertListMaintenanceProc.java` |
| マスクコントロール | `czResources/cssjs/TdMask.js` |
| ステータスマトリクス | `czConsv/WEB-INF/src/jp/co/isid/cz/integ/proc/StatusKeyManager.java` |
| バイト長計算 | `czResources/cssjs/ApCheck.js` (chkChara 関数) |

---

## 1. ページレイアウト

```
┌──────────────────────────────────────────────────────┐
│ [AppHeader] 保有資源管理 | ユーザー名 | ヘルプ        │
├────────┬─────────────────────────────────────────────┤
│ [Side  │ [WorkHoursPage]                              │
│  Nav]  │ ┌──────────────────────────────────────────┐│
│        │ │ [SearchPanel]                             ││
│ 工数入力│ │ 担当者:[山田太郎 ×] 年月:[2025/02 ▼]     ││
│ 工数管理│ │ [<<][>>]          [検索] [リセット]       ││
│ 分析   │ ├──────────────────────────────────────────┤│
│ 設定   │ │ [Toolbar]                                ││
│        │ │ [追加][コピー][転写][削除]  合計:120:30   ││
│        │ │                      [PJ工数][Excel]      ││
│        │ ├──────────────────────────────────────────┤│
│        │ │ [WorkHoursDataTable]                      ││
│        │ │ ☐|STS |日付   |対象SS |原因SS|カテゴリ|..││
│        │ │ ☐|作成|02/25 |SS001 |SS002 |障害対応|..││
│        │ │ ☐|確認|02/24 |SS003 |SS003 |保守   |.. ││
│        │ │  (セルクリック → インライン編集)           ││
│        │ ├──────────────────────────────────────────┤│
│        │ │ [StatusFooter]                            ││
│        │ │ [一括確認][一括作成中] 作成:3 確認:5 確定:10││
│        │ │ [メッセージエリア]                         ││
│        │ └──────────────────────────────────────────┘│
└────────┴─────────────────────────────────────────────┘
```

---

## 2. コンポーネント構成

```
pages/work-hours.vue (WorkHoursPage)
├── components/work-hours/SearchPanel.vue
├── components/work-hours/Toolbar.vue
├── components/work-hours/WorkHoursDataTable.vue
│   ├── (PrimeVue DataTable + Column)
│   ├── components/work-hours/cells/StatusCell.vue
│   ├── components/work-hours/cells/DateCell.vue
│   ├── components/work-hours/cells/SubsystemCell.vue
│   ├── components/work-hours/cells/CategoryCell.vue
│   ├── components/work-hours/cells/SubjectCell.vue
│   ├── components/work-hours/cells/HoursCell.vue
│   └── components/work-hours/cells/TextCell.vue
├── components/work-hours/StatusFooter.vue
├── components/work-hours/TransferDialog.vue   (翌月転写)
├── components/work-hours/ProjectSummaryDialog.vue (PJ工数)
├── components/common/SubsystemSearchDialog.vue (SS選択)
└── components/common/StaffSearchDialog.vue (担当者選択)
```

---

## 3. 状態管理 (Pinia Store)

### 3.1 `stores/workHours.ts`

```typescript
interface WorkHoursState {
  // データ
  records: WorkHoursRecord[]
  summary: WorkHoursSummary

  // 検索条件
  yearMonth: string          // "2025-02"
  staffId: string | null     // 代行モード時に指定
  staffName: string | null
  isDaiko: boolean
  sort: string               // "workDate:asc"

  // 月次制御
  monthControl: MonthControl

  // 権限
  permissions: WorkHoursPermissions
  statusMatrix: Record<string, Record<string, number>>

  // UI 状態
  selectedIds: number[]      // チェックボックス選択
  editingCell: EditingCell | null
  loading: boolean
  message: StatusMessage | null
}
```

**UI 状態表示**:
- `loading: true` → DataTable に PrimeVue `loading` プロパティ適用（スピナーオーバーレイ表示）
- `records` が空配列 → テーブル中央に「データがありません。[新規追加] ボタンで工数を登録してください」を表示
- 初回ページ表示時: 現在月で自動 fetch（`fetchRecords()` を `onMounted` で実行）

### 3.2 主要 Actions

| Action | API | US |
|--------|-----|-----|
| `fetchRecords()` | `GET /work-hours` | US-017 |
| `createRecord(data)` | `POST /work-hours` | US-010 |
| `updateField(id, field, value)` | `PATCH /work-hours/{id}` | US-011 |
| `deleteRecords(ids)` | `DELETE /work-hours` | US-014 |
| `copyRecords(ids)` | `POST /work-hours/copy` | US-012 |
| `transferNextMonth(ids, months)` | `POST /work-hours/transfer-next-month` | US-013 |
| `batchConfirm()` | `POST /work-hours/batch-confirm` | US-015 |
| `batchRevert()` | `POST /work-hours/batch-revert` | US-016 |
| `changeMonth(yearMonth)` | `GET /work-hours` (月切替) | US-017 |
| `switchDaiko(staffId)` | `POST /delegation/switch` → ヘッダー設定 → `fetchRecords` | US-01C |

### 3.3 主要 Getters

| Getter | 用途 |
|--------|------|
| `canAdd` | 追加ボタン表示可否（ステータスマトリクス） |
| `canCopy` | コピーボタン（選択レコードのステータスで判定） |
| `canDelete` | 削除ボタン（同上） |
| `canBatchConfirm` | 一括確認ボタン（STATUS_0 件数 > 0） |
| `canBatchRevert` | 一括作成中ボタン（STATUS_1 件数 > 0） |
| `isEditable(record)` | レコードが編集可能か（STATUS_0 のみ、管理者は拡張） |

---

## 4. コンポーネント詳細仕様

### 4.1 SearchPanel.vue — 検索パネル

| 要素 | コンポーネント | 動作 | US |
|------|-------------|------|-----|
| 担当者 | PrimeVue InputText (readonly) + StaffSearchDialog | クリックで担当者選択モーダル。選択後 `staffId` セット | US-01C |
| 年月 | PrimeVue Dropdown | 現在月を基準に前後 12 ヶ月（当月含む合計 25 件）。初期値は現在月。変更で `changeMonth()` → 自動 fetch | US-017 |
| << / >> | PrimeVue Button | 前月/翌月切替 | US-017 |
| 検索 | PrimeVue Button | `fetchRecords()` 実行 | - |
| リセット | PrimeVue Button | 初期値に戻す（担当者=ログインユーザー、代行モード解除、年月=現在月、ソート=workDate:asc）→ 自動 fetchRecords() | - |

**代行モード表示**:
- `isDaiko === true` の場合、担当者名の横に「代行中」バッジ表示
- 代行解除ボタン（自分に戻る）を表示

### 4.2 Toolbar.vue — ツールバー

| ボタン | 表示条件 | 有効条件 | 確認ダイアログ | US |
|--------|---------|---------|-------------|-----|
| 追加 | `canAdd` (ステータスマトリクス) | 常時 | なし | US-010 |
| コピー | `canCopy` | `selectedIds.length > 0` | なし | US-012 |
| 翌月転写 | `canCopy` | `selectedIds.length > 0` | TransferDialog | US-013 |
| 削除 | `canDelete` | `selectedIds.length > 0` | CZ-506 確認 | US-014 |
| 合計工数 | 常時表示 | - | - | - |
| PJ工数 | 常時表示 | クリックで ProjectSummaryDialog | なし | US-019 |
| Excel | 常時表示 | - | CZ-516 確認 | US-01A |

**ボタン表示ロジック**:
```
ステータスキー = monthControl から算出 (sts + ins + sum)
系列 = permissions.useTanSeries ? "tan" : "man"
StatusMatrixResolver.resolve(ステータスキー, 系列)
  → { add: 1/0/9, copy: 1/0/9, delete: 1/0/9, ... }
  → 9: 非表示, 0: グレーアウト, 1: 有効
```

### 4.3 WorkHoursDataTable.vue — メインテーブル

PrimeVue `DataTable` を使用。ヘッダー固定 + 水平スクロール。

#### 列定義（14列）

| # | 列 | field | 幅 | ソート | 編集方式 | 編集条件 |
|---|-----|-------|-----|:---:|---------|---------|
| 1 | CHK | - | 40px | - | Checkbox | ステータスマトリクスで判定 |
| 2 | ステータス | `status` | 80px | O | StatusCell (Dropdown) | **常時編集可** |
| 3 | 作業日 | `workDate` | 100px | O | DateCell (Calendar) | STATUS_0 のみ |
| 4 | 保守担当所属 | `department` | 120px | O | 表示のみ | - |
| 5 | 保守担当者名 | `staffName` | 100px | O | 表示のみ | - |
| 6 | 対象SS No | `targetSubsystem.subsystemNo` | 70px | O | 表示のみ | - |
| 7 | 対象SS名 | `targetSubsystem.subsystemName` | 150px | O | SubsystemCell (モーダル起動) | STATUS_0 のみ |
| 8 | 原因SS No | `causeSubsystem.subsystemNo` | 70px | O | 表示のみ | - |
| 9 | 原因SS名 | `causeSubsystem.subsystemName` | 150px | O | SubsystemCell (モーダル起動) | STATUS_0 のみ |
| 10 | 保守カテゴリ | `category.code` | 160px | O | CategoryCell (Dropdown) | STATUS_0 のみ |
| 11 | 件名 | `subject` | 220px | O | SubjectCell (InputText) | STATUS_0 のみ |
| 12 | 工数 | `hours` | 70px | O | HoursCell (InputText) | STATUS_0 のみ |
| 13 | TMR番号 | `tmrNo` | 70px | O | TextCell (InputText) | STATUS_0 のみ |
| 14 | 依頼書No | `workRequestNo` | 90px | O | TextCell (InputText) | STATUS_0 のみ |

#### インライン編集フロー（TdMask → PrimeVue セル編集）

```
現行（TdMask）:
  セルクリック → TdMask オーバーレイ表示 → 入力 → onBlur → Ajax POST

新規（PrimeVue）:
  セルクリック → 編集モード遷移（入力要素表示）→ 入力
  → フォーカスアウト or Enter → フロントバリデーション
  → 成功: PATCH API 呼出 → レスポンスでセル値 + 合計更新
  → 失敗: エラーメッセージ表示、元の値に復元
```

**編集可否の判定**:
```typescript
function isEditable(record: WorkHoursRecord, field: string): boolean {
  // ステータスは常時編集可
  if (field === 'status') return true

  // STATUS_0 のみ編集可（担当者系列）
  if (permissions.useTanSeries) {
    return record.status === 0
  }

  // 管理者系列: ステータスマトリクスで判定
  const key = buildStatusKey(record, monthControl)
  const matrix = StatusMatrixResolver.resolve(key, false)
  return matrix.update === 1
}
```

#### ステータスセルの色分け

| STATUS | ラベル | 背景色 | 文字色 |
|--------|--------|--------|--------|
| 0 | 作成中 | `#FBFBB6` (黄) | `#000` |
| 1 | 確認 | `#BDEAAD` (緑) | `#000` |
| 2 | 確定 | `#9DBDFE` (青) | `#000` |
| 9 | 非表示 | `#5D5D5D` (灰) | `#FFF` |

#### 行スタイル

- STATUS_0: 通常表示（編集可能フィールドにホバー時カーソルがテキスト入力に変化）
- STATUS_1/2: 編集不可フィールドはグレー背景 (`className: 'cell-readonly'`)
- 管理者系列(man): STATUS_1/2 でも編集可能フィールドは通常背景

#### ソート (US-01B)

- 列ヘッダークリックで昇順/降順トグル
- デフォルト: 作業日昇順 → ステータス順
- API に `sort` パラメータとして送信
- ソートアイコン: PrimeVue DataTable 標準の `sortField` / `sortOrder`

### 4.4 セルコンポーネント詳細

#### StatusCell.vue

```
表示モード: ステータスラベル（色付きバッジ）
編集モード: PrimeVue Dropdown
  options: [
    { label: "作成中", value: 0, style: { bg: "#FBFBB6" } },
    { label: "確認",   value: 1, style: { bg: "#BDEAAD" } },
    { label: "確定",   value: 2, style: { bg: "#9DBDFE" } },
  ]
変更時: PATCH /work-hours/{id} { field: "status", value: 1 }
```

**ステータス遷移ルール**:
- 担当者系列: 0↔1 のみ（2への遷移は管理者のみ）
- 管理者系列: 0→1→2、2→1→0 すべて可能

#### DateCell.vue

```
表示モード: "02/25" (MM/DD 短縮表示)
編集モード: PrimeVue Calendar (dateFormat="yy-mm-dd")
バリデーション:
  - 必須 (VR-001)
  - 選択月の範囲内 (VR-002): yearMonth の月初〜月末
変更時: PATCH /work-hours/{id} { field: "workDate", value: "2025-02-25" }
```

#### SubsystemCell.vue

```
表示モード: "SS001 会計モジュール"
  - sysKbn === 1 の場合、先頭に "◆" マーカー表示
編集モード: クリックで SubsystemSearchDialog を開く
  - mode: "target"（対象SS）or "cause"（原因SS）
  - 選択確定 → PATCH /work-hours/{id} { field: "targetSubsystemNo", value: "SUB001" }
```

#### CategoryCell.vue

```
表示モード: カテゴリ名
編集モード: PrimeVue Dropdown
  options: カテゴリマスタから年度別に取得 (GET /masters/categories?fiscalYear=2025)
バリデーション: 必須 (VR-005)
変更時: PATCH /work-hours/{id} { field: "categoryCode", value: "01" }
```

#### SubjectCell.vue

```
表示モード: 件名テキスト（30文字で折り返し表示）
編集モード: PrimeVue InputText (maxlength=128)
バリデーション:
  - 必須 (VR-006)
  - 128バイト以内（全角2バイト計算）
  - 禁止ワード不含 (VR-007, HOSYU_SYUBETU_0 の場合)
  - 改行コード自動除去
変更時: PATCH /work-hours/{id} { field: "subject", value: "..." }
```

#### HoursCell.vue

```
表示モード: "03:30"
編集モード: PrimeVue InputText (placeholder="HH:MM")
入力補助:
  - 短縮入力の自動変換 (BR-006):
    - 1桁: "3" → "03:00"（0X:00 形式）
    - 2桁: "12" → "12:00"（XX:00 形式、0〜24）
    - 3桁: "330" → "03:30"（0X:YY 形式、YY≤59）
    - 4桁: "1230" → "12:30"（XX:YY 形式、XX≤24, YY≤59）
    - "HH:MM" 形式: そのまま
    - 範囲外（XX>24 or YY>59）: バリデーションエラー (VR-009)
  - 空値 → バリデーションエラー（VR-008 必須チェック）
バリデーション:
  - 必須、0より大 (VR-008)
  - HH:MM 形式 (VR-009, CZ-125)
  - 15分単位: 00/15/30/45 (BR-002, CZ-147)
  - 日次合計24:00以下 (BR-003, VR-010, CZ-146)
  - 最小0:15 (BR-007, CZ-129)
変更時: PATCH /work-hours/{id} { field: "hours", value: "03:30" }
成功時: summary.totalHours を API レスポンスから更新
```

#### TextCell.vue (TMR番号 / 依頼書No / 依頼者名 共通)

```
表示モード: テキスト
編集モード: PrimeVue InputText
バリデーション (field 別):
  - tmrNo: 5文字以内、半角英数字 (VR-011)
  - workRequestNo: 空 or 7文字固定 (VR-012, CZ-137)
  - workRequesterName: 40文字以内 (VR-013)
  - 特定カテゴリ時: 依頼書No + 依頼者名の両方必須 (VR-014, CZ-142)
```

### 4.5 StatusFooter.vue — フッター

| 要素 | 表示条件 | 動作 |
|------|---------|------|
| 一括確認ボタン | `canBatchConfirm` かつ STATUS_0 件数 > 0 | 確認ダイアログ CZ-505 → `batchConfirm()` |
| 一括作成中ボタン | `canBatchRevert` かつ STATUS_1 件数 > 0 | 確認ダイアログ CZ-518 → `batchRevert()` |
| 作成中件数 | 常時 | STATUS_0 の件数。>0 の場合赤文字 |
| 確認件数 | 常時 | STATUS_1 の件数 |
| 確定件数 | 常時 | STATUS_2 の件数 |
| メッセージ | エラー/成功時 | 成功: 緑文字、3秒後自動消去。エラー: 赤文字、ユーザーが×ボタンで閉じるか次の操作まで表示継続 |

### 4.6 TransferDialog.vue — 翌月転写 (US-013)

```
┌─────────────────────────────────┐
│ 翌月以降へ転写                    │
│                                  │
│ 選択レコード: 3件                 │
│                                  │
│ 転写先月:                         │
│ ☑ 2025年03月                     │
│ ☑ 2025年04月                     │
│ ☐ 2025年05月                     │
│ ☐ 2025年06月                     │
│                                  │
│ ※ カテゴリが対象年度に存在しない   │
│   場合はカテゴリが空白になります   │
│                                  │
│        [転写実行] [キャンセル]     │
└─────────────────────────────────┘
```

- PrimeVue Dialog (modal)
- 転写先月: 現在月+1 から最大 12ヶ月先までチェックボックス
- 転写実行: `POST /work-hours/transfer-next-month`

### 4.7 ProjectSummaryDialog.vue — PJ 別工数 (US-019)

```
┌─────────────────────────────────────┐
│ プロジェクト別工数                     │
│                                      │
│ 対象月: 2025年02月                    │
│                                      │
│ ┌──────────┬────────┬──────┐         │
│ │ システム   │ SS名    │ 工数  │         │
│ ├──────────┼────────┼──────┤         │
│ │ 基幹      │ 会計    │ 40:00│         │
│ │ 基幹      │ 人事    │ 32:00│         │
│ │ 営業      │ CRM    │ 48:30│         │
│ │ 合計      │        │120:30│         │
│ └──────────┴────────┴──────┘         │
│                                      │
│                        [閉じる]       │
└─────────────────────────────────────┘
```

- PrimeVue Dialog (modal, width: 600px)
- 読み取り専用
- API: `GET /work-hours/project-summary?yearMonth=2025-02`

---

## 5. インタラクション仕様

### 5.1 新規レコード追加 (US-010)

```
1. ユーザー: [追加] ボタンクリック
2. Frontend: POST /work-hours { yearMonth } （ドラフトモード）
   ※ STATUS_0 でのドラフト作成時は VR-001〜015 のバリデーションを省略。
     必須フィールド未入力を許容し、空レコードを作成する。
     バリデーションは一括確認 (batch-confirm) 時に isInputCheck() で実施。
3. Backend: STATUS_0 で空レコード作成、id 採番
4. Frontend: テーブル先頭に新規行表示（ソート状態に関係なく常に先頭。未保存行は背景色 `var(--surface-100)` で区別）。保存後の再 fetch でソート順に従った位置に移動。作業日セルにフォーカス
5. ユーザー: 各セルをクリックしてインライン入力
6. Frontend: フォーカスアウトごとに PATCH API 呼出
```

### 5.2 インライン編集 (US-011)

```
1. ユーザー: セルをクリック
2. Frontend: isEditable(record, field) 判定
   → false: 何もしない（カーソルがデフォルトのまま）
   → true: 3 へ
3. Frontend: セルを編集モードに遷移（入力要素表示）
4. ユーザー: 値を入力
5. ユーザー: フォーカスアウト or Enter
6. Frontend: フロントバリデーション実行
   → エラー: セルに赤枠 + ツールチップでエラーメッセージ、元の値に復元
   → 成功: 7 へ
7. Frontend: PATCH /work-hours/{id} { field, value, updatedAt }
8. Backend: バリデーション + 保存
   → エラー: { error: { code, message, field } } → セルにエラー表示
   → 成功: { data: { id, field, oldValue, newValue, summary } }
9. Frontend: セル値更新 + summary (合計工数) 更新
```

### 5.3 レコード削除 (US-014)

```
1. ユーザー: チェックボックスで1件以上選択
2. ユーザー: [削除] ボタンクリック
3. Frontend: 確認ダイアログ表示
   "選択した {n} 件のレコードを削除します。よろしいですか？" (CZ-506)
4. ユーザー: [OK]
5. Frontend: DELETE /work-hours { ids: [...] }
6. Backend: ステータスマトリクス判定、削除実行
7. Frontend: テーブルから該当行を除去、summary 更新
```

### 5.4 一括確認 (US-015)

```
1. ユーザー: [一括確認] ボタンクリック
2. Frontend: 確認ダイアログ
   "作成中の状態を全て確認に変更します。よろしいですか？" (CZ-505)
3. ユーザー: [OK]
4. Frontend: POST /work-hours/batch-confirm { yearMonth }
5. Backend: トランザクション内で全 STATUS_0 レコードに isInputCheck() 実行
   → バリデーションエラー:
     { error: { code, message, recordId, field } }
   → Frontend: 該当行にスクロール + セルにフォーカス + エラーハイライト
   → コンフリクト (CZ-101) 発生時: 全件ロールバック。コンフリクトした件数と recordId をレスポンスに含める
6. Backend: 全パス → STATUS_0 → STATUS_1 に一括更新
7. Frontend: fetchRecords() で全件リロード
```

### 5.5 月切替 (US-017)

```
1. ユーザー: 年月 Dropdown 変更 or [<<][>>] クリック
2. Frontend: changeMonth(newYearMonth)
   → loading = true
   → GET /work-hours?yearMonth=2025-03&staffId=...
3. Frontend: records / summary / monthControl / permissions を更新
   → カテゴリリストが年度変更で更新される場合、カテゴリ Dropdown 再取得
4. Frontend: loading = false
```

### 5.6 代行モード (US-01C)

```
1. ユーザー: 担当者フィールドクリック → StaffSearchDialog 表示
2. ユーザー: 代行対象の担当者を選択
3. Frontend: POST /delegation/switch { targetStaffId: selectedStaffId }
   → Backend: canDelegate + isAllowedStaff() 検証
   → 失敗: 403 + CZ-307 → Toast エラー表示
   → 成功: { delegationStaffId, delegationStaffName, isDaiko: true }
4. Frontend: staffId = delegationStaffId
   → isDaiko = true
   → 以降の API リクエストに X-Delegation-Staff-Id ヘッダー付与
   → fetchRecords() で代行対象のレコード取得
5. ヘッダーに「代行中: 山田太郎」バッジ表示
6. 代行解除: [自分に戻る] ボタン
   → POST /delegation/switch { targetStaffId: null }
   → staffId = null, isDaiko = false
   → X-Delegation-Staff-Id ヘッダー除去
   → fetchRecords() で自分のレコード取得
```

### 5.7 ソート (US-01B)

```
1. ユーザー: 列ヘッダークリック
2. Frontend: PrimeVue DataTable の @sort イベント
   → sort パラメータ構築 (例: "workDate:asc")
   → fetchRecords() で API 再取得
3. ソートアイコン表示（▲/▼）
```

### 5.8 Excel 出力 (US-01A)

```
1. ユーザー: [Excel] ボタンクリック
2. Frontend: 確認ダイアログ (CZ-516)
3. ユーザー: [OK]
4. Frontend: fetch("/api/v1/work-hours/export/excel?yearMonth=2025-02&staffId=...") + Blob + createObjectURL でダウンロード（spec #9 GAP-E03 準拠）
5. ブラウザ: .xlsx ファイルダウンロード
```

---

## 6. バリデーション実装

### 6.1 フロントバリデーション（即時フィードバック）

セルのフォーカスアウト時に実行。エラー時はセルに赤枠 + ツールチップ。

| フィールド | ルール | メッセージ |
|-----------|--------|-----------|
| workDate | 空チェック | CZ-126「作業日は必須入力です」 |
| workDate | 月内範囲 | CZ-144「作業日は対象月の範囲内で入力してください」 |
| hours | 空チェック | CZ-126「工数は必須入力です」 |
| hours | HH:MM形式 | CZ-125「工数はHH:MM形式で入力してください」 |
| hours | 15分単位 | CZ-147「工数は15分単位で入力してください」 |
| subject | 空チェック | CZ-126「件名は必須入力です」 |
| subject | 128バイト超過 | CZ-126「件名は128文字以内で入力してください」 |
| workRequestNo | 1〜6文字 | CZ-137「作業依頼書Noは7文字で入力してください」 |

### 6.2 バックエンドバリデーション（確定的判定）

API レスポンスのエラーをセルに紐付けて表示:
- `error.field` でエラーの対象セルを特定
- `error.recordId` でエラーの対象行を特定（一括確認時）
- 日次合計24h超過 (VR-010) は Backend でのみ判定（他レコードの合計が必要）
- 禁止ワード (VR-007) は Backend でのみ判定（ワードリストは Backend 管理）

### 6.3 バイト長計算 (Frontend)

```typescript
function calculateByteLength(str: string): number {
  let bytes = 0
  for (const char of str) {
    const cp = char.codePointAt(0)!
    if (cp >= 0xFF61 && cp <= 0xFF9F) {
      bytes += 2  // 半角カタカナ: 2バイト
    } else if (cp > 0xFFFF) {
      bytes += 4  // BMP外（サロゲートペア）: 4バイト
    } else if (cp > 0x7F) {
      bytes += 2  // 全角: 2バイト
    } else {
      bytes += 1  // 半角: 1バイト
    }
  }
  return bytes
}
```

> CZ システムは業務用途のため BMP 外文字（絵文字等）は入力対象外。Backend バリデーションで BMP 外文字を含む入力は拒否する。

---

## 7. 権限によるUI制御

### 7.1 アクター別 UI 差異

| UI 要素 | ACT-01 報告担当 | ACT-02 報告管理 | ACT-03 全権管理 | ACT-04 管理モード |
|---------|:---:|:---:|:---:|:---:|
| 担当者選択（代行） | 自分のみ | O（部下選択可） | O | O |
| 追加ボタン | ステータスマトリクス(tan) | ステータスマトリクス(man) | man | man |
| STATUS_2 レコード編集 | 不可 | 可 | 可 | 可 |
| STATUS_2 レコード削除 | 不可 | 不可 | 不可 | 不可 |
| 一括確認 | O | O | O | O |
| 画面遷移リンク(020) | canNavigateForms 必要 | O | O | O |

### 7.2 雇用形態による制限

| 雇用形態 | 制限 |
|----------|------|
| 正社員 (TYPE_0) | なし |
| 臨時職員1 (TYPE_1) | 参照権限のみの可能性（dataAuthority.ins 確認） |
| 臨時職員2 (TYPE_2) | 同上 |
| 外部契約者 (TYPE_3) | 代行モード時、登録者IDが代行元正社員のIDになる |

---

## 8. エラーハンドリング

### 8.1 同時編集競合 (CZ-101)

```
PATCH /work-hours/{id} → 409 Conflict
Frontend:
  1. PrimeVue Toast で「別ユーザーにより更新が行われました」(CZ-101) 表示
  2. コンフリクトしたセルのみ API エラーレスポンスの最新値で上書き
  3. 他の未保存編集は保持（全件リフレッシュしない）
  4. updatedAt をレスポンスの最新値に更新し、リトライ可能にする
```

### 8.2 ステータス違反

```
PATCH /work-hours/{id} → 403 + CZ-106
Frontend:
  1. Toast で「ステータス不正のため操作不可」表示
  2. fetchRecords() で最新状態に更新
```

### 8.3 サービス時間外 (CZ-102)

```
任意の POST/PATCH/DELETE → 403 + CZ-102
Frontend:
  1. 画面全体にオーバーレイ表示
  2. 「サービス提供時間は6:00から23:30までです」
```

### 8.4 ネットワークエラー

```
Frontend:
  1. Toast で「通信エラーが発生しました。再試行してください」
  2. 編集中のセルを元の値に復元
  3. リトライボタン表示
```

---

## 9. レスポンシブ対応

| ブレークポイント | レイアウト |
|-------------|----------|
| >= 1280px | サイドナビ + フルテーブル（全14列表示） |
| 960-1279px | サイドナビ折りたたみ + テーブル水平スクロール |
| < 960px | サイドナビ非表示 + テーブル水平スクロール（必須列のみ固定） |

固定列（スクロール時も表示）: CHK, ステータス, 作業日, 対象SS名

---

## 10. テスト要件

### 10.1 コンポーネント単体テスト (Vitest)

| コンポーネント | テスト内容 |
|-------------|----------|
| SearchPanel | 年月 Dropdown 変更 → changeMonth 呼出 |
| Toolbar | ステータスマトリクスに応じたボタン表示/非表示/グレーアウト |
| WorkHoursDataTable | 14列のレンダリング、セルクリック → 編集モード遷移 |
| StatusCell | ステータス変更、色分け表示 |
| HoursCell | HH:MM パース、15分単位バリデーション、自動変換 |
| SubjectCell | バイト長計算、禁止ワードフロントチェック |
| StatusFooter | 件数カウント表示、一括ボタン表示条件 |
| TransferDialog | 転写先月チェック、API 呼出 |

### 10.2 Pinia Store テスト (Vitest)

| テスト | 内容 |
|--------|------|
| fetchRecords | API モック → state 更新確認 |
| updateField | 楽観的更新 → API 成功/失敗時の state 確認 |
| batchConfirm | バリデーションエラー時の recordId ハンドリング |
| canAdd/canCopy/canDelete | ステータスマトリクス全12状態 × 2系列のゲッター |

### 10.3 E2E テスト (Playwright)

| シナリオ | 内容 |
|---------|------|
| 基本フロー | ログイン → 工数入力 → レコード追加 → 各フィールド入力 → 一括確認 |
| バリデーション | 15分単位違反、24h超過、禁止ワード → エラー表示確認 |
| 権限テスト | ACT-01(担当者) vs ACT-03(全権管理者) でボタン表示差異確認 |
| 代行モード | 管理者でログイン → 担当者選択 → 代行入力 → 代行解除 |
| 月切替 | 前月/翌月切替 → データリロード確認 |

### 受け入れ基準（Given-When-Then）

**AC-WH-01: 新規行追加（US-010）**
- Given: STATUS_0 権限を持つユーザーが工数入力画面を表示している
- When: [追加] ボタンをクリックする
- Then: DataTable 先頭に空行が追加され、作業日セルにフォーカスが当たる

**AC-WH-02: インライン編集（US-011）**
- Given: DataTable に STATUS_0 のレコードが表示されている
- When: 工数セルをクリックして "130" を入力し、フォーカスアウトする
- Then: 値が "1:30" に自動変換され、PATCH API が送信されて保存される

**AC-WH-03: 短縮入力変換**
- Given: HoursCell が編集モードである
- When: "8" を入力してフォーカスアウトする
- Then: "8:00" に自動変換される

**AC-WH-04: 一括確定（US-015）**
- Given: STATUS_0 のレコードが3件あり、全て必須項目入力済み
- When: [一括確定] をクリックし、CZ-505 確認ダイアログで「はい」を選択する
- Then: 全3件が STATUS_0→STATUS_1 に遷移し、ステータスフッターが更新される

**AC-WH-05: 一括確定バリデーションエラー**
- Given: STATUS_0 のレコードが3件あり、うち1件は件名が空
- When: [一括確定] を実行する
- Then: CZ-126 エラーが表示され、該当行がハイライト+スクロールされ、全件のステータスは変更されない

**AC-WH-06: 楽観的ロック競合（US-01B）**
- Given: 2ユーザーが同一レコード（version=1）を表示している
- When: ユーザー A が編集保存後、ユーザー B が同レコードを編集保存する
- Then: ユーザー B に CZ-101（競合エラー）Toast が表示され、データがリロードされる

**AC-WH-07: 削除操作（US-012）**
- Given: STATUS_0 のレコードが選択されている
- When: [削除] ボタンをクリックし確認ダイアログで「はい」を選択する
- Then: レコードが論理削除され、DataTable から消える

**AC-WH-08: 月切替（US-013）**
- Given: 2025年02月の工数データが表示されている
- When: MonthSelector で 2025年01月を選択する
- Then: 2025年01月のデータが API から取得されて表示される

**AC-WH-09: 代行モード（US-014）**
- Given: ACT-09（外部契約者, canDelegate=true）でログインしている
- When: 代行先を正社員 A に設定する
- Then: 正社員 A の工数データが表示され、以降の操作は正社員 A 名義で記録される

**AC-WH-10: サービス時間外操作**
- Given: 現在時刻が 23:31 JST（サービス時間 6:00-23:30 外）
- When: 工数入力画面で操作を試みる
- Then: CZ-102 エラーのフルスクリーンオーバーレイが表示され、全操作がブロックされる

**AC-WH-11: ステータス別ボタン制御**
- Given: STATUS_1（確定済）のレコードが表示されている
- When: 画面を確認する
- Then: [編集][削除] ボタンが無効化され、[差戻] ボタンが表示される（管理者系列の場合）

**AC-WH-12: Excel 出力（US-01C）**
- Given: 工数データが表示されている状態
- When: [Excel] ボタンをクリックし確認ダイアログで「はい」を選択する
- Then: work_hours_YYYYMM.xlsx がダウンロードされ、完了 Toast が表示される

**AC-WH-13: 状態表示パターン**
- Given: 工数入力画面に遷移した
- When: API からのデータ取得中
- Then: スピナーオーバーレイが表示され、取得完了後にデータが描画される。データ0件の場合は「データがありません」メッセージが表示される
