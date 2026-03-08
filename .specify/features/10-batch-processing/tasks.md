# バッチ処理 タスク一覧

## 概要
- 総タスク数: 40
- 見積もり合計: 57.5 時間

現行 Oracle PL/SQL 13 バッチ SQL を PostgreSQL 互換 SQL + Spring Boot バッチモジュールに移行する。
3 パターン（A: API 連動型 / B: スケジュール型 / C: API トリガー型）で実装し、
ECS Scheduled Task + EventBridge Scheduler で定期実行する。

### 移行元 PL/SQL ファイル参照

全ファイルは `D:\PROJECT02\migration_source\migration_soource_irpmng_czConsv\` 配下に格納。

| # | バッチ名 | 移行元ファイル |
|---|----------|---------------|
| 1 | 制御マスタ作成 | `batch/sql/CZ_Mctlrcrdmake.sql` |
| 2 | 月次確認フラグ設定 | `batch/sql/CZ_Mjitu2kakutei.sql` |
| 3 | データ集約フラグ設定 | `batch/sql/CZ_Mjitu4syuukei.sql` |
| 4 | 月次未確認リセット | `batch/sql/CZ_ctlmstreset.sql` |
| 5 | サブシステム集計（対象別） | `batch/sql/CZ_subsknri01.sql` |
| 6 | サブシステム集計（原因別） | `batch/sql/CZ_subsknri02.sql` |
| 7 | グループキー作成 | `batch/sql/CZ_grouping.sql` |
| 8 | 半期集計 | `batch/sql/CZ_subsknri03.sql` |
| 9 | 月別内訳集計 | `batch/sql/CZ_subsknri04.sql` |
| 10 | 集計データリセット | `batch/sql/CZ_ctlmstreset.sql` (リセット部分) |
| 11 | 組織マスタ同期 | `batch/sql/czdbaton1.sql` |
| 12 | システムマスタ同期 | `batch/sql/czdbaton2.sql` |
| 13 | 担当者履歴更新 | `batch/sql/czdbatof1.sql`, `batch/sql/czdbatof2.sql` |

---

## タスク一覧

### Phase 1: バッチ共通基盤

- [ ] **T-001**: batch_execution_log テーブル DDL 作成・マイグレーション
  - 種別: 実装
  - 内容: バッチ実行履歴テーブル（batch_execution_log）の DDL を作成。id (BIGSERIAL PK), batch_name (VARCHAR(50)), started_at (TIMESTAMPTZ), finished_at (TIMESTAMPTZ), status (VARCHAR(10): RUNNING/SUCCESS/FAILED), records_affected (INTEGER), error_message (TEXT), created_at (TIMESTAMPTZ)
  - 成果物: `src/main/resources/db/migration/V***__create_batch_execution_log.sql`
  - 完了条件: Flyway マイグレーションが成功し、テーブルが作成される
  - 依存: なし
  - 見積もり: 0.5 時間

- [ ] **T-002**: BatchExecutionLogger 単体テスト作成
  - 種別: テスト
  - AC: AC-BA-09（実行ログ記録）, AC-BA-10（二重起動防止）
  - 内容: バッチ実行履歴の記録ロジックのテスト。開始記録（status=RUNNING）、成功完了（status=SUCCESS + records_affected）、失敗記録（status=FAILED + error_message）、二重起動チェック（同名バッチが RUNNING 状態の場合の検出）を検証
  - 成果物: `src/test/java/com/example/czConsv/batch/BatchExecutionLoggerTest.java`
  - 完了条件: テストが Red 状態
  - 依存: T-001
  - 見積もり: 1 時間

- [ ] **T-003**: BatchExecutionLogger 実装
  - 種別: 実装
  - 内容: バッチ実行履歴記録クラスの実装。logStart(batchName) → ID 返却、logSuccess(id, recordsAffected)、logFailure(id, errorMessage)、isRunning(batchName) による二重起動チェック
  - 成果物: `src/main/java/com/example/czConsv/batch/BatchExecutionLogger.java`
  - 完了条件: T-002 のテストが全て Green
  - 依存: T-002
  - 見積もり: 1 時間

- [ ] **T-004**: BatchDao（Doma 2 DAO）テスト作成
  - 種別: テスト
  - 内容: バッチ用 Doma 2 DAO の Testcontainers テスト。batch_execution_log テーブルへの CRUD 操作を検証
  - 成果物: `src/test/java/com/example/czConsv/batch/repository/BatchDaoTest.java`
  - 完了条件: テストが Red 状態
  - 依存: T-001
  - 見積もり: 1 時間

- [ ] **T-005**: BatchDao 実装
  - 種別: 実装
  - 内容: Doma 2 DAO インターフェースの実装。insertExecutionLog / updateExecutionLog / findRunningByName の 2Way SQL を作成
  - 成果物: `src/main/java/com/example/czConsv/batch/repository/BatchDao.java`, 2Way SQL ファイル
  - 完了条件: T-004 のテストが全て Green
  - 依存: T-004
  - 見積もり: 1 時間

- [ ] **T-006**: BatchConfig 設定クラス作成
  - 種別: 実装
  - 内容: バッチ用 Spring 設定クラス。@Profile("batch") の ComponentScan 設定、バッチ固有の Bean 定義
  - 成果物: `src/main/java/com/example/czConsv/batch/BatchConfig.java`
  - 完了条件: バッチプロファイルでアプリケーションが起動できる
  - 依存: なし
  - 見積もり: 0.5 時間

- [ ] **T-007**: BatchRunner テスト作成
  - 種別: テスト
  - 内容: CommandLineRunner の引数解析テスト。--batch=control-master / --batch=master-sync の正常系、引数なし・不明バッチ名の異常系を検証
  - 成果物: `src/test/java/com/example/czConsv/batch/BatchRunnerTest.java`
  - 完了条件: テストが Red 状態
  - 依存: T-006
  - 見積もり: 1 時間

- [ ] **T-008**: BatchRunner 実装
  - 種別: 実装
  - 内容: @Profile("batch") の CommandLineRunner 実装。--batch=<name> 引数解析、switch 文によるバッチ振り分け（control-master / master-sync）、引数不正時の IllegalArgumentException
  - 成果物: `src/main/java/com/example/czConsv/batch/BatchRunner.java`
  - 完了条件: T-007 のテストが全て Green
  - 依存: T-007
  - 見積もり: 0.5 時間

---

### Phase 2: パターン A — API 連動型（#2, 3, 4 制御フラグ操作）

- [ ] **T-009**: 月次確認フラグ設定テスト作成
  - 種別: テスト
  - AC: AC-BA-11（排他制御）
  - 移行元: `batch/sql/CZ_Mjitu2kakutei.sql`
  - 内容: WorkStatusService.monthlyConfirm() のテスト。SELECT FOR UPDATE による排他取得、gjkt_flg = '1' 更新、集計バッチ（#5, #6）の同期呼び出し確認、既に確認済みの場合のエラーハンドリングを検証
  - 成果物: `src/test/java/com/example/czConsv/service/WorkStatusServiceConfirmTest.java`
  - 完了条件: テストが Red 状態
  - 依存: なし
  - 見積もり: 1.5 時間

- [ ] **T-010**: 月次確認フラグ設定実装
  - 種別: 実装
  - 内容: WorkStatusService.monthlyConfirm() の実装。controlDao.selectForUpdate() → gjkt_flg='1' 更新 → SubsystemAggregationBatch 同期呼び出し。@Transactional でトランザクション管理
  - 成果物: `src/main/java/com/example/czConsv/service/WorkStatusService.java`（追記）
  - 完了条件: T-009 のテストが全て Green
  - 依存: T-009
  - 見積もり: 1 時間

- [ ] **T-011**: データ集約フラグ設定・月次未確認リセットテスト作成
  - 種別: テスト
  - AC: AC-BA-06（月次集約トリガー）
  - 移行元: `batch/sql/CZ_Mjitu4syuukei.sql`, `batch/sql/CZ_ctlmstreset.sql`
  - 内容: monthlyAggregate()（data_sk_flg='1' 設定 + 集計バッチ #7~#9 呼出）と monthlyUnconfirm()（両フラグリセット）のテスト。排他制御、フラグ更新、リセット後の状態を検証。triggerAggregation がトランザクション内で #5〜#10 を順次同期実行し、集計結果が tcz13/tcz14 に反映されることを検証
  - 成果物: `src/test/java/com/example/czConsv/service/WorkStatusServiceAggregateTest.java`
  - 完了条件: テストが Red 状態
  - 依存: なし
  - 見積もり: 1.5 時間

- [ ] **T-012**: データ集約フラグ設定・月次未確認リセット実装
  - 種別: 実装
  - 内容: monthlyAggregate() と monthlyUnconfirm() の実装。monthlyAggregate: data_sk_flg='1' + GroupKeyBatch/HalfAggregation/MonthlyAggregation 呼出。monthlyUnconfirm: gjkt_flg='0', data_sk_flg='0' リセット
  - 成果物: `src/main/java/com/example/czConsv/service/WorkStatusService.java`（追記）
  - 完了条件: T-011 のテストが全て Green
  - 依存: T-011
  - 見積もり: 1 時間

- [ ] **T-012a**: 繰越データ作成テスト作成
  - 種別: テスト
  - AC: AC-BA-03（繰越データ作成）, AC-BA-04（繰越データ重複防止）
  - 内容: WorkStatusService の繰越処理テスト。POST /work-hours/transfer 呼出時に翌期の tcz01 レコードが作成され分類・システム情報が引き継がれることを検証（AC-BA-03）。繰越先に既にレコードが存在する場合に INSERT WHERE NOT EXISTS で重複作成されないことを検証（AC-BA-04）
  - 成果物: `src/test/java/com/example/czConsv/service/WorkStatusServiceTransferTest.java`
  - 完了条件: テストが Red 状態
  - 依存: なし
  - 見積もり: 1.5 時間

- [ ] **T-012b**: 繰越データ作成実装
  - 種別: 実装
  - 内容: WorkStatusService の繰越処理。確定済みデータから翌期の tcz01 レコードを作成し、分類・システム情報を引き継ぐ。INSERT WHERE NOT EXISTS で重複防止
  - 成果物: `src/main/java/com/example/czConsv/service/WorkStatusService.java`（追記）
  - 完了条件: T-012a のテストが全て Green
  - 依存: T-012a
  - 見積もり: 1 時間

---

### Phase 3: パターン B — スケジュール型（#1 制御マスタ作成）

- [ ] **T-013**: ControlMasterBatch 単体テスト作成
  - 種別: テスト
  - AC: AC-BA-01（制御マスタ作成）, AC-BA-02（制御マスタ冪等性）
  - 移行元: `batch/sql/CZ_Mctlrcrdmake.sql`
  - 内容: 翌月の mcz04_ctrl レコード作成のテスト。正常系（翌月レコード作成: sysid='00','01' の2レコード INSERT、AC-BA-01）、重複時スキップ（NOT EXISTS、AC-BA-02）、冪等性（再実行で同結果）を Testcontainers で検証
  - 成果物: `src/test/java/com/example/czConsv/batch/ControlMasterBatchTest.java`
  - 完了条件: テストが Red 状態
  - 依存: T-003, T-005
  - 見積もり: 1.5 時間

- [ ] **T-014**: ControlMasterBatch 実装
  - 種別: 実装
  - 内容: 制御マスタ作成バッチの実装。次月の yyyymm を計算、mcz04_ctrl に sysid='00','01' の2レコードを INSERT（EXISTS チェック付き）、BatchExecutionLogger で実行記録
  - 成果物: `src/main/java/com/example/czConsv/batch/ControlMasterBatch.java`, 2Way SQL ファイル
  - 完了条件: T-013 のテストが全て Green
  - 依存: T-013
  - 見積もり: 1 時間

---

### Phase 4: パターン C — API トリガー型（#5, 6 サブシステム集計）

- [ ] **T-015**: SubsystemAggregationBatch 単体テスト作成（対象別 #5）
  - 種別: テスト
  - AC: AC-BA-05（サブシステム集計 対象別）
  - 移行元: `batch/sql/CZ_subsknri01.sql`
  - 内容: 対象 SS 別集計のテスト。tcz01_hosyu_kousuu → tcz13_subsys_sum (sumkbn='0') への集計。DELETE + INSERT の冪等性、ステータス IN ('1','2') フィルタ、SUM(hs_kousuu) の正確性（3件の合計値一致、AC-BA-05）、JOIN 条件の正当性を Testcontainers で検証
  - 成果物: `src/test/java/com/example/czConsv/batch/SubsystemAggregationBatchTest.java`
  - 完了条件: テストが Red 状態
  - 依存: T-003, T-005
  - 見積もり: 2 時間

- [ ] **T-016**: SubsystemAggregationBatch 単体テスト作成（原因別 #6）
  - 種別: テスト
  - 移行元: `batch/sql/CZ_subsknri02.sql`
  - 内容: 原因 SS 別集計のテスト。genin_sknno/subsknno による集計 + sumkbn='1'。#5 との差分（対象→原因の JOIN 切替）が正しいことを検証
  - 成果物: `src/test/java/com/example/czConsv/batch/SubsystemAggregationBatchTest.java`（追記）
  - 完了条件: テストが Red 状態
  - 依存: T-003, T-005
  - 見積もり: 1 時間

- [ ] **T-017**: SubsystemAggregationBatch 実装
  - 種別: 実装
  - 内容: サブシステム集計バッチ。executeTarget(yearMonth): sumkbn='0' の DELETE + 対象別 INSERT、executeCause(yearMonth): sumkbn='1' の DELETE + 原因別 INSERT。2Way SQL で Oracle → PostgreSQL 変換済みクエリを使用
  - 成果物: `src/main/java/com/example/czConsv/batch/SubsystemAggregationBatch.java`, `src/main/resources/META-INF/.../subsystem_aggregation_target.sql`, `subsystem_aggregation_cause.sql`
  - 完了条件: T-015, T-016 のテストが全て Green
  - 依存: T-015, T-016
  - 見積もり: 2 時間

---

### Phase 5: パターン C — API トリガー型（#7 グループキー, #8 半期集計, #9 月別集計, #10 リセット）

- [ ] **T-018**: GroupKeyBatch 単体テスト作成
  - 種別: テスト
  - 移行元: `batch/sql/CZ_grouping.sql`
  - 内容: グループキー UPSERT のテスト。新規 INSERT（tcz14_grp_key に新レコード作成）、既存 UPDATE（ON CONFLICT DO UPDATE で hshk_bunrui_code 等が更新される）、delflg='0' フィルタ、冪等性を Testcontainers で検証
  - 成果物: `src/test/java/com/example/czConsv/batch/GroupKeyBatchTest.java`
  - 完了条件: テストが Red 状態
  - 依存: T-003, T-005
  - 見積もり: 1.5 時間

- [ ] **T-019**: GroupKeyBatch 実装
  - 種別: 実装
  - 内容: グループキー作成バッチ。mav01_sys から nendo_half/sknno 別に分類・担当情報を集約し tcz14_grp_key に UPSERT（INSERT ... ON CONFLICT DO UPDATE）
  - 成果物: `src/main/java/com/example/czConsv/batch/GroupKeyBatch.java`, `group_key_upsert.sql`
  - 完了条件: T-018 のテストが全て Green
  - 依存: T-018
  - 見積もり: 1 時間

- [ ] **T-020**: HalfAggregationBatch 単体テスト作成
  - 種別: テスト
  - 移行元: `batch/sql/CZ_subsknri03.sql`
  - 内容: 半期推移集計データ作成のテスト。tcz13_subsys_sum からの集計結果の正確性、年度期間ルール（2015年度下期3ヶ月、2016年以降6ヶ月）適用を検証
  - 成果物: `src/test/java/com/example/czConsv/batch/HalfAggregationBatchTest.java`
  - 完了条件: テストが Red 状態
  - 依存: T-003, T-005
  - 見積もり: 1.5 時間

- [ ] **T-021**: HalfAggregationBatch 実装
  - 種別: 実装
  - 内容: 半期集計バッチ。半期推移画面（FORM_030）用の集計データを作成
  - 成果物: `src/main/java/com/example/czConsv/batch/HalfAggregationBatch.java`, 2Way SQL ファイル
  - 完了条件: T-020 のテストが全て Green
  - 依存: T-020
  - 見積もり: 1.5 時間

- [ ] **T-022**: MonthlyAggregationBatch 単体テスト作成
  - 種別: テスト
  - 移行元: `batch/sql/CZ_subsknri04.sql`
  - 内容: 月別内訳集計データ作成のテスト。月別内訳画面（FORM_040）用の集計結果の正確性を検証
  - 成果物: `src/test/java/com/example/czConsv/batch/MonthlyAggregationBatchTest.java`
  - 完了条件: テストが Red 状態
  - 依存: T-003, T-005
  - 見積もり: 1 時間

- [ ] **T-023**: MonthlyAggregationBatch 実装
  - 種別: 実装
  - 内容: 月別内訳集計バッチ。月別内訳画面（FORM_040）用の集計データを作成
  - 成果物: `src/main/java/com/example/czConsv/batch/MonthlyAggregationBatch.java`, 2Way SQL ファイル
  - 完了条件: T-022 のテストが全て Green
  - 依存: T-022
  - 見積もり: 1 時間

- [ ] **T-024**: AggregationResetBatch 単体テスト作成
  - 種別: テスト
  - 移行元: `batch/sql/CZ_ctlmstreset.sql` (リセット部分)
  - 内容: 集計データリセットのテスト。指定月の tcz13_subsys_sum データ DELETE、対象外月のデータが残ることの確認、冪等性（DELETE 再実行で0件影響）を検証
  - 成果物: `src/test/java/com/example/czConsv/batch/AggregationResetBatchTest.java`
  - 完了条件: テストが Red 状態
  - 依存: T-003, T-005
  - 見積もり: 1 時間

- [ ] **T-025**: AggregationResetBatch 実装
  - 種別: 実装
  - 内容: 集計データリセットバッチ。tcz13_subsys_sum の指定月データを DELETE
  - 成果物: `src/main/java/com/example/czConsv/batch/AggregationResetBatch.java`, 2Way SQL ファイル
  - 完了条件: T-024 のテストが全て Green
  - 依存: T-024
  - 見積もり: 0.5 時間

---

### Phase 6: パターン B — スケジュール型（#11, 12, 13 マスタ同期）

- [ ] **T-026**: MasterSyncBatch 単体テスト作成（組織マスタ #11）
  - 種別: テスト
  - AC: AC-BA-07（組織マスタ UPSERT）, AC-BA-08（バリデーション・行単位スキップ）
  - 移行元: `batch/sql/czdbaton1.sql`
  - 内容: 組織マスタ同期（staging_org_import → mcz12_orgn_kr UPSERT）のテスト。新規追加（AC-BA-07）、既存更新（org_name 変更）、論理削除、バリデーション（必須項目/コード体系/親組織存在チェック、AC-BA-08）、行単位エラースキップ（正常行は処理継続、AC-BA-08）を Testcontainers で検証
  - 成果物: `src/test/java/com/example/czConsv/batch/MasterSyncBatchOrganizationTest.java`
  - 完了条件: テストが Red 状態
  - 依存: T-003, T-005
  - 見積もり: 2 時間

- [ ] **T-027**: MasterSyncBatch 単体テスト作成（システムマスタ #12, 担当者履歴 #13）
  - 種別: テスト
  - 移行元: `batch/sql/czdbaton2.sql`, `batch/sql/czdbatof1.sql`, `batch/sql/czdbatof2.sql`
  - 内容: システム/サブシステムマスタ同期（mav01_sys, mav03_subsys UPSERT）と担当者履歴更新（tcz16_tnt_busyo_rireki 記録）のテスト。差分同期の正確性を Testcontainers で検証
  - 成果物: `src/test/java/com/example/czConsv/batch/MasterSyncBatchSystemTest.java`
  - 完了条件: テストが Red 状態
  - 依存: T-003, T-005
  - 見積もり: 1.5 時間

- [ ] **T-028**: MasterSyncBatch 実装
  - 種別: 実装
  - 内容: マスタ同期バッチの実装。3種のマスタ同期（組織 #11 / システム #12 / 担当者 #13）をステージングテーブル経由の共通 UPSERT パターンで実装。CSV アップロード / ASTERIA API 共用のステージングテーブル → INSERT ... ON CONFLICT DO UPDATE
  - 成果物: `src/main/java/com/example/czConsv/batch/MasterSyncBatch.java`, `master_sync_organization.sql`, `master_sync_system.sql`, `master_sync_staff.sql`
  - 完了条件: T-026, T-027 のテストが全て Green
  - 依存: T-026, T-027
  - 見積もり: 2.5 時間

---

### Phase 7: マスタ同期受信 API（ASTERIA 連携）

- [ ] **T-029**: マスタ同期受信 API テスト作成
  - 種別: テスト
  - 内容: POST /api/v1/batch/master-sync/{masterType} の MockMvc テスト。masterType=organization/management-target の正常系（JSON 配列 → ステージングテーブル INSERT）、API キー認証（X-Api-Key ヘッダ）検証、不正 masterType の 400 エラー、認証失敗の 401 エラーを検証
  - 成果物: `src/test/java/com/example/czConsv/controller/MasterSyncApiControllerTest.java`
  - 完了条件: テストが Red 状態
  - 依存: T-028
  - 見積もり: 1.5 時間

- [ ] **T-030**: マスタ同期受信 API 実装
  - 種別: 実装
  - 内容: ASTERIA 連携用の受信 API Controller。POST /api/v1/batch/master-sync/{masterType} エンドポイント。X-Api-Key 認証フィルタ、JSON ペイロード → ステージングテーブル INSERT、MasterSyncBatch の UPSERT ロジック呼出
  - 成果物: `src/main/java/com/example/czConsv/controller/MasterSyncApiController.java`
  - 完了条件: T-029 のテストが全て Green
  - 依存: T-029
  - 見積もり: 1.5 時間

---

### Phase 8: 統合テスト

- [ ] **T-031**: 月次フロー統合テスト
  - 種別: テスト
  - 内容: 制御マスタ作成(#1) → 月次確認(#2) → サブシステム集計(#5,#6) → データ集約(#3) → グループキー(#7) → 半期集計(#8) → 月別集計(#9) の一連のフローを Testcontainers で検証。各ステップの DB 状態と集計結果の数値一致を確認
  - 成果物: `src/test/java/com/example/czConsv/batch/integration/MonthlyFlowIntegrationTest.java`
  - 完了条件: テストが Green
  - 依存: T-014, T-017, T-019, T-021, T-023, T-025
  - 見積もり: 2.5 時間

- [ ] **T-032**: 集計精度検証テスト
  - 種別: テスト
  - 内容: tcz01_hosyu_kousuu に手動テストデータを投入し、各集計バッチ実行後の tcz13_subsys_sum の数値が期待値と一致することを検証。境界値（工数 0、最大値）、複数カテゴリの集計を含む
  - 成果物: `src/test/java/com/example/czConsv/batch/integration/AggregationAccuracyTest.java`
  - 完了条件: テストが Green
  - 依存: T-017, T-021, T-023
  - 見積もり: 2 時間

- [ ] **T-033**: リカバリ・冪等性テスト
  - 種別: テスト
  - 内容: バッチ途中失敗 → 再実行で正常完了の冪等性を検証。DELETE + INSERT パターンの再実行安全性、UPSERT パターンの重複安全性、トランザクションロールバック後の再実行を Testcontainers で検証
  - 成果物: `src/test/java/com/example/czConsv/batch/integration/RecoveryIdempotencyTest.java`
  - 完了条件: テストが Green
  - 依存: T-017, T-019, T-028
  - 見積もり: 1.5 時間

- [ ] **T-034**: 排他制御テスト
  - 種別: テスト
  - AC: AC-BA-10（二重起動防止）, AC-BA-11（排他制御 SELECT FOR UPDATE）
  - 内容: 月次確認中の同時バッチ実行による排他エラー検証。SELECT FOR UPDATE のロック競合（ロック解放まで待機しデータ不整合なし、AC-BA-11）、BatchExecutionLogger の RUNNING チェックによる二重起動防止（起動スキップ＋警告ログ、AC-BA-10）を検証
  - 成果物: `src/test/java/com/example/czConsv/batch/integration/ExclusiveControlTest.java`
  - 完了条件: テストが Green
  - 依存: T-010, T-003
  - 見積もり: 1.5 時間

- [ ] **T-034a**: バッチ失敗時アラート通知テスト
  - 種別: テスト
  - AC: AC-BA-13（バッチ失敗時アラート）
  - 内容: バッチ実行中にエラーが発生し status='FAILED' で記録された場合、構造化 JSON ログに ERROR レベルで出力されることを検証。CloudWatch Metric Filter の検出対象となるログフォーマット（{"level":"ERROR","batch_name":"...","status":"FAILED"}）を確認
  - 成果物: `src/test/java/com/example/czConsv/batch/integration/BatchFailureAlertTest.java`
  - 完了条件: テストが Green
  - 依存: T-003
  - 見積もり: 1 時間

---

### Phase 9: Docker・インフラ設定

- [ ] **T-035**: Docker Compose バッチコンテナ追加
  - 種別: 実装
  - 内容: docker-compose.yml にバッチコンテナ（eclipse-temurin:21-jdk, @Profile("batch")）を追加。コマンド引数で --batch=<name> を指定可能にする。ヘルスチェック設定、DB 依存の depends_on 設定
  - 成果物: `docker-compose.yml`（修正）
  - 完了条件: `docker compose run batch --batch=control-master` でバッチが実行できる
  - 依存: T-008, T-014, T-028
  - 見積もり: 1 時間

- [ ] **T-036**: AWS ECS タスク定義・EventBridge スケジュール設定ファイル作成
  - 種別: 実装
  - 内容: ECS タスク定義（cz-batch）と EventBridge Scheduler 定義（cz-monthly-ctrl: cron(0 0 1 * ? *)、cz-daily-sync: cron(0 2 * * ? *)）の設定ファイルを作成。CloudWatch Logs 設定、環境変数（DB_HOST 等）のパラメータ化
  - 成果物: `aws/batch-task-definition.json`, `aws/eventbridge-schedules.json`
  - 完了条件: 設定ファイルが構文的に正しく、必要な全パラメータが定義されている
  - 依存: T-035
  - 見積もり: 1.5 時間

- [ ] **T-036a**: ECS スケジュール実行検証テスト
  - 種別: テスト
  - AC: AC-BA-12（ECS スケジュール実行）
  - 内容: Docker Compose 環境で CommandLineRunner が --batch=control-master 引数でバッチを正常実行できることを検証。引数解析、バッチ振り分け、@Profile("batch") での起動を確認。ECS タスク定義の構文妥当性（JSON Schema 検証）と EventBridge cron 式の妥当性も検証
  - 成果物: `src/test/java/com/example/czConsv/batch/integration/EcsScheduleExecutionTest.java`
  - 完了条件: テストが Green
  - 依存: T-035, T-036
  - 見積もり: 1 時間

---

## 依存関係図

```
Phase 1 (基盤):
T-001 → T-002 → T-003
      → T-004 → T-005
T-006 → T-007 → T-008

Phase 2 (パターン A):
T-009 → T-010
T-011 → T-012
T-012a → T-012b                              ← NEW (AC-BA-03, AC-BA-04)

Phase 3 (パターン B #1):
T-003 + T-005 → T-013 → T-014

Phase 4 (パターン C #5,6):
T-003 + T-005 → T-015 → T-017
              → T-016 ↗

Phase 5 (パターン C #7-10):
T-003 + T-005 → T-018 → T-019
              → T-020 → T-021
              → T-022 → T-023
              → T-024 → T-025

Phase 6 (パターン B #11-13):
T-003 + T-005 → T-026 → T-028
              → T-027 ↗

Phase 7 (ASTERIA API):
T-028 → T-029 → T-030

Phase 8 (統合テスト):
T-014 + T-017 + T-019 + T-021 + T-023 + T-025 → T-031
T-017 + T-021 + T-023 → T-032
T-017 + T-019 + T-028 → T-033
T-010 + T-003 → T-034
T-003 → T-034a                               ← NEW (AC-BA-13)

Phase 9 (インフラ):
T-008 + T-014 + T-028 → T-035 → T-036
T-035 + T-036 → T-036a                       ← NEW (AC-BA-12)
```

## AC トレーサビリティ・マトリクス

| AC | 説明 | テストタスク |
|---|---|---|
| AC-BA-01 | 制御マスタ作成 | T-013 |
| AC-BA-02 | 制御マスタ冪等性 | T-013 |
| AC-BA-03 | 繰越データ作成 | T-012a |
| AC-BA-04 | 繰越データ重複防止 | T-012a |
| AC-BA-05 | サブシステム集計 対象別 | T-015 |
| AC-BA-06 | 月次集約トリガー | T-011, T-031 |
| AC-BA-07 | マスタ同期 — 組織マスタ | T-026 |
| AC-BA-08 | マスタ同期バリデーション | T-026 |
| AC-BA-09 | バッチ実行ログ記録 | T-002 |
| AC-BA-10 | 二重起動防止 | T-002, T-034 |
| AC-BA-11 | 排他制御（SELECT FOR UPDATE） | T-009, T-034 |
| AC-BA-12 | ECS スケジュール実行 | T-036a |
| AC-BA-13 | バッチ失敗時アラート | T-034a |
