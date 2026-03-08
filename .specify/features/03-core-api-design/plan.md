# コア API 設計 実装計画

## 概要

現行 CZ（保有資源管理システム）の 88 Unit / 139 JSP による MPA アーキテクチャを、
約 40 の RESTful エンドポイント（8 コントローラー）に集約する。
Spring Boot 3.4 + Doma 2 による Backend REST API と、
Nuxt.js 3 の `useApi` composable によるフロントエンド API 連携層を設計・実装する。

spec.md で定義された全エンドポイント（セクション 2〜3）、ビジネスルール（セクション 4）、
データアクセス制御（セクション 5）、同時編集制御（セクション 6）、Excel 出力（セクション 7）を
段階的に実装する。

---

## アーキテクチャ

### フロントエンド

#### useApi composable

全 API 呼出を一元管理する composable を実装する。

- **JWT ヘッダー自動付与**: `Authorization: Bearer <JWT>` を全リクエストに付与
- **代行モードヘッダー**: 代行時は `X-Delegation-Staff-Id` を自動付与
- **メソッド**: `get()`, `post()`, `patch()`, `delete()`, `getBlob()`（Excel 出力用）
- **エラーハンドリング**: spec #8 連携（CzBusinessException → Toast / Overlay）
  - CZ-100〜299: フィールドレベルエラー → セル赤枠 + ツールチップ
  - CZ-300〜499: システムエラー → エラーオーバーレイ
  - CZ-500〜799: 確認ダイアログ → モーダル表示
  - CZ-800〜999: 情報メッセージ → Toast 通知
- **リトライ**: ネットワークエラー時に最大 3 回リトライ（指数バックオフ）
- **409 Conflict 処理**: CZ-101 楽観ロック競合 → 再読込確認ダイアログ

#### API レスポンス型定義（TypeScript）

```typescript
// 共通レスポンス
interface ApiResponse<T> {
  data: T
  meta?: { totalCount?: number; page?: number; pageSize?: number }
}

// エラーレスポンス
interface ApiError {
  code: string        // CZ-000〜999
  message: string
  field?: string
  params?: string[]
  recordId?: number   // バッチ操作時
}
```

### バックエンド

#### Controller 層（8 コントローラー）

| コントローラー | ベースパス | エンドポイント数 | 主要 US |
|--------------|----------|:--------------:|---------|
| AuthController | `/api/v1/auth` | 2 | US-050 |
| WorkHoursController | `/api/v1/work-hours` | 10 | US-010〜01A |
| WorkStatusController | `/api/v1/work-status` | 7 | US-020〜027 |
| HalfTrendsController | `/api/v1/half-trends` | 4 | US-030〜034 |
| MonthlyBreakdownController | `/api/v1/monthly-breakdown` | 5 | US-040〜043 |
| MySystemController | `/api/v1/my-systems` | 3 | US-033, US-045 |
| MasterController | `/api/v1/masters` | 7 | DLG_001〜005 |
| DelegationController | `/api/v1/delegation` | 2 | US-01C |

各コントローラーの責務:
- リクエストパラメータの受信とバインド
- `@PreAuthorize` による権限チェック（spec #2 の CzPermissions 利用）
- Service 呼出とレスポンス DTO 変換
- 例外は `@ControllerAdvice` でグローバルハンドリング

#### Service 層

| サービス | 責務 |
|---------|------|
| WorkHoursService | 工数 CRUD、コピー、翌月転写、一括確認/戻し |
| WorkStatusService | 工数状況検索、インライン編集、承認/戻し |
| MonthlyControlService | 月次確認/集約/未確認戻し（SELECT FOR UPDATE 排他） |
| HalfTrendsService | 3 階層ドリルダウン集計（分類別→システム別→サブシステム別） |
| MonthlyBreakdownService | 月別内訳 3 階層集計 |
| MySystemService | MY システム登録/解除/一覧 |
| MasterService | マスタ参照（組織/システム/サブシステム/担当者/カテゴリ） |
| ValidationService | VR-001〜015 バリデーション + 禁止ワード + バイト長計算 |
| FiscalYearResolver | 年度半期解決（2014以前 / 2015特殊 / 2016以降） |
| ExcelExportService | Apache POI (XSSF) による 6 テンプレート出力 |
| OrganizationScopeResolver | 組織スコープフィルタ（JWT dataAuthority → 許可組織リスト） |
| StatusMatrixResolver | 12 状態 × 2 系列のボタン制御判定（spec #2 で実装済み） |

#### Repository 層（Doma 2 DAO + 2Way SQL）

| DAO | 対応テーブル | 主要クエリ |
|-----|-----------|----------|
| WorkHoursDao | tcz01_hosyu_kousuu | 月別取得、日次合計、ステータス一括更新 |
| WorkStatusDao | tcz01_hosyu_kousuu (集約ビュー) | 組織別検索、ステータスフィルタ |
| ControlDao | mcz04_ctrl | SELECT FOR UPDATE、月次制御更新 |
| HalfTrendsDao | tcz01 + tcz13 + tcz14 | 半期集計（分類/システム/サブシステム軸） |
| MonthlyBreakdownDao | tcz01 + tcz13 + tcz14 | 月別集計（4 種の集計パターン） |
| MySystemDao | tcz19_my_sys | ユーザー別 MY システム管理 |
| OrganizationDao | mcz12_orgn_kr | 組織ツリー、階層検索 |
| SubsystemDao | mav03_subsys + mav01_sys | システム/サブシステム検索 |
| StaffDao | tcz16_tnt_busyo_rireki | 担当者検索、代行可能者一覧 |
| CategoryDao | mcz02_hosyu_kategori | 年度別カテゴリ取得 |

#### DTO 層（リクエスト / レスポンス）

**リクエスト DTO**（Java Record で実装）:
- WorkHoursCreateRequest, WorkHoursUpdateRequest
- WorkHoursCopyRequest, WorkHoursTransferRequest
- BatchConfirmRequest, BatchRevertRequest
- ApproveRequest, RevertRequest
- MonthlyControlRequest（確認/集約/未確認共通）
- DelegationSwitchRequest
- MySystemCreateRequest

**レスポンス DTO**:
- WorkHoursListResponse（records + summary + permissions + monthControl）
- WorkHoursUpdateResponse（field + oldValue + newValue + summary）
- WorkStatusListResponse（records + monthlyControl + permissions + meta）
- HalfTrendsResponse（rows + grandTotal + monthLabels）
- MonthlyBreakdownResponse
- MySystemListResponse
- MasterListResponse（汎用ページネーション付き）
- DelegationResponse
- ErrorResponse（code + message + field + params + recordId）

#### FiscalYearResolver

年度・半期の解決ロジック:

| 年度 | 上期 | 下期 | 月数 |
|------|------|------|:----:|
| 2014 以前 | 4月〜9月 | 10月〜3月 | 6 |
| 2015（特殊） | 4月〜9月 | 10月〜12月 | 3 |
| 2016 以降 | 1月〜6月 | 7月〜12月 | 6 |

### データベース

#### 使用テーブル（spec #1 の全 16 テーブル）

**トランザクション**: tcz01_hosyu_kousuu, tcz13_subsys_sum, tcz14_grp_key, tcz19_my_sys, tcz16_tnt_busyo_rireki, batch_execution_log

**マスタ**: mcz02_hosyu_kategori, mcz04_ctrl, mav01_sys, mav03_subsys, mcz15_ts_sys, mcz03_apl_bunrui_grp, mcz17_hshk_bunrui_grp, mcz12_orgn_kr, mcz24_tanka, mcz21_kanri_taisyo

#### 2Way SQL テンプレート（共通パターン）

全クエリに適用する共通条件:
- **論理削除フィルタ**: `WHERE delflg = '0'`
- **組織スコープフィルタ**: `WHERE organization_code IN (/* organizationCodes */)`
- **楽観ロック**: `WHERE upddate = /* updatedAt */` を UPDATE 文に付与
- **ソート**: `ORDER BY /* sort */` を動的に構築

---

## Constitution Check

| 原則 | 準拠 | 確認内容 |
|------|------|----------|
| I. マイグレーション・ファースト | ✅ | 88 Unit → 約 40 エンドポイント / 8 コントローラー。CZ-000〜999 エラーコード完全踏襲。12状態マトリクス再現。VR-001〜015 全バリデーション。禁止ワード12語 |
| II. 仕様完全性 | ✅ | spec.md セクション 2〜9 の全エンドポイント・ビジネスルール・テスト要件を計画に反映 |
| III. Docker-First | ✅ | backend コンテナ :8080、PostgreSQL :5432、Redis :6379、auth-mock :8180 で完結 |
| IV. TDD | ✅ | 168パターン（マトリクス）+ VR全ルール + 権限テスト + 楽観ロック + FiscalYearResolver。踏襲ロジック 100% |
| V. UX-First | ✅ | permissions + statusMatrix + summary をレスポンスに含め、UI 側の再計算不要に |
| VI. Production Safety | ✅ | 環境変数・プロファイル分離。dev 専用コードは CI grep 検査 |
| VIII. CI/CD Safety | ✅ | 単体テストは H2、統合テストは Testcontainers + PostgreSQL |
| IX. 最適技術選定 | ✅ | RESTful リソース指向、Doma 2 + 2Way SQL、Java Record DTO |
| X. コード品質 | ✅ | Checkstyle/ESLint 準拠、Conventional Commits |

---

## 技術的リスク

| リスク | 影響度 | 対策 |
|---|---|---|
| 楽観ロックの粒度（セル単位更新 vs レコード単位ロック） | 中 | 現行踏襲でレコード単位ロック採用。セル単位は Phase 4 以降で検討 |
| ソートパラメータの SQL インジェクション | 高 | 許可カラム名ホワイトリスト + SortParser ユーティリティで安全にパース |
| FiscalYearResolver の 2015 年度特殊ケース | 中 | パラメタライズドテストで 2014/2015/2016 全パターン網羅 |
| 一括確認のトランザクション範囲 | 中 | 全バリデーション → 全パス後に一括 UPDATE。`@Transactional` で完全ロールバック |
| 組織スコープフィルタの IN 句性能 | 中 | Redis キャッシュで組織階層保持。IN 句はサブクエリ変換を検討 |
| Excel 出力の OutOfMemoryError | 中 | Apache POI SXSSF（ストリーミングモード）採用、行数上限設定 |

---

## 依存関係

### 依存先

| spec | 内容 |
|---|---|
| #1 database-schema | テーブル定義 16 テーブル、Doma 2 Entity/DAO 層 |
| #2 auth-infrastructure | JWT 解析、CzPermissions、StatusMatrixResolver、OrganizationScopeResolver |

### 依存元

| spec | 依存内容 |
|---|---|
| #4 work-hours-input | WorkHoursController の全エンドポイント |
| #5 work-status-list | WorkStatusController の全エンドポイント |
| #6 analysis-screens | HalfTrendsController, MonthlyBreakdownController |
| #9 excel-export | ExcelExportService、各 export/excel エンドポイント |
| #10 batch-processing | WorkStatusService の月次制御、集計データ参照 |

---

## 実装規模の見積もり

| カテゴリ | ファイル数 | 概要 |
|---|---|---|
| Controllers | 8 | REST API エンドポイント約 40 本 |
| Services | 12 | ビジネスロジック + ユーティリティ |
| DAOs (Doma 2) | 10 | Repository 層 |
| Entities (Doma 2) | 16 | spec #1 の全テーブル対応 |
| Request DTOs | 11 | Java Record |
| Response DTOs | 9 | Java Record |
| 2Way SQL テンプレート | 約 30 | .sql ファイル |
| TypeScript 型定義 | 約 15 | interface / type |
| useApi composable | 1 | フロントエンド API 連携層 |
| テスト | 約 200 | マトリクス 168 + VR 45 + 権限 15 + 楽観ロック 5 + その他 |
| **合計** | **~112** | |

---

## 実装順序

1. **Entity + DAO 層**: spec #1 のテーブル定義に基づくエンティティと基本 CRUD
2. **ValidationService + FiscalYearResolver**: ビジネスルールの核心部分（TDD で先行実装）
3. **WorkHoursService / Controller**: 最も複雑な工数入力 API（10 エンドポイント）
4. **WorkStatusService / Controller**: 工数状況一覧 + 月次制御（7 エンドポイント）
5. **HalfTrendsService / Controller**: 半期推移 3 階層ドリルダウン
6. **MonthlyBreakdownService / Controller**: 月別内訳 3 階層ドリルダウン
7. **MasterController / MySystemController / DelegationController**: マスタ参照・補助機能
8. **ExcelExportService**: 6 テンプレートの Excel 出力
9. **useApi composable + TypeScript 型定義**: フロントエンド API 連携層
10. **統合テスト + 権限テスト**: 全エンドポイントの横断テスト
