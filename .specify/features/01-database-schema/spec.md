# Database Schema Migration: Oracle → PostgreSQL

## 概要

既存 CZ（保有資源管理システム）の Oracle Database スキーマを
PostgreSQL 16 に移行する。全テーブル定義、型マッピング、制約、
インデックスを再定義し、DDL を生成する。

**DDL 配置パス**: `db/init/01-schema.sql`（constitution scaffold 準拠）
> 06_devenv_infrastructure.md では `infra/docker/db/init.sql` と記載があるが、
> constitution のプロジェクト骨格定義（scaffold-database.md）に従い
> `db/init/01-schema.sql` を正とする。docker-compose.yml の volumes で
> `./db/init:/docker-entrypoint-initdb.d` とマウントする。

**対象 GAP-ID**: GAP-B01, GAP-D01〜D06, GAP-D09, GAP-N08, GAP-O01

## 移行元情報

- **移行元ソース**: `D:\PROJECT02\migration_source\migration_soource_irpmng_czConsv`
- **エンティティ定義**: `czConsv/WEB-INF/src/jp/co/isid/cz/integ/tbl/`
- **バッチSQL**: `batch/sql/` (13ファイル)

---

## 1. テーブル一覧

### 1.1 トランザクションテーブル

| # | Oracle テーブル名 | PostgreSQL テーブル名 | 日本語名 | 説明 |
|---|------------------|----------------------|---------|------|
| T1 | TCZ01HOSYUKOUSUU | tcz01_hosyu_kousuu | 保守工数 | メイン業務テーブル。工数データ |
| T2 | TCZ13SUBSYSSUMTBL | tcz13_subsys_sum | サブシステム集計 | バッチ集計結果 |
| T3 | TCZ14GRPKEYTBL | tcz14_grp_key | グループキー | グルーピング集計 |
| T4 | TCZ19MYSYSTBL | tcz19_my_sys | MYシステム | お気に入りシステム |
| T5 | TCZ16_TNTBUSYORIREKI | tcz16_tnt_busyo_rireki | 担当部署履歴 | 人事異動履歴 |
| T6 | — (新規) | batch_execution_log | バッチ実行履歴 | バッチ実行の監視・障害調査（GAP-O03） |

### 1.2 マスタテーブル

| # | Oracle テーブル名 | PostgreSQL テーブル名 | 日本語名 | 説明 |
|---|------------------|----------------------|---------|------|
| M1 | MCZ02HOSYUKATEGORIMST | mcz02_hosyu_kategori | 保守カテゴリマスタ | 保守区分定義（有効期間付き） |
| M2 | MCZ04CTRLMST | mcz04_ctrl | コントロールマスタ | 月次処理制御 |
| M3 | TBMAV01_SKNNO | mav01_sys | システム管理Noマスタ | システム基本情報 |
| M4 | TBMAV03_SUBSYSTEMNO | mav03_subsys | サブシステムNoマスタ | サブシステム定義 |
| M5 | TBMCZ15_TSSYSNOMST | mcz15_ts_sys | 対象システムNoマスタ | 対象システム定義 |
| M6 | MCZ03APLBUNRUIGRPMST | mcz03_apl_bunrui_grp | アプリ分類グループマスタ | アプリ分類 |
| M7 | MCZ17_HSHKBUNRUIGRPMST | mcz17_hshk_bunrui_grp | 保守報告分類グループマスタ | 保守報告分類 |
| M8 | MCZ12ORGNKRSPRD | mcz12_orgn_kr | 組織構造マスタ | 組織階層情報 |
| M9 | TBMCZ24_TANKAMST | mcz24_tanka | 単価マスタ | 工数単価情報 |
| M10 | TBMCZ21_KANRITAISYO | mcz21_kanri_taisyo | 管理対象マスタ | 管理対象定義 |

### 1.3 命名規則の変更

- **Oracle**: 大文字 + プレフィックス結合（例: `TCZ01HOSYUKOUSUU`）
- **PostgreSQL**: スネークケース小文字（例: `tcz01_hosyu_kousuu`）
- **カラム名**: Oracle のプレフィックス（`TCZ01_`）は維持し、スネークケース化

### 1.4 DB ↔ API カラム命名マッピング方針

DB カラムはスネークケース（PostgreSQL 標準）、API / Java エンティティは
camelCase（Java 標準）とする。Doma 2 の `@Column` アノテーションで明示的にマッピングする。

**tcz01_hosyu_kousuu 主要カラムのマッピング表:**

| DB カラム名 (snake_case) | Java フィールド名 (camelCase) | API JSON キー (camelCase) | 備考 |
|---|---|---|---|
| seqno | seqNo | seqNo | BIGSERIAL → Long |
| skbtcd | skbtcd | skbtcd | 識別コード（2桁、変換不要） |
| hssgytnt_esqid | hssgytntEsqid | staffId | API では意味的な名前に変換 |
| hssgytnt_name | hssgytntName | staffName | API では意味的な名前に変換 |
| year_half | yearHalf | yearHalf | YYYYX 形式 |
| sgyymd | sgyymd | workDate | API では DATE → ISO 8601 (YYYY-MM-DD) |
| taisyo_sknno | taisyoSknno | targetSystemNo | API では意味的な名前に変換 |
| taisyo_subsysno | taisyoSubsysno | targetSubsystemNo | API では意味的な名前に変換 |
| cause_sysno | causeSysno | causeSystemNo | API では意味的な名前に変換 |
| cause_subsysno | causeSubsysno | causeSubsystemNo | API では意味的な名前に変換 |
| kenmei | kenmei | subject | API では英語名 |
| hs_kategori | hsKategori | categoryCode | API では英語名 |
| hs_syubetu | hsSyubetu | maintenanceType | API では英語名 |
| status | status | status | 変換不要 |
| kousuu | kousuu | hours | DB: NUMERIC(7,2) 時間単位 → API: "HH:MM" 文字列表現 |
| initnt | initnt | createdBy | API では英語名 |
| inidate | inidate | createdAt | TIMESTAMP WITH TIME ZONE |
| updtnt | updtnt | updatedBy | API では英語名 |
| upddate | upddate | updatedAt | 楽観ロック (CZ-101) に使用 |
| delflg | delflg | — | API には公開しない（内部制御） |

**mcz04_ctrl 主要カラムのマッピング表:**

| DB カラム名 (snake_case) | API JSON キー (camelCase) | 備考 |
|---|---|---|
| sysid | sysId | '00'=管理モード(jinjiMode:false), '01'=人事モード(jinjiMode:true) |
| yyyymm | yearMonth | DB: VARCHAR(6) YYYYMM → API: "YYYY-MM" ハイフン付きに変換 |
| gjkt_flg | monthlyConfirmFlag | 月次確認フラグ (0/1) |
| data_sk_flg | dataAggregationFlag | データ集約フラグ (0/1) |

**変換ルール:**
- DB→API 方向: Doma 2 Entity で DB カラムを Java フィールドにマッピング、
  API レスポンス DTO で Java フィールドを API キーに変換
- `yyyymm` の変換: DB 格納時は `VARCHAR(6)` YYYYMM、API 表現時は `YYYY-MM` 形式。
  変換は Service 層で実施（`"202602"` ↔ `"2026-02"`）

---

## 2. 型マッピング（GAP-D01, GAP-D02）

| Oracle 型 | PostgreSQL 型 | 対象 | 備考 |
|-----------|--------------|------|------|
| CHAR(n) | VARCHAR(n) | 全カラム | GAP-D02: 固定長→可変長 |
| VARCHAR2(n) | VARCHAR(n) | 文字列カラム | |
| NUMBER / NUMERIC(n,m) | NUMERIC(n,m) | 工数等数値 | |
| DATE | TIMESTAMP WITH TIME ZONE | 監査カラム | GAP-D06: 文字列日付→DATE型 |
| CHAR(8) 日付 | DATE | 作業日等 | GAP-D06: YYYYMMDD→DATE |
| CHAR(5) 半期 | VARCHAR(5) | 年度半期 | GAP-D05: YYYYX形式は維持（ビジネスルール依存） |
| SYSDATE | CURRENT_TIMESTAMP | デフォルト値 | |
| SEQUENCE (採番マスタ) | BIGSERIAL / SEQUENCE | TCZ01_SEQNO | GAP-D04 |

### 2.1 日付型の変換方針（GAP-D05, GAP-D06）

**作業日 (TCZ01_SGYYMD)**:
- Oracle: CHAR(8) `'20260226'`
- PostgreSQL: `DATE` 型に変更
- データ移行時に `TO_DATE(sgyymd, 'YYYYMMDD')` で変換

**年度半期 (TCZ01_YEARHALF)**:
- Oracle: CHAR(5) `'20260'` (2026年上期)
- PostgreSQL: `VARCHAR(5)` のまま維持
- 理由: 年度半期の計算ロジック（2015年特例含む）がこの形式に依存

**監査カラム (INIDATE, UPDDATE)**:
- Oracle: DATE
- PostgreSQL: `TIMESTAMP WITH TIME ZONE` + `DEFAULT CURRENT_TIMESTAMP`

---

## 3. テーブル定義詳細

### T1: tcz01_hosyu_kousuu（保守工数）— メインテーブル

主キー: `(seqno, skbtcd)`

| カラム名 | PostgreSQL 型 | NULL | デフォルト | 説明 |
|---------|--------------|------|----------|------|
| seqno | BIGSERIAL | NO | 自動採番 | 連番（GAP-D04: 採番マスタ→SEQUENCE） |
| skbtcd | VARCHAR(2) | NO | '01' | 識別コード（01=ISID, 02=WorkSys） |
| hssgytnt_esqid | VARCHAR(6) | NO | | 保守作業担当者ESQID |
| hssgytnt_name | VARCHAR(40) | YES | | 保守作業担当者名 |
| year_half | VARCHAR(5) | NO | | 年度半期 YYYYX |
| sgyymd | DATE | NO | | 作業年月日（GAP-D06: CHAR→DATE） |
| sys_kbn | VARCHAR(1) | YES | | システム区分 |
| cause_sys_kbn | VARCHAR(1) | YES | | 原因システム区分 |
| taisyo_sknno | VARCHAR(8) | YES | | 対象システム管理No |
| taisyo_subsysno | VARCHAR(8) | YES | | 対象サブシステムNo |
| taisyo_aplid | VARCHAR(8) | YES | | 対象アプリID |
| cause_sysno | VARCHAR(8) | YES | | 原因システムNo |
| cause_subsysno | VARCHAR(8) | YES | | 原因サブシステムNo |
| cause_aplid | VARCHAR(8) | YES | | 原因アプリID |
| kenmei | VARCHAR(128) | YES | | 件名（VR-006: 128バイト以内。CHECK: `octet_length(kenmei) <= 128`） |
| hs_kategori | VARCHAR(4) | YES | | 保守カテゴリID（FK→mcz02） |
| hs_syubetu | VARCHAR(1) | YES | | 保守種別（0/1/2/3/4） |
| hs_unyou_kubun | VARCHAR(1) | YES | | 保守運用区分 |
| tmr_no | VARCHAR(5) | YES | | TMR番号（VR-011: 半角英数5文字） |
| sgy_iraisyo_no | VARCHAR(7) | YES | | 作業依頼書No（VR-012: 空 or 7文字） |
| sgy_iraisya_esqid | VARCHAR(6) | YES | | 作業依頼者ESQID |
| sgy_iraisya_name | VARCHAR(40) | YES | | 作業依頼者名 |
| status | VARCHAR(1) | NO | '0' | ステータス（0:作成中/1:確認/2:確定/9:非表示） |
| kousuu | NUMERIC(7,2) | YES | | 工数（時間単位、小数表現。例: 2時間30分 = 2.50）。API では "HH:MM" 文字列に変換して返却 |
| initnt | VARCHAR(6) | YES | | 作成者 |
| inidate | TIMESTAMP WITH TIME ZONE | YES | CURRENT_TIMESTAMP | 作成日時 |
| updtnt | VARCHAR(6) | YES | | 更新者 |
| upddate | TIMESTAMP WITH TIME ZONE | YES | CURRENT_TIMESTAMP | 更新日時。**楽観ロック用**（CZ-101: 同時編集検知に使用） |
| updpgid | VARCHAR(20) | YES | | 更新プログラムID |
| delflg | VARCHAR(1) | NO | '0' | 削除フラグ（GAP-D03: 論理削除維持） |

### M2: mcz04_ctrl（コントロールマスタ）

主キー: `(sysid, yyyymm)`

| カラム名 | PostgreSQL 型 | NULL | デフォルト | 説明 |
|---------|--------------|------|----------|------|
| sysid | VARCHAR(2) | NO | | システムID（'00'=管理モード/jinjiMode:false, '01'=人事モード/jinjiMode:true。spec #2 セクション 2.2 参照） |
| yyyymm | VARCHAR(6) | NO | | 対象年月 YYYYMM（DB格納: '202602'。API表現時は 'YYYY-MM' 形式に変換） |
| online_flg | VARCHAR(1) | NO | '0' | オンラインフラグ（0=利用可/1=停止） |
| renketsu_flg | VARCHAR(1) | YES | '0' | 連結フラグ |
| gjkt_flg | VARCHAR(1) | YES | '0' | 月次確認フラグ |
| data_sk_flg | VARCHAR(1) | YES | '0' | データ集約フラグ |
| initnt | VARCHAR(6) | YES | | 作成者 |
| inidate | TIMESTAMP WITH TIME ZONE | YES | CURRENT_TIMESTAMP | 作成日時 |
| updtnt | VARCHAR(6) | YES | | 更新者 |
| upddate | TIMESTAMP WITH TIME ZONE | YES | CURRENT_TIMESTAMP | 更新日時 |
| updpgid | VARCHAR(20) | YES | | 更新プログラムID |
| delflg | VARCHAR(1) | NO | '0' | 削除フラグ |

### M1: mcz02_hosyu_kategori（保守カテゴリマスタ）

主キー: `(hs_kategori, yukou_kaishiki, yukou_syuryoki)`

| カラム名 | PostgreSQL 型 | NULL | デフォルト | 説明 |
|---------|--------------|------|----------|------|
| hs_kategori | VARCHAR(4) | NO | | カテゴリID |
| yukou_kaishiki | DATE | NO | | 有効開始日（GAP-D06: VARCHAR(8)→DATE） |
| yukou_syuryoki | DATE | NO | | 有効終了日（GAP-D06: VARCHAR(8)→DATE） |
| hs_syubetu | VARCHAR(1) | YES | | 保守種別（0-4） |
| hs_unyou_kubun | VARCHAR(1) | YES | | 保守運用区分 |
| hs_kategori_mei | VARCHAR(100) | YES | | カテゴリ名 |
| hs_kategori_naiyo | VARCHAR(500) | YES | | カテゴリ説明 |
| initnt | VARCHAR(6) | YES | | 作成者 |
| inidate | TIMESTAMP WITH TIME ZONE | YES | CURRENT_TIMESTAMP | 作成日時 |
| updtnt | VARCHAR(6) | YES | | 更新者 |
| upddate | TIMESTAMP WITH TIME ZONE | YES | CURRENT_TIMESTAMP | 更新日時 |
| updpgid | VARCHAR(20) | YES | | 更新プログラムID |
| delflg | VARCHAR(1) | NO | '0' | 削除フラグ |

### M3: mav01_sys（システム管理Noマスタ）

主キー: `(skbtcd, sknno)`

| カラム名 | PostgreSQL 型 | NULL | デフォルト | 説明 |
|---------|--------------|------|----------|------|
| skbtcd | VARCHAR(2) | NO | | 識別コード |
| sknno | VARCHAR(8) | NO | | システム管理No |
| sys_mei | VARCHAR(100) | YES | | システム名 |
| sys_mei_kn | VARCHAR(100) | YES | | システム名カナ |
| yukou_kaishiki | VARCHAR(8) | YES | | 有効開始日 |
| yukou_syuryoki | VARCHAR(8) | YES | | 有効終了日 |
| initnt | VARCHAR(6) | YES | | 作成者 |
| inidate | TIMESTAMP WITH TIME ZONE | YES | CURRENT_TIMESTAMP | 作成日時 |
| updtnt | VARCHAR(6) | YES | | 更新者 |
| upddate | TIMESTAMP WITH TIME ZONE | YES | CURRENT_TIMESTAMP | 更新日時 |
| updpgid | VARCHAR(20) | YES | | 更新プログラムID |
| delflg | VARCHAR(1) | NO | '0' | 削除フラグ |

### M4: mav03_subsys（サブシステムNoマスタ）

主キー: `(skbtcd, sknno, subsysno)`

| カラム名 | PostgreSQL 型 | NULL | デフォルト | 説明 |
|---------|--------------|------|----------|------|
| skbtcd | VARCHAR(2) | NO | | 識別コード |
| sknno | VARCHAR(8) | NO | | システム管理No |
| subsysno | VARCHAR(8) | NO | | サブシステムNo |
| aplid | VARCHAR(8) | YES | | アプリID |
| subsys_mei | VARCHAR(100) | YES | | サブシステム名 |
| subsys_mei_kn | VARCHAR(100) | YES | | サブシステム名カナ |
| yukou_kaishiki | VARCHAR(8) | YES | | 有効開始日 |
| yukou_syuryoki | VARCHAR(8) | YES | | 有効終了日 |
| initnt | VARCHAR(6) | YES | | 作成者 |
| inidate | TIMESTAMP WITH TIME ZONE | YES | CURRENT_TIMESTAMP | 作成日時 |
| updtnt | VARCHAR(6) | YES | | 更新者 |
| upddate | TIMESTAMP WITH TIME ZONE | YES | CURRENT_TIMESTAMP | 更新日時 |
| updpgid | VARCHAR(20) | YES | | 更新プログラムID |
| delflg | VARCHAR(1) | NO | '0' | 削除フラグ |

### T2: tcz13_subsys_sum（サブシステム集計）

主キー: `(yyyymm, sumkbn, skbtcd, sknno, subsknno, hs_syubetu, hs_unyou_kubun, hs_kategori_id)`

| カラム名 | PostgreSQL 型 | NULL | デフォルト | 説明 |
|---------|--------------|------|----------|------|
| yyyymm | VARCHAR(6) | NO | | 年月 |
| nendo_half | VARCHAR(5) | NO | | 年度半期 |
| month | VARCHAR(2) | NO | | 月 |
| sumkbn | VARCHAR(1) | NO | | 集計区分（0=対象別/1=原因別） |
| sys_kbn | VARCHAR(1) | YES | | システム区分 |
| sknno | VARCHAR(8) | NO | | システム管理No |
| subsknno | VARCHAR(8) | NO | | サブシステムNo |
| aplid | VARCHAR(8) | YES | | アプリID |
| hs_syubetu | VARCHAR(1) | NO | | 保守種別 |
| hs_unyou_kubun | VARCHAR(1) | NO | | 保守運用区分 |
| hs_kategori_id | VARCHAR(4) | NO | | 保守カテゴリID |
| skbtcd | VARCHAR(2) | NO | | 識別コード |
| hs_kousuu | NUMERIC(10,2) | YES | | 保守工数（集計値） |
| initnt | VARCHAR(6) | YES | | 作成者 |
| inidate | TIMESTAMP WITH TIME ZONE | YES | CURRENT_TIMESTAMP | 作成日時 |
| updtnt | VARCHAR(6) | YES | | 更新者 |
| upddate | TIMESTAMP WITH TIME ZONE | YES | CURRENT_TIMESTAMP | 更新日時 |
| updpgid | VARCHAR(20) | YES | | 更新プログラムID |
| delflg | VARCHAR(1) | NO | '0' | 削除フラグ |

### T3: tcz14_grp_key（グループキー）

主キー: `(nendo_half, sknno)`

| カラム名 | PostgreSQL 型 | NULL | デフォルト | 説明 |
|---------|--------------|------|----------|------|
| nendo_half | VARCHAR(5) | NO | | 年度半期 |
| sknno | VARCHAR(8) | NO | | システム管理No |
| hshk_bunrui_code | VARCHAR(10) | YES | | 保守報告分類コード |
| is_kyk_hs_tnt_bs_code | VARCHAR(10) | YES | | IS保守担当者コード |
| kh_tnt_bs_code | VARCHAR(10) | YES | | 管理担当者コード |
| sys_kan_k_tbs_code | VARCHAR(10) | YES | | システム管理部署コード |
| initnt | VARCHAR(6) | YES | | 作成者 |
| inidate | TIMESTAMP WITH TIME ZONE | YES | CURRENT_TIMESTAMP | 作成日時 |
| updtnt | VARCHAR(6) | YES | | 更新者 |
| upddate | TIMESTAMP WITH TIME ZONE | YES | CURRENT_TIMESTAMP | 更新日時 |
| updpgid | VARCHAR(20) | YES | | 更新プログラムID |
| delflg | VARCHAR(1) | NO | '0' | 削除フラグ |

### T4: tcz19_my_sys（MYシステム）

主キー: `(tnt_esqid, sknno)`

| カラム名 | PostgreSQL 型 | NULL | デフォルト | 説明 |
|---------|--------------|------|----------|------|
| tnt_esqid | VARCHAR(6) | NO | | 担当者ESQID |
| sknno | VARCHAR(8) | NO | | システム管理No |
| initnt | VARCHAR(6) | YES | | 作成者 |
| inidate | TIMESTAMP WITH TIME ZONE | YES | CURRENT_TIMESTAMP | 作成日時 |
| updtnt | VARCHAR(6) | YES | | 更新者 |
| upddate | TIMESTAMP WITH TIME ZONE | YES | CURRENT_TIMESTAMP | 更新日時 |
| updpgid | VARCHAR(20) | YES | | 更新プログラムID |
| delflg | VARCHAR(1) | NO | '0' | 削除フラグ |

### M5: mcz15_ts_sys（対象システムNoマスタ）

主キー: `(tssknno, tssubsysno, yukou_syuryoki)`

| カラム名 | PostgreSQL 型 | NULL | デフォルト | 説明 |
|---------|--------------|------|----------|------|
| tssknno | VARCHAR(8) | NO | | 対象システム管理No |
| tssubsysno | VARCHAR(8) | NO | | 対象サブシステムNo |
| yukou_kaishiki | VARCHAR(5) | YES | | 有効開始日 |
| yukou_syuryoki | VARCHAR(5) | NO | | 有効終了日 |
| aplid | VARCHAR(8) | YES | | アプリID |
| tssysname | VARCHAR(128) | YES | | 対象システム名 |
| tssysname_kn | VARCHAR(128) | YES | | 対象システム名カナ |
| tssubsysname | VARCHAR(128) | YES | | 対象サブシステム名 |
| tssubsysname_kn | VARCHAR(128) | YES | | 対象サブシステム名カナ |
| initnt | VARCHAR(6) | YES | | 作成者 |
| inidate | TIMESTAMP WITH TIME ZONE | YES | CURRENT_TIMESTAMP | 作成日時 |
| updtnt | VARCHAR(6) | YES | | 更新者 |
| upddate | TIMESTAMP WITH TIME ZONE | YES | CURRENT_TIMESTAMP | 更新日時 |
| updpgid | VARCHAR(20) | YES | | 更新プログラムID |
| delflg | VARCHAR(1) | NO | '0' | 削除フラグ |

### M9: mcz24_tanka（単価マスタ）

主キー: `(yukou_syuryoki, skbtcd, tanka_kbn)`

| カラム名 | PostgreSQL 型 | NULL | デフォルト | 説明 |
|---------|--------------|------|----------|------|
| yukou_kaishiki | VARCHAR(5) | YES | | 有効開始日 |
| yukou_syuryoki | VARCHAR(5) | NO | | 有効終了日 |
| skbtcd | VARCHAR(2) | NO | | 識別コード |
| tanka_kbn | VARCHAR(2) | NO | | 単価区分 |
| tanka | NUMERIC(13,0) | YES | | 単価（円） |
| initnt | VARCHAR(6) | YES | | 作成者 |
| inidate | TIMESTAMP WITH TIME ZONE | YES | CURRENT_TIMESTAMP | 作成日時 |
| updtnt | VARCHAR(6) | YES | | 更新者 |
| upddate | TIMESTAMP WITH TIME ZONE | YES | CURRENT_TIMESTAMP | 更新日時 |
| updpgid | VARCHAR(20) | YES | | 更新プログラムID |
| delflg | VARCHAR(1) | NO | '0' | 削除フラグ |

### M10: mcz21_kanri_taisyo（管理対象マスタ）

主キー: `(kanritsy_esqid, kanritnt_esqid)`

| カラム名 | PostgreSQL 型 | NULL | デフォルト | 説明 |
|---------|--------------|------|----------|------|
| kanritsy_esqid | VARCHAR(6) | NO | | 管理対象者ESQID |
| kanritnt_esqid | VARCHAR(6) | NO | | 管理担当者ESQID |

### M6: mcz03_apl_bunrui_grp（アプリ分類グループマスタ）

主キー: `(sknno, yukou_kaishiki, yukou_syuryoki)`

| カラム名 | PostgreSQL 型 | NULL | デフォルト | 説明 |
|---------|--------------|------|----------|------|
| sknno | VARCHAR(8) | NO | | システム管理No |
| yukou_kaishiki | VARCHAR(5) | NO | | 有効開始時期（YYYYX 半期形式。GAP-D05 維持） |
| yukou_syuryoki | VARCHAR(5) | NO | | 有効終了時期（YYYYX 半期形式。GAP-D05 維持） |
| aplbunruicode | VARCHAR(4) | YES | | アプリケーション分類コード |
| initnt | VARCHAR(6) | YES | | 作成者 |
| inidate | TIMESTAMP WITH TIME ZONE | YES | CURRENT_TIMESTAMP | 作成日時 |
| updtnt | VARCHAR(6) | YES | | 更新者 |
| upddate | TIMESTAMP WITH TIME ZONE | YES | CURRENT_TIMESTAMP | 更新日時 |
| updpgid | VARCHAR(20) | YES | | 更新プログラムID |
| delflg | VARCHAR(1) | NO | '0' | 削除フラグ |

### M7: mcz17_hshk_bunrui_grp（保守報告分類グループマスタ）

主キー: `(sknno, yukou_kaishiki, yukou_syuryoki)`

| カラム名 | PostgreSQL 型 | NULL | デフォルト | 説明 |
|---------|--------------|------|----------|------|
| sknno | VARCHAR(8) | NO | | システム管理No |
| yukou_kaishiki | VARCHAR(5) | NO | | 有効開始時期（YYYYX 半期形式。GAP-D05 維持） |
| yukou_syuryoki | VARCHAR(5) | NO | | 有効終了時期（YYYYX 半期形式。GAP-D05 維持） |
| bunruicode | VARCHAR(4) | YES | | 保守報告分類コード |
| initnt | VARCHAR(6) | YES | | 作成者 |
| inidate | TIMESTAMP WITH TIME ZONE | YES | CURRENT_TIMESTAMP | 作成日時 |
| updtnt | VARCHAR(6) | YES | | 更新者 |
| upddate | TIMESTAMP WITH TIME ZONE | YES | CURRENT_TIMESTAMP | 更新日時 |
| updpgid | VARCHAR(20) | YES | | 更新プログラムID |
| delflg | VARCHAR(1) | NO | '0' | 削除フラグ |

### M8: mcz12_orgn_kr（組織構造マスタ）

主キー: `(sikcd)`

> バッチ #11（spec #10）で外部人事システムから UPSERT 同期。OrganizationScopeResolver（spec #2 セクション 6.3）が参照する 7 階層組織展開テーブル。

| カラム名 | PostgreSQL 型 | NULL | デフォルト | 説明 |
|---------|--------------|------|----------|------|
| sikcd | VARCHAR(7) | NO | | 組織コード |
| endymd | DATE | YES | | 有効期間終了日（GAP-D06: CHAR(8)→DATE） |
| startymd | DATE | YES | | 有効期間開始日（GAP-D06: CHAR(8)→DATE） |
| krikaisocd | VARCHAR(2) | YES | | 経理階層コード |
| krijsikcd | VARCHAR(7) | YES | | 経理上位組織コード |
| egsyocd | VARCHAR(2) | YES | | 営業所コード |
| showno | VARCHAR(14) | YES | | 表示順 |
| jsikhyojijun | VARCHAR(2) | YES | | 上位組織表示順序 |
| hyojikn | VARCHAR(40) | YES | | 表示名（カナ） |
| hyojikj | VARCHAR(60) | YES | | 表示名（漢字） |
| hyojiryaku | VARCHAR(40) | YES | | 表示名略 |
| iocd | VARCHAR(7) | YES | | 異動有コード |
| sikcdhonb | VARCHAR(7) | YES | | 組織コード（本部）— 階層: HONBU |
| honbhyojikn | VARCHAR(40) | YES | | 本部表示名（カナ） |
| honbhyojikj | VARCHAR(60) | YES | | 本部表示名（漢字） |
| honbhyojiryaku | VARCHAR(40) | YES | | 本部表示名略 |
| sikcdkyk | VARCHAR(7) | YES | | 組織コード（局）— 階層: KYOKU |
| kykhyojikn | VARCHAR(40) | YES | | 局表示名（カナ） |
| kykhyojikj | VARCHAR(60) | YES | | 局表示名（漢字） |
| kykhyojiryaku | VARCHAR(40) | YES | | 局表示名略 |
| sikcdsitu | VARCHAR(7) | YES | | 組織コード（室）— 階層: SHITSU |
| situhyojikn | VARCHAR(40) | YES | | 室表示名（カナ） |
| situhyojikj | VARCHAR(60) | YES | | 室表示名（漢字） |
| situhyojiryaku | VARCHAR(40) | YES | | 室表示名略 |
| sikcdbu | VARCHAR(7) | YES | | 組織コード（部）— 階層: BU |
| buhyojikn | VARCHAR(40) | YES | | 部表示名（カナ） |
| buhyojikj | VARCHAR(60) | YES | | 部表示名（漢字） |
| buhyojiryaku | VARCHAR(40) | YES | | 部表示名略 |
| sikcdka | VARCHAR(7) | YES | | 組織コード（課）— 階層: KA |
| kahyojikn | VARCHAR(40) | YES | | 課表示名（カナ） |
| kahyojikj | VARCHAR(60) | YES | | 課表示名（漢字） |
| kahyojiryaku | VARCHAR(40) | YES | | 課表示名略 |

### T5: tcz16_tnt_busyo_rireki（担当部署履歴）

主キー: `(tnt_kubun, sknno, tnt_end_ymd)`

| カラム名 | PostgreSQL 型 | NULL | デフォルト | 説明 |
|---------|--------------|------|----------|------|
| tnt_kubun | VARCHAR(1) | NO | | 担当区分 |
| sknno | VARCHAR(8) | NO | | システム管理No |
| tnt_str_ymd | DATE | YES | | 担当開始日（GAP-D06: CHAR(8)→DATE） |
| tnt_end_ymd | DATE | NO | | 担当終了日（GAP-D06: CHAR(8)→DATE） |
| tnt_busyo | VARCHAR(7) | YES | | 担当部署コード |
| initnt | VARCHAR(6) | YES | | 作成者 |
| inidate | TIMESTAMP WITH TIME ZONE | YES | CURRENT_TIMESTAMP | 作成日時 |
| updtnt | VARCHAR(6) | YES | | 更新者 |
| upddate | TIMESTAMP WITH TIME ZONE | YES | CURRENT_TIMESTAMP | 更新日時 |
| updpgid | VARCHAR(20) | YES | | 更新プログラムID |
| delflg | VARCHAR(1) | NO | '0' | 削除フラグ |

### T6: batch_execution_log（バッチ実行履歴）— 新規追加

主キー: `(id)`

| カラム名 | PostgreSQL 型 | NULL | デフォルト | 説明 |
|---------|--------------|------|----------|------|
| id | BIGSERIAL | NO | 自動採番 | 実行ログID |
| batch_name | VARCHAR(50) | NO | | バッチ名（例: 'subsys-aggregation', 'grpkey-aggregation'） |
| started_at | TIMESTAMP WITH TIME ZONE | NO | | 実行開始日時 |
| finished_at | TIMESTAMP WITH TIME ZONE | YES | | 実行終了日時 |
| status | VARCHAR(10) | NO | | 実行状態（'RUNNING' / 'SUCCESS' / 'FAILED'） |
| records_affected | INTEGER | YES | 0 | 処理件数 |
| error_message | TEXT | YES | | エラーメッセージ（FAILED時のみ） |
| created_at | TIMESTAMP WITH TIME ZONE | YES | CURRENT_TIMESTAMP | レコード作成日時 |

> **根拠**: #10 batch-processing で定義。バッチ実行の監視・障害調査用。
> 旧システムには存在しない新規テーブル（GAP-O03）。

---

## 4. インデックス

| テーブル | インデックス名 | カラム | 用途 |
|---------|-------------|-------|------|
| tcz01_hosyu_kousuu | idx_tcz01_tnt_yyyymm | (hssgytnt_esqid, year_half) | 担当者×期間検索 |
| tcz01_hosyu_kousuu | idx_tcz01_sgyymd | (sgyymd) | 作業日検索 |
| tcz01_hosyu_kousuu | idx_tcz01_taisyo | (taisyo_sknno, taisyo_subsysno) | 対象SS検索 |
| tcz01_hosyu_kousuu | idx_tcz01_status | (status, delflg) | ステータス絞り込み |
| tcz13_subsys_sum | idx_tcz13_yyyymm | (yyyymm, skbtcd) | 月次集計検索 |
| mav03_subsys | idx_mav03_search | (skbtcd, sknno) | SS検索 |

---

## 5. 制約

### 5.1 CHECK 制約

| テーブル | カラム | 制約 | 根拠 |
|---------|-------|------|------|
| tcz01_hosyu_kousuu | status | IN ('0','1','2','9') | ステータスマトリクス |
| tcz01_hosyu_kousuu | skbtcd | IN ('01','02') | ISID/WorkSys |
| tcz01_hosyu_kousuu | delflg | IN ('0','1') | 論理削除 |
| mcz04_ctrl | online_flg | IN ('0','1') | オンライン制御 |
| mcz04_ctrl | gjkt_flg | IN ('0','1') | 月次確認 |
| mcz04_ctrl | data_sk_flg | IN ('0','1') | データ集約 |

### 5.2 外部キー（論理参照）

GAP-D03 により論理削除を維持するため、物理的な FOREIGN KEY 制約は設けず、
アプリケーション層で参照整合性を保証する。
Doma 2 の DAO 層でバリデーションを実装し、テストで 100% カバーする。

**論理削除の運用ポリシー**:
- 物理削除は行わない（監査証跡として保持）
- 全クエリの WHERE 条件に `delflg = '0'` を付与する
- Doma 2 の 2Way SQL テンプレートで `delflg = '0'` 条件を共通化する

---

## 6. エンコーディング（GAP-N08）

- **Oracle**: Windows-31J (Shift_JIS系)
- **PostgreSQL**: UTF-8
- データ移行時に文字コード変換を実施
- Windows-31J 固有文字（①②③等の丸囲み数字、髙﨑等の旧字体）の移行テスト必須

---

## 7. 連番生成の変更（GAP-D04）

- **Oracle**: `SaibanMst`（採番マスタテーブル）による連番生成。SEQNO は `CHAR(12)` 文字列
- **PostgreSQL**: `BIGSERIAL` 型（内部で SEQUENCE を自動生成）。SEQNO は `BIGINT`
- `tcz01_hosyu_kousuu.seqno` に適用

### 7.1 SEQNO 移行手順

旧システムの SEQNO は `CHAR(12)` の文字列連番（例: `'000000000001'`）。
新システムでは `BIGSERIAL`（`BIGINT` + `SEQUENCE`）に変換する。

**移行ステップ:**

1. **仮テーブル作成**: 旧データを一時テーブルに VARCHAR(12) のまま INSERT
2. **型変換**: `CAST(TRIM(LEADING '0' FROM seqno_old) AS BIGINT)` で BIGINT に変換。
   先頭ゼロを除去してからキャスト
3. **本テーブルへ INSERT**: 変換済み BIGINT 値を `tcz01_hosyu_kousuu.seqno` に INSERT
   （BIGSERIAL カラムへの明示的な値挿入には `OVERRIDING SYSTEM VALUE` を使用）
4. **SEQUENCE 初期値設定**: 移行完了後に SEQUENCE の現在値を調整
   ```sql
   SELECT setval(
     pg_get_serial_sequence('tcz01_hosyu_kousuu', 'seqno'),
     (SELECT MAX(seqno) FROM tcz01_hosyu_kousuu)
   );
   ```
5. **整合性検証**: 旧テーブルの件数 = 新テーブルの件数であることを確認。
   重複 SEQNO が存在しないことを UNIQUE 制約で保証

**注意事項:**
- 旧データに非数値文字が含まれる場合はエラーとなる。事前に `seqno ~ '^[0-9]+$'` でバリデーション
- CHAR(12) → BIGINT の最大値は `999999999999`（BIGINT の範囲内: 最大 9.2×10¹⁸）

---

## 8. テスト要件

- 全テーブルの DDL が正常に実行されること
- `docker compose up` で initdb.d が自動実行されること
- 既存データの型変換（CHAR→VARCHAR, YYYYMMDD→DATE）が正しいこと
- CHECK 制約が不正値を拒否すること
- インデックスが想定クエリで使用されること（EXPLAIN ANALYZE）
- UTF-8 エンコーディングで日本語データが正常に格納・取得できること

### 受け入れ基準（Given-When-Then）

**AC-DB-01: DDL 正常実行**
- Given: 空の PostgreSQL 16 データベースが存在する
- When: `db/init/01-schema.sql` を実行する
- Then: 16 テーブル（T1-T6, M1-M10）、6 インデックス、6 CHECK 制約がエラーなく作成される

**AC-DB-02: Docker 初期化**
- Given: docker-compose.yml で db サービスが定義されている
- When: `docker compose up db` を実行する
- Then: initdb.d 経由で DDL が自動実行され、全テーブルが作成される

**AC-DB-03: CHECK 制約の不正値拒否**
- Given: tcz01_hosyu_kousuu テーブルが存在する
- When: status = '3' のレコードを INSERT する
- Then: CHECK 制約違反エラーが発生し INSERT が拒否される

**AC-DB-04: 型変換の正確性**
- Given: Oracle の CHAR(8) '20260226' 形式のデータがある
- When: DATE 型にキャストして PostgreSQL に INSERT する
- Then: sgyymd カラムに 2026-02-26 が正しく格納される

**AC-DB-05: SEQNO BIGSERIAL 採番**
- Given: tcz01_hosyu_kousuu テーブルにレコードが存在する
- When: seqno を指定せず新規レコードを INSERT する
- Then: BIGSERIAL により一意の seqno が自動採番される

**AC-DB-06: インデックス利用**
- Given: tcz01_hosyu_kousuu にテストデータが投入されている
- When: `WHERE hssgytnt_esqid = ? AND year_half = ?` で検索する
- Then: EXPLAIN ANALYZE で idx_tcz01_tnt_yyyymm のインデックススキャンが確認できる

**AC-DB-07: UTF-8 エンコーディング**
- Given: PostgreSQL の DB エンコーディングが UTF-8 である
- When: '①②③髙﨑' を含むレコードを INSERT する
- Then: SELECT で同一の文字列が取得できる（文字化けなし）

**AC-DB-08: 論理削除ポリシー**
- Given: delflg = '0' のレコードが存在する
- When: delflg = '1' に UPDATE する
- Then: レコードは物理削除されず論理削除状態となり、delflg = '0' の検索結果に含まれない

**AC-DB-09: Doma 2 Entity マッピング**
- Given: 全16テーブルに対応する Doma 2 Entity クラスが存在する
- When: DAO 経由で SELECT を実行する
- Then: 全カラムが Entity フィールドに正しくマッピングされる

**AC-DB-10: シードデータ投入**
- Given: 空のデータベースに DDL が適用済みである
- When: `db/init/03-seed.sql` を実行する
- Then: マスタテーブル（M1-M10）に初期データが投入される

---

## 9. 現行仕様との対応

### 9.1 US-ID × テーブル対応表（全38ストーリー）

#### 工数入力（FORM_010）

| US-ID | ユーザーストーリー | 関連テーブル |
|-------|-----------------|-------------|
| US-010 | 工数レコードの新規登録 | tcz01_hosyu_kousuu, mcz04_ctrl |
| US-011 | Ajaxインライン編集 | tcz01_hosyu_kousuu |
| US-012 | レコードのコピー | tcz01_hosyu_kousuu |
| US-013 | 翌月以降への転写 | tcz01_hosyu_kousuu, mcz02_hosyu_kategori |
| US-014 | レコードの削除 | tcz01_hosyu_kousuu |
| US-015 | 一括確認（ステータス一括変更） | tcz01_hosyu_kousuu, mcz04_ctrl |
| US-016 | 一括作成中戻し | tcz01_hosyu_kousuu |
| US-017 | 月の切り替えとナビゲーション | mcz04_ctrl, mcz02_hosyu_kategori |
| US-018 | サブシステム選択ダイアログ | mav01_sys, mav03_subsys, mcz15_ts_sys |
| US-019 | プロジェクト別工数参照 | tcz01_hosyu_kousuu |
| US-01A | Excel出力 | tcz01_hosyu_kousuu |
| US-01B | ソート操作 | tcz01_hosyu_kousuu |
| US-01C | 代行入力 | tcz01_hosyu_kousuu, mcz21_kanri_taisyo |
| US-01D | 管理者による全ステータス操作 | tcz01_hosyu_kousuu, mcz04_ctrl |

#### 工数状況一覧（FORM_020）

| US-ID | ユーザーストーリー | 関連テーブル |
|-------|-----------------|-------------|
| US-020 | 月次ステータスの一覧表示 | tcz01_hosyu_kousuu, mcz04_ctrl |
| US-021 | 月次未確認（未確認に戻す） | mcz04_ctrl |
| US-022 | 月次確認 | mcz04_ctrl |
| US-023 | 月次集約 | mcz04_ctrl |
| US-024 | レコード単位の承認 | tcz01_hosyu_kousuu |
| US-025 | 承認の取り消し（戻す） | tcz01_hosyu_kousuu |
| US-026 | インライン工数編集（Ajax） | tcz01_hosyu_kousuu |
| US-027 | ページネーション | tcz01_hosyu_kousuu |

#### 半期推移（FORM_030-032）

| US-ID | ユーザーストーリー | 関連テーブル |
|-------|-----------------|-------------|
| US-030 | 分類別集計の閲覧 | tcz13_subsys_sum, mcz03_apl_bunrui_grp |
| US-031 | システム別ドリルダウン | tcz13_subsys_sum, mav01_sys |
| US-032 | サブシステム別ドリルダウン | tcz13_subsys_sum, mav03_subsys |
| US-033 | MYシステム登録/解除 | tcz19_my_sys |
| US-034 | 半期推移のExcel出力 | tcz13_subsys_sum |
| US-035 | 半期推移のソート | tcz13_subsys_sum |
| US-036 | 分類別画面への戻り | — (画面制御のみ) |

#### 月別内訳（FORM_040-042）

| US-ID | ユーザーストーリー | 関連テーブル |
|-------|-----------------|-------------|
| US-040 | 月別内訳の分類別閲覧 | tcz13_subsys_sum, mcz03_apl_bunrui_grp |
| US-041 | システム別ドリルダウン | tcz13_subsys_sum, mav01_sys |
| US-042 | サブシステム別ドリルダウン | tcz13_subsys_sum, mav03_subsys |
| US-043 | 月別内訳のExcel出力（4種類） | tcz13_subsys_sum, tcz14_grp_key |
| US-044 | タブ切り替え（半期⇔月別） | — (画面制御のみ) |
| US-045 | MYシステム登録/解除（月別） | tcz19_my_sys |

#### 共通機能

| US-ID | ユーザーストーリー | 関連テーブル |
|-------|-----------------|-------------|
| US-050 | SSO認証ログイン | — (JWT/セッション。DB参照なし) |
| US-051 | サービス時間外制御 | — (アプリケーション設定) |
| US-052 | 緊急停止制御 | mcz04_ctrl (online_flg) |
| US-053 | 同時編集検知 | tcz01_hosyu_kousuu (upddate: 楽観ロック) |
| US-054 | メインメニュー | — (画面制御のみ) |

### 9.2 テーブル × US-ID 逆引き表

| テーブル | 参照元 US-ID |
|---------|------------|
| tcz01_hosyu_kousuu | US-010〜016, 019, 01A〜01D, 020, 024〜027, 053 |
| mcz04_ctrl | US-010, 015, 017, 01D, 020〜023, 052 |
| mcz02_hosyu_kategori | US-013, 017 |
| mav01_sys | US-018, 031, 041 |
| mav03_subsys | US-018, 032, 042 |
| mcz15_ts_sys | US-018 |
| mcz03_apl_bunrui_grp | US-030, 040 |
| mcz17_hshk_bunrui_grp | US-030 (分類2使用時) |
| tcz13_subsys_sum | US-030〜032, 034〜035, 040〜043 |
| tcz14_grp_key | US-043 |
| tcz19_my_sys | US-033, 045 |
| mcz21_kanri_taisyo | US-01C |
| mcz12_orgn_kr | (DLG-ORG: 組織選択ダイアログ) |
| mcz24_tanka | (バッチ集計: 金額計算) |
| tcz16_tnt_busyo_rireki | (人事異動: 担当部署変更履歴) |
| batch_execution_log | (#10 batch-processing: バッチ監視) |

---

## 10. 差異一覧

| GAP-ID | 区分 | 変更内容 |
|--------|------|---------|
| GAP-B01 | IMPROVE | Oracle → PostgreSQL 16 (AWS RDS) |
| GAP-D01 | IMPROVE | Oracle固有SQL → PostgreSQL準拠SQL |
| GAP-D02 | IMPROVE | CHAR固定長 → VARCHAR可変長 |
| GAP-D03 | KEEP | 論理削除（DELFLG）維持 |
| GAP-D04 | IMPROVE | 採番マスタ → PostgreSQL SEQUENCE (BIGSERIAL) |
| GAP-D05 | KEEP | YYYYX半期形式は維持（ビジネスルール依存） |
| GAP-D06 | IMPROVE | YYYYMMDD文字列 → DATE型 |
| GAP-D09 | IMPROVE | DataDirect Oracle JDBC → PostgreSQL JDBC |
| GAP-N08 | IMPROVE | Windows-31J → UTF-8 |
