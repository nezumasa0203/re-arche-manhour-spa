# 工数状況一覧 (FORM_020) 実装計画

## 概要

管理モードユーザーが配下担当者の工数報告状況を把握し、
月次確認・集約・承認ワークフローを実行する画面。
現行 MPA の左右フレーム分割 + 手動 JS 同期を、
Nuxt.js 3 SPA の PrimeVue DataTable + CSS `position:sticky` 固定列に移行する。

8 ユーザーストーリー（US-020〜US-027）をカバーし、
月次制御フロー（未確認→確認→集約）と
レコード承認フロー（確認→確定→戻し）を SPA 上で再現する。

---

## アーキテクチャ

### フロントエンド

#### ページ構成

| ページ | URL | 説明 |
|---|---|---|
| WorkStatusPage | `/work-status` | 工数状況一覧メイン画面 |

#### コンポーネント設計（8件）

| # | コンポーネント | 概要 |
|---|---|---|
| 1 | pages/work-status.vue | ページコンテナ |
| 2 | SearchPanel.vue | 検索パネル。年月、組織、担当者、ステータスフィルタ |
| 3 | MonthlyControlBar.vue | 月次制御バー。未確認/確認/集約ステータス + 遷移ボタン |
| 4 | Toolbar.vue | ツールバー。承認/戻す/Excel ボタン、全選択/解除 |
| 5 | WorkStatusDataTable.vue | メインテーブル。PrimeVue DataTable + ページネーション |
| 6 | HoursEditCell.vue | インライン工数編集セル（管理者のみ） |
| 7 | Pagination.vue | PrimeVue Paginator ラッパー（50件/ページ） |
| 8 | (共通) OrganizationSearchDialog / StaffSearchDialog | 組織・担当者選択モーダル |

#### 状態管理（Pinia Store）

`stores/workStatus.ts`:
- **State**: records, yearMonth, organizationCode, staffId, statusFilter, includeStatus0, sort, page, pageSize, totalCount, monthlyControl, permissions, selectedIds, loading
- **Actions**: fetchRecords, updateHours, approveRecords, revertRecords, monthlyConfirm, monthlyAggregate, monthlyUnconfirm, changeMonth, changePage, exportExcel
- **Getters**: filteredStatusFilter, canApproveSelected, canRevertSelected, monthlyStatusLevel, isHoursEditable, totalPages

#### 月次制御フロー

```
未確認 (00) ──[確認]──→ 確認済 (10) ──[集約]──→ 集約済 (11)
    ↑                       │                      │
    └──────[未確認]──────────┘                      │
    ↑                                              │
    └──────────────[未確認]─────────────────────────┘
```

- 確認: CZ-510 確認ダイアログ → `monthlyConfirm()` → gjkt_flg=1
- 集約: CZ-511 確認ダイアログ → `monthlyAggregate()` → data_sk_flg=1 + **パターン C 集計バッチ同期実行**
- 未確認: CZ-509 確認ダイアログ → `monthlyUnconfirm()` → 両フラグリセット

### バックエンド

spec #3 で定義された WorkStatusController の 7 エンドポイントを使用。
月次制御操作は `SELECT FOR UPDATE` による排他制御。
月次集約時にパターン C 集計バッチ（spec #10 の #5〜10）が同期実行される。

### データベース

tcz01_hosyu_kousuu、mcz04_ctrl を使用（spec #1 定義済み）。

---

## Constitution Check

| 原則 | 準拠 | 確認内容 |
|------|------|----------|
| I. マイグレーション・ファースト | ✅ | 左右フレーム→PrimeVue DataTable+sticky列。月次制御3段階フロー完全再現。レコード承認/戻しフロー踏襲 |
| II. 仕様完全性 | ✅ | ワイヤーフレーム、月次制御状態遷移、ページネーション、検索条件、権限制御を定義済み |
| III. Docker-First | ✅ | Nuxt.js 3 フロントエンドコンテナで稼働 |
| IV. TDD | ✅ | MonthlyControlBar の状態遷移テスト、承認/戻しフローテスト、ページネーションテスト |
| V. UX-First | ✅ | MonthlyControlBar でステータス可視化、ページネーション（50件/ページ）、ローディング/空データ/エラーの4状態 |
| IX. 最適技術選定 | ✅ | PrimeVue DataTable + Paginator 標準 API |
| X. コード品質 | ✅ | ESLint/Prettier 準拠 |

---

## 技術的リスク

| リスク | 影響度 | 対策 |
|---|---|---|
| 月次集約時のパターン C 集計バッチのレスポンスタイム | 中 | 集計対象データ量を事前調査。ローディング表示でUXを担保 |
| 月次制御の同時操作（複数管理者） | 中 | SELECT FOR UPDATE による排他制御。エラー時は CZ-101 メッセージ |
| 大量レコード（150件+）のページネーション | 低 | サーバーサイドページネーション（50件/ページ） |
| インライン工数編集（管理者のみ）の楽観ロック | 低 | updatedAt による 409 Conflict 検出 |
| ステータスフィルタの初期値（STATUS_0 非表示）のUX | 低 | 「作成中も表示」チェックボックスで明示的に切替 |

---

## 依存関係

### 依存先

| spec | 依存内容 |
|------|---------|
| #1 database-schema | tcz01, mcz04 テーブル定義 |
| #2 auth-infrastructure | useAuth, CzPermissions, OrganizationScopeResolver |
| #3 core-api-design | WorkStatusController の 7 エンドポイント |
| #7 common-components | StatusBadge, MonthSelector, OrganizationSearchDialog, StaffSearchDialog, ConfirmDialog, useApi, useStatusMatrix, useMessage |
| #8 validation-error-system | useValidation (VR-008〜010 工数バリデーション), useNotification |
| #10 batch-processing | パターン C 集計バッチ（月次集約時に同期実行） |

### 依存元

| spec | 依存内容 |
|------|---------|
| #9 excel-export | 工数状況一覧 Excel テンプレート (#2) |

---

## 実装規模の見積もり

| カテゴリ | ファイル数 | 概要 |
|---|---|---|
| ページ | 1 | pages/work-status.vue |
| コンポーネント | 7 | SearchPanel + MonthlyControlBar + Toolbar + DataTable + HoursEditCell + Pagination |
| Pinia Store | 1 | stores/workStatus.ts |
| テスト（Vitest） | ~8 | Store アクション + MonthlyControlBar 状態遷移 + Getters |
| テスト（Playwright E2E） | ~5 | 月次確認→集約フロー、承認/戻し、ページネーション、検索、Excel |
| **合計** | **~22** | |

---

## 実装順序

| 順序 | 対象 | 理由 |
|------|------|------|
| 1 | Pinia Store (workStatus.ts) | 全コンポーネントの基盤。テストファースト |
| 2 | SearchPanel | 検索の骨格（年月、組織、担当者、フィルタ） |
| 3 | MonthlyControlBar | 月次制御フロー（核心機能） |
| 4 | WorkStatusDataTable + HoursEditCell + Pagination | メインテーブル + ページネーション |
| 5 | Toolbar（承認/戻し/Excel） | バッチ操作 |
| 6 | E2E テスト | 全操作フローの統合検証 |
