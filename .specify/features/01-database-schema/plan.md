# Database Schema Migration 実装計画

## 概要

既存 CZ（保有資源管理システム）の Oracle Database スキーマを PostgreSQL 16 に完全移行する。
16テーブル（トランザクション6 + マスタ10）の DDL を PostgreSQL 準拠で再定義し、
型マッピング（CHAR→VARCHAR、YYYYMMDD→DATE、採番マスタ→BIGSERIAL）、制約、インデックスを含む
`db/init/01-schema.sql` を生成する。Docker 初期化スクリプトとして自動実行される。

---

## アーキテクチャ

### フロントエンド

- 本 spec はデータベース層のみ。フロントエンド成果物はなし。

### バックエンド

| コンポーネント | 役割 | 備考 |
|---|---|---|
| Doma 2 Entity クラス (16テーブル分) | DB カラムと Java フィールドのマッピング | `@Column` で snake_case → camelCase |
| Doma 2 DAO インターフェース (16テーブル分) | データアクセス層 | 2Way SQL テンプレート使用 |
| 共通 SQL フラグメント | `delflg = '0'` 条件の共通化 | 全クエリに適用 |

**Doma 2 Entity 設計方針:**
- DB カラム名はスネークケースをそのまま使用（PostgreSQL 標準）
- Java フィールド名は camelCase（`@Column(name = "hssgytnt_esqid")` で明示マッピング）
- API レスポンス DTO は別途 spec #3 で定義（Entity ≠ DTO）

### データベース

**DDL 配置**: `db/init/01-schema.sql`

| カテゴリ | テーブル数 | 内容 |
|---|---|---|
| トランザクション | 6 | tcz01, tcz13, tcz14, tcz16, tcz19, batch_execution_log |
| マスタ | 10 | mcz02, mcz03, mcz04, mcz12, mcz15, mcz17, mcz21, mcz24, mav01, mav03 |
| インデックス | 6 | 主要検索パターン対応 |
| CHECK 制約 | 6 | ステータス、フラグ、識別コード |

**DDL 生成順序（依存関係順）:**
1. uuid-ossp 拡張の有効化
2. マスタテーブル（参照先を先に作成）
3. トランザクションテーブル（参照元を後に作成）
4. インデックス
5. 初期データ投入（mcz04_ctrl 等の制御データ）

**型マッピングの実装:**
- CHAR(n) → VARCHAR(n): 全固定長カラム
- CHAR(8) 日付 → DATE: sgyymd, tnt_str_ymd, tnt_end_ymd, startymd, endymd
- DATE → TIMESTAMP WITH TIME ZONE: 監査カラム (inidate, upddate)
- SEQUENCE（採番マスタ）→ BIGSERIAL: tcz01_hosyu_kousuu.seqno
- SYSDATE → CURRENT_TIMESTAMP: デフォルト値
- Windows-31J → UTF-8: DB エンコーディング

---

## Constitution Check

| 原則 | 準拠 | 確認内容 |
|------|------|----------|
| I. マイグレーション・ファースト | ✅ | 全16テーブルを Oracle から完全移行。YYYYX 半期形式維持（GAP-D05）、論理削除維持（GAP-D03） |
| II. 仕様完全性 | ✅ | 全テーブル定義、型マッピング、制約、インデックス、US-ID 対応表を網羅 |
| III. Docker-First | ✅ | `db/init/01-schema.sql` を `docker-entrypoint-initdb.d` にマウント。`docker compose up` で自動実行 |
| IV. TDD | ✅ | DDL 実行テスト、CHECK 制約テスト、インデックス利用テスト（EXPLAIN ANALYZE）を計画 |
| V. UX-First | — | DB 層のみ。直接該当なし |
| VI. Production Safety | ✅ | 環境別接続設定は application.yml のプロファイル分離で対応 |
| VII. 認証モック | — | DB 層のみ。直接該当なし |
| VIII. CI/CD Safety | ✅ | 単体テストは H2 In-Memory、統合テストは Testcontainers + PostgreSQL 16 |
| IX. 最適技術選定 | ✅ | Doma 2 + 2Way SQL で型安全なデータアクセス。PostgreSQL 標準機能のみ使用 |
| X. コード品質 | ✅ | スネークケース命名統一、Checkstyle 準拠、Conventional Commits |

---

## 技術的リスク

| リスク | 影響度 | 対策 |
|---|---|---|
| Windows-31J 固有文字（丸囲み数字、旧字体）の変換失敗 | 高 | 移行テストで `①②③髙﨑` 等の特殊文字を検証。PostgreSQL UTF-8 で正常格納を確認 |
| CHAR(12) → BIGSERIAL 変換時のデータ不整合 | 高 | 事前バリデーション（`seqno ~ '^[0-9]+$'`）、件数検証、SEQUENCE 初期値設定の手順を明確化 |
| Oracle 固有 SQL の PostgreSQL 互換性 | 中 | バッチ SQL 13ファイルの全変換。`SYSDATE`→`CURRENT_TIMESTAMP`、`NVL`→`COALESCE`、`ROWNUM`→`LIMIT` |
| YYYYMMDD→DATE 変換の不正データ | 中 | 移行前に `TO_DATE` でパース可能かバリデーション。'00000000' 等の異常値ハンドリング |
| 論理削除 `delflg = '0'` のクエリ漏れ | 中 | Doma 2 の 2Way SQL テンプレートで共通化。コードレビューチェックリストに含める |
| H2 と PostgreSQL の SQL 方言差異（テスト信頼性） | 低 | 単体テストは H2（高速）、統合テストは Testcontainers + PostgreSQL（正確性）の2層構造 |

---

## 依存関係

### 依存先（本機能が必要とするもの）

| 項目 | 内容 |
|---|---|
| Docker 環境 | constitution の Docker-First に基づく `docker-compose.yml` |
| 移行元ソース | `D:\PROJECT02\migration_source\migration_soource_irpmng_czConsv` のエンティティ定義 |

### 依存元（本機能に依存するもの）

| spec | 依存内容 |
|---|---|
| #2 auth-infrastructure | mcz12_orgn_kr（組織階層）、mcz21_kanri_taisyo（管理対象） |
| #3 core-api-design | 全テーブルの Doma 2 Entity / DAO 定義 |
| #4 work-hours-input | tcz01_hosyu_kousuu、mcz04_ctrl、mcz02_hosyu_kategori |
| #5 work-status-list | tcz01_hosyu_kousuu、mcz04_ctrl |
| #6 analysis-screens | tcz13_subsys_sum、tcz14_grp_key、mav01_sys、mav03_subsys |
| #9 excel-export | 各テーブルからのデータ取得 |
| #10 batch-processing | tcz13_subsys_sum、tcz14_grp_key、batch_execution_log |

---

## 実装規模の見積もり

| カテゴリ | ファイル数 | 概要 |
|---|---|---|
| DDL スクリプト | 1 | `db/init/01-schema.sql`（16テーブル + インデックス + CHECK 制約） |
| Doma 2 Entity | 16 | 各テーブル対応の Java Entity クラス |
| Doma 2 DAO | 16 | 各テーブル対応の DAO インターフェース |
| 2Way SQL | 16+ | 基本 CRUD + 検索用 SQL テンプレート |
| テスト（DDL） | 1 | DDL 実行・制約検証テスト |
| テスト（Entity/DAO） | 16 | Doma 2 DAO 単体テスト |
| テスト（統合） | 1 | Testcontainers による PostgreSQL 統合テスト |
| **合計** | **~67** | |
