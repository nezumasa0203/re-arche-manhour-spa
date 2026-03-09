# コア API 設計 タスク一覧

## 概要
- 総タスク数: 66
- 見積もり合計: 106.5 時間

現行 CZ の 88 Unit / 139 JSP による MPA アーキテクチャを、
約 40 の RESTful エンドポイント（8 コントローラー）に集約する。
Spring Boot 3.4 + Doma 2 による Backend REST API と、
Nuxt.js 3 の useApi composable によるフロントエンド API 連携層を実装する。

---

## タスク一覧

### Phase 1: DTO 層（リクエスト / レスポンス）

- [x] **T-001**: リクエスト DTO のテストを作成
  - 種別: テスト
  - 内容: WorkHoursCreateRequest, WorkHoursUpdateRequest, WorkHoursCopyRequest, WorkHoursTransferRequest, BatchConfirmRequest, BatchRevertRequest, ApproveRequest, RevertRequest, MonthlyControlRequest, DelegationSwitchRequest, MySystemCreateRequest の 11 Java Record に対する単体テストを作成。null/空値/境界値のテスト
  - 成果物: `src/test/java/.../dto/request/*RequestTest.java`
  - 完了条件: 全 DTO の生成・等価性テストが Red 状態
  - 依存: なし
  - 見積もり: 1.5 時間

- [x] **T-002**: リクエスト DTO を実装
  - 種別: 実装
  - 内容: 11 Java Record を実装。Jakarta Validation アノテーション（@NotNull, @Size, @Pattern）を付与
  - 成果物: `src/main/java/.../dto/request/*.java`
  - 完了条件: T-001 のテストが全て Green
  - 依存: T-001
  - 見積もり: 1.5 時間

- [x] **T-003**: レスポンス DTO のテストを作成
  - 種別: テスト
  - 内容: WorkHoursListResponse, WorkHoursUpdateResponse, WorkStatusListResponse, HalfTrendsResponse, MonthlyBreakdownResponse, MySystemListResponse, MasterListResponse, DelegationResponse, ErrorResponse の 9 Java Record に対する単体テスト。JSON シリアライゼーション/デシリアライゼーションの検証を含む
  - 成果物: `src/test/java/.../dto/response/*ResponseTest.java`
  - 完了条件: Red 状態
  - 依存: なし
  - 見積もり: 1.5 時間

- [x] **T-004**: レスポンス DTO を実装
  - 種別: 実装
  - 内容: 9 Java Record を実装。records + summary + permissions + monthControl 等のネスト構造を含む
  - 成果物: `src/main/java/.../dto/response/*.java`
  - 完了条件: T-003 のテストが全て Green
  - 依存: T-003
  - 見積もり: 1.5 時間

### Phase 2: FiscalYearResolver（年度半期解決）

- [x] **T-005**: FiscalYearResolver の単体テストを作成
  - 種別: テスト
  - 内容: 年度・半期解決ロジックのパラメタライズドテスト。2014年以前（4-9月/10-3月）、2015年特殊（4-9月/10-12月 3ヶ月のみ）、2016年以降（1-6月/7-12月）の全パターン。月リスト生成、基準日解決、半期判定を含む
  - 成果物: `src/test/java/.../service/FiscalYearResolverTest.java`
  - 完了条件: 2014/2015/2016 の各年度パターンのテストが Red 状態
  - 依存: なし
  - 見積もり: 1.5 時間

- [x] **T-006**: FiscalYearResolver を実装
  - 種別: 実装
  - 内容: `service/FiscalYearResolver.java` を実装。getMonthList(fiscalYear, halfPeriod), resolveFiscalYear(yearMonth), isFirstHalf(yearMonth) メソッド。2015年度下期の3ヶ月特殊ケースを正確に処理
  - 成果物: `src/main/java/.../service/FiscalYearResolver.java`
  - 完了条件: T-005 のテストが全て Green
  - 依存: T-005
  - 見積もり: 1 時間

### Phase 3: ValidationService（VR-001〜015 + 禁止ワード）

- [x] **T-007**: ValidationService の単体テストを作成（VR-001〜009）
  - 種別: テスト
  - 内容: 作業日必須(VR-001)、月内範囲(VR-002)、SS必須(VR-003,004)、カテゴリ必須(VR-005)、件名必須+128バイト(VR-006)、禁止ワード(VR-007)、工数必須(VR-008)、HH:MM+15分単位(VR-009) の境界値テスト
  - 成果物: `src/test/java/.../service/ValidationServiceTest.java`
  - 完了条件: 各ルールの正常値・境界値・異常値テストが Red
  - 依存: なし
  - 見積もり: 2 時間

- [x] **T-008**: ValidationService の単体テストを作成（VR-010〜015）
  - 種別: テスト
  - 内容: 日次合計24h(VR-010)、TMR番号5文字(VR-011)、依頼書No7文字固定(VR-012)、依頼者名40文字(VR-013)、条件付き必須(VR-014)、カテゴリ重複(VR-015) の境界値テスト
  - 成果物: `src/test/java/.../service/ValidationServiceTest.java`（追記）
  - 完了条件: 各ルールの正常値・境界値・異常値テストが Red
  - 依存: なし
  - 見積もり: 1.5 時間

- [x] **T-009**: ValidationService を実装
  - 種別: 実装
  - 内容: VR-001〜015 の全バリデーションロジック、禁止ワード12語チェック、バイト長計算、isInputCheck() 相当の一括バリデーション。禁止ワードリストは application.yml から注入
  - 成果物: `src/main/java/.../service/ValidationService.java`
  - 完了条件: T-007, T-008 のテストが全て Green
  - 依存: T-007, T-008
  - 見積もり: 2 時間

### Phase 4: Service 層（ビジネスロジック）

- [x] **T-010**: WorkHoursService の単体テストを作成
  - 種別: テスト
  - 内容: fetchByMonth, create（ドラフトモード）, updateField, delete, copy, transferNextMonth, batchConfirm, batchRevert の各メソッドのテスト。DAO をモック化。ステータスマトリクス連携、楽観ロック検証を含む
  - 成果物: `src/test/java/.../service/WorkHoursServiceTest.java`
  - 完了条件: 全メソッドの正常系・異常系テストが Red
  - 依存: T-002, T-004, T-009
  - 見積もり: 2.5 時間

- [x] **T-011**: WorkHoursService を実装
  - 種別: 実装
  - 内容: 10 エンドポイントに対応するサービスメソッドを実装。ドラフトモード（バリデーション省略）、一括確認（全 STATUS_0 に isInputCheck 実行 → 最初のエラーで中断）、一括戻し（STATUS_1→0）、コピー（STATUS→0 リセット）、翌月転写（カテゴリ年度不一致時ブランク化）
  - 成果物: `src/main/java/.../service/WorkHoursService.java`
  - 完了条件: T-010 のテストが全て Green
  - 依存: T-010
  - 見積もり: 2.5 時間

- [x] **T-012**: WorkStatusService の単体テストを作成
  - 種別: テスト
  - 内容: search（組織別検索・ステータスフィルタ・ページネーション）, updateHours（インライン編集・楽観ロック）, approve（STATUS_1→2）, revert（STATUS_2→1）のテスト
  - 成果物: `src/test/java/.../service/WorkStatusServiceTest.java`
  - 完了条件: 全メソッドのテストが Red
  - 依存: T-002, T-004
  - 見積もり: 1.5 時間

- [x] **T-013**: WorkStatusService を実装
  - 種別: 実装
  - 内容: 工数状況検索、インライン工数編集（VR-008,009,010 バリデーション + 楽観ロック）、承認/戻し（ステータスマトリクス判定 + 権限チェック canManage/canFullAccess）
  - 成果物: `src/main/java/.../service/WorkStatusService.java`
  - 完了条件: T-012 のテストが全て Green
  - 依存: T-012
  - 見積もり: 1.5 時間

- [x] **T-014**: MonthlyControlService の単体テストを作成
  - 種別: テスト
  - 内容: monthlyConfirm（GetsujiKakuteiFlg=1）, monthlyAggregate（DataSyuukeiFlg=1, 確認済チェック）, monthlyUnconfirm（両フラグ=0 リセット）のテスト。SELECT FOR UPDATE の排他制御検証
  - 成果物: `src/test/java/.../service/MonthlyControlServiceTest.java`
  - 完了条件: 全メソッドのテストが Red
  - 依存: T-002
  - 見積もり: 1 時間

- [x] **T-015**: MonthlyControlService を実装
  - 種別: 実装
  - 内容: MCZ04CTRLMST の SELECT FOR UPDATE でロック取得、月次確認/集約/未確認戻し。canInputPeriod/canAggregate 権限チェック
  - 成果物: `src/main/java/.../service/MonthlyControlService.java`
  - 完了条件: T-014 のテストが全て Green
  - 依存: T-014
  - 見積もり: 1 時間

- [x] **T-016**: HalfTrendsService の単体テストを作成
  - 種別: テスト
  - 内容: getCategories(STEP_0), getSystems(STEP_1), getSubsystems(STEP_2) の3階層ドリルダウン集計テスト。FiscalYearResolver 連携、月別データ構造、grandTotal 計算
  - 成果物: `src/test/java/.../service/HalfTrendsServiceTest.java`
  - 完了条件: 3 STEP のテストが Red
  - 依存: T-004, T-006
  - 見積もり: 1.5 時間

- [x] **T-017**: HalfTrendsService を実装
  - 種別: 実装
  - 内容: 分類別(STEP_0)→システム別(STEP_1)→サブシステム別(STEP_2) の3階層集計。月ラベル動的生成（2015年特殊対応）。displayMode（hours/cost）切替。filterType（all/system/my）フィルタ
  - 成果物: `src/main/java/.../service/HalfTrendsService.java`
  - 完了条件: T-016 のテストが全て Green
  - 依存: T-016
  - 見積もり: 1.5 時間

- [x] **T-018**: MonthlyBreakdownService の単体テストを作成
  - 種別: テスト
  - 内容: getCategories, getSystems, getSubsystems, getDetail の集計テスト。HalfTrends と同構造だが月指定あり
  - 成果物: `src/test/java/.../service/MonthlyBreakdownServiceTest.java`
  - 完了条件: テストが Red
  - 依存: T-004, T-006
  - 見積もり: 1 時間

- [x] **T-019**: MonthlyBreakdownService を実装
  - 種別: 実装
  - 内容: 月別内訳の3階層集計 + 詳細。4種の集計パターン（標準/管理用/管理詳細/月別）
  - 成果物: `src/main/java/.../service/MonthlyBreakdownService.java`
  - 完了条件: T-018 のテストが全て Green
  - 依存: T-018
  - 見積もり: 1 時間

- [x] **T-020**: MySystemService の単体テストを作成
  - 種別: テスト
  - 内容: getMySystemList, registerMySystem, removeMySystem のテスト。ユーザー別 MY システム管理
  - 成果物: `src/test/java/.../service/MySystemServiceTest.java`
  - 完了条件: テストが Red
  - 依存: T-002
  - 見積もり: 0.5 時間

- [x] **T-021**: MySystemService を実装
  - 種別: 実装
  - 内容: tcz19_my_sys テーブルの CRUD
  - 成果物: `src/main/java/.../service/MySystemService.java`
  - 完了条件: T-020 のテストが全て Green
  - 依存: T-020
  - 見積もり: 0.5 時間

- [x] **T-022**: MasterService の単体テストを作成
  - 種別: テスト
  - 内容: getOrganizations, getOrganizationTree, getSystems, getSubsystems, searchStaff, getCategories, getControl のテスト。ページネーション、キーワード検索
  - 成果物: `src/test/java/.../service/MasterServiceTest.java`
  - 完了条件: テストが Red
  - 依存: T-004
  - 見積もり: 1 時間

- [x] **T-023**: MasterService を実装
  - 種別: 実装
  - 内容: 7 マスタ参照メソッド。組織ツリー構築、年度別カテゴリ取得
  - 成果物: `src/main/java/.../service/MasterService.java`
  - 完了条件: T-022 のテストが全て Green
  - 依存: T-022
  - 見積もり: 1 時間

- [x] **T-023b**: ExcelExportService の単体テストを作成
  - 種別: テスト
  - 内容: 6 テンプレート（工数明細, 工数状況一覧, 半期推移, 月別内訳標準, 月別内訳管理用, 月別内訳管理詳細）の Excel 出力テスト。Apache POI SXSSF ストリーミングモード。ヘッダー行、データ行、合計行の構造検証。Content-Type/Content-Disposition ヘッダー検証
  - 成果物: `src/test/java/.../service/ExcelExportServiceTest.java`
  - 完了条件: 6 テンプレートの出力テストが Red
  - 依存: T-004
  - 見積もり: 1.5 時間

- [x] **T-023c**: ExcelExportService を実装
  - 種別: 実装
  - 内容: Apache POI (XSSF/SXSSF) による 6 テンプレートの .xlsx 生成。行数上限設定による OutOfMemoryError 防止。Content-Disposition ヘッダーのファイル名動的生成（例: work_hours_202502.xlsx）
  - 成果物: `src/main/java/.../service/ExcelExportService.java`
  - 完了条件: T-023b のテストが全て Green
  - 依存: T-023b
  - 見積もり: 2 時間

### Phase 5: 2Way SQL テンプレート

- [x] **T-024**: WorkHoursDao 用 2Way SQL テンプレートを作成
  - 種別: 実装
  - 内容: selectByMonth.sql, selectDailyTotal.sql, updateStatusBatch.sql, insertDraft.sql 等の Doma 2 2Way SQL テンプレート。論理削除フィルタ（delflg='0'）、組織スコープフィルタ（IN 句）、楽観ロック（upddate 条件）を共通適用
  - 成果物: `src/main/resources/META-INF/.../dao/WorkHoursDao/*.sql`
  - 完了条件: SQL テンプレートが構文エラーなし
  - 依存: T-002
  - 見積もり: 2 時間

- [x] **T-025**: WorkStatusDao / ControlDao 用 2Way SQL テンプレートを作成
  - 種別: 実装
  - 内容: selectByOrganization.sql（組織別検索 + ステータスフィルタ + ページネーション）, selectForUpdate.sql（MCZ04 排他制御）, updateControl.sql 等
  - 成果物: `src/main/resources/META-INF/.../dao/*.sql`
  - 完了条件: SQL テンプレートが構文エラーなし
  - 依存: T-002
  - 見積もり: 1.5 時間

- [x] **T-026**: HalfTrendsDao / MonthlyBreakdownDao 用 2Way SQL テンプレートを作成
  - 種別: 実装
  - 内容: selectCategorySummary.sql, selectSystemSummary.sql, selectSubsystemSummary.sql の半期集計・月別集計クエリ。tcz01 + tcz13 + tcz14 のJOIN。GROUP BY 分類/システム/サブシステム軸。動的月カラム
  - 成果物: `src/main/resources/META-INF/.../dao/*.sql`
  - 完了条件: SQL テンプレートが構文エラーなし
  - 依存: T-002
  - 見積もり: 2 時間

- [x] **T-027**: マスタ系 DAO 用 2Way SQL テンプレートを作成
  - 種別: 実装
  - 内容: OrganizationDao（ツリー取得・階層検索）、SubsystemDao（キーワード検索・ページネーション）、StaffDao（担当者検索・代行可能者一覧）、CategoryDao（年度別取得）、MySystemDao の SQL テンプレート
  - 成果物: `src/main/resources/META-INF/.../dao/*.sql`
  - 完了条件: SQL テンプレートが構文エラーなし
  - 依存: T-002
  - 見積もり: 1.5 時間

### Phase 6: DAO 統合テスト

- [x] **T-028**: WorkHoursDao の統合テストを作成
  - 種別: テスト
  - 内容: Testcontainers + PostgreSQL でのDAO統合テスト。月別取得、ドラフト挿入、フィールド更新、ステータス一括更新、日次合計集計、論理削除フィルタ、楽観ロック
  - 成果物: `src/test/java/.../repository/WorkHoursDaoIntegrationTest.java`
  - 完了条件: PostgreSQL 実データでテスト Green
  - 依存: T-024
  - 見積もり: 2 時間

- [x] **T-029**: WorkStatusDao / ControlDao の統合テストを作成
  - 種別: テスト
  - 内容: 組織別検索、ステータスフィルタ、ページネーション、SELECT FOR UPDATE 排他制御の検証
  - 成果物: `src/test/java/.../repository/*DaoIntegrationTest.java`
  - 完了条件: PostgreSQL 実データでテスト Green
  - 依存: T-025
  - 見積もり: 1.5 時間

- [x] **T-030**: 集計系 DAO の統合テストを作成
  - 種別: テスト
  - 内容: HalfTrendsDao / MonthlyBreakdownDao の3階層集計クエリの検証。テストデータセットを準備し、分類別/システム別/サブシステム別の集計結果を検証
  - 成果物: `src/test/java/.../repository/*DaoIntegrationTest.java`
  - 完了条件: PostgreSQL 実データでテスト Green
  - 依存: T-026
  - 見積もり: 2 時間

- [x] **T-031**: マスタ系 DAO の統合テストを作成
  - 種別: テスト
  - 内容: OrganizationDao（ツリー構築）、SubsystemDao（キーワード検索）、StaffDao（代行可能者）の検証
  - 成果物: `src/test/java/.../repository/*DaoIntegrationTest.java`
  - 完了条件: PostgreSQL 実データでテスト Green
  - 依存: T-027
  - 見積もり: 1.5 時間

### Phase 7: Controller 層

- [x] **T-032**: SortParser ユーティリティのテストと実装
  - 種別: テスト + 実装
  - 内容: ソートパラメータのパース + SQLインジェクション防止。許可カラム名ホワイトリスト方式。`sort=workDate:asc,status:desc` → ORDER BY 句変換
  - 成果物: `src/main/java/.../util/SortParser.java`, `src/test/java/.../util/SortParserTest.java`
  - 完了条件: ホワイトリスト外カラム指定時に例外。正常パースのテスト Green
  - 依存: なし
  - 見積もり: 1 時間

- [x] **T-033**: WorkHoursController の統合テストを作成
  - 種別: テスト
  - 内容: @WebMvcTest で 10 エンドポイントの統合テスト。GET /work-hours（月指定・ソート）、POST /work-hours（ドラフト/全フィールド）、PATCH /work-hours/{id}（フィールド単位）、DELETE /work-hours、POST /copy, /transfer-next-month, /batch-confirm, /batch-revert、GET /project-summary, /export/excel
  - 成果物: `src/test/java/.../controller/WorkHoursControllerTest.java`
  - 完了条件: 10 エンドポイントの正常系テストが Red
  - 依存: T-011, T-032
  - 見積もり: 2.5 時間

- [x] **T-034**: WorkHoursController を実装
  - 種別: 実装
  - 内容: @RestController。各エンドポイントで @PreAuthorize 権限チェック、リクエストバインド、Service 呼出、レスポンス DTO 変換
  - 成果物: `src/main/java/.../controller/WorkHoursController.java`
  - 完了条件: T-033 のテストが全て Green
  - 依存: T-033
  - 見積もり: 2 時間

- [x] **T-035**: WorkStatusController の統合テストを作成
  - 種別: テスト
  - 内容: 7 エンドポイントの統合テスト。GET /work-status, PATCH /work-status/{id}/hours, POST /approve, /revert, /monthly-confirm, /monthly-aggregate, /monthly-unconfirm
  - 成果物: `src/test/java/.../controller/WorkStatusControllerTest.java`
  - 完了条件: 7 エンドポイントの正常系テストが Red
  - 依存: T-013, T-015
  - 見積もり: 2 時間

- [x] **T-036**: WorkStatusController を実装
  - 種別: 実装
  - 内容: @RestController。各エンドポイントの権限チェック・Service 呼出・DTO 変換
  - 成果物: `src/main/java/.../controller/WorkStatusController.java`
  - 完了条件: T-035 のテストが全て Green
  - 依存: T-035
  - 見積もり: 1.5 時間

- [x] **T-037**: HalfTrendsController の統合テストを作成
  - 種別: テスト
  - 内容: 4 エンドポイント（categories, systems, subsystems, export/excel）の統合テスト
  - 成果物: `src/test/java/.../controller/HalfTrendsControllerTest.java`
  - 完了条件: テストが Red
  - 依存: T-017
  - 見積もり: 1 時間

- [x] **T-038**: HalfTrendsController を実装
  - 種別: 実装
  - 内容: 3 STEP のドリルダウン + Excel 出力エンドポイント
  - 成果物: `src/main/java/.../controller/HalfTrendsController.java`
  - 完了条件: T-037 のテストが全て Green
  - 依存: T-037
  - 見積もり: 1 時間

- [x] **T-039**: MonthlyBreakdownController の統合テストを作成
  - 種別: テスト
  - 内容: 5 エンドポイント（categories, systems, subsystems, detail, export/excel）の統合テスト。Excel 出力は 4 種（type パラメータ: standard/management/management-detail/monthly）
  - 成果物: `src/test/java/.../controller/MonthlyBreakdownControllerTest.java`
  - 完了条件: テストが Red
  - 依存: T-019
  - 見積もり: 1.5 時間

- [x] **T-040**: MonthlyBreakdownController を実装
  - 種別: 実装
  - 内容: 3 STEP + detail + Excel 出力エンドポイント
  - 成果物: `src/main/java/.../controller/MonthlyBreakdownController.java`
  - 完了条件: T-039 のテストが全て Green
  - 依存: T-039
  - 見積もり: 1 時間

- [x] **T-041**: MasterController の統合テストを作成
  - 種別: テスト
  - 内容: 7 エンドポイント（organizations, organizations/tree, systems, subsystems, staff, categories, control）の統合テスト
  - 成果物: `src/test/java/.../controller/MasterControllerTest.java`
  - 完了条件: テストが Red
  - 依存: T-023
  - 見積もり: 1.5 時間

- [x] **T-042**: MasterController を実装
  - 種別: 実装
  - 内容: 7 マスタ参照エンドポイント
  - 成果物: `src/main/java/.../controller/MasterController.java`
  - 完了条件: T-041 のテストが全て Green
  - 依存: T-041
  - 見積もり: 1 時間

- [x] **T-043**: MySystemController / DelegationController の統合テストを作成
  - 種別: テスト
  - 内容: MySystem: GET/POST/DELETE。Delegation: GET /available-staff, POST /switch。代行切替の canDelegate + isAllowedStaff 検証
  - 成果物: `src/test/java/.../controller/*ControllerTest.java`
  - 完了条件: テストが Red
  - 依存: T-021, T-023
  - 見積もり: 1.5 時間

- [x] **T-044**: MySystemController / DelegationController を実装
  - 種別: 実装
  - 内容: MY システム CRUD 3 エンドポイント + 代行 2 エンドポイント
  - 成果物: `src/main/java/.../controller/MySystemController.java`, `DelegationController.java`
  - 完了条件: T-043 のテストが全て Green
  - 依存: T-043
  - 見積もり: 1 時間

- [x] **T-045**: AuthController の統合テストを作成
  - 種別: テスト
  - 内容: GET /auth/me（現在ユーザー情報 + 4層権限）、GET /auth/status-matrix（ステータスマトリクス全量取得）
  - 成果物: `src/test/java/.../controller/AuthControllerTest.java`
  - 完了条件: テストが Red
  - 依存: なし
  - 見積もり: 0.5 時間

- [x] **T-046**: AuthController を実装
  - 種別: 実装
  - 内容: 認証情報 + ステータスマトリクスの 2 エンドポイント。CzPrincipal から権限情報を取得して返却
  - 成果物: `src/main/java/.../controller/AuthController.java`
  - 完了条件: T-045 のテストが全て Green
  - 依存: T-045
  - 見積もり: 0.5 時間

### Phase 8: グローバルエラーハンドリング + WebConfig

- [x] **T-047**: GlobalExceptionHandler のテストを作成
  - 種別: テスト
  - 内容: CzBusinessException → エラーレスポンス変換、OptimisticLockException → 409 + CZ-101、一般 Exception → 500 + CZ-300 のテスト
  - 成果物: `src/test/java/.../config/GlobalExceptionHandlerTest.java`
  - 完了条件: テストが Red
  - 依存: なし
  - 見積もり: 1 時間

- [x] **T-048**: GlobalExceptionHandler を実装
  - 種別: 実装
  - 内容: @RestControllerAdvice。CzBusinessException のコードレンジ → HTTP ステータスマッピング（CZ-1xx→400、CZ-102→403、CZ-3xx→500）。メッセージパラメータ展開
  - 成果物: `src/main/java/.../config/GlobalExceptionHandler.java`
  - 完了条件: T-047 のテストが全て Green
  - 依存: T-047
  - 見積もり: 1 時間

- [x] **T-049**: WebConfig（CORS・JSON設定）を実装
  - 種別: 実装
  - 内容: CORS 設定（frontend:3000 許可）、Jackson の日付形式（ISO 8601）、ObjectMapper カスタマイズ
  - 成果物: `src/main/java/.../config/WebConfig.java`
  - 完了条件: API レスポンスが仕様通りの JSON 形式で返却される
  - 依存: なし
  - 見積もり: 0.5 時間

### Phase 9: フロントエンド API 連携層

- [x] **T-050**: useApi composable のテストを作成
  - 種別: テスト
  - 内容: get/post/patch/delete/getBlob メソッドのテスト。JWT 自動付与、X-Delegation-Staff-Id 代行ヘッダー、エラーインターセプター（401→ログイン、403→Toast、409→再読込確認、500→エラーToast）、リトライ（ネットワークエラー時3回）
  - 成果物: `tests/composables/useApi.test.ts`
  - 完了条件: テストが Red
  - 依存: なし
  - 見積もり: 1.5 時間

- [x] **T-051**: useApi composable を実装
  - 種別: 実装
  - 内容: $fetch ベースの API クライアント。baseURL: /api/v1。JWT ヘッダー自動付与（useAuth 連携）。代行モードヘッダー。CZ エラーコードレンジ別の自動 severity 判定。指数バックオフリトライ。getBlob（Excel 出力用 Blob レスポンス）
  - 成果物: `composables/useApi.ts`
  - 完了条件: T-050 のテストが全て Green
  - 依存: T-050
  - 見積もり: 1.5 時間

- [x] **T-052**: TypeScript API レスポンス型定義を作成
  - 種別: 実装
  - 内容: ApiResponse<T>, ApiError, WorkHoursRecord, WorkHoursSummary, WorkHoursPermissions, MonthControl, WorkStatusRecord, HalfTrendsRow, MonthlyBreakdownRow, SubsystemItem, OrganizationItem, StaffItem, CategoryItem 等の interface/type 約15件
  - 成果物: `types/api.ts`, `types/models.ts`
  - 完了条件: 型定義が TypeScript コンパイルエラーなし
  - 依存: なし
  - 見積もり: 1.5 時間

### Phase 10: 権限テスト

- [x] **T-053**: 15 アクター × 主要操作の権限テストを作成
  - 種別: テスト
  - 内容: ACT-01（報告担当者）〜ACT-15（外部契約者・全社参照）の15アクターで WorkHoursController / WorkStatusController の主要操作（CRUD + 承認 + 月次制御）をテスト。JWT モックで各アクターの権限を設定
  - 成果物: `src/test/java/.../controller/PermissionIntegrationTest.java`
  - 完了条件: 15 アクターの権限パターンテスト Green
  - 依存: T-034, T-036
  - 見積もり: 2.5 時間

- [x] **T-054**: ステータスマトリクス × API 操作のテストを作成
  - 種別: テスト
  - 内容: 12状態 × 2系列 × 7操作 = 168 パターンのパラメタライズドテスト。各パターンで API エンドポイントを呼び出し、操作可否が StatusMatrixResolver の判定と一致することを検証
  - 成果物: `src/test/java/.../controller/StatusMatrixApiTest.java`
  - 完了条件: 168 パターンのテスト Green
  - 依存: T-034, T-036
  - 見積もり: 2 時間

### Phase 10b: 受け入れ基準テスト（AC-API-01〜12）

- [x] **T-054b**: 受け入れ基準 GWT テストを作成（AC-API-01〜12）
  - 種別: テスト
  - 内容: spec.md「受け入れ基準（Given-When-Then）」セクションの 12 シナリオを @SpringBootTest 統合テストとして実装。各 AC に対応する GWT シナリオ:
    AC-API-01: ドラフト作成 → 201 Created + status=0
    AC-API-02: 一括確認でバリデーション不正 → 400 + CZ-126 + recordId + 全件ロールバック
    AC-API-03: 楽観的ロック競合 → 409 + CZ-101
    AC-API-04: STATUS_1→2 承認 → 200 OK
    AC-API-05: STATUS_0→2 不正遷移 → 400 + CZ-106
    AC-API-06: 月次確認 → getsuji_kakutei_flg=1 + 集約トリガー
    AC-API-07: 権限不足（ACT-07, canConfirm=false）→ 403 + CZ-308
    AC-API-08: 代行モードで POST → created_by に代行先 ID
    AC-API-09: 禁止語句「カ層」→ 400 + CZ-141
    AC-API-10: ページネーション → 20件 + totalCount=50 + totalPages=3
    AC-API-11: Excel 出力権限なし → 403 + CZ-308
    AC-API-12: 2015年度下期 → 3ヶ月分のみ返却
  - 成果物: `src/test/java/.../acceptance/CoreApiAcceptanceTest.java`
  - 完了条件: 12 AC シナリオの GWT テストが全て Green
  - 依存: T-034, T-036, T-038, T-040, T-042, T-044, T-046
  - 見積もり: 3 時間

### Phase 10c: エッジケース・境界値テスト

- [x] **T-054c**: エッジケース・境界値テストを作成（25 ケース）
  - 種別: テスト
  - 内容: spec.md「エッジケース・境界値」セクションの 25 ケースをパラメタライズドテストとして実装:
    【工数時間境界】0:00→CZ-129、0:15→正常、23:45→正常、24:00→日次合計検証、24:15→CZ-146
    【件名バイト長境界】127B→正常、128B→正常、129B→CZ-128、全角半角混在128B境界→octet_length 判定
    【一括操作境界】空配列→400、ID 1件→正常、重複 ID→重複除去 or エラー、異ステータス混在→エラー+全件ロールバック
    【ページネーション境界】page=0→1ページ目正規化、page=-1→400、pageSize=0→400、pageSize>totalCount→全件+totalPages=1
    【年月境界】月末日(2/28,2/29)→正常、閏年2/29→正常、上期下期切替月→FiscalYearResolver 正確判定
    【同時操作】2ユーザー同月月次確定→先勝ち+後発409、一括確定中個別編集→楽観ロック競合検出
    【代行境界】自分自身への代行→400、存在しない staffId→400
  - 成果物: `src/test/java/.../acceptance/EdgeCaseBoundaryTest.java`
  - 完了条件: 25 エッジケースの全テストが Green
  - 依存: T-034, T-036, T-044, T-046
  - 見積もり: 4 時間

### Phase 11: 楽観的ロック + 同時編集テスト

- [x] **T-055**: 楽観的ロック競合テストを作成
  - 種別: テスト
  - 内容: PATCH /work-hours/{id} で updatedAt 不一致時に 409 + CZ-101 が返ることを検証。同時更新シナリオ（ユーザーA取得→ユーザーB更新→ユーザーA更新試行→409）
  - 成果物: `src/test/java/.../controller/OptimisticLockTest.java`
  - 完了条件: 競合テスト Green
  - 依存: T-034
  - 見積もり: 1 時間

- [x] **T-056**: 月次制御排他テストを作成
  - 種別: テスト
  - 内容: 月次確認/集約/未確認の同時実行テスト。SELECT FOR UPDATE による排他制御が機能することを検証
  - 成果物: `src/test/java/.../service/MonthlyControlConcurrencyTest.java`
  - 完了条件: 排他制御テスト Green
  - 依存: T-015
  - 見積もり: 1 時間

### Phase 12: 組織スコープフィルタ

- [x] **T-057**: OrganizationScopeResolver のテストを作成
  - 種別: テスト
  - 内容: JWT の dataAuthority + organizationCode から許可組織コードリストを解決するロジック。7階層組織構造の展開テスト。KYOKU/BU/KA の各スコープレベルでの組織展開を検証
  - 成果物: `src/test/java/.../security/OrganizationScopeResolverTest.java`
  - 完了条件: 7 スコープレベルのテストが Red
  - 依存: なし
  - 見積もり: 1 時間

- [x] **T-058**: OrganizationScopeResolver を実装
  - 種別: 実装
  - 内容: mcz12_orgn_kr の7階層テーブルから組織コードリストを解決。Redis キャッシュ連携（キャッシュヒット時はDB問い合わせ省略）。SQL WHERE organization_code IN (...) 生成
  - 成果物: `src/main/java/.../security/OrganizationScopeResolver.java`
  - 完了条件: T-057 のテストが全て Green
  - 依存: T-057
  - 見積もり: 1.5 時間

### Phase 13: リファクタリング + 品質

- [x] **T-059**: Controller 共通処理の抽出
  - 種別: リファクタ
  - 内容: 8 コントローラーの共通パターン（権限チェック、ページネーション処理、ソートパラメータ処理）をベースクラスまたはユーティリティに抽出
  - 成果物: 既存コントローラーファイルのリファクタリング
  - 完了条件: 既存テストが全て Green のまま
  - 依存: T-034〜T-046
  - 見積もり: 1.5 時間

- [x] **T-060**: API ドキュメント自動生成設定
  - 種別: 実装
  - 内容: SpringDoc OpenAPI を設定し、全 40 エンドポイントの API ドキュメントを自動生成。`/swagger-ui.html` でブラウザからアクセス可能に
  - 成果物: `build.gradle`（依存追加）、`src/main/java/.../config/OpenApiConfig.java`
  - 完了条件: Swagger UI で全エンドポイントが表示される
  - 依存: T-034〜T-046
  - 見積もり: 1 時間

- [x] **T-061**: Checkstyle / SpotBugs 準拠確認
  - 種別: テスト
  - 内容: 全 Java ファイルの Checkstyle / SpotBugs チェック。警告ゼロ確認
  - 成果物: Checkstyle / SpotBugs レポート
  - 完了条件: 警告ゼロ
  - 依存: T-059
  - 見積もり: 1 時間

- [x] **T-062**: カバレッジ確認
  - 種別: テスト
  - 内容: JaCoCo によるカバレッジレポート生成。新規コード 80%+、踏襲ロジック（ValidationService, FiscalYearResolver, StatusMatrixResolver 連携）100% を確認
  - 成果物: JaCoCo レポート
  - 完了条件: カバレッジ目標達成
  - 依存: T-054, T-054b, T-054c, T-055, T-056
  - 見積もり: 1 時間

---

## 依存関係図

```
Phase 1 (DTO)
T-001→T-002  T-003→T-004

Phase 2 (FiscalYear)
T-005→T-006

Phase 3 (Validation)
T-007→T-009  T-008→T-009

Phase 4 (Service)
T-010→T-011  T-012→T-013  T-014→T-015  T-016→T-017
T-018→T-019  T-020→T-021  T-022→T-023  T-023b→T-023c

Phase 5 (SQL) → Phase 6 (DAO Test)
T-024→T-028  T-025→T-029  T-026→T-030  T-027→T-031

Phase 7 (Controller)
T-033→T-034  T-035→T-036  T-037→T-038  T-039→T-040
T-041→T-042  T-043→T-044  T-045→T-046

Phase 8 (Error)
T-047→T-048

Phase 9 (Frontend)
T-050→T-051

Phase 10-10c (Cross-cutting)
T-053  T-054  T-054b  T-054c  T-055  T-056  T-057→T-058

Phase 13 (Quality)
T-059→T-060→T-061→T-062
```
