# #6 Analysis Screens — 整合性チェックレポート

**実施日**: 2026-02-26
**対象**: `06-analysis-screens/spec.md`
**チェック対象**: constitution.md, analysis 6ファイル, spec #1-#5/#7-#10

---

## A. Constitution.md との整合性

| チェック項目 | 結果 | 詳細 |
|---|---|---|
| Nuxt.js 3 SPA | ✅ 合致 | constitution 技術スタック「Nuxt.js 3」。spec: pages/analytics.vue |
| PrimeVue UI | ✅ 合致 | constitution 技術スタック「PrimeVue」。spec: DataTable, TabView, Dropdown, SelectButton, Button, Breadcrumb 使用 |
| UX-First | ✅ 合致 | constitution V「操作フィードバック」。spec: Toast エラー通知、ローディング状態、MYシステム楽観的更新 |
| レスポンシブ対応 | ✅ 合致 | constitution V「両対応」。spec: セクション 8 で 3 ブレークポイント定義 |
| フラットデザイン | ✅ 合致 | constitution V「影なし・border 基調」。spec: ゼロ値薄グレー表示、合計行太字 |
| TDD | ✅ 合致 | constitution IV。spec: セクション 10 で Vitest + Playwright テスト要件定義 |
| Docker-First | ✅ 合致 | API 呼出先が Backend コンテナ上の REST API |
| 4層権限モデル | ✅ 合致 | constitution VII。spec: セクション 6 で組織スコープフィルタ、アクター別 UI 差異定義 |
| FiscalYearResolver | ✅ 合致 | constitution I「年度期間ルール 3 件」。spec: セクション 4.1 で 2014以前/2015特殊/2016以降 定義 |

### 指摘事項

なし。constitution の全原則に準拠している。

---

## B. Analysis 6ファイルとの整合性

### 01_system_analysis.md との整合

| 項目 | 分析 | spec | 結果 |
|---|---|---|---|
| 6画面×各9フレーム (計54フレーム) | FORM_030-042 | 1ページ + 2タブ + Breadcrumb に統合 | ✅ IMPROVE (GAP-F30-01) |
| HalfSuiiSelectCond セッション管理 | セッション保存で画面間状態共有 | Pinia store で状態管理 | ✅ IMPROVE |

### 02_actor_definition.md との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| 全アクターアクセス可能 | ✅ 合致 | セクション 6.1 で全 ACT がアクセス可能 |
| 組織スコープ | ✅ 合致 | ACT-01 自組織 / ACT-02 配下 / ACT-03,10 全組織 / ACT-13 局配下 |
| 工数/コスト切替権限 | ⚠️ 矛盾 | `tab011.bit1` (canNavigateForms) としているが、ACT-13 (bit1=0) の O 表示と矛盾 → **FIX-A01** |

### 03_user_stories.md との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| US-030〜US-045 (16件) | ✅ 合致 | セクション 3.2 Actions で全 US に対応 |
| US-033 (MYシステム) | ✅ 合致 | セクション 4.7 MySystemStar + toggleMy |
| US-034/043 (Excel出力) | ✅ 合致 | セクション 5.7 で半期推移+月別内訳 4種 |
| US-035 (ソート) | ✅ 合致 | セクション 4.6 列ヘッダークリック |
| US-036 (ドリルアップ) | ✅ 合致 | セクション 5.4 Breadcrumb + 分類別に戻る |
| US-044 (タブ切替) | ✅ 合致 | セクション 4.2 ドリルダウン階層維持 |

### 04_screen_transition.md との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| SCR-030〜042 | ✅ 合致 | 6画面を 1 SPA ページに統合 |
| 3階層ドリルダウン | ✅ 合致 | STEP_0(分類)→STEP_1(システム)→STEP_2(サブシステム) |
| DLG_001/002 (組織選択) | ✅ 合致 | セクション 2 OrganizationSearchDialog |
| DLG_003/004 (SS選択) | ✅ 合致 | セクション 2 SubsystemSearchDialog（指定フィルタ用） |

### 05_gap_analysis.md との整合

| GAP-ID | 区分 | spec での対応 | 結果 |
|---|---|---|---|
| GAP-F30-01 | IMPROVE | 6画面54フレーム → 1ページ+タブ+Breadcrumb | ✅ セクション 1, 2 |
| GAP-F30-02 | IMPROVE | CSS sticky + PrimeVue frozen 固定列 | ✅ セクション 4.6 |
| GAP-F30-03 | KEEP | 3階層ドリルダウン + Breadcrumb | ✅ セクション 5.2〜5.4 |
| GAP-F30-04 | KEEP | MYシステム星マーク登録/解除 | ✅ セクション 4.7 |
| GAP-F30-05 | KEEP | 4種 Excel出力（テンプレート選択ダイアログ） | ✅ セクション 5.7 |
| GAP-F30-06 | ADD/P1 | BarChart / LineChart（テーブルとトグル切替） | ✅ セクション 4.8 |
| GAP-F30-07 | ADD/P2 | URL パラメータでドリルダウン状態反映 | ✅ セクション 0 概要 |
| GAP-F30-08 | ADD/P3 | 前年同期比較（将来対応） | ✅ P3 スコープ明記 |
| GAP-F30-09 | IMPROVE | 工数/コスト切替 + フィルタ URL 永続化 | ✅ セクション 4.1, 5.8 |

### 06_devenv_infrastructure.md との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| Frontend コンテナ (Nuxt.js 3, :3000) | ✅ 合致 | ページ URL `/analytics` |
| Chart.js | ✅ 合致 | セクション 4.8 で vue-chartjs を使用 |
| ORM 記載 | ℹ️ 注意 | 06_devenv の JPA+Hibernate 記載は既知事項 |

---

## C. 他 Spec (#1-#5, #7-#10) との整合性

### 検出された不整合

| # | 不整合内容 | 重要度 | FIX-ID | 関連 |
|---|---|---|---|---|
| 1 | セクション 6.1 の工数/コスト切替権限が `tab011.bit1` (canNavigateForms) になっている。ACT-13 (tab011='10', bit0=1, bit1=0) は「O」と表記されているが、`tab011.bit1` 条件では表示**不可**になるため矛盾。正しくは `tab011.bit0` (canExportHours) | 🟡 P1 | FIX-A01 | #2 auth 7.1, FIX-A07 |

### 不整合の詳細

#### FIX-A01: 工数/コスト切替の権限条件誤り（P1 — 権限バグ）

セクション 6.1 アクター別 UI 差異:
```
| 工数/コスト切替 | tab011.bit1 必要 | O | O | O | O |
```

アクター別 tab011 値（spec #2 Auth Mock 定義）:

| アクター | tab011 | bit0 (canExportHours) | bit1 (canNavigateForms) | 表の値 |
|---|---|---|---|---|
| ACT-01 報告担当 | '00' | 0 | 0 | `tab011.bit1 必要` (不可) |
| ACT-02 報告管理 | '11' | 1 | 1 | O |
| ACT-03 全権管理 | '11' | 1 | 1 | O |
| ACT-10 全社 | '11' | 1 | 1 | O |
| **ACT-13 局** | **'10'** | **1** | **0** | **O** ← bit1=0 なのに O → **矛盾** |

`tab011.bit1` (canNavigateForms = 画面遷移リンク) は FORM_010↔FORM_020 の遷移制御用であり、
コスト表示とは無関係。`tab011.bit0` (canExportHours = 保守H時間出力) がコスト/工数の
表示制御に適切:
- ACT-01 (bit0=0) → 不可 ✓
- ACT-13 (bit0=1) → 可 ✓（表の O と整合）

### 整合している点

| 項目 | 結果 |
|---|---|
| spec #1 (database-schema) — tcz19_my_sys テーブルの INSERT/DELETE が MYシステム操作と整合 | ✅ |
| spec #2 (auth-infrastructure) — OrganizationScopeResolver による組織フィルタが集計 API に適用 | ✅ |
| spec #2 (auth-infrastructure) — FiscalYearResolver の年度半期ルール 3 パターンが整合 | ✅ |
| spec #3 (core-api-design) — GET /half-trends/categories, /systems, /subsystems の API 仕様が整合 | ✅ |
| spec #3 (core-api-design) — GET /monthly-breakdown/categories, /systems, /subsystems が整合 | ✅ |
| spec #3 (core-api-design) — POST/DELETE /my-systems の API 仕様が整合 | ✅ |
| spec #3 (core-api-design) — GET .../export/excel の 6 テンプレートが整合 | ✅ |
| spec #3 (core-api-design) — displayMode パラメータ (hours/cost) が API 仕様と整合 | ✅ |
| spec #3 (core-api-design) — filterType パラメータ (all/system/my) が API 仕様と整合 | ✅ |
| spec #5 (work-status-list) — FORM_020 との画面レイアウト共通パターン（AppHeader + SideNav）が整合 | ✅ |
| spec #7 (common-components) — OrganizationSearchDialog / SubsystemSearchDialog の共有 | ✅ |
| spec #9 (excel-export) — 6 テンプレート定義 (tmp_HalfSuii 等) が整合 | ✅ |

---

## D. 旧システムとの仕様整合性・変更点まとめ

### KEEP（踏襲）— 変更なし

| # | 項目 | 旧 | 新 | 根拠 |
|---|---|---|---|---|
| 1 | 3階層ドリルダウン | 分類→システム→サブシステム | 同一の 3 階層 | GAP-F30-03 |
| 2 | MYシステム星マーク | ★/☆ トグル | 同一のトグル操作 | GAP-F30-04 |
| 3 | 4種 Excel出力 | 月別内訳4テンプレート | 同一のテンプレート選択 | GAP-F30-05 |
| 4 | 年度半期ルール | 2014以前/2015特殊/2016以降 | FiscalYearResolver で同一ロジック | — |
| 5 | 工数/コスト切替 | 表示モード切替 | 同一の切替 | GAP-F30-09 |
| 6 | 分類1/分類2 構造 | 2階層カテゴリ構造 | 同一の構造 | — |
| 7 | 合計行 | テーブル最下部に表示 | 同一の表示 | — |
| 8 | ソート | サーバーサイドソート | API sort パラメータに移行 | — |

### IMPROVE（改善）— 技術的改善

| # | 項目 | 旧 (Java MPA) | 新 (Nuxt.js 3 SPA) | 影響度 | GAP-ID |
|---|---|---|---|---|---|
| 1 | **画面構成** | 6画面×9フレーム = 54フレーム | 1ページ + 2タブ + Breadcrumb | 🔴 HIGH | GAP-F30-01 |
| 2 | **ドリルダウン** | ページ遷移 (030→031→032) | SPA 内の step 遷移 + URL 状態管理 | 🔴 HIGH | GAP-F30-03 |
| 3 | **タブ切替** | 別画面遷移 (030↔040) + セッション保存 | TabView + Pinia store 状態共有 | 🟡 MID | — |
| 4 | **固定列** | FRAMESET による物理分割 | CSS sticky + PrimeVue frozen | 🟡 MID | GAP-F30-02 |
| 5 | **フィルタ条件** | セッション保存 | URL パラメータ永続化 | 🟢 LOW | GAP-F30-07/09 |

### ADD（追加）

| # | 項目 | 詳細 | 根拠 |
|---|---|---|---|
| 1 | **Breadcrumb ナビ** | ドリルダウン位置の視覚的表示 + クリックで任意階層に戻り | UX 改善 |
| 2 | **URL 状態管理** | ドリルダウン状態が URL パラメータに反映、直接アクセスで復元 | GAP-F30-07 |
| 3 | **グラフ表示** (P1) | Chart.js (vue-chartjs) で棒グラフ・折れ線グラフ | GAP-F30-06 |
| 4 | **MYシステム楽観的更新** | ★/☆ トグル即時反映 → API 失敗時ロールバック | UX 改善 |
| 5 | **Vitest + Playwright** | コンポーネント単体 + Store + E2E テスト | TDD |
| 6 | **動的列構成** | step に応じて DataTable 列を動的切替 | SPA 設計 |

### REMOVE（削除）

| # | 項目 | 旧の概要 | 削除理由 |
|---|---|---|---|
| 1 | **54フレーム構成** | 各画面9フレーム × 6画面 | 1 SPA ページに統合 |
| 2 | **HalfSuiiSelectCond セッション** | 検索条件のセッション保存/復元 | Pinia store + URL パラメータに置換 |
| 3 | **ページ遷移型ドリルダウン** | 030→031→032 の画面遷移 | SPA 内の step 遷移に置換 |

### 注意が必要な移行ポイント

| # | ポイント | 詳細 |
|---|---|---|
| 1 | **2015年度特殊ルール** | 下期が3ヶ月（10月〜12月）のため、月別列が3列になる。DataTable の動的列生成でこのケースを考慮する必要あり |
| 2 | **MYシステムの楽観的更新** | 旧: ページリロードで反映。新: 即時反映 + 失敗時ロールバック。ネットワーク遅延時の UX テストが必要 |
| 3 | **Excel出力テンプレート** | 旧テンプレート (.xls) と新テンプレート (.xlsx) のフォーマット差異検証 |

---

## E. 推奨アクション

> **全1件完了** ✅（P1: 1件）

| ID | 優先度 | ステータス | アクション | 修正箇所 |
|---|---|---|---|---|
| FIX-A01 | P1 | ✅ 完了 | 工数/コスト切替の権限条件を `tab011.bit1` (canNavigateForms) → `canExportHours` (tab011.bit0) に修正 | spec #6 セクション 6.1 |

---

## F. 修正履歴

| 日時 | FIX-ID | 修正内容 | 修正者 |
|---|---|---|---|
| 2026-02-26 | FIX-A01 | セクション 6.1 の工数/コスト切替権限条件を `tab011.bit1 必要` → `canExportHours 必要` に修正。`tab011.bit1` (canNavigateForms = 画面遷移リンク) はコスト表示とは無関係。`tab011.bit0` (canExportHours = 保守H時間出力) が ACT-13 (bit0=1, bit1=0) の O 表示と整合する正しい条件 | Claude |
