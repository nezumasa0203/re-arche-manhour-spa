# Excel 出力 タスク一覧

## 概要
- 総タスク数: 34
- 見積もり合計: 50.5 時間

全画面（FORM_010/020/030/040）からの Excel 出力機能。
Apache POI HSSF (.xls) + 非表示 IFRAME を Apache POI XSSF (.xlsx) + Blob ダウンロードに移行する。
6 テンプレートを 4 つの Creator クラスで実装し、フロントエンドの useExcelExport composable で統一的にダウンロードする。

**参照フロー図**: spec.md セクション 5.1「Excel 出力フロー図」（ユーザー → Frontend → Backend のシーケンス: ボタンクリック → CZ-516 確認 → スピナー → GET /excel → 権限チェック → Excel 生成 → Blob レスポンス → DL 実行 → Toast）

---

## 受け入れ基準トレーサビリティ

| AC | 概要 | 検証タスク |
|----|------|-----------|
| AC-EX-01 | 工数明細 Excel ダウンロード | T-031 |
| AC-EX-02 | Excel ファイル内容の正確性 | T-003, T-004 |
| AC-EX-03 | 空データ時の Excel 出力 | T-003, T-033 |
| AC-EX-04 | 権限不足時の拒否 (403 CZ-308) | T-022, T-033 |
| AC-EX-05 | ダウンロードキャンセル (CZ-516「いいえ」) | T-028 |
| AC-EX-06 | ダウンロード失敗時エラー (CZ-315) | T-026, T-028 |
| AC-EX-07 | Excel 生成エラー (CZ-317) | T-020, T-022 |
| AC-EX-08 | 半期推移 Excel (M1〜M6) | T-010, T-032 |
| AC-EX-09 | 2015年度下期特殊ケース (3列) | T-010 |
| AC-EX-10 | ローディング中ボタン無効化 | T-028 |

---

## タスク一覧

### Phase 1: 共通基盤（ExcelStyleHelper）

- [ ] **T-001**: ExcelStyleHelper 単体テスト作成
  - 種別: テスト
  - 内容: MS ゴシック 10pt、ヘッダー背景色 #D9D9D9 + 太字 + 中央揃え、細線罫線、HH:MM 書式、#,##0 書式、A4 横向き印刷設定など共通スタイルの検証テストを作成
  - 成果物: `src/test/java/com/example/czConsv/excel/ExcelStyleHelperTest.java`
  - 完了条件: テストが Red 状態（コンパイルエラー or 失敗）
  - 依存: なし
  - 見積もり: 1.5 時間

- [ ] **T-002**: ExcelStyleHelper 実装
  - 種別: 実装
  - 内容: Apache POI XSSF の共通スタイルヘルパーを実装。フォント、ヘッダースタイル、データ行スタイル、工数値書式（HH:MM）、コスト値書式（#,##0）、日付書式（YYYY/MM/DD）、罫線、印刷設定（A4 横向き、ヘッダー行繰り返し）を提供
  - 成果物: `src/main/java/com/example/czConsv/excel/ExcelStyleHelper.java`
  - 完了条件: T-001 のテストが全て Green
  - 依存: T-001
  - 見積もり: 1.5 時間

---

### Phase 2: 工数明細テンプレート (#1 WorkHoursExcelCreator)

- [ ] **T-003**: WorkHoursExcelCreator 単体テスト作成
  - 種別: テスト
  - 内容: 工数明細テンプレート（15 列）の検証テスト。タイトル行、サブタイトル行、ヘッダー行（STS/作業日/対象SS 等）、データ行の値・書式、合計行の工数合計、空データ時のヘッダーのみ出力、列幅設定を検証
  - 成果物: `src/test/java/com/example/czConsv/excel/WorkHoursExcelCreatorTest.java`
  - 完了条件: テストが Red 状態
  - 依存: T-002
  - 見積もり: 1.5 時間

- [ ] **T-004**: WorkHoursExcelCreator 実装
  - 種別: 実装
  - 内容: FORM_010 工数明細テンプレートの Excel 生成。15 列定義（No/ステータス/作業日/保守担当所属/保守担当者名/対象SS No/対象SS名/原因SS No/原因SS名/保守カテゴリ/件名/工数/TMR番号/依頼書No/依頼者名）、タイトル行、サブタイトル（対象月・担当者）、合計行、シート名設定
  - 成果物: `src/main/java/com/example/czConsv/excel/WorkHoursExcelCreator.java`
  - 完了条件: T-003 のテストが全て Green
  - 依存: T-003
  - 見積もり: 2 時間

- [ ] **T-005**: WorkHours DAO findForExport メソッドテスト作成
  - 種別: テスト
  - 内容: 工数明細の Excel 出力用データ取得クエリのテスト。yearMonth/staffId/sort パラメータによるフィルタリングとソート順を検証。Testcontainers による PostgreSQL 実環境テスト
  - 成果物: `src/test/java/com/example/czConsv/dao/WorkHoursDaoExportTest.java`
  - 完了条件: テストが Red 状態
  - 依存: なし
  - 見積もり: 1 時間

- [ ] **T-006**: WorkHours DAO findForExport メソッド実装
  - 種別: 実装
  - 内容: 既存 WorkHoursDao に findForExport() メソッドを追加。Doma 2 の 2Way SQL で yearMonth, staffId, sort パラメータに対応するクエリを実装
  - 成果物: `src/main/java/com/example/czConsv/dao/WorkHoursDao.java`, `src/main/resources/META-INF/com/example/czConsv/dao/WorkHoursDao/findForExport.sql`
  - 完了条件: T-005 のテストが全て Green
  - 依存: T-005
  - 見積もり: 1 時間

---

### Phase 3: 工数状況一覧テンプレート (#2 WorkStatusExcelCreator)

- [ ] **T-007**: WorkStatusExcelCreator 単体テスト作成
  - 種別: テスト
  - 内容: 工数状況一覧テンプレートの検証テスト。検索条件（yearMonth/organizationCode/statusFilter）反映、ヘッダー行、データ行、空データ時の出力を検証
  - 成果物: `src/test/java/com/example/czConsv/excel/WorkStatusExcelCreatorTest.java`
  - 完了条件: テストが Red 状態
  - 依存: T-002
  - 見積もり: 1 時間

- [ ] **T-008**: WorkStatusExcelCreator 実装
  - 種別: 実装
  - 内容: FORM_020 工数状況一覧テンプレートの Excel 生成。検索条件をサブタイトルに反映、データ行の出力
  - 成果物: `src/main/java/com/example/czConsv/excel/WorkStatusExcelCreator.java`
  - 完了条件: T-007 のテストが全て Green
  - 依存: T-007
  - 見積もり: 1.5 時間

- [ ] **T-009**: WorkStatus DAO findForExport メソッドテスト・実装
  - 種別: テスト / 実装
  - 内容: 工数状況一覧の Excel 出力用データ取得クエリのテストと実装。yearMonth, organizationCode, statusFilter パラメータ対応
  - 成果物: `src/test/java/com/example/czConsv/dao/WorkStatusDaoExportTest.java`, `src/main/java/com/example/czConsv/dao/WorkStatusDao.java`, 2Way SQL ファイル
  - 完了条件: テストが全て Green
  - 依存: なし
  - 見積もり: 1.5 時間

---

### Phase 4: 半期推移テンプレート (#3 HalfTrendsExcelCreator)

- [ ] **T-010**: HalfTrendsExcelCreator 単体テスト作成
  - 種別: テスト
  - 内容: 半期推移テンプレートの検証テスト。STEP_0（分類1+分類2+月別6列+合計）、STEP_1（MY+SYS No+システム名+月別6列+合計）、STEP_2（SYS No+システム名+SS No+SS名+月別6列+合計）の動的列構成を検証。合計行の太字、2015年度下期の3列特例もパラメタライズドテストで検証
  - 成果物: `src/test/java/com/example/czConsv/excel/HalfTrendsExcelCreatorTest.java`
  - 完了条件: テストが Red 状態
  - 依存: T-002
  - 見積もり: 2 時間

- [ ] **T-011**: HalfTrendsExcelCreator 実装
  - 種別: 実装
  - 内容: FORM_030 半期推移テンプレートの Excel 生成。step パラメータに応じた動的列ヘッダー生成、月ラベル列の動的構成、年度期間ルール（2015年度下期3ヶ月、2016年以降6ヶ月）対応、合計行
  - 成果物: `src/main/java/com/example/czConsv/excel/HalfTrendsExcelCreator.java`
  - 完了条件: T-010 のテストが全て Green
  - 依存: T-010
  - 見積もり: 2 時間

- [ ] **T-012**: HalfTrends DAO findForExport メソッドテスト・実装
  - 種別: テスト / 実装
  - 内容: 半期推移の Excel 出力用データ取得クエリのテストと実装。fiscalYear, halfPeriod, step, category1Code, category2Code, systemNo パラメータ対応
  - 成果物: `src/test/java/com/example/czConsv/dao/HalfTrendsDaoExportTest.java`, DAO および 2Way SQL ファイル
  - 完了条件: テストが全て Green
  - 依存: なし
  - 見積もり: 1.5 時間

---

### Phase 5: 月別内訳テンプレート (#4/5/6 MonthlyBreakdownExcelCreator)

- [ ] **T-013**: MonthlyBreakdownExcelCreator 単体テスト作成（標準 type=standard）
  - 種別: テスト
  - 内容: 月別内訳（標準）テンプレートの検証テスト。半期推移と同構造で特定月1列出力を検証
  - 成果物: `src/test/java/com/example/czConsv/excel/MonthlyBreakdownExcelCreatorTest.java`
  - 完了条件: テストが Red 状態
  - 依存: T-002
  - 見積もり: 1 時間

- [ ] **T-014**: MonthlyBreakdownExcelCreator 単体テスト作成（管理用 type=management）
  - 種別: テスト
  - 内容: 月別内訳（管理用）テンプレートの検証テスト。STEP 階層固定左列（分類/システム名/サブシステム名）+ 保全カテゴリ別動的右列（2行ヘッダ）+ 合計列。DB 値 / 24 変換と #,##0.00 書式を検証
  - 成果物: `src/test/java/com/example/czConsv/excel/MonthlyBreakdownExcelCreatorTest.java`（追記）
  - 完了条件: テストが Red 状態
  - 依存: T-002
  - 見積もり: 1.5 時間

- [ ] **T-015**: MonthlyBreakdownExcelCreator 単体テスト作成（管理詳細 type=management-detail）
  - 種別: テスト
  - 内容: 月別内訳（管理詳細）テンプレートの検証テスト。25 固定列 + 保全カテゴリ動的列。連番、作業日書式、対象/原因システム情報、担当者、件名、ステータス、合計工数、動的列の工数、最終行の SUM 集計行を検証。DB 値 / 24 変換を検証
  - 成果物: `src/test/java/com/example/czConsv/excel/MonthlyBreakdownExcelCreatorTest.java`（追記）
  - 完了条件: テストが Red 状態
  - 依存: T-002
  - 見積もり: 1.5 時間

- [ ] **T-016**: MonthlyBreakdownExcelCreator 実装（標準）
  - 種別: 実装
  - 内容: FORM_040 月別内訳（標準）テンプレートの Excel 生成。半期推移と同構造、特定月のデータのみ1列で出力。type パラメータによる分岐ロジック
  - 成果物: `src/main/java/com/example/czConsv/excel/MonthlyBreakdownExcelCreator.java`
  - 完了条件: T-013 のテストが全て Green
  - 依存: T-013
  - 見積もり: 1.5 時間

- [ ] **T-017**: MonthlyBreakdownExcelCreator 実装（管理用）
  - 種別: 実装
  - 内容: 月別内訳（管理用）テンプレートの Excel 生成。STEP 階層固定3列 + 動的保全カテゴリ列生成（カテゴリ数 x 2 列: 保全/運用）+ 合計列。2行ヘッダ構造、DB 値 / 24 変換
  - 成果物: `src/main/java/com/example/czConsv/excel/MonthlyBreakdownExcelCreator.java`（追記）
  - 完了条件: T-014 のテストが全て Green
  - 依存: T-014, T-016
  - 見積もり: 2 時間

- [ ] **T-018**: MonthlyBreakdownExcelCreator 実装（管理詳細）
  - 種別: 実装
  - 内容: 月別内訳（管理詳細）テンプレートの Excel 生成。25 固定列（No/作業日/対象システム情報/原因システム情報/担当者/件名/依頼書No/依頼者名/TMR No/予備列/ステータス/人事区分/合計工数）+ 動的保全カテゴリ列 + 最終 SUM 集計行。DB 値 / 24 変換
  - 成果物: `src/main/java/com/example/czConsv/excel/MonthlyBreakdownExcelCreator.java`（追記）
  - 完了条件: T-015 のテストが全て Green
  - 依存: T-015, T-017
  - 見積もり: 2 時間

- [ ] **T-019**: MonthlyBreakdown DAO findForExport メソッドテスト・実装
  - 種別: テスト / 実装
  - 内容: 月別内訳の Excel 出力用データ取得クエリのテストと実装。type（standard/management/management-detail）に応じた3種のクエリを実装
  - 成果物: DAO テストクラス、DAO メソッド、2Way SQL ファイル（3種）
  - 完了条件: テストが全て Green
  - 依存: なし
  - 見積もり: 2 時間

---

### Phase 6: ExcelExportService 統合

- [ ] **T-020**: ExcelExportService 統合テスト作成
  - 種別: テスト
  - 内容: ExcelExportService の統合テスト。各 export メソッド（exportWorkHours/exportWorkStatus/exportHalfTrends/exportMonthlyBreakdown）の DAO → Creator → byte[] 生成フローを検証。生成された .xlsx ファイルの妥当性（Apache POI で再読込して内容確認）、ファイルサイズが 1000 行で 1MB 未満であることを検証。**AC-EX-07**: Creator 例外発生時に CZ-317 エラーが返されることを検証
  - 成果物: `src/test/java/com/example/czConsv/service/ExcelExportServiceTest.java`
  - 完了条件: テストが Red 状態
  - 依存: T-004, T-008, T-011, T-016, T-017, T-018
  - 見積もり: 2 時間

- [ ] **T-021**: ExcelExportService 実装
  - 種別: 実装
  - 内容: Excel 生成統括サービスの実装。パラメータ受付 → DAO 呼出 → Creator 委譲 → byte[] 返却。HalfTrendsExportParams / MonthlyBreakdownExportParams パラメータクラスの作成
  - 成果物: `src/main/java/com/example/czConsv/service/ExcelExportService.java`, パラメータクラス
  - 完了条件: T-020 のテストが全て Green
  - 依存: T-020
  - 見積もり: 1.5 時間

---

### Phase 7: Controller エンドポイント

- [ ] **T-022**: Excel 出力 Controller テスト作成
  - 種別: テスト
  - 内容: 4 つの Controller の exportExcel() エンドポイントの MockMvc テスト。正常系のレスポンスヘッダー（Content-Type, Content-Disposition, Cache-Control）検証、**AC-EX-04**: 権限チェック（canExportHours/canNavigateForms）による 403 (CZ-308) 検証、**AC-EX-07**: ExcelExportService 例外時の 500 CZ-317 検証、**API パラメータバリデーション**（spec 4.4 準拠）: yearMonth 形式(YYYY-MM, 2015-04〜現在月)、staffId(6桁)、sort(field:dir)、fiscalYear(2014〜現在年度)、halfPeriod(FIRST/SECOND)、organizationCode(6桁)、displayMode(hours/cost)、step(0/1/2)、type(standard/management/management-detail)、month(01-12, 半期範囲内) の各バリデーション失敗時 400 Bad Request を検証
  - 成果物: `src/test/java/com/example/czConsv/controller/WorkHoursControllerExportTest.java` 等（4ファイル）
  - 完了条件: テストが Red 状態
  - 依存: T-021
  - 見積もり: 2.5 時間

- [ ] **T-023**: Excel 出力 Controller 実装
  - 種別: 実装
  - 内容: 既存 4 Controller（WorkHoursController/WorkStatusController/HalfTrendsController/MonthlyBreakdownController）に exportExcel() メソッドを追加。レスポンスヘッダー設定、権限チェック（canExportHours, canNavigateForms）、エラーハンドリング（CZ-308 権限エラー、CZ-317 Excel 生成エラー）、**API パラメータバリデーション**（spec 4.4 準拠: yearMonth/staffId/sort/fiscalYear/halfPeriod/organizationCode/displayMode/step/type/month の形式・範囲チェック、不正時 400 Bad Request）
  - 成果物: 既存 Controller 4 ファイルに exportExcel() メソッド追加
  - 完了条件: T-022 のテストが全て Green
  - 依存: T-022
  - 見積もり: 2 時間

---

### Phase 8: フロントエンド共通基盤

- [ ] **T-024**: useApi.getBlob() メソッドテスト作成
  - 種別: テスト
  - 内容: useApi composable の getBlob() メソッドの Vitest テスト。$fetch の responseType: 'blob' 呼び出し、JWT ヘッダー自動付与、エラーハンドリング（CZ-315）を検証
  - 成果物: `frontend/tests/composables/useApi.getBlob.test.ts`
  - 完了条件: テストが Red 状態
  - 依存: なし
  - 見積もり: 1 時間

- [ ] **T-025**: useApi.getBlob() メソッド実装
  - 種別: 実装
  - 内容: 既存 useApi composable に getBlob() メソッドを追加。$fetch(url, { responseType: 'blob' }) で Blob レスポンスを取得、JWT ヘッダー付与、CZ-315 エラーハンドリング
  - 成果物: `frontend/composables/useApi.ts`（追記）
  - 完了条件: T-024 のテストが全て Green
  - 依存: T-024
  - 見積もり: 0.5 時間

- [ ] **T-026**: useExcelExport composable テスト作成
  - 種別: テスト
  - 内容: useExcelExport composable の Vitest テスト。useApi.getBlob() 呼び出し、Blob → createObjectURL → ダウンロードリンク生成 → revokeObjectURL のフロー、エラー時のハンドリングを検証
  - 成果物: `frontend/tests/composables/useExcelExport.test.ts`
  - 完了条件: テストが Red 状態
  - 依存: T-025
  - 見積もり: 1 時間

- [ ] **T-027**: useExcelExport composable 実装
  - 種別: 実装
  - 内容: useExcelExport composable の実装。download(url, filename) メソッド: useApi.getBlob() で取得 → URL.createObjectURL → リンク生成 → クリック → revokeObjectURL。ローディング状態管理、エラーハンドリング
  - 成果物: `frontend/composables/useExcelExport.ts`
  - 完了条件: T-026 のテストが全て Green
  - 依存: T-026
  - 見積もり: 0.5 時間

---

### Phase 9: フロントエンド UI 統合

- [ ] **T-028**: Excel 出力ボタンコンポーネントテスト作成
  - 種別: テスト
  - 内容: 各画面の Excel 出力ボタンの Vitest テスト。canExportHours による v-if 表示制御、CZ-516 確認ダイアログ表示、**AC-EX-05**: CZ-516 確認ダイアログで「いいえ」選択時にダウンロード未実行・元の画面状態維持を検証、**AC-EX-10**: ダウンロード中のスピナー + disabled 状態・二重クリック防止を検証、完了時 Toast 通知、**AC-EX-06**: エラー時 CZ-315 Toast 表示・スピナー解除を検証
  - 成果物: `frontend/tests/components/ExcelExportButton.test.ts`
  - 完了条件: テストが Red 状態
  - 依存: T-027
  - 見積もり: 1.5 時間

- [ ] **T-029**: Excel 出力ボタンコンポーネント実装
  - 種別: 実装
  - 内容: 共通 ExcelExportButton コンポーネント実装。canExportHours 権限による v-if 制御、クリック時 CZ-516 確認ダイアログ、ダウンロード中スピナー + disabled、完了時「Excel出力が完了しました」Toast、エラー時 CZ-315 表示
  - 成果物: `frontend/components/ExcelExportButton.vue`
  - 完了条件: T-028 のテストが全て Green
  - 依存: T-028
  - 見積もり: 1 時間

- [ ] **T-030**: 各画面への Excel 出力ボタン統合
  - 種別: 実装
  - 内容: FORM_010/020/030/040 の各画面に ExcelExportButton を配置。各テンプレートのエンドポイント URL とファイル名を設定。テンプレート 3/4/5/6 は canNavigateForms 権限も考慮
  - 成果物: 各画面コンポーネント（4ファイル修正）
  - 完了条件: 各画面で Excel 出力ボタンが表示され、クリックで正しいエンドポイントに対してダウンロードが実行される
  - 依存: T-029, T-023
  - 見積もり: 2 時間

---

### Phase 10: E2E テスト

- [ ] **T-031**: 工数明細 Excel ダウンロード E2E テスト
  - 種別: テスト
  - 内容: Playwright E2E テスト。FORM_010 で Excel ボタンクリック → CZ-516 確認ダイアログ OK → .xlsx ダウンロード → ファイル存在確認 → Content-Type 検証
  - 成果物: `frontend/e2e/excel-export-work-hours.spec.ts`
  - 完了条件: テストが Green
  - 依存: T-030
  - 見積もり: 1.5 時間

- [ ] **T-032**: 半期推移・月別内訳 Excel ダウンロード E2E テスト
  - 種別: テスト
  - 内容: Playwright E2E テスト。FORM_030 各 STEP での Excel 出力、FORM_040 テンプレート選択（standard/management/management-detail）→ 各 type の出力確認
  - 成果物: `frontend/e2e/excel-export-trends.spec.ts`
  - 完了条件: テストが Green
  - 依存: T-030
  - 見積もり: 2 時間

- [ ] **T-033**: 空データ・権限エラー E2E テスト
  - 種別: テスト
  - 内容: Playwright E2E テスト。レコード 0 件 → ヘッダーのみの Excel 生成、canExportHours=false のアクターでボタン非表示確認、組織スコープが Excel 出力範囲に反映されることの検証
  - 成果物: `frontend/e2e/excel-export-edge-cases.spec.ts`
  - 完了条件: テストが Green
  - 依存: T-030
  - 見積もり: 1.5 時間

---

### Phase 11: リファクタリング・最終確認

- [ ] **T-034**: コード品質レビューとリファクタリング
  - 種別: リファクタ
  - 内容: 全 Excel 出力コードのレビュー。Creator 間の重複コード共通化、ExcelStyleHelper のカバレッジ確認、エラーハンドリングの統一性確認、Checkstyle / ESLint エラー解消、ファイル名の RFC 5987 対応（filename*=UTF-8''... 併記）
  - 成果物: 既存ファイルのリファクタリング
  - 完了条件: Checkstyle / ESLint エラーなし、テスト全 Green、カバレッジ 80% 以上
  - 依存: T-031, T-032, T-033
  - 見積もり: 2 時間

---

## 依存関係図

```
Phase 1:
T-001 → T-002

Phase 2-4 (Creator):
T-002 → T-003 → T-004
       → T-007 → T-008
       → T-010 → T-011
       → T-013, T-014, T-015 → T-016 → T-017 → T-018
T-005 → T-006 (DAO: 並行可)
T-009 (DAO: 並行可)
T-012 (DAO: 並行可)
T-019 (DAO: 並行可)

Phase 6-7:
T-004 + T-008 + T-011 + T-016 + T-017 + T-018 → T-020 → T-021 → T-022 → T-023

Phase 8-9:
T-024 → T-025 → T-026 → T-027 → T-028 → T-029
T-023 + T-029 → T-030

Phase 10:
T-030 → T-031, T-032, T-033

Phase 11:
T-031 + T-032 + T-033 → T-034
```
