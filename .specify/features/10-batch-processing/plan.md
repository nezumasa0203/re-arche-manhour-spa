# バッチ処理 実装計画

## 概要

現行 Oracle PL/SQL で実装された 13 バッチ SQL を PostgreSQL 互換 SQL + Spring Boot バッチモジュールに移行する。
実行基盤はシェルスクリプト + cron から ECS Scheduled Task (Fargate) + EventBridge Scheduler に刷新。
バッチは 3 パターンに分類し、それぞれの実行特性に最適な方式で実装する。

- **パターン A（API 連動型）**: バッチ #2, 3, 4 — FORM_020 の画面操作に連動
- **パターン B（スケジュール型）**: バッチ #1, 11, 12, 13 — ECS Scheduled Task で定期実行
- **パターン C（API トリガー型）**: バッチ #5, 6, 7, 8, 9, 10 — 月次操作時に同期実行

**対応 GAP**: GAP-O01〜O05

---

## アーキテクチャ

### フロントエンド

N/A（バッチ処理はバックエンド専用機能）

### バックエンド

#### パターン A: API 連動型（#2, 3, 4）

`WorkStatusService` 内のメソッドとして実装。別途バッチクラス不要。

- `monthlyConfirm()` — gjkt_flg = '1' 更新（#2）
- `monthlyAggregate()` — data_sk_flg = '1' 更新（#3）
- `monthlyUnconfirm()` — 両フラグリセット（#4）
- SELECT FOR UPDATE による排他制御

#### パターン B: スケジュール型（#1, 11, 12, 13）

- **BatchRunner** — `CommandLineRunner` + `@Profile("batch")` + `--batch=<name>` 引数方式
- 本番: `@Scheduled` は使用しない（ECS Scheduled Task + EventBridge で制御）
- 開発用: `@Profile("dev")` + `@Scheduled` は動作確認目的で許容

| バッチ | スケジュール | 内容 |
|---|---|---|
| #1 ControlMasterBatch | 毎月1日 0:00 | 翌月の mcz04_ctrl レコード作成 |
| #11 MasterSyncBatch | 毎日 2:00 | 組織マスタ同期 |
| #12 MasterSyncBatch | 毎日 2:00 | システム/サブシステムマスタ同期 |
| #13 MasterSyncBatch | 毎日 2:00 | 担当者履歴同期 |

#### パターン C: API トリガー型（#5, 6, 7, 8, 9, 10）

`WorkStatusService` 内から**同期呼び出し**で実行する集計バッチ群。
API リクエストのトランザクション内で完結し、失敗時はロールバックされる。

| バッチ | クラス | 内容 |
|---|---|---|
| #5 | SubsystemAggregationBatch | 対象別 SS 集計（DELETE + INSERT） |
| #6 | SubsystemAggregationBatch | 原因別 SS 集計（DELETE + INSERT） |
| #7 | GroupKeyBatch | グループキー UPSERT |
| #8 | HalfAggregationBatch | 半期推移集計 |
| #9 | MonthlyAggregationBatch | 月別内訳集計 |
| #10 | AggregationResetBatch | 集計データ DELETE |

> triggerAggregation は直接メソッド呼び出し（非同期/イベント駆動ではない）。
> トランザクション境界を呼び出し元と共有し、失敗時のロールバック一貫性を保証。

#### 共通基盤

| コンポーネント | 役割 |
|---|---|
| BatchExecutionLogger | batch_execution_log テーブルへの実行履歴記録 |
| BatchDao | Doma 2 DAO。2Way SQL による PostgreSQL クエリ実行 |

#### Oracle → PostgreSQL SQL 変換ルール

| Oracle | PostgreSQL |
|---|---|
| `SYSDATE` | `CURRENT_TIMESTAMP` |
| `NVL(a, b)` | `COALESCE(a, b)` |
| `MERGE INTO` | `INSERT ... ON CONFLICT DO UPDATE` |
| `ROWNUM` | `LIMIT` / `ROW_NUMBER()` |
| PL/SQL プロシージャ | Spring Boot Java メソッド |

### データベース

既存テーブル（spec #1 定義済み）+ batch_execution_log（新規）を使用。

### AWS インフラ

#### EventBridge Scheduler（2 スケジュール）

| スケジュール | cron 式 | 実行バッチ |
|---|---|---|
| cz-monthly-ctrl | `cron(0 0 1 * ? *)` | #1 制御マスタ作成 |
| cz-daily-sync | `cron(0 2 * * ? *)` | #11, 12, 13 マスタ同期 |

集計バッチ (#5〜10) はパターン C のため EventBridge 不要。

#### 監視

CloudWatch Logs → Metric Filter → Alarm → SNS → Slack/メール通知

---

## Constitution Check

| 原則 | 準拠 | 確認内容 |
|------|------|----------|
| I. マイグレーション・ファースト | ✅ | 13 PL/SQL バッチを全て PostgreSQL + Spring Boot に移行。業務ロジック完全踏襲 |
| III. Docker-First | ✅ | docker-compose にバッチコンテナ定義。ローカル開発は Docker 上で完結 |
| IV. TDD | ✅ | 全バッチクラスの単体テスト + 統合テスト。集計ロジックはカバレッジ 100% |
| VI. Production Safety | ✅ | @Scheduled は @Profile("dev") のみ。本番は CommandLineRunner + EventBridge |
| VIII. CI/CD Safety | ✅ | バッチテストは CI パイプラインに組み込み |
| IX. 最適技術選定 | ✅ | CommandLineRunner + Doma 2。過剰な Spring Batch フレームワーク不使用 |

---

## 技術的リスク

| リスク | 影響度 | 対策 |
|---|---|---|
| 外部マスタ連携の2方式実装 | 解決済 | CSV アップロード + ASTERIA 経由 API の2方式で確定（spec.md セクション 3.6）。共通ステージングテーブル経由の UPSERT ロジックで統一 |
| パターン C のトランザクションサイズ | 中 | 大量データ時のレスポンスタイム増大。事前調査とタイムアウト設定調整 |
| 冪等性の保証 | 中 | DELETE + INSERT / UPSERT パターンで冪等性担保。テストで検証 |
| Oracle → PostgreSQL SQL 変換 | 中 | GROUP BY、JOIN の挙動差異に注意。Testcontainers で実環境テスト |
| EventBridge 二重起動 | 低 | batch_execution_log で RUNNING チェック + 排他制御 |

---

## 依存関係

### 依存先

| spec | 理由 |
|---|---|
| #1 database-schema | 全テーブル定義。batch_execution_log DDL |
| #3 core-api-design | パターン A/C の API エンドポイント定義 |
| #5 work-status-list | パターン C トリガー呼び出し元 |

### 依存元

| spec | 理由 |
|---|---|
| #6 analysis-screens | 集計バッチ (#5〜9) の出力データを表示 |
| #2 auth-infrastructure | 組織マスタ同期 (#11) のデータに依存 |

---

## 実装規模の見積もり

| カテゴリ | ファイル数 | 概要 |
|---|---|---|
| バッチクラス（Java） | 8 | ControlMaster, SubsystemAggregation, GroupKey, HalfAggregation, MonthlyAggregation, AggregationReset, MasterSync, BatchRunner |
| 共通基盤（Java） | 3 | BatchConfig, BatchExecutionLogger, BatchDao |
| 2Way SQL ファイル | 8〜10 | 各バッチ用 PostgreSQL クエリ |
| テストクラス | 10〜12 | 単体テスト 8 + 統合テスト 3〜4 |
| DDL | 1 | batch_execution_log テーブル（spec #1 に定義済み） |
| Docker 設定 | 1 | docker-compose.yml にバッチコンテナ追加 |
| AWS 設定 | 3 | ECS タスク定義、EventBridge スケジュール x2 |
| **合計** | **~36** | |

---

## 実装順序

| Phase | タスク | 内容 |
|---|---|---|
| Phase 1 | 基盤 | BatchExecutionLogger, BatchDao, BatchConfig, BatchRunner |
| Phase 2 | パターン A (#2,3,4) | monthlyConfirm/Aggregate/Unconfirm（WorkStatusService） |
| Phase 3 | パターン B (#1,11,12,13) | ControlMasterBatch, MasterSyncBatch + Docker バッチコンテナ |
| Phase 4 | パターン C (#5-10) | 集計バッチ群 + WorkStatusService 連携（同期呼び出し統合） |
| Phase 5 | インフラ・監視 | ECS タスク定義、EventBridge、CloudWatch |
