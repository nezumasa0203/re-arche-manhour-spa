# 分析画面 (FORM_030-042): SPA 画面仕様

## 概要

半期推移と月別内訳の2つの分析ビューを、3階層ドリルダウン＋タブ切替で
提供する分析画面。現行 MPA の 6画面（030/031/032/040/041/042）×
各9フレーム構成（計54フレーム）を、Nuxt.js 3 SPA の
**1ページ + 2タブ + Breadcrumb ドリルダウン** に統合する。

**対応ユーザーストーリー**: US-030〜US-045（16件）
**対応分析ドキュメント**:
- `analysis/03_user_stories.md` セクション 1.3, 1.4
- `analysis/04_screen_transition.md` — SCR-030〜042
- `analysis/05_gap_analysis.md` — GAP-F30-01〜09

**画面 URL**: `/analytics`
**URL パラメータ対応** (GAP-F30-07):
- `/analytics?tab=half&year=2025&half=FIRST&step=0`
- `/analytics?tab=half&year=2025&half=FIRST&step=1&cat1=01&cat2=01-01`
- `/analytics?tab=monthly&year=2025&half=FIRST&month=01&step=0`

**主要アクター**: ACT-01〜08, ACT-10〜15（ACT-09 外部契約者は分析画面アクセス不可。spec #2 セクション 2.5 TYPE_3 制限）。さらに `canNavigateForms`（tab011.bit1）が必要（spec #7 SideNav メニュー制御）

---

## 1. ページレイアウト

```
┌──────────────────────────────────────────────────────────────────┐
│ [AppHeader] 保有資源管理 | ユーザー名 | ヘルプ                      │
├────────┬─────────────────────────────────────────────────────────┤
│ [Side  │ [AnalyticsPage]                                         │
│  Nav]  │ ┌──────────────────────────────────────────────────────┐│
│        │ │ [SearchPanel]                                        ││
│ 工数入力│ │ 年度:[2025 ▼] 半期:[上期 ▼] 組織:[IT推進部 ▼]        ││
│ 工数管理│ │ 表示:[工数 / コスト]  絞込:[全部 / 指定 / MY]         ││
│▶分析   │ │                              [検索] [リセット]        ││
│ 設定   │ ├──────────────────────────────────────────────────────┤│
│        │ │ [TabView]  [半期推移] | [月別内訳]                    ││
│        │ │                                                      ││
│        │ │ (月別内訳タブ選択時: 月セレクタ表示)                    ││
│        │ │ 月: [01月 ▼]                                         ││
│        │ ├──────────────────────────────────────────────────────┤│
│        │ │ [Breadcrumb]                                         ││
│        │ │ 分類別 > 障害対応 / 本番障害 > 基幹システム             ││
│        │ ├──────────────────────────────────────────────────────┤│
│        │ │ [Toolbar]                                            ││
│        │ │ [分類別に戻る] [Excel出力]  [テーブル / グラフ]         ││
│        │ ├──────────────────────────────────────────────────────┤│
│        │ │ [AnalyticsDataTable / AnalyticsChart]                ││
│        │ │                                                      ││
│        │ │ (STEP_0: 分類別集計)                                  ││
│        │ │ |分類1  |分類2    | 1月  | 2月  |...| 6月  | 合計  | ││
│        │ │ |障害   |本番障害 |120:00|130:00|...|110:00|780:00 | ││
│        │ │ |障害   |開発不具合| 80:00| 75:00|...| 90:00|520:00 | ││
│        │ │  (行クリック → STEP_1: システム別ドリルダウン)          ││
│        │ │                                                      ││
│        │ │ (STEP_1: システム別)                                   ││
│        │ │ |★|SYS No|システム名  | 1月  | 2月  |...| 合計  |    ││
│        │ │ |★|SYS001|基幹システム|120:00|130:00|...|780:00 |    ││
│        │ │  (行クリック → STEP_2: サブシステム別ドリルダウン)      ││
│        │ │                                                      ││
│        │ │ [合計行]                                              ││
│        │ └──────────────────────────────────────────────────────┘│
└────────┴─────────────────────────────────────────────────────────┘
```

---

## 2. コンポーネント構成

```
pages/analytics.vue (AnalyticsPage)
├── components/analytics/SearchPanel.vue
├── components/analytics/TabView.vue (PrimeVue TabView)
│   ├── [Tab 0] HalfTrendsTab.vue (半期推移)
│   └── [Tab 1] MonthlyBreakdownTab.vue (月別内訳)
│       └── components/analytics/MonthSelector.vue
├── components/analytics/Breadcrumb.vue
├── components/analytics/Toolbar.vue
├── components/analytics/AnalyticsDataTable.vue
│   ├── (PrimeVue DataTable + 動的列生成)
│   └── components/analytics/cells/MySystemStar.vue
├── components/analytics/AnalyticsChart.vue (P1: GAP-F30-06)
│   ├── BarChart.vue
│   └── LineChart.vue
├── components/common/OrganizationSearchDialog.vue (組織選択)
└── components/common/SubsystemSearchDialog.vue (SS選択: 指定フィルタ用)
```

---

## 3. 状態管理 (Pinia Store)

### 3.1 `stores/analytics.ts`

```typescript
interface AnalyticsState {
  // 検索条件
  fiscalYear: number              // 年度 (例: 2025)
  halfPeriod: 'FIRST' | 'SECOND'  // 上期/下期
  month: string | null            // 月別内訳タブ: "01"〜"12"
  organizationCode: string | null
  organizationName: string | null
  displayMode: 'hours' | 'cost'   // 工数/コスト切替
  filterType: 'all' | 'system' | 'my' // 全部/指定/MY

  // タブ
  activeTab: 'half' | 'monthly'   // 半期推移 / 月別内訳

  // ドリルダウン
  step: 0 | 1 | 2                 // 現在の階層
  drilldownContext: DrilldownContext

  // データ
  rows: AnalyticsRow[]
  grandTotal: MonthlyValues
  monthLabels: string[]           // ["01月", "02月", ...] or ["1月", "2月", ...]

  // MYシステム
  mySystems: string[]             // MYシステム No のリスト

  // 表示モード
  viewMode: 'table' | 'chart'     // テーブル/グラフ切替 (P1)

  // UI
  sort: string
  loading: boolean
  message: StatusMessage | null
}

interface DrilldownContext {
  // STEP_1 用: 選択された分類
  category1?: { code: string; name: string }
  category2?: { code: string; name: string }
  // STEP_2 用: 選択されたシステム
  system?: { systemNo: string; systemName: string }
}

interface AnalyticsRow {
  // STEP_0: 分類別
  category1?: { code: string; name: string }
  category2?: { code: string; name: string }
  // STEP_1: システム別
  systemNo?: string
  systemName?: string
  isMy?: boolean
  // STEP_2: サブシステム別
  subsystemNo?: string
  subsystemName?: string
  aplId?: string

  // 月別データ (M1〜M6)
  months: Record<string, MonthlyValue>
  total: MonthlyValue
}

interface MonthlyValue {
  hours: string       // "120:00"
  minutes: number     // 7200
  cost: number        // 960000
}
```

### 3.2 主要 Actions

| Action | API | US |
|--------|-----|-----|
| `fetchCategories()` | `GET /half-trends/categories` or `/monthly-breakdown/categories` | US-030, US-040 |
| `fetchSystems(cat1, cat2)` | `GET /half-trends/systems` or `/monthly-breakdown/systems` | US-031, US-041 |
| `fetchSubsystems(cat1, cat2, sysNo)` | `GET /half-trends/subsystems` or `/monthly-breakdown/subsystems` | US-032, US-042 |
| `drillDown(row)` | step++ → fetchSystems / fetchSubsystems | US-031, US-041 |
| `drillUp()` | step-- → fetchCategories / fetchSystems | US-036 |
| `goToStep(n)` | Breadcrumb クリックで任意階層に戻る | US-036 |
| `switchTab(tab)` | タブ切替 → 同じ step で再取得 | US-044 |
| `toggleMy(systemNo)` | `POST/DELETE /my-systems` | US-033, US-045 |
| `exportExcel(type?)` | `GET .../export/excel` | US-034, US-043 |
| `changeSort(sortKey)` | sort パラメータ更新 → 再取得 | US-035 |

### 3.3 主要 Getters

| Getter | 用途 |
|--------|------|
| `currentApiBase` | activeTab に応じて `/half-trends` or `/monthly-breakdown` を返す |
| `breadcrumbItems` | step + drilldownContext からパンくずリストを構築 |
| `dynamicColumns` | step に応じた左列定義（分類/システム/サブシステム）を返す |
| `monthColumns` | monthLabels から月別列定義を生成 |
| `filteredRows` | filterType === 'my' の場合、mySystems でフィルタ |
| `canExportExcel` | rows.length > 0 AND canExportHours（tab011.bit0）AND canNavigateForms（tab011.bit1）（spec #9 FIX-E01 準拠） |

---

## 4. コンポーネント詳細仕様

### 4.1 SearchPanel.vue — 検索パネル

| 要素 | コンポーネント | 動作 |
|------|-------------|------|
| 年度 | PrimeVue Dropdown | 2014〜現在年度。変更で再検索 |
| 半期 | PrimeVue Dropdown | 上期 / 下期。変更で再検索 |
| 組織 | PrimeVue InputText (readonly) + OrganizationSearchDialog | 組織選択 → 再検索 |
| 工数/コスト | PrimeVue SelectButton | `displayMode` 切替。テーブルの値列が切り替わる |
| 全部/指定/MY | PrimeVue SelectButton | `filterType` 切替 (FLAG_0/FLAG_1/FLAG_2) |
| 検索 | PrimeVue Button | `fetchCategories()` (STEP_0 にリセット) |
| リセット | PrimeVue Button | 検索条件を初期値に戻す |

**年度半期ルール** (FiscalYearResolver):

| 年度 | 上期 | 下期 | 月ラベル |
|------|------|------|---------|
| 2014年度以前 | 4月〜9月 | 10月〜3月 | ["4月","5月","6月","7月","8月","9月"] |
| 2015年度（特殊） | 4月〜9月 | 10月〜12月（3ヶ月） | ["10月","11月","12月"] |
| 2016年度以降 | 1月〜6月 | 7月〜12月 | ["1月","2月","3月","4月","5月","6月"] |

**フィルタタイプ**:
- **全部** (FLAG_0): 全システムを表示
- **指定** (FLAG_1): SubsystemSearchDialog でシステムを選択してフィルタ
- **MY** (FLAG_2): MYシステム登録済みのシステムのみ表示

### URL パラメータバリデーション

| パラメータ | 形式 | 必須 | 許容値 | 不正値時のフォールバック |
|-----------|------|:----:|--------|----------------------|
| tab | string | No | `half`, `month` | デフォルト `half` |
| year | integer | No | 2014 〜 現在年度 | 現在年度 |
| half | string | No | `FIRST`, `SECOND` | 現在半期 |
| step | integer | No | 0, 1, 2 | 0（分類別）にリセット |
| cat1 | string | step≧1 時 | 有効な分類コード | step=0 にリセット |
| cat2 | string | step≧1 時 | 有効なサブ分類コード | step=0 にリセット |
| systemNo | string | step=2 時 | 有効なシステム番号 | step=1 にリセット |
| month | string | tab=month 時 | `01`〜`12`（半期内） | 半期の先頭月 |
| displayMode | string | No | `hours`, `cost` | `hours` |
| filterType | string | No | `all`, `system`, `my` | `all` |

**バリデーションルール**:
- URL パラメータが不正な場合はフォールバック値を使用し、エラーは表示しない（ユーザビリティ優先）
- step パラメータに対して必要な cat1/cat2/systemNo が不足している場合、step を自動降格する
- 半期範囲外の month が指定された場合、半期の先頭月にリセットする

### 4.2 TabView.vue — タブ切替 (US-044)

PrimeVue `TabView` で半期推移と月別内訳を切替。

```
[半期推移] | [月別内訳]
```

**タブ切替時の動作**:
1. `activeTab` を切替
2. **ドリルダウン階層を維持**: step, drilldownContext を保持
3. 対応する API で再取得（半期推移 ↔ 月別内訳）
4. URL パラメータを更新 (`?tab=half` ↔ `?tab=monthly`)

現行では `HalfSuiiSelectCond` / `MonthUtiwakeSelectCond` をセッションに保存して
タブ遷移時に復元していたが、SPA では Pinia store で状態を共有するため
セッション管理は不要。

**月別内訳タブ固有**: MonthSelector が追加表示される。

### 4.3 MonthSelector.vue — 月セレクタ（月別内訳タブ専用）

月別内訳タブでのみ表示。半期内の月を選択する。

```
月: [01月 ▼]
  選択肢: monthLabels から生成（年度半期ルールに準拠）
  変更時: month を更新 → fetchCategories() / fetchSystems() / fetchSubsystems()
```

### 4.4 Breadcrumb.vue — パンくずナビゲーション

ドリルダウンの現在位置を表示し、上位階層への戻りを提供。

| step | 表示 |
|------|------|
| 0 | `分類別` (非リンク) |
| 1 | `分類別` > `障害対応 / 本番障害` |
| 2 | `分類別` > `障害対応 / 本番障害` > `基幹システム` |

- 各パンくずはクリック可能（上位階層のリンク）
- `goToStep(n)` で対応する階層にジャンプ

### 4.5 Toolbar.vue — ツールバー

| ボタン | 表示条件 | 動作 | US |
|--------|---------|------|-----|
| 分類別に戻る | step > 0 | `goToStep(0)` | US-036 |
| Excel出力 | `canExportExcel`（rows.length > 0 AND canExportHours AND canNavigateForms） | ExcelExportDialog | US-034, US-043 |
| テーブル/グラフ | 常時 | `viewMode` 切替 (P1: GAP-F30-06) | - |

**Excel出力ダイアログ** (月別内訳タブ時):

月別内訳は4種類のテンプレートから選択:

```
┌────────────────────────────────┐
│ Excel出力テンプレート選択        │
│                                │
│ ○ 月別内訳（標準）              │
│ ○ 管理用月別内訳               │
│ ○ 管理用月別内訳（詳細）        │
│ ○ 半期推移                     │
│                                │
│         [出力] [キャンセル]     │
└────────────────────────────────┘
```

半期推移タブ時はテンプレート選択なし（`tmp_HalfSuii.xlsx` 固定。spec #9 GAP-E01: .xls→.xlsx 移行済み）。

### 4.6 AnalyticsDataTable.vue — 分析データテーブル

PrimeVue `DataTable` を使用。左側固定列 + 月別列 + 合計列。
列構成は `step` に応じて動的に変化する。

#### STEP_0: 分類別集計

| # | 列 | field | 幅 | 固定 | 備考 |
|---|-----|-------|-----|:---:|------|
| 1 | 分類1 | `category1.name` | 160px | O | 行クリックでドリルダウン |
| 2 | 分類2 | `category2.name` | 160px | O | （分類2がない場合は非表示） |
| 3〜8 | M1〜M6 | `months.M1`〜`months.M6` | 100px | - | 右寄せ。displayMode で hours/cost 切替 |
| 9 | 合計 | `total` | 100px | - | 右寄せ、太字 |

#### STEP_1: システム別

| # | 列 | field | 幅 | 固定 | 備考 |
|---|-----|-------|-----|:---:|------|
| 1 | ★ | `isMy` | 40px | O | MySystemStar — クリックで MY 登録/解除 |
| 2 | SYS No | `systemNo` | 80px | O | |
| 3 | システム名 | `systemName` | 200px | O | 行クリックでドリルダウン |
| 4〜9 | M1〜M6 | `months.M1`〜`months.M6` | 100px | - | |
| 10 | 合計 | `total` | 100px | - | |

#### STEP_2: サブシステム別

| # | 列 | field | 幅 | 固定 | 備考 |
|---|-----|-------|-----|:---:|------|
| 1 | ★ | `isMy` | 40px | O | MySystemStar（親システムの MY 状態を継承） |
| 2 | SYS No | `systemNo` | 80px | O | |
| 3 | システム名 | `systemName` | 150px | O | |
| 4 | SS No | `subsystemNo` | 80px | O | |
| 5 | SS名 | `subsystemName` | 150px | O | |
| 6〜11 | M1〜M6 | `months.M1`〜`months.M6` | 100px | - | |
| 12 | 合計 | `total` | 100px | - | |

#### 共通仕様

- **合計行**: テーブル最下部に `grandTotal` を太字で表示
- **セル値フォーマット**:
  - 工数モード: `HH:MM` 形式（例: "120:30"）
  - コストモード: 3桁カンマ区切り + "円"（例: "960,000円"）
- **行クリック**: STEP_0/1 で行クリック → `drillDown(row)` でドリルダウン
- **ソート** (US-035): 列ヘッダークリックで昇順/降順トグル
  - ソートキー: BUNRUI1, BUNRUI2, SYS_NO, SYS_NAME, SUB_SYS_NO, SUB_SYS_NAME, M1〜M6, 合計
- **固定列**: CSS `position:sticky` + PrimeVue `frozen` (GAP-F30-02)
- **ゼロ値**: "0:00" or "0円" を薄いグレーで表示（視認性向上）

### 4.7 MySystemStar.vue — MYシステム星マーク (US-033, US-045)

```
表示: ★ (MYシステム登録済み) / ☆ (未登録)
色: ★ = #FFD700 (ゴールド), ☆ = #CCC (グレー)
クリック:
  ☆ → ★: POST /my-systems { systemNo: "SYS001" }
  ★ → ☆: DELETE /my-systems/SYS001
  → 成功時: mySystems リスト更新、星マーク即時切替（楽観的更新）
```

### 4.8 AnalyticsChart.vue — グラフ表示 (P1: GAP-F30-06)

P1 スコープ。テーブルとトグルで切替可能。

#### 棒グラフ (BarChart)

- X軸: 月ラベル (M1〜M6)
- Y軸: 工数 (HH:MM) or コスト (円)
- 系列: STEP_0 は分類別、STEP_1 はシステム別の上位5件 + その他
- ライブラリ: Chart.js (vue-chartjs)

#### 折れ線グラフ (LineChart)

- X軸: 月ラベル
- Y軸: 工数 or コスト
- 系列: 棒グラフと同じ
- 合計ラインを太線で表示

---

## 5. インタラクション仕様

### 5.1 初期表示 (US-030)

```
1. ユーザー: サイドナビから「分析」を選択
2. Frontend: AnalyticsPage マウント
   → URL パラメータ確認 (tab, year, half, step, ...)
   → パラメータなし: デフォルト値設定
     fiscalYear = 現在年度
     halfPeriod = 現在月から判定 (1-6月→FIRST, 7-12月→SECOND)
     activeTab = 'half'
     step = 0
   → fetchCategories()
3. Frontend: GET /half-trends/categories?fiscalYear=2025&halfPeriod=FIRST
4. Backend: 組織スコープフィルタ + 集計クエリ → 分類別結果
5. Frontend: rows / grandTotal / monthLabels を state にセット
   → DataTable レンダリング（分類別集計 + 月別列 + 合計列）
```

### 5.2 ドリルダウン: 分類別 → システム別 (US-031)

```
1. ユーザー: 分類行（例: "障害対応 / 本番障害"）をクリック
2. Frontend: drillDown(row)
   → step = 1
   → drilldownContext.category1 = { code: "01", name: "障害対応" }
   → drilldownContext.category2 = { code: "01-01", name: "本番障害" }
   → fetchSystems("01", "01-01")
3. Frontend: GET /half-trends/systems?fiscalYear=2025&halfPeriod=FIRST
                                      &category1Code=01&category2Code=01-01
4. Backend: 分類コードでフィルタ → システム別結果 + MYシステムフラグ
5. Frontend: rows 更新 → DataTable を STEP_1 列構成で再レンダリング
   → Breadcrumb 更新: "分類別 > 障害対応 / 本番障害"
   → URL 更新: ?tab=half&step=1&cat1=01&cat2=01-01
```

### 5.3 ドリルダウン: システム別 → サブシステム別 (US-032)

```
1. ユーザー: システム行（例: "SYS001 基幹システム"）をクリック
2. Frontend: drillDown(row)
   → step = 2
   → drilldownContext.system = { systemNo: "SYS001", systemName: "基幹システム" }
   → fetchSubsystems("01", "01-01", "SYS001")
3. Frontend: GET /half-trends/subsystems?...&systemNo=SYS001
4. Frontend: rows 更新 → DataTable を STEP_2 列構成で再レンダリング
   → Breadcrumb 更新: "分類別 > 障害対応 / 本番障害 > 基幹システム"
```

### 5.4 ドリルアップ (US-036)

```
方法1: Breadcrumb のリンクをクリック
  → goToStep(n) で対応階層に戻る
  → drilldownContext の不要部分をクリア

方法2: [分類別に戻る] ボタン
  → goToStep(0) で STEP_0 に直接戻る

いずれの場合も検索条件は保持される
```

### 5.5 タブ切替: 半期推移 ↔ 月別内訳 (US-044)

```
1. ユーザー: [月別内訳] タブクリック
2. Frontend: switchTab('monthly')
   → activeTab = 'monthly'
   → step, drilldownContext を保持
   → month が未設定なら monthLabels[0] をデフォルト設定
   → MonthSelector を表示
   → 現在の step に応じた API を呼出:
     step=0: GET /monthly-breakdown/categories?...&month=01
     step=1: GET /monthly-breakdown/systems?...&month=01&category1Code=...
     step=2: GET /monthly-breakdown/subsystems?...&month=01&systemNo=...
3. Frontend: rows 更新 → DataTable リレンダリング
   → URL 更新: ?tab=monthly&month=01&...
```

### 5.6 MYシステム登録/解除 (US-033, US-045)

```
1. ユーザー: システム行の ☆ (未登録) をクリック
2. Frontend: toggleMy("SYS001")
   → 楽観的更新: mySystems に "SYS001" 追加、★ 表示に切替
   → POST /my-systems { systemNo: "SYS001" }
3. Backend: tcz19_my_sys に INSERT (tnt_esqid, sknno)
4. Frontend:
   → 成功: そのまま
   → 失敗: mySystems から削除、☆ に戻す + Toast エラー

解除の場合:
1. ユーザー: ★ (登録済み) をクリック
2. Frontend: toggleMy("SYS001")
   → 楽観的更新: mySystems から "SYS001" 削除、☆ 表示に切替
   → DELETE /my-systems/SYS001
3. Backend: tcz19_my_sys から DELETE
```

### 5.7 Excel 出力 (US-034, US-043)

```
半期推移タブ:
1. ユーザー: [Excel出力] クリック
2. Frontend: 確認ダイアログ (CZ-516)
3. ユーザー: [OK]
4. Frontend: fetch("/api/v1/half-trends/export/excel?fiscalYear=2025&...") + Blob + createObjectURL でダウンロード（spec #9 GAP-E03 準拠）
5. ブラウザ: .xlsx ダウンロード (tmp_HalfSuii テンプレート)

月別内訳タブ:
1. ユーザー: [Excel出力] クリック
2. Frontend: ExcelExportDialog 表示（4種テンプレート選択）
3. ユーザー: テンプレート選択 → [出力]
4. Frontend: fetch("/api/v1/monthly-breakdown/export/excel?type=standard&...") + Blob + createObjectURL でダウンロード（spec #9 GAP-E03 準拠）
   type: standard / management / management-detail / half-trend
5. ブラウザ: .xlsx ダウンロード
```

### 5.8 工数/コスト切替

```
1. ユーザー: SearchPanel の [工数] / [コスト] を切替
2. Frontend: displayMode = 'cost'
   → DataTable の月別列 + 合計列の表示フォーマットが切替:
     工数: "120:30" → コスト: "960,000円"
   → API パラメータに displayMode=cost を付与して再取得
     (Backend でコスト計算が必要な場合)
   → グラフ表示中: Y軸ラベルと値も切替
```

---

## 6. 権限によるUI制御

### 6.1 アクター別 UI 差異

分析画面は `canNavigateForms`（tab011.bit1 = true）のアクターのみアクセス可能。ACT-09（外部契約者）はアクセス不可（spec #2 TYPE_3 制限）。

| UI 要素 | ACT-01 報告担当 | ACT-02 報告管理 | ACT-03 全権管理 | ACT-10 全社 | ACT-13 局 |
|---------|:---:|:---:|:---:|:---:|:---:|
| 画面アクセス | canNavigateForms 必要（tab011.bit1） | O | O | O | O |
| 組織検索範囲 | 自組織のみ | 配下 | 全組織 | 全組織 | 局配下 |
| MYシステム操作 | O | O | O | O | O |
| Excel出力 | canExportHours 必要 | O | O | O | O |
| ドリルダウン | O | O | O | O | O |
| 工数/コスト切替 | canExportHours 必要 | O | O | O | O |

> ACT-01（tab011='00'）は canNavigateForms=false のため、SideNav に「分析」メニューが表示されずアクセス不可。ACT-04/05（tab011='10'）は canNavigateForms=false のため同様にアクセス不可。

### 6.2 データアクセススコープ

集計 API は `dataAuthority.ref` による組織スコープフィルタで制限:
- 集計結果はログインユーザーの参照可能組織の範囲に限定
- 組織選択 Dropdown の選択肢も同スコープで制限

---

## 7. エラーハンドリング

### 7.1 データなし

```
API レスポンス: rows = []
Frontend: DataTable にエンプティメッセージ表示
  「該当する集計データがありません。
   検索条件を変更してください。」
```

### 7.2 MYシステム操作失敗

```
POST/DELETE /my-systems → 500
Frontend:
  1. 楽観的更新をロールバック（★ ↔ ☆ を元に戻す）
  2. Toast で「MYシステムの更新に失敗しました」
```

### 7.3 Excel出力エラー

```
GET .../export/excel → 500
Frontend:
  1. Toast で「Excel出力に失敗しました。再試行してください」
```

### 7.4 ネットワークエラー

```
Frontend:
  1. Toast で「通信エラーが発生しました」
  2. ローディング解除
```

---

## 8. レスポンシブ対応

| ブレークポイント | レイアウト |
|-------------|----------|
| >= 1280px | サイドナビ + フルテーブル（固定列 + 月別6列 + 合計） |
| 960-1279px | サイドナビ折りたたみ + テーブル水平スクロール |
| < 960px | サイドナビ非表示 + テーブル水平スクロール（固定列を最小化） |

固定列（スクロール時も表示）:
- STEP_0: 分類1, 分類2
- STEP_1: ★, システム名
- STEP_2: ★, SS名

---

## 9. GAP 対応マッピング

| GAP ID | 区分 | 本 spec での対応 |
|--------|------|----------------|
| GAP-F30-01 | IMPROVE | セクション 1, 2 — 6画面54フレーム → 1ページ + タブ + Breadcrumb |
| GAP-F30-02 | IMPROVE | セクション 4.6 — CSS sticky + PrimeVue frozen で固定列 |
| GAP-F30-03 | KEEP | セクション 5.2〜5.4 — 3階層ドリルダウン + Breadcrumb ナビ |
| GAP-F30-04 | KEEP | セクション 4.7 — MySystemStar で星マーク登録/解除 |
| GAP-F30-05 | KEEP | セクション 5.7 — 4種 Excel 出力（テンプレート選択ダイアログ） |
| GAP-F30-06 | ADD/P1 | セクション 4.8 — BarChart / LineChart（テーブルとトグル切替） |
| GAP-F30-07 | ADD/P2 | セクション 0 概要 — URL パラメータでドリルダウン状態を反映 |
| GAP-F30-08 | ADD/P3 | P3 スコープ: 前年同期比較は将来対応 |
| GAP-F30-09 | IMPROVE | セクション 4.1 — 工数/コスト切替 + フィルタ条件 URL 永続化 |

---

## 10. テスト要件

### 受け入れ基準（Given-When-Then）

**AC-AN-01: 初期表示**
- Given: canNavigateForms 権限を持つユーザーがログインしている
- When: サイドナビの「分析」をクリックする
- Then: 半期推移タブが選択状態で、現在年度・現在半期の分類別集計テーブル（STEP_0）が表示される

**AC-AN-02: ドリルダウン（分類別→システム別）**
- Given: STEP_0 の分類別集計テーブルが表示されている
- When: 分類行「障害対応 / 本番障害」をクリックする
- Then: STEP_1（システム別）テーブルに切り替わり、Breadcrumb に「分類別 > 障害対応 / 本番障害」が表示され、URL に step=1&cat1=01&cat2=01-01 が反映される

**AC-AN-03: ドリルダウン（システム別→サブシステム別）**
- Given: STEP_1 のシステム別テーブルが表示されている
- When: システム行をクリックする
- Then: STEP_2（サブシステム別）テーブルに切り替わり、Breadcrumb が3階層に更新される

**AC-AN-04: ドリルアップ（Breadcrumb）**
- Given: STEP_2（サブシステム別）が表示されている
- When: Breadcrumb の「分類別」をクリックする
- Then: STEP_0 に戻り、URL パラメータがリセットされる

**AC-AN-05: タブ切替（半期推移→月別内訳）**
- Given: 半期推移タブでデータが表示されている
- When: 「月別内訳」タブをクリックする
- Then: 月別内訳テーブルが表示され、URL に tab=month が反映される。ドリルダウン状態は維持される

**AC-AN-06: MYシステム登録**
- Given: STEP_1 のシステム一覧で未登録システム（☆）が表示されている
- When: ☆マークをクリックする
- Then: 即座に★に変わり（楽観的更新）、API 成功で維持、失敗時は☆に戻り Toast エラー表示

**AC-AN-07: MYシステム解除**
- Given: STEP_1 で登録済みシステム（★）が表示されている
- When: ★マークをクリックする
- Then: 即座に☆に変わり、API で MY システムが削除される

**AC-AN-08: 工数/コスト切替**
- Given: 工数モード（HH:MM 表示）で集計テーブルが表示されている
- When: 表示モードを「コスト」に切り替える
- Then: 全セルがコスト表示（3桁カンマ+円）に切り替わる

**AC-AN-09: 2015年度下期特殊ケース**
- Given: 年度=2015、半期=下期を選択している
- When: 半期推移テーブルを表示する
- Then: M1〜M3 の3列（10月, 11月, 12月）のみ表示される（通常は M1〜M6 の6列）

**AC-AN-10: URL パラメータ同期（ブックマーク）**
- Given: STEP_1 で特定の分類を選択し、月別内訳タブを表示している
- When: 現在の URL をコピーして新しいタブで開く
- Then: 同一の分類・ステップ・タブ状態で画面が復元される

**AC-AN-11: ゼロ値の表示**
- Given: 集計結果に工数ゼロのセルが含まれている
- When: テーブルを表示する
- Then: ゼロ値セルは薄グレー (#CCC) で "0:00" と表示される

**AC-AN-12: データなし**
- Given: 対象年度・半期に集計データが存在しない
- When: 分析画面を表示する
- Then: 「該当する集計データがありません」メッセージが表示される

**AC-AN-13: Excel 出力**
- Given: 分析テーブルにデータが表示されている
- When: [Excel] ボタンをクリックし確認ダイアログで「はい」を選択する
- Then: 対応するテンプレートの Excel ファイルがダウンロードされる

### 10.1 コンポーネント単体テスト (Vitest)

| コンポーネント | テスト内容 |
|-------------|----------|
| SearchPanel | 年度半期 Dropdown 変更、工数/コスト切替、フィルタタイプ切替 |
| TabView | タブ切替 → switchTab 呼出、ドリルダウン階層維持 |
| MonthSelector | 月選択 → 月別内訳タブのみ表示確認、月変更 → 再取得 |
| Breadcrumb | step 0/1/2 での表示内容、パンくずクリック → goToStep |
| AnalyticsDataTable | 3ステップの動的列構成、行クリック → drillDown、ソート |
| MySystemStar | ★/☆ 表示切替、クリック → toggleMy 呼出 |
| AnalyticsChart | (P1) 棒グラフ/折れ線グラフのレンダリング、データ反映 |

### 10.2 Pinia Store テスト (Vitest)

| テスト | 内容 |
|--------|------|
| fetchCategories | API モック → rows / grandTotal / monthLabels 更新 |
| drillDown | step 遷移 + drilldownContext 更新 + API 呼出 |
| drillUp | step 戻り + drilldownContext クリア + API 呼出 |
| goToStep | 任意階層ジャンプ + 不要な context クリア |
| switchTab | activeTab 切替 + 同 step で再取得 |
| toggleMy | 楽観的更新 → API 成功/失敗時のロールバック |
| currentApiBase | activeTab に応じた正しい API パスを返す |
| dynamicColumns | step に応じた列定義の正確性 |
| FiscalYearResolver | 2014以前 / 2015特殊(3ヶ月) / 2016以降の月ラベル生成 |

### 10.3 E2E テスト (Playwright)

| シナリオ | 内容 |
|---------|------|
| 基本フロー | ログイン → 分析画面 → 分類別テーブル表示確認 |
| ドリルダウン | 分類行クリック → システム別 → サブシステム別 → パンくずで戻り |
| タブ切替 | 半期推移 → 月別内訳 → ドリルダウン階層維持確認 |
| MYシステム | ☆クリック → ★に変化 → フィルタ "MY" 選択 → 登録システムのみ表示 |
| 工数/コスト切替 | [コスト] 選択 → テーブル値がカンマ区切り円表示に変化 |
| Excel出力 | 半期推移 Excel → ダウンロード確認。月別内訳 → テンプレート選択 |
| ソート | 列ヘッダークリック → 昇順/降順切替確認 |
| 月選択 | 月別内訳タブ → 月変更 → データ再取得確認 |
| URL状態 | ドリルダウン → URL パラメータ反映 → URL 直接アクセスで状態復元 |
| 年度半期ルール | 2015年度下期 → 月ラベルが3列（10月,11月,12月）のみ |
| 権限 | ACT-01(担当者) → 組織スコープが自組織のみに制限 |
