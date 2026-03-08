# 分析画面 (FORM_030-042) 実装計画

## 概要

半期推移と月別内訳の2つの分析ビューを、3階層ドリルダウン + タブ切替で提供する分析画面。
現行 MPA の 6画面 × 各9フレーム構成（計54フレーム）を、
Nuxt.js 3 SPA の **1ページ + 2タブ + Breadcrumb ドリルダウン** に統合する。

16 ユーザーストーリー（US-030〜US-045）をカバー。
3段階ドリルダウン（分類別→システム別→サブシステム別）、
工数/コスト表示切替、MYシステム管理、4種Excel出力を SPA 上で再現する。

---

## アーキテクチャ

### フロントエンド

#### ページ構成

| ページ | URL | 説明 |
|---|---|---|
| AnalyticsPage | `/analytics` | 分析画面メイン。URL パラメータでタブ/階層/条件を管理 |

URL パラメータ例:
- `/analytics?tab=half&year=2025&half=FIRST&step=0`
- `/analytics?tab=monthly&year=2025&half=FIRST&month=01&step=1&cat1=01`

#### コンポーネント設計（12件）

| # | コンポーネント | 概要 |
|---|---|---|
| 1 | pages/analytics.vue | ページコンテナ。URL パラメータ同期 |
| 2 | SearchPanel.vue | 検索パネル。年度、半期、組織、工数/コスト、全部/指定/MY |
| 3 | TabView.vue | PrimeVue TabView。半期推移 / 月別内訳 タブ切替 |
| 4 | HalfTrendsTab.vue | 半期推移タブコンテナ |
| 5 | MonthlyBreakdownTab.vue | 月別内訳タブコンテナ + MonthSelector |
| 6 | Breadcrumb.vue | パンくずナビゲーション（分類別 > システム > サブシステム） |
| 7 | Toolbar.vue | 分類別に戻る / Excel出力 / テーブル・グラフ切替 |
| 8 | AnalyticsDataTable.vue | PrimeVue DataTable + 動的列生成（月別M1〜M6） |
| 9 | MySystemStar.vue | MYシステム ★ アイコン（クリックでトグル） |
| 10 | AnalyticsChart.vue | グラフ表示（Phase 1: GAP-F30-06） |
| 11 | (共通) OrganizationSearchDialog | 組織選択 |
| 12 | (共通) SubsystemSearchDialog | システム指定フィルタ用 |

#### 状態管理（Pinia Store）

`stores/analytics.ts`:
- **State**: fiscalYear, halfPeriod, month, organizationCode, displayMode, filterType, activeTab, step, drilldownContext, rows, grandTotal, monthLabels, mySystems, viewMode, sort, loading
- **Actions**: fetchCategories, fetchSystems, fetchSubsystems, drillDown, drillUp, goToStep, switchTab, toggleMy, exportExcel, changeSort
- **Getters**: currentApiBase, breadcrumbItems, dynamicColumns, monthColumns, filteredRows, canExportExcel

#### 3段階ドリルダウン

```
STEP_0: 分類別集計（分類1 + 分類2 × 月別M1〜M6 + 合計）
  ↓ 行クリック
STEP_1: システム別（★MYマーク + システムNo + システム名 × 月別M1〜M6 + 合計）
  ↓ 行クリック
STEP_2: サブシステム別（サブシステムNo + サブシステム名 × 月別M1〜M6 + 合計）
```

- タブ切替時はドリルダウン階層を維持（step + drilldownContext 保持）
- Breadcrumb クリックで任意階層に戻れる
- URL パラメータにドリルダウン状態を反映（ブックマーク/共有可能）

#### 動的列生成

月別列は FiscalYearResolver の結果に基づいて動的に生成:
- 通常: M1〜M6 の6列
- 2015年度下期: M1〜M3 の3列（特殊ケース）

### バックエンド

spec #3 で定義された HalfTrendsController（4エンドポイント）と
MonthlyBreakdownController（5エンドポイント）を使用。
MySystemController（3エンドポイント）も使用。

### データベース

tcz13_subsys_sum、tcz14_grp_key、mcz03_apl_bunrui_grp、mav01_sys、mav03_subsys、tcz19_my_sys を使用（spec #1 定義済み）。
集計データは spec #10 のパターン C 集計バッチ出力。

---

## Constitution Check

| 原則 | 準拠 | 確認内容 |
|------|------|----------|
| I. マイグレーション・ファースト | ✅ | 6画面54フレーム→1ページ2タブ。3段階ドリルダウン完全再現。MYシステム機能踏襲 |
| II. 仕様完全性 | ✅ | ワイヤーフレーム、3階層ドリルダウン、URL パラメータ、動的列、4種Excel、年度半期ルールを定義済み |
| III. Docker-First | ✅ | Nuxt.js 3 フロントエンドコンテナで稼働 |
| IV. TDD | ✅ | ドリルダウン遷移テスト、FiscalYearResolver 全パターン（#3で実装済み）、MYシステムトグルテスト、動的列生成テスト |
| V. UX-First | ✅ | Breadcrumb ドリルダウン、タブ切替でコンテキスト維持、URL パラメータでブックマーク可能 |
| IX. 最適技術選定 | ✅ | PrimeVue TabView + DataTable 標準 API |
| X. コード品質 | ✅ | ESLint/Prettier 準拠 |

---

## 技術的リスク

| リスク | 影響度 | 対策 |
|---|---|---|
| 動的列生成の2015年度下期特殊ケース | 中 | FiscalYearResolver のパラメタライズドテストで3パターン検証 |
| ドリルダウン状態の URL パラメータ同期 | 中 | useRoute/useRouter の watch で双方向同期。テストで検証 |
| 大規模データの集計パフォーマンス（tcz13集計テーブル） | 低 | 集計はバッチ処理済み。DataTable は集計結果の表示のみ |
| タブ切替時のドリルダウン階層維持 | 低 | Pinia Store で step + drilldownContext を共有 |
| MYシステムの★トグルの即座反映 | 低 | Optimistic UI パターン（API 成功前に UI 更新、失敗時にロールバック） |

---

## 依存関係

### 依存先

| spec | 依存内容 |
|------|---------|
| #1 database-schema | tcz13, tcz14, mcz03, mav01, mav03, tcz19 テーブル定義 |
| #2 auth-infrastructure | useAuth, DataAuthority（参照範囲制御）、canNavigateForms |
| #3 core-api-design | HalfTrendsController 4EP + MonthlyBreakdownController 5EP + MySystemController 3EP |
| #7 common-components | AppBreadcrumb, MonthSelector, OrganizationSearchDialog, SubsystemSearchDialog, useApi, useMessage |
| #8 validation-error-system | useNotification（Toast 通知） |
| #10 batch-processing | パターン C 集計バッチの出力データ（tcz13_subsys_sum, tcz14_grp_key） |

### 依存元

| spec | 依存内容 |
|------|---------|
| #9 excel-export | 半期推移/月別内訳 Excel テンプレート (#3〜#6) |

---

## 実装規模の見積もり

| カテゴリ | ファイル数 | 概要 |
|---|---|---|
| ページ | 1 | pages/analytics.vue |
| コンポーネント | 10 | SearchPanel + TabView + 2タブ + Breadcrumb + Toolbar + DataTable + MySystemStar + Chart |
| Pinia Store | 1 | stores/analytics.ts |
| テスト（Vitest） | ~10 | Store アクション + ドリルダウン遷移 + 動的列生成 + Getters + URL パラメータ同期 |
| テスト（Playwright E2E） | ~6 | ドリルダウン3階層、タブ切替、MYシステム、Excel出力、工数/コスト切替、URL パラメータ |
| **合計** | **~28** | |

---

## 実装順序

| 順序 | 対象 | 理由 |
|------|------|------|
| 1 | Pinia Store (analytics.ts) | 全コンポーネントの基盤。テストファースト |
| 2 | SearchPanel + URL パラメータ同期 | 検索の骨格 + ブックマーク機能 |
| 3 | TabView + HalfTrendsTab + MonthlyBreakdownTab | タブ構造 |
| 4 | AnalyticsDataTable + 動的列生成 | メインテーブル。STEP_0 表示 |
| 5 | Breadcrumb + ドリルダウン（STEP_1, STEP_2） | 3階層ナビゲーション |
| 6 | MySystemStar + MYシステム管理 | ★トグル機能 |
| 7 | Toolbar（分類戻り/Excel/グラフ切替） | 補助機能 |
| 8 | E2E テスト | 全ドリルダウンフローの統合検証 |
