# Excel 出力 実装計画

## 概要

6 テンプレート (.xlsx) を Apache POI XSSF で生成し、フロントエンドから `useApi.getBlob()` 経由の
Blob ダウンロードで取得する。現行 HSSF (.xls) + 非表示 IFRAME 方式からの完全移行。

---

## アーキテクチャ

### フロントエンド

| 要素 | 説明 |
|------|------|
| `useExcelExport` composable | `useApi.getBlob()` を利用した Blob ダウンロード。raw fetch 禁止 — JWT ヘッダー自動付与のため `useApi` 経由必須 |
| CZ-516 確認ダイアログ | Excel 出力前に確認表示 |
| ボタン状態制御 | ダウンロード中: スピナー + disabled。二重クリック防止 |
| Toast 通知 | 完了時「完了」、失敗時 CZ-315 エラー表示 |
| 権限制御 | `canExportHours` (tab011.bit0)。テンプレート 3/4/5/6 は `canNavigateForms` (tab011.bit1) も必要 |
| 権限エラー | Backend からの 403 は **CZ-308** で処理 |

### バックエンド

#### サービス層

| クラス | 責務 |
|--------|------|
| `ExcelExportService` | Excel 生成統括。パラメータ受付 → DAO 呼出 → Creator 委譲 → `byte[]` 返却 |

#### Excel Creator（4 クラス）

| Creator | テンプレート | 備考 |
|---------|-----------|------|
| `WorkHoursExcelCreator` | #1 工数明細 | 15列定義、合計行あり |
| `WorkStatusExcelCreator` | #2 工数状況一覧 | 検索条件反映 |
| `HalfTrendsExcelCreator` | #3 半期推移 | STEP_0/1/2 で動的列。2015年度下期は3列 |
| `MonthlyBreakdownExcelCreator` | #4 標準 / #5 管理用 / #6 管理詳細 | type パラメータで分岐。#5: STEP階層+動的カテゴリ列(2行ヘッダ)、#6: 25固定列+動的カテゴリ列 |

#### 共通スタイル

| クラス | 設定内容 |
|--------|---------|
| `ExcelStyleHelper` | MS ゴシック 10pt、ヘッダー #D9D9D9 + 太字 + 中央揃え、細線罫線、A4横向き印刷 |

#### エンドポイント一覧

| # | エンドポイント | 主要パラメータ |
|---|---------------|---------------|
| 1 | `GET /api/v1/work-hours/export/excel` | yearMonth, staffId, sort |
| 2 | `GET /api/v1/work-status/export/excel` | yearMonth, organizationCode, statusFilter |
| 3 | `GET /api/v1/half-trends/export/excel` | fiscalYear, halfPeriod, step, category1Code, systemNo |
| 4-6 | `GET /api/v1/monthly-breakdown/export/excel` | type (standard/management/management-detail) |

### データベース

新規テーブルなし。既存 DAO に `findForExport()` メソッド追加。

---

## Constitution Check

| 原則 | 準拠 | 確認内容 |
|------|------|----------|
| I. マイグレーション・ファースト | ✅ | 現行6テンプレート完全踏襲。管理用/管理詳細もレガシーソースから復元済み |
| II. 仕様完全性 | ✅ | テンプレート #5/#6 のレイアウトをレガシーソースから復元し spec.md に定義済み |
| III. Docker-First | ✅ | Apache POI は Java 依存のみ。追加コンテナ不要 |
| IV. TDD | ✅ | Creator 単体テスト → 統合テスト → E2E テスト |
| V. UX-First | ✅ | CZ-516 確認 → スピナー → Toast の3段フィードバック |
| IX. 最適技術選定 | ✅ | Apache POI XSSF は安定ライブラリ |
| X. コード品質 | ✅ | ExcelStyleHelper で共通化、Creator パターンで責務分離 |

---

## 技術的リスク

| リスク | 影響度 | 対策 |
|---|---|---|
| テンプレート #5/#6 のレイアウト | 解決済 | レガシーソース（MonthUtiwakeExcelReportCreator / MonthUtiwakeKanriDetailExcelReportCreator）から復元し spec.md セクション 3.5/3.6 に定義済み |
| 大量データの性能（1000+ 行） | 中 | SXSSFWorkbook（ストリーミング）検討。サイズ・時間計測 |
| `useApi.getBlob()` 未実装 | 中 | spec #7 で useApi に getBlob() 追加が前提 |
| 半期推移の動的列ヘッダー | 低 | 2015年度下期3ヶ月のパラメタライズドテスト |
| ファイル名の文字コード | 低 | RFC 5987 (`filename*=UTF-8''...`) 併記 |

---

## 依存関係

### 依存先

| spec | 依存内容 |
|------|---------|
| #3 core-api-design | REST API エンドポイント基盤 |
| #7 common-components | useApi composable の getBlob() メソッド |
| #1 database-schema | DAO / テーブル定義 |
| #2 auth-infrastructure | 権限チェック（canExportHours, canNavigateForms） |

### 依存元

なし（リーフノード）

---

## 実装規模の見積もり

| カテゴリ | ファイル数 | 概要 |
|---|---|---|
| Backend Service | 1 | ExcelExportService |
| Backend Creator | 4 | 各テンプレート用 Creator |
| Backend Helper | 1 | ExcelStyleHelper |
| Backend Controller 追加 | 4 | 既存 Controller に exportExcel() メソッド追加 |
| Backend DAO 追加 | 4 | findForExport() メソッド追加 |
| Frontend composable | 1 | useExcelExport |
| Frontend UI | 4 | 各画面の Excel ボタン追加 |
| テスト（Backend） | 10 | Creator 単体 + 統合 |
| テスト（Frontend） | 6 | Vitest 2 + Playwright E2E 4 |
| **合計** | **~35** | |

---

## 実装順序

| 順序 | 作業 | 前提 |
|------|------|------|
| 1 | ExcelStyleHelper + 単体テスト | なし |
| 2 | WorkHoursExcelCreator (#1) + テスト | 順序1 |
| 3 | WorkStatusExcelCreator (#2) + テスト | 順序1 |
| 4 | HalfTrendsExcelCreator (#3) + テスト | 順序1 |
| 5 | MonthlyBreakdownExcelCreator (#4) + テスト | 順序1 |
| 6 | ExcelExportService + 統合テスト | 順序2-5 |
| 7 | Controller exportExcel() 追加 | 順序6 |
| 8 | useExcelExport + フロントエンド統合 | 順序7 + #7 getBlob() |
| 9 | E2E テスト | 順序8 |
| 10 | テンプレート #5/#6（管理用/管理詳細） | 順序5（spec.md セクション 3.5/3.6 のレイアウト定義に準拠） |
