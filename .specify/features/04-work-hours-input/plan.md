# 工数入力画面 (FORM_010) 実装計画

## 概要

保守担当者が日々の保守工数を入力・管理する画面。
現行 MPA の FRAMESET 4フレーム + TdMask Ajax インライン編集を、
Nuxt.js 3 SPA の単一ページ + PrimeVue DataTable インライン編集に移行する。

13 ユーザーストーリー（US-010〜US-01D）をカバーし、
ドラフトモード → インライン編集 → 一括確認のワークフローを SPA 上で完全再現する。

---

## アーキテクチャ

### フロントエンド

#### ページ構成

| ページ | URL | 説明 |
|---|---|---|
| WorkHoursPage | `/work-hours` | 工数入力メイン画面 |

#### コンポーネント設計（14件）

| # | コンポーネント | 概要 |
|---|---|---|
| 1 | pages/work-hours.vue | ページコンテナ。Store 初期化 + 子コンポーネント配置 |
| 2 | SearchPanel.vue | 検索パネル。担当者選択、年月Dropdown（±12ヶ月）、代行モード表示 |
| 3 | Toolbar.vue | ツールバー。追加/コピー/転写/削除/合計/PJ工数/Excel ボタン |
| 4 | WorkHoursDataTable.vue | メインテーブル。PrimeVue DataTable + 14列定義 + インライン編集 |
| 5 | StatusCell.vue | ステータス Dropdown（常時編集可、0/1/2/9） |
| 6 | DateCell.vue | 作業日 Calendar（STATUS_0 のみ編集可） |
| 7 | SubsystemCell.vue | SS選択セル（モーダル起動） |
| 8 | CategoryCell.vue | カテゴリ Dropdown |
| 9 | SubjectCell.vue | 件名 InputText（128バイト制限） |
| 10 | HoursCell.vue | 工数入力セル（HoursInput 利用） |
| 11 | TextCell.vue | TMR番号/依頼書No 入力 |
| 12 | StatusFooter.vue | 一括確認/一括作成中ボタン + ステータス件数表示 |
| 13 | TransferDialog.vue | 翌月転写ダイアログ（対象月選択） |
| 14 | ProjectSummaryDialog.vue | PJ別工数参照ダイアログ |

#### 状態管理（Pinia Store）

`stores/workHours.ts`:
- **State**: records, summary, yearMonth, staffId, isDaiko, sort, monthControl, permissions, statusMatrix, selectedIds, editingCell, loading
- **Actions**: fetchRecords, createRecord, updateField, deleteRecords, copyRecords, transferNextMonth, batchConfirm, batchRevert, changeMonth, switchDaiko
- **Getters**: canAdd, canCopy, canDelete, canBatchConfirm, canBatchRevert, isEditable

#### インライン編集フロー

```
セルクリック → 編集モード遷移（入力要素表示）→ 入力
→ フォーカスアウト or Enter → フロントバリデーション (useValidation)
→ 成功: PATCH API → レスポンスでセル値 + 合計更新
→ 失敗: エラーメッセージ表示、元の値に復元
```

### バックエンド

spec #3 で定義された WorkHoursController の 10 エンドポイントを使用。
本 spec 固有の Backend 実装はなし（Controller/Service/DAO は #3 で実装済み）。

### データベース

tcz01_hosyu_kousuu（メイン）、mcz04_ctrl、mcz02_hosyu_kategori を使用（spec #1 定義済み）。

---

## Constitution Check

| 原則 | 準拠 | 確認内容 |
|------|------|----------|
| I. マイグレーション・ファースト | ✅ | TdMask→PrimeVue セル編集完全移行。ドラフトモード→一括確認フロー踏襲。14列テーブル定義踏襲 |
| II. 仕様完全性 | ✅ | ワイヤーフレーム、14列定義、インライン編集フロー、ステータスマトリクス連携を定義済み |
| III. Docker-First | ✅ | Nuxt.js 3 フロントエンドコンテナで稼働 |
| IV. TDD | ✅ | Vitest でコンポーネント単体テスト + Playwright E2E テスト。isEditable 判定ロジックの全パターンテスト |
| V. UX-First | ✅ | PrimeVue DataTable インライン編集、即時 PATCH + 合計更新、ローディング/エラー/空データの4状態 |
| VI. Production Safety | ✅ | 代行モード関連は CzPrincipal による認証制御 |
| IX. 最適技術選定 | ✅ | PrimeVue DataTable 標準 API、Pinia Store |
| X. コード品質 | ✅ | ESLint/Prettier 準拠、セル編集コンポーネントの共通パターン化 |

---

## 技術的リスク

| リスク | 影響度 | 対策 |
|---|---|---|
| PrimeVue DataTable のインライン編集パフォーマンス（100+行） | 高 | 仮想スクロール検討。ただし現行は月単位表示で件数は限定的 |
| TdMask→PrimeVue セル編集のUX差異 | 中 | blur/Enter イベントの挙動をテストで検証。Tab キー移動もサポート |
| ドラフトモードのバリデーションタイミング | 中 | 新規追加時はバリデーション省略、一括確認時に全 VR 実行。spec.md 記載の通り |
| 代行モード + 楽観ロックの競合 | 低 | 代行元/代行先の同時編集は 409 Conflict で検出 |
| ステータスマトリクスの FE 実装精度 | 高 | buildStatusKey + resolveStatusMatrix の全パターンテスト（spec #7 で実装済みを利用） |

---

## 依存関係

### 依存先

| spec | 依存内容 |
|------|---------|
| #1 database-schema | tcz01, mcz04, mcz02 テーブル定義 |
| #2 auth-infrastructure | useAuth, CzPermissions, StatusMatrixResolver, 代行モード |
| #3 core-api-design | WorkHoursController の 10 エンドポイント |
| #7 common-components | HoursInput, StatusBadge, MonthSelector, SubsystemSearchDialog, StaffSearchDialog, ConfirmDialog, useApi, useStatusMatrix |
| #8 validation-error-system | useValidation (VR-001〜015), useNotification, CZ メッセージカタログ |

### 依存元

| spec | 依存内容 |
|------|---------|
| #9 excel-export | 工数明細 Excel テンプレート (#1) |

---

## 実装規模の見積もり

| カテゴリ | ファイル数 | 概要 |
|---|---|---|
| ページ | 1 | pages/work-hours.vue |
| コンポーネント | 13 | SearchPanel + Toolbar + DataTable + 7セルコンポーネント + Footer + 2ダイアログ |
| Pinia Store | 1 | stores/workHours.ts |
| テスト（Vitest） | ~10 | Store アクション + isEditable + セルコンポーネント |
| テスト（Playwright E2E） | ~6 | 新規追加→編集→確認フロー、コピー、転写、削除、代行モード、ソート |
| **合計** | **~31** | |

---

## 実装順序

| 順序 | 対象 | 理由 |
|------|------|------|
| 1 | Pinia Store (workHours.ts) | 全コンポーネントの基盤。テストファースト |
| 2 | SearchPanel + Toolbar | 検索・操作の骨格 |
| 3 | WorkHoursDataTable + セルコンポーネント群 | メイン機能。PrimeVue DataTable 設定 |
| 4 | インライン編集フロー | PATCH API 連携 + バリデーション統合 |
| 5 | StatusFooter + 一括確認/一括戻し | バッチ操作 |
| 6 | TransferDialog + ProjectSummaryDialog | 補助機能 |
| 7 | 代行モード統合 | StaffSearchDialog + X-Delegation-Staff-Id |
| 8 | E2E テスト | 全操作フローの統合検証 |
