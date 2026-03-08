# #10 Batch Processing — 整合性チェックレポート

**実施日**: 2026-02-26
**対象**: `10-batch-processing/spec.md`
**チェック対象**: constitution.md, analysis 6ファイル, spec #1-#9

---

## A. Constitution.md との整合性

| チェック項目 | 結果 | 詳細 |
|---|---|---|
| Migration-First | ✅ 合致 | constitution I。13 バッチ SQL を完全踏襲して PostgreSQL に移行 |
| Spring Boot REST API | ✅ 合致 | constitution 技術スタック。Spring Boot バッチモジュール + Doma 2 DAO |
| Doma 2 ORM | ✅ 合致 | セクション 4.1 で `BatchDao.java` (Doma 2 DAO) + 2Way SQL ファイル配置 |
| PostgreSQL 16 | ✅ 合致 | constitution 技術スタック。Oracle → PostgreSQL 変換（セクション 6） |
| Docker-First | ✅ 合致 | 開発環境は Docker compose 上の PostgreSQL で動作 |
| TDD | ✅ 合致 | constitution IV。セクション 8 で単体テスト + 統合テスト + 冪等性テスト |
| CI/CD | ✅ 合致 | constitution 技術スタック。ECS Scheduled Task + EventBridge |

### 指摘事項

なし。constitution の全原則に準拠している。

---

## B. Analysis 6ファイルとの整合性

### 01_system_analysis.md との整合

| 項目 | 分析 | spec | 結果 |
|---|---|---|---|
| Oracle PL/SQL 13 バッチ | プロシージャ + シェルスクリプト | PostgreSQL SQL + Spring Boot バッチ | ✅ IMPROVE (GAP-O01) |
| 手動実行 (cron) | サーバー cron | EventBridge Scheduler | ✅ IMPROVE (GAP-O02) |
| ログファイル監視 | テキストログ | CloudWatch Logs + アラーム | ✅ IMPROVE (GAP-O04) |
| 通知なし | — | SNS → Slack/メール | ✅ ADD |

### 03_user_stories.md との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| 月次制御フロー | ✅ 合致 | 制御マスタ作成 → 確認 → 集約 → 集計 の一連フロー |
| データ集計正確性 | ✅ 合致 | 対象/原因別 SS 集計、半期推移、月別内訳の 4 種集計 |

### 05_gap_analysis.md との整合

| 項目 | GAP ID | 結果 | 詳細 |
|---|---|---|---|
| PL/SQL → PostgreSQL + Java | GAP-O01 | ✅ 合致 | セクション 2, 3 |
| スケジューラ改善 | GAP-O02 | ✅ 合致 | EventBridge Scheduler |
| 実行管理画面 | GAP-O03 | ✅ 合致 | batch_execution_log + P2 管理画面 |
| ログ構造化 | GAP-O04 | ✅ 合致 | JSON ログ + CloudWatch |
| ヘルスチェック | GAP-O05 | ✅ 合致 | /actuator/health で対応 |

---

## C. 他 Spec との整合性

### spec #1 (database-schema) との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| mcz04_ctrl テーブル | ✅ 合致 | 制御マスタの gjkt_flg / data_sk_flg 操作 |
| tcz01_hosyu_kousuu | ✅ 合致 | 集計元テーブル |
| tcz13_subsys_sum | ✅ 合致 | 集計先テーブル |
| tcz14_grp_key | ✅ 合致 | グループキーテーブル |
| mcz12_orgn_kr | ✅ 合致 | 組織マスタ同期先 |
| batch_execution_log | ⚠️ 参照なし | セクション 4.3 で新規テーブル定義。spec #1 への追記が望ましいが、バッチ固有テーブルのため spec #10 での定義は許容。実装時に DDL を追加する |

### spec #2 (auth-infrastructure) との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| 組織階層マスタ同期 | ✅ 合致 | mcz12_orgn_kr への UPSERT。OrganizationScopeResolver の依存データ |

### spec #3 (core-api-design) との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| 月次確認 API | ⚠️ フラグ操作不一致 | spec #3 セクション 3.19 (monthly-confirm) は gjkt_flg='1' のみ設定。spec #10 セクション 3.2 は `setGjktFlg("1")` + `setDataSkFlg("0")` で data_sk_flg も操作 → **FIX-B01** |
| 月次集約 API | ✅ 合致 | spec #3 セクション 3.21 で data_sk_flg='1' 設定 |
| 月次未確認 API | ✅ 合致 | spec #3 セクション 3.20 で両フラグリセット (FIX-S02b 適用済み) |

### spec #4 (work-hours-input) との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| tcz01 レコード構造 | ✅ 合致 | 集計 SQL の WHERE 条件がレコードステータスを参照 |

### spec #5 (work-status-list) との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| FORM_020 月次制御操作 | ✅ 合致 | パターン A (API 連動型) がバッチ #2/3/4 に対応 |
| canInputPeriod 権限 | ✅ 合致 | 月次制御は canInputPeriod 権限者のみ（spec #5 FIX-S01/S02 適用済み） |

### spec #6 (analysis-screens) との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| 半期推移データ | ✅ 合致 | バッチ #7, #8 の集計結果を FORM_030 で表示 |
| 月別内訳データ | ✅ 合致 | バッチ #9 の集計結果を FORM_040 で表示 |

---

## D. レガシーシステムとの差異分類

### KEEP（踏襲）

| 項目 | 現行 | 新システム |
|---|---|---|
| 13 バッチ処理の業務ロジック | PL/SQL プロシージャ | PostgreSQL SQL + Spring Boot Java |
| SS 集計 (対象/原因) | Oracle SQL | PostgreSQL 互換 SQL |
| グループキー作成 | Oracle MERGE INTO | PostgreSQL ON CONFLICT UPSERT |
| マスタ同期 (組織/SS/担当者) | Oracle SQL | PostgreSQL UPSERT |
| 制御マスタ月次初期化 | PL/SQL | PostgreSQL INSERT ... WHERE NOT EXISTS |

### IMPROVE（改善）

| 項目 | 現行 | 新システム | GAP ID |
|---|---|---|---|
| 実行基盤 | シェルスクリプト手動 | ECS Scheduled Task + EventBridge | GAP-O01, O02 |
| スケジューラ | cron (サーバー) | EventBridge Scheduler (マネージド) | GAP-O02 |
| 監視 | ログファイル手動確認 | CloudWatch Logs + アラーム | GAP-O04 |
| 通知 | なし | SNS → Slack/メール | — |
| 実行履歴 | なし | batch_execution_log テーブル | GAP-O03 |
| API連動型 (#2/3/4) | バッチ独立実行 | 画面操作と直接連動 | — |

### ADD（新規追加）

| 項目 | 説明 |
|---|---|
| batch_execution_log | バッチ実行履歴テーブル（管理画面は P2） |
| 構造化 JSON ログ | CloudWatch Logs 連携 |
| 失敗通知 | SNS → Slack/メール |
| 冪等性保証 | 重複実行時のデータ整合性 |

### REMOVE（廃止）

| 項目 | 理由 |
|---|---|
| Oracle PL/SQL プロシージャ | PostgreSQL 関数 + Spring Boot に移行 |
| シェルスクリプト | ECS タスク定義に移行 |
| cron ジョブ | EventBridge Scheduler に移行 |

---

## E. 推奨アクション

### FIX-B01: monthlyConfirm の data_sk_flg 不正操作 (P2)

**箇所**: セクション 3.2

**問題**: `monthlyConfirm()` で `ctrl.setDataSkFlg("0")` を明示的に設定しているが、
spec #3 セクション 3.19 (monthly-confirm) の処理定義では gjkt_flg='1' のみ設定し、
data_sk_flg には触れない。

**spec #3 セクション 3.19 の定義**:
```
処理:
1. 権限チェック: canInputPeriod() が true であること
2. MCZ04CTRLMST を SELECT FOR UPDATE でロック取得
3. GetsujiKakuteiFlg = '1' に更新
```

data_sk_flg を操作するのは:
- **月次集約** (spec #3 セクション 3.21): data_sk_flg = '1' に設定
- **月次未確認** (spec #3 セクション 3.20): gjkt_flg = '0', data_sk_flg = '0' にリセット

monthlyConfirm で data_sk_flg を '0' にすると、確認と集約の責務分離が崩れる。
通常フローでは害はないが、不正な操作順序の場合にデータ不整合を招く可能性がある。

**修正**: `ctrl.setDataSkFlg("0")` 行を削除する。

---

## F. 変更履歴

| 日付 | FIX ID | 内容 | 対象ファイル | 状態 |
|---|---|---|---|---|
| 2026-02-26 | FIX-B01 | monthlyConfirm から data_sk_flg 操作を削除（spec #3 section 3.19 との整合） | 10-batch-processing/spec.md | ✅ 完了 |
