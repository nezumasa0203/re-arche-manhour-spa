# #1 Database Schema — 整合性チェックレポート

**実施日**: 2026-02-26
**対象**: `01-database-schema/spec.md`
**チェック対象**: constitution.md, analysis 6ファイル, spec #2-#10

---

## A. Constitution.md との整合性

| チェック項目 | 結果 | 詳細 |
|---|---|---|
| PostgreSQL 16 | ✅ 合致 | spec: PostgreSQL 16、constitution: PostgreSQL 16 |
| Oracle → PostgreSQL 移行 | ✅ 合致 | GAP-B01 を正しく参照 |
| Doma 2 ORM | ✅ 解消 (FIX-01) | セクション 1.4 に Doma 2 `@Column` マッピング方針を追加。DB snake_case ↔ Java camelCase の変換ルールを明文化済み |
| 論理削除 (DELFLG) 維持 | ✅ 合致 | GAP-D03 KEEP、全テーブルに delflg カラムあり |
| UTF-8 エンコーディング | ✅ 合致 | GAP-N08 対応 |
| 12状態ステータスマトリクス | ✅ 合致 | status CHECK ('0','1','2','9') + mcz04_ctrl の gjkt_flg/data_sk_flg |
| TDD / テスト要件 | ✅ 合致 | セクション8にテスト要件記述あり |
| Docker-First | ✅ 合致 | docker compose up での initdb.d 実行を明記 |

### 指摘事項

1. ~~**ORM との整合**~~ → ✅ FIX-01 で解消。セクション 1.4 にカラム命名マッピング方針を追加。constitution の Doma 2 指定に合わせ `@Column` アノテーション方針を明文化。なお 06_devenv_infrastructure.md の `Spring Data JPA + Hibernate` 記載は分析時点の選択肢であり、constitution で Doma 2 を確定済み。

---

## B. Analysis 6ファイルとの整合性

### 01_system_analysis.md との整合

| 項目 | 分析 | spec | 結果 |
|---|---|---|---|
| トランザクションテーブル数 | 4+ | 5（T1-T5） | ✅ `tcz16_tnt_busyo_rireki` が追加 |
| マスタテーブル数 | 7+ | 10（M1-M10） | ✅ 分析の「7+」を具体化 |
| TCZ01 主キー | SEQNO + SKBTCD | (seqno, skbtcd) | ✅ 合致 |
| TCZ01 SEQNO 型 | CHAR(12) | BIGSERIAL | ✅ 解消 (FIX-09): 意図的変更 (GAP-D04)。セクション 7.1 に CHAR(12)→BIGINT の5段階移行手順を明記済み |
| TCZ01 SGYYMD 型 | CHAR(8) YYYYMMDD | DATE | ✅ GAP-D06 の通り |
| TCZ01 STATUS 値 | 0/1/2 | CHECK ('0','1','2','9') | ✅ STATUS_9（非表示）を含めて完全 |

### 02_actor_definition.md との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| mcz04_ctrl の sysid | ✅ 合致 | '00'=管理者(M0), '01'=担当者(M1)。spec と分析で一致 |
| 4層権限モデルのDB反映 | ℹ️ 対象外 | 権限情報は ALB+OIDC の JWT クレームから取得するため DB テーブルは不要（#2 auth-infrastructure の管轄）。開発用シードデータは auth-mock の actors.json で管理 |

### 03_user_stories.md との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| US-ID 対応表 | ✅ 解消 (FIX-07) | セクション 9 を全面改訂。全38ストーリー (US-010〜054) の画面別対応表 + テーブル×US-ID 逆引き表を追加済み |
| VR ルール対応 | ✅ 合致 | VR-006 (件名128バイト)、VR-011 (TMR5文字)、VR-012 (依頼書No7文字) が型に反映済み |

### 04_screen_transition.md との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| 画面で使用するテーブル | ✅ 合致 | FORM_010→tcz01, FORM_020→tcz01+mcz04, FORM_030-042→tcz13+tcz14+tcz19 |
| ダイアログで使用するテーブル | ✅ 合致 | DLG-ORG→mcz12, DLG-SYS→mav01/mav03, DLG-TNT→組織階層 |

### 05_gap_analysis.md との整合

| GAP-ID | spec での対応 | 結果 |
|---|---|---|
| GAP-B01 | セクション10 IMPROVE | ✅ |
| GAP-D01 | セクション2 型マッピング | ✅ |
| GAP-D02 | CHAR→VARCHAR変換 | ✅ |
| GAP-D03 | 論理削除維持 | ✅ |
| GAP-D04 | 採番→BIGSERIAL | ✅ |
| GAP-D05 | YYYYX形式維持 | ✅ |
| GAP-D06 | YYYYMMDD→DATE | ✅ |
| GAP-D09 | JDBC変更 | ✅ |
| GAP-N08 | UTF-8 | ✅ |
| GAP-D07 | POI .xls→.xlsx | N/A（Excel仕様のため対象外） |

### 06_devenv_infrastructure.md との整合

| 項目 | 結果 | 詳細 |
|---|---|---|
| db コンテナ | ✅ 合致 | postgres:16, ポート 5432 |
| 初期化SQL配置 | ✅ 解消 (FIX-06) | `db/init/01-schema.sql` を正と明記済み。infra定義との差異も注記追加 |

---

## C. 他 Spec (#2-#10) との整合性

### 検出された不整合（全件解消済み）

> **9件中 8件を修正完了、1件は許容として対応済み。未解決の不整合はなし。**

| # | 不整合内容 | 対応状況 | FIX-ID | 対応内容 |
|---|---|---|---|---|
| 1 | カラム命名規則の不一致 (元: HIGH) | ✅ 解消 | FIX-01 | セクション 1.4 にマッピング表追加 |
| 2 | mcz04_ctrl の yyyymm 形式 (元: MID) | ✅ 解消 | FIX-02 | DB/API 双方の形式と変換ルールを明記 |
| 3 | 工数の型 (元: MID) | ✅ 解消 | FIX-03 | 時間単位 (2.50=2h30m)、API は "HH:MM" |
| 4 | tcz01 の year_half カラム名 (元: MID) | ℹ️ 許容 | — | テーブル別に旧システムの命名を踏襲。year_half (tcz01) / nendo_half (tcz13,tcz14) |
| 5 | batch_execution_log テーブル (元: MID) | ✅ 解消 | FIX-05 | T6 として追加 |
| 6 | mcz15, mcz24, mcz21 の詳細定義不足 (元: MID) | ✅ 解消 | FIX-04 | 旧ソースから復元して追加 |
| 7 | tcz16_tnt_busyo_rireki の詳細定義不足 (元: MID) | ✅ 解消 | FIX-04 | 旧ソースから復元して追加 |
| 8 | optimistic locking 用 updatedAt (元: MID) | ✅ 解消 | FIX-08 | upddate 説明に楽観ロック用 (CZ-101) を明記 |
| 9 | 初期化SQLパス (元: MID) | ✅ 解消 | FIX-06 | constitution 準拠パスを正とし注記追加 |

### 整合している点

| 項目 | 結果 |
|---|---|
| 全テーブル名（15テーブル）が他specで正しく参照されている | ✅ |
| CHECK制約の値域が validation-error-system (#8) と合致 | ✅ |
| 外部キーの論理参照方針が core-api-design (#3) と合致 | ✅ |
| 12状態ステータスマトリクスの値が全specで一貫 | ✅ |
| 年度半期 YYYYX 形式の維持方針が analysis-screens (#6) と合致 | ✅ |

---

## D. 旧システムとの仕様整合性・変更点まとめ

### KEEP（踏襲）— 変更なし

| # | 項目 | 旧 | 新 | 根拠 |
|---|---|---|---|---|
| 1 | テーブル数 | 4 トランザクション + 7+ マスタ | 5 トランザクション + 10 マスタ | 全テーブル移行 |
| 2 | 主キー構造 | 各テーブル同一 | 同一 | — |
| 3 | 論理削除 (DELFLG) | 全テーブルに存在 | 全テーブルに存在 | GAP-D03 |
| 4 | 監査カラム (INITNT/INIDATE/UPDTNT/UPDDATE/UPDPGID) | 全テーブルに存在 | 全テーブルに存在（型変更あり） | — |
| 5 | 年度半期 YYYYX 形式 | CHAR(5) | VARCHAR(5) | GAP-D05 |
| 6 | 12状態ステータスマトリクス | sts_base_key 000-911 | status CHECK + mcz04 flags | — |
| 7 | カテゴリの有効期間管理 | 複合キーに開始/終了日 | 同一 | — |
| 8 | MYシステム (tcz19) | 担当者×システム | 同一 | — |

### IMPROVE（改善）— 技術的改善

> 下表の「影響度」は移行作業の規模を示す指標であり、未解決の問題ではありません。

| # | 項目 | 旧 (Oracle) | 新 (PostgreSQL) | 影響度 | GAP-ID |
|---|---|---|---|---|---|
| 1 | **DB エンジン** | Oracle Database | PostgreSQL 16 | 🔴 HIGH | GAP-B01 |
| 2 | **文字コード** | Windows-31J (Shift_JIS) | UTF-8 | 🔴 HIGH | GAP-N08 |
| 3 | **固定長→可変長** | CHAR(n) 全カラム | VARCHAR(n) | 🟡 MID | GAP-D02 |
| 4 | **日付文字列→DATE型** | CHAR(8) YYYYMMDD | DATE | 🟡 MID | GAP-D06 |
| 5 | **監査日時** | Oracle DATE | TIMESTAMP WITH TIME ZONE | 🟡 MID | GAP-D06 |
| 6 | **連番生成** | 採番マスタ (SaibanMst) | BIGSERIAL / SEQUENCE | 🟡 MID | GAP-D04 |
| 7 | **JDBC ドライバ** | DataDirect Oracle JDBC | PostgreSQL JDBC | 🟢 LOW | GAP-D09 |
| 8 | **命名規則** | 大文字+プレフィックス結合 (TCZ01HOSYUKOUSUU) | スネークケース小文字 (tcz01_hosyu_kousuu) | 🟢 LOW | — |
| 9 | **SQL 構文** | Oracle PL/SQL 固有 | PostgreSQL 準拠 SQL | 🔴 HIGH | GAP-D01 |

### ADD（追加）

| # | 項目 | 詳細 | GAP-ID |
|---|---|---|---|
| 1 | **batch_execution_log テーブル** | バッチ実行履歴の管理（#10 batch-processing で定義） | GAP-O03 |
| 2 | **インデックス強化** | 6つの明示的インデックス定義（旧システムでは暗黙） | — |
| 3 | **CHECK 制約** | 6つの明示的 CHECK 制約（status, skbtcd, delflg, flags） | — |

### 注意が必要な移行ポイント

| # | ポイント | 詳細 |
|---|---|---|
| 1 | **SEQNO 型変換** | 旧: CHAR(12)（文字列連番） → 新: BIGSERIAL（整数連番）。既存データの変換とSEQUENCE初期値設定が必要 |
| 2 | **日付変換** | 旧: CHAR(8) '20260226' → 新: DATE '2026-02-26'。`TO_DATE()` による一括変換 |
| 3 | **文字コード変換** | Windows-31J 固有文字（①②③、髙﨑等）の移行テスト必須 |
| 4 | **CHAR パディング** | Oracle CHAR は固定長（空白パディング）。PostgreSQL VARCHAR は可変長。WHERE 句での比較結果が変わる可能性あり |

---

## E. 推奨アクション（全9件完了）

> **P0: 3件完了 / P1: 5件完了 / P2: 1件完了 — 残件なし**

| ID | ステータス | アクション | 修正箇所 |
|---|---|---|---|
| FIX-01 | ✅ 完了 | カラム命名マッピング表を追加 | spec 1.4 |
| FIX-02 | ✅ 完了 | yyyymm 格納形式を統一 | spec 3 M2 |
| FIX-03 | ✅ 完了 | 工数カラムの格納単位を確定 | spec 3 T1 |
| FIX-04 | ✅ 完了 | 未定義テーブルのカラム定義追加 (M5,M9,M10,T5) | spec 3 |
| FIX-05 | ✅ 完了 | batch_execution_log テーブルを追加 (T6) | spec 1.1 + 3 |
| FIX-06 | ✅ 完了 | 初期化SQLパスの統一 | spec 概要 |
| FIX-07 | ✅ 完了 | US-ID 対応表の拡充 (全38ストーリー) | spec 9 |
| FIX-08 | ✅ 完了 | 楽観ロック用 upddate の明記 | spec 3 T1 |
| FIX-09 | ✅ 完了 | SEQNO 移行手順の明記 | spec 7.1 |

---

## F. 修正履歴

| 日時 | FIX-ID | 修正内容 | 修正者 |
|---|---|---|---|
| 2026-02-26 | FIX-01 | セクション 1.4「DB ↔ API カラム命名マッピング方針」を新規追加。tcz01_hosyu_kousuu (20カラム) + mcz04_ctrl (4カラム) のマッピング表、Doma 2 `@Column` による変換ルールを記載 | Claude |
| 2026-02-26 | FIX-02 | M2 mcz04_ctrl.yyyymm の説明に「DB格納: YYYYMM 6桁 / API表現: YYYY-MM ハイフン付き」の変換仕様を明記。セクション 1.4 変換ルールにも Service 層変換の詳細を追加 | Claude |
| 2026-02-26 | FIX-03 | T1 tcz01_hosyu_kousuu.kousuu の説明を「時間単位、小数表現（例: 2h30m = 2.50）。API では "HH:MM" 文字列に変換」に更新 | Claude |
| 2026-02-26 | FIX-04 | 4テーブルのカラム定義を追加: M5 mcz15_ts_sys (15カラム)、M9 mcz24_tanka (11カラム)、M10 mcz21_kanri_taisyo (2カラム)、T5 tcz16_tnt_busyo_rireki (11カラム)。旧ソースコードのエンティティ定義から復元 | Claude |
| 2026-02-26 | FIX-05 | T6 batch_execution_log (8カラム) をテーブル一覧 + 定義詳細に追加。#10 batch-processing で定義された新規テーブル (GAP-O03) | Claude |
| 2026-02-26 | FIX-06 | 概要セクションに DDL 配置パス `db/init/01-schema.sql` を正とする旨を明記。06_devenv_infrastructure.md の `infra/docker/db/init.sql` との差異を注記 | Claude |
| 2026-02-26 | FIX-07 | セクション 9 を全面改訂。5行の簡易表 → 全38ストーリー (US-010〜054) の画面別対応表 + テーブル×US-ID 逆引き表を追加 | Claude |
| 2026-02-26 | FIX-08 | T1 tcz01_hosyu_kousuu.upddate の説明に「**楽観ロック用**（CZ-101: 同時編集検知に使用）」を追記 | Claude |
| 2026-02-26 | FIX-09 | セクション 7.1「SEQNO 移行手順」を新規追加。CHAR(12)→BIGINT の5段階変換手順、setval() による SEQUENCE 初期値設定、バリデーション注意事項を記載 | Claude |
