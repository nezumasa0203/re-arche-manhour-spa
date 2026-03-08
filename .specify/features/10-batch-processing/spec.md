# バッチ処理: Oracle PL/SQL → PostgreSQL + スケジューラ移行仕様

## 概要

現行 Oracle PL/SQL で実装された 13 バッチ SQL を
PostgreSQL 互換 SQL + Spring Boot バッチ (ECS スケジュールタスク) に移行する。
バッチ処理は主に月次集計・制御マスタ管理・データグルーピングを担う。

**対応 GAP**: GAP-O01〜O05

---

## 1. バッチ処理一覧

### 1.1 現行バッチ SQL（13ファイル）

| # | バッチ名 | 種別 | 頻度 | 概要 | 関連テーブル |
|---|---------|------|------|------|------------|
| 1 | 制御マスタ作成 | 初期化 | 月次 | 新月度の mcz04_ctrl レコード作成 | mcz04_ctrl |
| 2 | 月次確認フラグ設定 | 制御 | オンデマンド | gjkt_flg = '1' 更新 | mcz04_ctrl |
| 3 | データ集約フラグ設定 | 制御 | オンデマンド | data_sk_flg = '1' 更新 | mcz04_ctrl |
| 4 | 月次未確認リセット | 制御 | オンデマンド | gjkt_flg = '0', data_sk_flg = '0' リセット | mcz04_ctrl |
| 5 | サブシステム集計（対象別）| 集計 | 月次 | tcz01 → tcz13 (sumkbn='0') | tcz01, tcz13_subsys_sum |
| 6 | サブシステム集計（原因別）| 集計 | 月次 | tcz01 → tcz13 (sumkbn='1') | tcz01, tcz13_subsys_sum |
| 7 | グループキー作成 | 集計 | 半期 | tcz14_grp_key レコード生成 | tcz14_grp_key, mav01_sys |
| 8 | 半期集計 | 集計 | 半期 | 半期推移画面用の集計データ作成 | tcz13_subsys_sum |
| 9 | 月別内訳集計 | 集計 | 月次 | 月別内訳画面用の集計データ作成 | tcz13_subsys_sum |
| 10 | 集計データリセット | 保守 | オンデマンド | tcz13 の該当月データ DELETE | tcz13_subsys_sum |
| 11 | 組織マスタ同期 | マスタ | 日次 | 外部組織マスタ → mcz12_orgn_kr 同期 | mcz12_orgn_kr |
| 12 | システムマスタ同期 | マスタ | 日次 | 外部システムマスタ → mav01/mav03 同期 | mav01_sys, mav03_subsys |
| 13 | 担当者履歴更新 | マスタ | 日次 | 担当者所属変更の履歴記録 | tcz16_tnt_busyo_rireki |

### 移行元 PL/SQL ファイル参照

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

※ 全ファイルは `D:\PROJECT02\migration_source\migration_soource_irpmng_czConsv\` 配下に格納

### 1.2 バッチ分類

| 分類 | バッチ # | 実行タイミング |
|------|---------|-------------|
| **月次制御** | 1, 2, 3, 4 | 月初自動 + 画面操作連動 |
| **集計（API トリガー）** | 5, 6, 7, 8, 9, 10 | 月次確認/集約操作時に同期実行（パターン C） |
| **マスタ同期** | 11, 12, 13 | 日次夜間バッチ |

---

## 2. 移行方針

### 2.1 実行基盤

| 項目 | 現行 | 新規 |
|------|------|------|
| DB | Oracle PL/SQL | **PostgreSQL 関数 + Spring Boot** |
| 実行 | シェルスクリプト手動実行 | **ECS Scheduled Task (Fargate)** |
| スケジューラ | cron (サーバー) | **AWS EventBridge Scheduler** |
| 監視 | ログファイル | **CloudWatch Logs + アラーム** |
| 通知 | なし | **SNS → Slack/メール通知** |

### 2.2 実装パターン

バッチ処理は2パターンで実装:

**パターン A: API 連動型** (バッチ #2, 3, 4)
- 画面操作 (FORM_020 月次確認/集約/未確認) で直接実行
- 既存 `WorkStatusService` 内で処理
- 別途バッチ不要（API で完結）

**パターン B: スケジュール型** (バッチ #1, 11, 12, 13)
- Spring Boot アプリケーションのバッチモジュールとして実装
- ECS Scheduled Task で定期実行（EventBridge → コンテナ起動 → バッチ実行 → 終了）
- `CommandLineRunner` + `--batch=<name>` 引数で実行するバッチを指定

**パターン C: API トリガー型** (バッチ #5, 6, 7, 8, 9, 10)
- 月次確認/集約操作（FORM_020）を起点に同期実行
- `WorkStatusService` 内から集計ロジックを直接呼び出す
- 別途スケジューラ不要（API リクエスト内で完結）

---

## 3. バッチ詳細仕様

### 3.1 制御マスタ作成 (#1)

**実行タイミング**: 毎月1日 0:00 (EventBridge)
**処理内容**:

```sql
-- 翌月の制御マスタレコードを作成（存在しない場合のみ）
INSERT INTO mcz04_ctrl (sysid, yyyymm, online_flg, gjkt_flg, data_sk_flg,
                         initnt, inidate, delflg)
SELECT s.sysid, :next_yyyymm, '0', '0', '0',
       'BATCH', CURRENT_TIMESTAMP, '0'
FROM (VALUES ('00'), ('01')) AS s(sysid)
WHERE NOT EXISTS (
    SELECT 1 FROM mcz04_ctrl c
    WHERE c.sysid = s.sysid AND c.yyyymm = :next_yyyymm
);
```

### 3.2 月次確認/集約/リセット (#2, 3, 4)

**パターン A**: API 連動型。`WorkStatusService` で実装済み。

```java
// WorkStatusService.java (既存) — パターン C: API トリガー型
@Transactional
public void monthlyConfirm(String yearMonth, String organizationCode) {
    // SELECT FOR UPDATE による排他
    ControlEntity ctrl = controlDao.selectForUpdate(sysid, yearMonth);
    ctrl.setGjktFlg("1");
    // data_sk_flg は操作しない（集約は別操作: spec #3 section 3.21）
    controlDao.update(ctrl);
    // 確認後の集計を同一トランザクション内で同期実行
    subsystemAggregationBatch.executeTarget(yearMonth);  // バッチ #5
    subsystemAggregationBatch.executeCause(yearMonth);    // バッチ #6
}
```

> **triggerAggregation の方式**: 非同期キューやイベント駆動ではなく、
> `SubsystemAggregationBatch` のロジックを直接メソッド呼び出しする同期方式。
> トランザクション内で実行するため、集計失敗時は確認フラグ設定もロールバックされる。
> バッチ #7〜#9 も同様に、集約操作（`data_sk_flg` 設定）時に同期呼び出しする。

### 3.3 サブシステム集計 — 対象別 (#5)

**実行タイミング**: 月次確認フラグ設定後に自動実行
**処理内容**: `tcz01_hosyu_kousuu` から対象 SS 別に工数を集計し `tcz13_subsys_sum` に格納。

```sql
-- 既存データ削除
DELETE FROM tcz13_subsys_sum
WHERE yyyymm = :yyyymm AND sumkbn = '0';

-- 対象サブシステム別集計
INSERT INTO tcz13_subsys_sum (
    yyyymm, nendo_half, month, sumkbn,
    sys_kbn, sknno, subsknno, aplid,
    hs_syubetu, hs_unyou_kubun, hs_kategori_id,
    skbtcd, hs_kousuu,
    initnt, inidate, delflg
)
SELECT
    t.yyyymm,
    :nendo_half,
    SUBSTRING(t.yyyymm, 5, 2),
    '0',                           -- sumkbn: 対象別
    s.sys_kbn,
    t.taisyo_sknno,               -- 対象システムNo
    t.taisyo_subsknno,            -- 対象サブシステムNo
    sub.aplid,
    k.hs_syubetu,
    k.hs_unyou_kubun,
    t.hs_kategori_id,
    t.skbtcd,
    SUM(t.hs_kousuu),             -- 工数合計
    'BATCH', CURRENT_TIMESTAMP, '0'
FROM tcz01_hosyu_kousuu t
JOIN mav01_sys s ON s.sknno = t.taisyo_sknno
JOIN mav03_subsys sub ON sub.sknno = t.taisyo_sknno
                      AND sub.subsysno = t.taisyo_subsknno
JOIN mcz02_hosyu_kategori k ON k.hs_kategori = t.hs_kategori_id
WHERE t.yyyymm = :yyyymm
  AND t.delflg = '0'
  AND t.status IN ('1', '2')      -- 確認/確定のみ
GROUP BY t.yyyymm, s.sys_kbn, t.taisyo_sknno, t.taisyo_subsknno,
         sub.aplid, k.hs_syubetu, k.hs_unyou_kubun, t.hs_kategori_id,
         t.skbtcd;
```

### 3.4 サブシステム集計 — 原因別 (#6)

#5 と同様。`taisyo_sknno/subsknno` を `genin_sknno/subsknno` に変更、`sumkbn = '1'`。

### 3.5 グループキー作成 (#7)

**実行タイミング**: 半期の最初の月次集約完了後
**処理内容**: システム別の分類・担当情報をグルーピングキーとして作成。

```sql
INSERT INTO tcz14_grp_key (
    nendo_half, sknno,
    hshk_bunrui_code, is_kyk_hs_tnt_bs_code,
    kh_tnt_bs_code, sys_kan_k_tbs_code,
    initnt, inidate, delflg
)
SELECT
    :nendo_half,
    s.sknno,
    s.hshk_bunrui_code,
    s.is_kyk_hs_tnt_bs_code,
    s.kh_tnt_bs_code,
    s.sys_kan_k_tbs_code,
    'BATCH', CURRENT_TIMESTAMP, '0'
FROM mav01_sys s
WHERE s.delflg = '0'
ON CONFLICT (nendo_half, sknno) DO UPDATE SET
    hshk_bunrui_code = EXCLUDED.hshk_bunrui_code,
    kh_tnt_bs_code = EXCLUDED.kh_tnt_bs_code,
    updtnt = 'BATCH',
    upddate = CURRENT_TIMESTAMP;
```

### 3.6 マスタ同期 (#11, 12, 13)

**実行タイミング**: 毎日 2:00 (EventBridge)
**処理内容**: 外部マスタ（人事システム等）からの差分同期。

UPSERT パターンで実装:

外部マスタ連携は **2方式を並行実装** する:

#### 方式 A: CSV ファイルアップロード

管理者が CSV ファイルをアップロードし、バッチがステージングテーブル経由で UPSERT する。

```
[管理者] → CSV アップロード → [staging_org_import] → バッチ UPSERT → [mcz12_orgn_kr]
```

```sql
-- ステージングテーブルからの UPSERT
INSERT INTO mcz12_orgn_kr (org_code, parent_org_code, org_name, org_level, ...)
SELECT org_code, parent_org_code, org_name, org_level, ...
FROM staging_org_import
WHERE import_batch_id = :batchId
ON CONFLICT (org_code) DO UPDATE SET
    org_name = EXCLUDED.org_name,
    parent_org_code = EXCLUDED.parent_org_code,
    upddate = CURRENT_TIMESTAMP;
```

- CSV フォーマット: UTF-8、ヘッダ行あり、カンマ区切り
- バリデーション: 必須項目チェック、コード体系チェック、親組織存在チェック
- エラー処理: 行単位でスキップし、エラーログに記録

#### 方式 B: ASTERIA 経由 API 接続

ASTERIA（データ連携基盤）が外部システム（人事システム等）から取得したデータを
CZ の受信 API に POST する。

```
[外部システム] → [ASTERIA] → POST /api/v1/batch/master-sync → [staging → UPSERT]
```

```sql
-- ASTERIA から受信したデータは staging テーブルに INSERT 後、
-- 方式 A と同じ UPSERT ロジックで mcz12_orgn_kr に反映
```

- 受信 API: `POST /api/v1/batch/master-sync/{masterType}` （masterType: `organization`, `management-target`）
- 認証: API キー認証（`X-Api-Key` ヘッダ）
- ペイロード: JSON 配列形式
- 共通ステージングテーブル: 方式 A/B とも同一テーブルを使用し、UPSERT ロジックを共通化

#### レガシー方式との対応

> **レガシー**: 同一 Oracle インスタンス内の外部テーブル（`MEU00SIK` 等）を PL/SQL バッチで直接 JOIN し BULK COLLECT → FOR LOOP UPDATE。
> **移行後**: 外部テーブルへの直接アクセスを廃止し、CSV アップロード / ASTERIA API の2経路でステージングテーブルに投入後、共通 UPSERT ロジックで同期。

---

## 4. Spring Boot バッチ実装

### 4.1 パッケージ構成

```
com.example.czConsv/
├── batch/
│   ├── BatchConfig.java                 ← バッチ設定
│   ├── ControlMasterBatch.java          ← #1 制御マスタ作成
│   ├── SubsystemAggregationBatch.java   ← #5, 6 SS集計
│   ├── GroupKeyBatch.java               ← #7 グループキー
│   ├── HalfAggregationBatch.java        ← #8 半期集計
│   ├── MonthlyAggregationBatch.java     ← #9 月別集計
│   ├── AggregationResetBatch.java       ← #10 集計リセット
│   ├── MasterSyncBatch.java             ← #11, 12, 13 マスタ同期
│   └── BatchExecutionLogger.java        ← 実行履歴記録
├── batch/repository/
│   └── BatchDao.java                    ← バッチ用 Doma 2 DAO
└── batch/sql/
    ├── subsystem_aggregation_target.sql  ← 2Way SQL
    ├── subsystem_aggregation_cause.sql
    ├── group_key_upsert.sql
    └── master_sync_*.sql
```

### 4.2 バッチ実行方式

**パターン B（スケジュール型）: CommandLineRunner 方式**

本番環境では EventBridge がコンテナを起動し、引数でバッチを指定する。
`@Scheduled` は使用しない（二重スケジューリング防止）。

```java
@Component
@Profile("batch")
public class BatchRunner implements CommandLineRunner {

    @Override
    public void run(String... args) {
        String batchName = Arrays.stream(args)
            .filter(a -> a.startsWith("--batch="))
            .map(a -> a.substring("--batch=".length()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("--batch=<name> is required"));

        switch (batchName) {
            case "control-master" -> controlMasterBatch.execute();
            case "master-sync"   -> masterSyncBatch.execute();
            default -> throw new IllegalArgumentException("Unknown batch: " + batchName);
        }
    }
}
```

> **開発環境**: `@Profile("dev")` + `@Scheduled` で開発中のローカル動作確認に限定使用可。
> 本番・ステージング環境では `@Profile("batch")` の `CommandLineRunner` のみ。

**パターン C（API トリガー型）: 同期呼び出し**

```java
// WorkStatusService.java 内
@Transactional
public void monthlyConfirm(String yearMonth, String organizationCode) {
    ControlEntity ctrl = controlDao.selectForUpdate(sysid, yearMonth);
    ctrl.setGjktFlg("1");
    controlDao.update(ctrl);
    // 集計バッチ #5, #6 を同一トランザクション内で同期実行
    subsystemAggregationBatch.executeTarget(yearMonth);  // #5 対象別
    subsystemAggregationBatch.executeCause(yearMonth);    // #6 原因別
}
```

### 4.3 実行履歴テーブル

```sql
CREATE TABLE batch_execution_log (
    id              BIGSERIAL PRIMARY KEY,
    batch_name      VARCHAR(50) NOT NULL,
    started_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at     TIMESTAMP WITH TIME ZONE,
    status          VARCHAR(10) NOT NULL,  -- 'RUNNING','SUCCESS','FAILED'
    records_affected INTEGER DEFAULT 0,
    error_message   TEXT,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

---

## 5. AWS インフラ構成

### 5.1 ECS Scheduled Task

```yaml
# aws/batch-task-definition.json
{
  "family": "cz-batch",
  "containerDefinitions": [{
    "name": "cz-batch",
    "image": "${ECR_REPO}/cz-backend:latest",
    "command": ["java", "-jar", "app.jar", "--spring.profiles.active=batch"],
    "environment": [
      { "name": "SPRING_PROFILES_ACTIVE", "value": "batch" },
      { "name": "DB_HOST", "value": "${DB_HOST}" }
    ],
    "logConfiguration": {
      "logDriver": "awslogs",
      "options": {
        "awslogs-group": "/ecs/cz-batch",
        "awslogs-region": "ap-northeast-1"
      }
    }
  }]
}
```

### 5.2 EventBridge スケジュール

| スケジュール名 | cron 式 | バッチ |
|-------------|---------|--------|
| cz-monthly-ctrl | `cron(0 0 1 * ? *)` | #1 制御マスタ作成 |
| cz-daily-sync | `cron(0 2 * * ? *)` | #11, 12, 13 マスタ同期 |

集計バッチ (#5〜10) はパターン C（API トリガー型）で実行。月次確認/集約操作時に `WorkStatusService` 内から同期呼び出し。EventBridge スケジュール不要。

### 5.3 監視・通知 (GAP-O02, O04)

```
バッチ実行
  → CloudWatch Logs (構造化 JSON ログ)
  → CloudWatch Metric Filter (ERROR 検出)
  → CloudWatch Alarm
  → SNS Topic
  → Slack / メール通知
```

---

## 6. Oracle → PostgreSQL 移行ポイント

| Oracle | PostgreSQL | 備考 |
|--------|-----------|------|
| `SYSDATE` | `CURRENT_TIMESTAMP` | |
| `NVL(a, b)` | `COALESCE(a, b)` | |
| `TO_DATE('20250201','YYYYMMDD')` | `TO_DATE('20250201','YYYYMMDD')` | 互換 |
| `MERGE INTO` | `INSERT ... ON CONFLICT DO UPDATE` | UPSERT |
| `ROWNUM` | `LIMIT` / `ROW_NUMBER()` | |
| `VARCHAR2(n)` | `VARCHAR(n)` | |
| `NUMBER(10,2)` | `NUMERIC(10,2)` | |
| PL/SQL プロシージャ | PostgreSQL 関数 or Spring Boot Java | ビジネスロジックは Java 推奨 |

---

## 7. GAP 対応マッピング

| GAP ID | 区分 | 本 spec での対応 |
|--------|------|----------------|
| GAP-O01 | IMPROVE | セクション 2, 3 — PL/SQL → PostgreSQL SQL + Spring Boot |
| GAP-O02 | IMPROVE | セクション 5.2, 5.3 — EventBridge + CloudWatch 監視 + 通知 |
| GAP-O03 | ADD/P2 | セクション 4.3 — batch_execution_log テーブル（管理画面は P2） |
| GAP-O04 | IMPROVE | セクション 5.3 — 構造化 JSON ログ + CloudWatch Logs |
| GAP-O05 | ADD | 既存 `/actuator/health` エンドポイントで対応済み |

---

## 8. テスト要件

### 8.1 バッチ単体テスト

| テスト | 内容 |
|--------|------|
| ControlMasterBatch | 翌月レコード作成、重複時スキップ |
| SubsystemAggregationBatch | 対象別/原因別の集計結果の正確性 |
| GroupKeyBatch | UPSERT（新規 INSERT + 既存 UPDATE） |
| MasterSyncBatch | 差分同期（追加/更新/論理削除） |
| AggregationResetBatch | 該当月データの完全削除 |

### 8.2 統合テスト

| テスト | 内容 |
|--------|------|
| 月次フロー | 制御マスタ作成 → 確認 → 集約 → 集計 → 画面表示確認 |
| 集計精度 | tcz01 の手動データ → tcz13 集計結果の数値一致 |
| マスタ同期 | 外部データ変更 → バッチ実行 → 反映確認 |
| リカバリ | バッチ途中失敗 → 再実行 → 冪等性確認 |
| 排他制御 | 月次確認中のバッチ実行 → 排他エラー |

### 8.3 受け入れ基準（Given-When-Then）

**AC-BA-01: 制御マスタ作成（#1, パターン B）**
- Given: 2025年03月の制御マスタが存在しない
- When: ControlMasterBatch を実行する
- Then: sysid='00','01' の2レコードが yyyymm='202503' で作成される

**AC-BA-02: 制御マスタ冪等性**
- Given: 2025年03月の制御マスタが既に存在する
- When: ControlMasterBatch を再実行する
- Then: 新規レコードは作成されず、既存データも変更されない

**AC-BA-03: 繰越データ作成（#2, パターン A）**
- Given: 2025年上期のデータが確定済み
- When: POST /work-hours/transfer が呼ばれる
- Then: 翌期の tcz01 レコードが作成され、分類・システム情報が引き継がれる

**AC-BA-04: 繰越データ重複防止**
- Given: 繰越先にすでにレコードが存在する
- When: 繰越バッチを実行する
- Then: INSERT WHERE NOT EXISTS により重複作成されない

**AC-BA-05: サブシステム集計 対象別（#5, パターン C）**
- Given: tcz01 に yyyymm='202502', status IN ('1','2') のレコードが3件
- When: executeTarget('202502') を同期実行する
- Then: tcz13 に sumkbn='0' の集計レコードが作成され、hs_kousuu は3件の合計値と一致する

**AC-BA-06: 月次集約トリガー**
- Given: 月次確定（monthly-confirm）が完了した
- When: triggerAggregation がトランザクション内で同期呼び出しされる
- Then: パターン C バッチ（#5〜#10）が順次実行され、集計結果が tcz13/tcz14 に反映される

**AC-BA-07: マスタ同期 — 組織マスタ（#11, パターン B）**
- Given: 外部ソースに新しい組織データが存在する
- When: OrganizationSyncBatch を実行する
- Then: mcz12_orgn_kr テーブルが UPSERT で更新される

**AC-BA-08: マスタ同期バリデーション**
- Given: 外部データに必須項目が空のレコードが含まれている
- When: マスタ同期バッチを実行する
- Then: 該当行はスキップされ、エラーログに記録される。他の正常行は処理が継続される

**AC-BA-09: バッチ実行ログ記録**
- Given: 任意のバッチが実行開始される
- When: バッチ処理が完了する
- Then: batch_execution_log に batch_name, status(SUCCESS/FAILED), started_at, finished_at, processed_count が記録される

**AC-BA-10: 二重起動防止**
- Given: batch_execution_log に同一バッチの status='RUNNING' レコードが存在する
- When: 同一バッチの起動を試みる
- Then: 起動がスキップされ、警告ログが出力される

**AC-BA-11: 排他制御（SELECT FOR UPDATE）**
- Given: 集約バッチが tcz01 レコードをロック中
- When: 別プロセスが同一レコードを更新しようとする
- Then: ロック解放まで待機し、データ不整合が発生しない

**AC-BA-12: ECS スケジュール実行（パターン B）**
- Given: EventBridge ルールが cron(0 0 * * ? *) で設定されている
- When: スケジュール時刻に到達する
- Then: ECS タスクが起動し、CommandLineRunner が --batch=control-master 引数でバッチを実行する

**AC-BA-13: バッチ失敗時アラート**
- Given: バッチ実行中にエラーが発生し status='FAILED' で記録される
- When: CloudWatch アラームが FAILED を検知する
- Then: SNS 経由で運用チームに通知される
