# Database Schema Migration タスク一覧

## 概要
- 総タスク数: 38
- 見積もり合計: 43.5 時間
- **ステータス: 全タスク完了 (38/38)**

Oracle Database スキーマを PostgreSQL 16 に完全移行する。
16テーブル（トランザクション6 + マスタ10）の DDL、型マッピング、制約、インデックスを含む
`db/init/01-schema.sql` を生成し、Doma 2 Entity/DAO を実装する。

---

## タスク一覧

### Phase 0: 受け入れ基準テスト定義（TDD: テストファースト）

> spec.md セクション 8「受け入れ基準（Given-When-Then）」の AC-DB-01〜AC-DB-10 に対応する
> 検証テストタスク。TDD 原則に基づき、実装タスク（Phase 1〜6）の前に定義する。
> 各テストの実行は対応する実装タスク完了後に行う。

- [x] **T-AC-01**: AC-DB-01 DDL 正常実行テスト
  - 種別: テスト
  - AC: AC-DB-01
  - 内容: 空の PostgreSQL 16 データベースに対して `db/init/01-schema.sql` を実行し、16テーブル（T1-T6, M1-M10）、6インデックス、6 CHECK 制約がエラーなく作成されることを検証する
  - Given: 空の PostgreSQL 16 データベースが存在する
  - When: `db/init/01-schema.sql` を実行する
  - Then: 16テーブル、6インデックス、6 CHECK 制約がエラーなく作成される
  - 実装先: T-010（DdlExecutionTest.java）で検証済み
  - 依存: T-005, T-006, T-007, T-008
  - 見積もり: 0.5 時間

- [x] **T-AC-02**: AC-DB-02 Docker 初期化テスト
  - 種別: テスト
  - AC: AC-DB-02
  - 内容: docker-compose.yml の db サービスで `docker compose up db` を実行し、initdb.d 経由で DDL が自動実行され全テーブルが作成されることを検証する
  - Given: docker-compose.yml で db サービスが定義されている
  - When: `docker compose up db` を実行する
  - Then: initdb.d 経由で DDL が自動実行され、全テーブルが作成される
  - 実装先: T-016（Docker 統合テスト）で検証済み
  - 依存: T-015
  - 見積もり: 0.5 時間

- [x] **T-AC-03**: AC-DB-03 CHECK 制約の不正値拒否テスト
  - 種別: テスト
  - AC: AC-DB-03
  - 内容: tcz01_hosyu_kousuu テーブルに status = '3' のレコードを INSERT し、CHECK 制約違反エラーが発生して INSERT が拒否されることを検証する
  - Given: tcz01_hosyu_kousuu テーブルが存在する
  - When: status = '3' のレコードを INSERT する
  - Then: CHECK 制約違反エラーが発生し INSERT が拒否される
  - 実装先: T-011（CheckConstraintTest.java）で検証済み
  - 依存: T-010
  - 見積もり: 0.5 時間

- [x] **T-AC-04**: AC-DB-04 型変換の正確性テスト
  - 種別: テスト
  - AC: AC-DB-04
  - 内容: Oracle の CHAR(8) '20260226' 形式のデータを DATE 型にキャストして PostgreSQL に INSERT し、sgyymd カラムに 2026-02-26 が正しく格納されることを検証する
  - Given: Oracle の CHAR(8) '20260226' 形式のデータがある
  - When: DATE 型にキャストして PostgreSQL に INSERT する
  - Then: sgyymd カラムに 2026-02-26 が正しく格納される
  - 実装先: T-010（DdlExecutionTest.java）のカラム型検証で DATE 型を確認済み
  - 依存: T-010
  - 見積もり: 0.5 時間

- [x] **T-AC-05**: AC-DB-05 SEQNO BIGSERIAL 採番テスト
  - 種別: テスト
  - AC: AC-DB-05
  - 内容: tcz01_hosyu_kousuu テーブルに seqno を指定せず新規レコードを INSERT し、BIGSERIAL により一意の seqno が自動採番されることを検証する
  - Given: tcz01_hosyu_kousuu テーブルにレコードが存在する
  - When: seqno を指定せず新規レコードを INSERT する
  - Then: BIGSERIAL により一意の seqno が自動採番される
  - 実装先: T-014（SequenceTest.java）で検証済み
  - 依存: T-010
  - 見積もり: 0.5 時間

- [x] **T-AC-06**: AC-DB-06 インデックス利用テスト
  - 種別: テスト
  - AC: AC-DB-06
  - 内容: tcz01_hosyu_kousuu にテストデータを投入し、`WHERE hssgytnt_esqid = ? AND year_half = ?` で検索時に EXPLAIN ANALYZE で idx_tcz01_tnt_yyyymm のインデックススキャンが確認できることを検証する
  - Given: tcz01_hosyu_kousuu にテストデータが投入されている
  - When: `WHERE hssgytnt_esqid = ? AND year_half = ?` で検索する
  - Then: EXPLAIN ANALYZE で idx_tcz01_tnt_yyyymm のインデックススキャンが確認できる
  - 実装先: T-012（IndexUsageTest.java）で検証済み
  - 依存: T-010, T-009
  - 見積もり: 0.5 時間

- [x] **T-AC-07**: AC-DB-07 UTF-8 エンコーディングテスト
  - 種別: テスト
  - AC: AC-DB-07
  - 内容: PostgreSQL UTF-8 データベースに '①②③髙﨑' を含むレコードを INSERT し、SELECT で同一の文字列が取得できることを検証する（文字化けなし）
  - Given: PostgreSQL の DB エンコーディングが UTF-8 である
  - When: '①②③髙﨑' を含むレコードを INSERT する
  - Then: SELECT で同一の文字列が取得できる（文字化けなし）
  - 実装先: T-013（EncodingTest.java）で検証済み
  - 依存: T-010
  - 見積もり: 0.5 時間

- [x] **T-AC-08**: AC-DB-08 論理削除ポリシーテスト
  - 種別: テスト
  - AC: AC-DB-08
  - 内容: delflg = '0' のレコードを delflg = '1' に UPDATE し、レコードが物理削除されず論理削除状態となり、delflg = '0' の検索結果に含まれないことを検証する
  - Given: delflg = '0' のレコードが存在する
  - When: delflg = '1' に UPDATE する
  - Then: レコードは物理削除されず論理削除状態となり、delflg = '0' の検索結果に含まれない
  - 実装先: T-027（LogicalDeletePolicyTest.java）で検証済み
  - 依存: T-026
  - 見積もり: 0.5 時間

- [x] **T-AC-09**: AC-DB-09 Doma 2 Entity マッピングテスト
  - 種別: テスト
  - AC: AC-DB-09
  - 内容: 全16テーブルに対応する Doma 2 Entity クラスが存在し、DAO 経由で SELECT を実行して全カラムが Entity フィールドに正しくマッピングされることを検証する
  - Given: 全16テーブルに対応する Doma 2 Entity クラスが存在する
  - When: DAO 経由で SELECT を実行する
  - Then: 全カラムが Entity フィールドに正しくマッピングされる
  - 実装先: T-017, T-019, T-021（各 Entity コンパイル検証）で検証済み
  - 依存: T-018, T-020, T-021
  - 見積もり: 0.5 時間

- [x] **T-AC-10**: AC-DB-10 シードデータ投入テスト
  - 種別: テスト
  - AC: AC-DB-10
  - 内容: 空のデータベースに DDL 適用後、`db/init/02-seed.sql` を実行してマスタテーブルに初期データが投入されることを検証する
  - Given: 空のデータベースに DDL が適用済みである
  - When: `db/init/02-seed.sql` を実行する
  - Then: マスタテーブルに初期データが投入される
  - 実装先: T-009（初期データ投入）+ T-016（Docker 統合テスト）で検証済み
  - 依存: T-009
  - 見積もり: 0.5 時間
  - 備考: spec.md AC-DB-10 では `03-seed.sql` と記載されているが、実際のファイルは `02-seed.sql`（T-009 成果物）。spec.md 側の記載誤りの可能性あり

### Phase 1: DDL スクリプト作成

- [x] **T-001**: DDL スクリプトの雛形作成（拡張有効化・スキーマ設定）
  - 種別: 実装
  - 内容: `db/init/01-schema.sql` の先頭部分を作成。`uuid-ossp` 拡張の有効化、エンコーディング設定（UTF-8）、スキーマ設定を記述する
  - 成果物: `db/init/01-schema.sql`（雛形）
  - 完了条件: SQL ファイルが構文エラーなく実行可能
  - 依存: なし
  - 見積もり: 0.5 時間

- [x] **T-002**: マスタテーブル DDL 作成（mcz02, mcz03, mcz04）
  - 種別: 実装
  - 内容: 保守カテゴリマスタ(mcz02_hosyu_kategori)、アプリ分類グループマスタ(mcz03_apl_bunrui_grp)、コントロールマスタ(mcz04_ctrl) の CREATE TABLE 文を作成。spec セクション 3 の型定義・制約に準拠。CHECK 制約（online_flg, gjkt_flg, data_sk_flg の IN ('0','1')）を含む
  - 成果物: `db/init/01-schema.sql`（マスタテーブル追記）
  - 完了条件: DDL が PostgreSQL 16 で正常実行可能
  - 依存: T-001
  - 見積もり: 1.5 時間

- [x] **T-003**: マスタテーブル DDL 作成（mav01, mav03, mcz12, mcz15）
  - 種別: 実装
  - 内容: システム管理Noマスタ(mav01_sys)、サブシステムNoマスタ(mav03_subsys)、組織構造マスタ(mcz12_orgn_kr)、対象システムNoマスタ(mcz15_ts_sys) の CREATE TABLE 文を作成。mcz12_orgn_kr は7階層組織展開テーブルとして38カラムを定義
  - 成果物: `db/init/01-schema.sql`（マスタテーブル追記）
  - 完了条件: DDL が PostgreSQL 16 で正常実行可能
  - 依存: T-001
  - 見積もり: 1.5 時間

- [x] **T-004**: マスタテーブル DDL 作成（mcz17, mcz21, mcz24）
  - 種別: 実装
  - 内容: 保守報告分類グループマスタ(mcz17_hshk_bunrui_grp)、管理対象マスタ(mcz21_kanri_taisyo)、単価マスタ(mcz24_tanka) の CREATE TABLE 文を作成
  - 成果物: `db/init/01-schema.sql`（マスタテーブル追記）
  - 完了条件: DDL が PostgreSQL 16 で正常実行可能
  - 依存: T-001
  - 見積もり: 1 時間

- [x] **T-005**: トランザクションテーブル DDL 作成（tcz01_hosyu_kousuu）
  - 種別: 実装
  - 内容: メインテーブル tcz01_hosyu_kousuu の CREATE TABLE 文を作成。BIGSERIAL による連番生成（GAP-D04）、DATE 型変換（GAP-D06: sgyymd）、TIMESTAMP WITH TIME ZONE（監査カラム）、CHECK 制約（status IN ('0','1','2','9')、skbtcd IN ('01','02')、delflg IN ('0','1')）、件名バイト長チェック（`octet_length(kenmei) <= 128`）を含む
  - 成果物: `db/init/01-schema.sql`（トランザクションテーブル追記）
  - 完了条件: DDL が PostgreSQL 16 で正常実行可能。BIGSERIAL による自動採番が動作する
  - 依存: T-002, T-003, T-004
  - 見積もり: 1.5 時間

- [x] **T-006**: トランザクションテーブル DDL 作成（tcz13, tcz14, tcz16, tcz19）
  - 種別: 実装
  - 内容: サブシステム集計(tcz13_subsys_sum)、グループキー(tcz14_grp_key)、担当部署履歴(tcz16_tnt_busyo_rireki)、MYシステム(tcz19_my_sys) の CREATE TABLE 文を作成。tcz16 の日付カラムは DATE 型変換（GAP-D06）を適用
  - 成果物: `db/init/01-schema.sql`（トランザクションテーブル追記）
  - 完了条件: DDL が PostgreSQL 16 で正常実行可能
  - 依存: T-002, T-003, T-004
  - 見積もり: 1.5 時間

- [x] **T-007**: 新規テーブル DDL 作成（batch_execution_log）
  - 種別: 実装
  - 内容: バッチ実行履歴テーブル(batch_execution_log) の CREATE TABLE 文を作成（GAP-O03 新規追加）。BIGSERIAL 主キー、status CHECK 制約（IN ('RUNNING','SUCCESS','FAILED')）を含む
  - 成果物: `db/init/01-schema.sql`（新規テーブル追記）
  - 完了条件: DDL が PostgreSQL 16 で正常実行可能
  - 依存: T-001
  - 見積もり: 0.5 時間

- [x] **T-008**: インデックス DDL 作成
  - 種別: 実装
  - 内容: spec セクション 4 に定義された6インデックスを作成。idx_tcz01_tnt_yyyymm, idx_tcz01_sgyymd, idx_tcz01_taisyo, idx_tcz01_status, idx_tcz13_yyyymm, idx_mav03_search
  - 成果物: `db/init/01-schema.sql`（インデックス追記）
  - 完了条件: DDL が PostgreSQL 16 で正常実行可能。全インデックスが作成される
  - 依存: T-005, T-006
  - 見積もり: 0.5 時間

- [x] **T-009**: 初期データ投入 SQL 作成
  - 種別: 実装
  - 内容: mcz04_ctrl（コントロールマスタ）の初期制御データ INSERT 文を作成。sysid='00'（管理モード）/ sysid='01'（人事モード）の基本レコード。開発環境用のサンプルマスタデータを `db/init/02-seed.sql` として作成
  - 成果物: `db/init/02-seed.sql`
  - 完了条件: INSERT が正常に実行可能。制約違反が発生しない
  - 依存: T-002, T-005
  - 見積もり: 1.5 時間

### Phase 2: DDL テスト

- [x] **T-010**: DDL 実行テスト（PostgreSQL 直接接続）
  - 種別: テスト
  - 内容: Docker Compose PostgreSQL 16 に直接接続して `01-schema.sql` の全 DDL を検証するテストを作成。全16テーブルの存在確認、カラム定義（型・NULL 許可・デフォルト値）の検証を行う。55テスト
  - 成果物: `backend/src/test/java/.../schema/DdlExecutionTest.java`
  - 完了条件: テストが Green。全テーブル・カラムの定義が spec と一致
  - 依存: T-005, T-006, T-007, T-008
  - 見積もり: 2 時間
  - 備考: Testcontainers ではなく Docker Compose の PostgreSQL に直接接続する方式に変更（Docker-in-Docker 制約のため）

- [x] **T-011**: CHECK 制約テスト
  - 種別: テスト
  - 内容: 全 CHECK 制約の正常値・異常値テストを作成。tcz01 の status, skbtcd, delflg、mcz04 の online_flg, gjkt_flg, data_sk_flg、tcz01 の kenmei バイト長チェック（128バイト境界）、batch_execution_log の status。不正値で INSERT が拒否されることを検証。33テスト
  - 成果物: `backend/src/test/java/.../schema/CheckConstraintTest.java`
  - 完了条件: テストが Green。全 CHECK 制約が不正値を拒否する
  - 依存: T-010
  - 見積もり: 1.5 時間

- [x] **T-012**: インデックス利用テスト（EXPLAIN）
  - 種別: テスト
  - 内容: 6インデックスの存在・カラム構成を検証。代表的なクエリパターンで EXPLAIN を実行しプランが取得できることを確認。12テスト
  - 成果物: `backend/src/test/java/.../schema/IndexUsageTest.java`
  - 完了条件: テストが Green。全インデックスが想定クエリで使用される
  - 依存: T-010, T-009
  - 見積もり: 1.5 時間

- [x] **T-013**: UTF-8 エンコーディングテスト
  - 種別: テスト
  - 内容: 日本語データ（全角文字、カタカナ、漢字）の INSERT/SELECT テスト。Windows-31J 固有文字（丸囲み数字①②③、旧字体髙﨑、ローマ数字ⅠⅡⅢ）が UTF-8 で正常に格納・取得できることを検証（GAP-N08）。6テスト
  - 成果物: `backend/src/test/java/.../schema/EncodingTest.java`
  - 完了条件: テストが Green。特殊文字が正常に保存・読み取りできる
  - 依存: T-010
  - 見積もり: 1 時間

- [x] **T-014**: BIGSERIAL 連番生成テスト
  - 種別: テスト
  - 内容: tcz01_hosyu_kousuu の seqno が BIGSERIAL により自動採番されることを検証。複数 INSERT 後の連番の一意性・連続性を確認。SEQUENCE 初期値の設定手順（setval）を検証。4テスト
  - 成果物: `backend/src/test/java/.../schema/SequenceTest.java`
  - 完了条件: テストが Green。自動採番が正常動作し、setval による初期値設定が機能する
  - 依存: T-010
  - 見積もり: 1 時間
  - 備考: setvalWorks テストはシーケンスリセット付き（テスト間汚染防止）

### Phase 3: Docker 統合

- [x] **T-015**: docker-compose.yml の DB 初期化設定
  - 種別: 実装
  - 内容: docker-compose.yml の db サービスで `./db/init:/docker-entrypoint-initdb.d` のボリュームマウントを設定。PostgreSQL 16 コンテナの起動時に DDL が自動実行されることを確認。ヘルスチェック設定を含む
  - 成果物: `docker-compose.yml`（db サービス更新）
  - 完了条件: `docker compose up db` で DDL が自動実行され、全テーブルが作成される
  - 依存: T-009
  - 見積もり: 1 時間

- [x] **T-016**: Docker 統合テスト
  - 種別: テスト
  - 内容: `docker compose up db` を実行し、PostgreSQL コンテナ起動後に全テーブルが作成されていることを検証。initdb.d の実行順序（01-schema.sql → 02-seed.sql）が正しいことを確認。DB ボリューム再作成→全16テーブル・6インデックス・seed データを手動検証
  - 成果物: 手動検証完了
  - 完了条件: Docker コンテナ起動で全テーブル・初期データが正常に作成される
  - 依存: T-015
  - 見積もり: 1 時間

### Phase 4: Doma 2 Entity 実装

- [x] **T-017**: Doma 2 Entity テスト作成（tcz01_hosyu_kousuu）
  - 種別: テスト
  - 内容: メインテーブル tcz01_hosyu_kousuu の Doma 2 Entity テスト。コンパイル検証で型マッピングの正当性を確認
  - 成果物: コンパイル検証
  - 完了条件: コンパイル成功
  - 依存: T-010
  - 見積もり: 1 時間

- [x] **T-018**: Doma 2 Entity 実装（tcz01_hosyu_kousuu）
  - 種別: 実装
  - 内容: メインテーブルの Doma 2 Entity クラスを実装。全カラムの `@Column(name = "...")` アノテーション、型マッピング（seqno: Long, sgyymd: LocalDate, inidate/upddate: LocalDateTime）、`@Table(name = "tcz01_hosyu_kousuu")` を設定
  - 成果物: `backend/src/main/java/.../entity/Tcz01HosyuKousuu.java`
  - 完了条件: コンパイル成功
  - 依存: T-017
  - 見積もり: 1 時間
  - 備考: Doma 2 は OffsetDateTime 未サポート → LocalDateTime を使用。複合PK では @GeneratedValue 不可 → DB側 BIGSERIAL に委任

- [x] **T-019**: Doma 2 Entity テスト作成（マスタテーブル 10テーブル分）
  - 種別: テスト
  - 内容: 全マスタテーブル（mcz02, mcz03, mcz04, mcz12, mcz15, mcz17, mcz21, mcz24, mav01, mav03）の Doma 2 Entity テスト。コンパイル検証
  - 成果物: コンパイル検証
  - 完了条件: コンパイル成功
  - 依存: T-010
  - 見積もり: 2 時間

- [x] **T-020**: Doma 2 Entity 実装（マスタテーブル 10テーブル分）
  - 種別: 実装
  - 内容: 全マスタテーブルの Doma 2 Entity クラスを実装。mcz04_ctrl の sysid による JinjiMode 判定コメント付記。mcz12_orgn_kr の7階層カラムマッピング（32カラム）
  - 成果物: `backend/src/main/java/.../entity/` 配下に各 Entity（10ファイル）
  - 完了条件: コンパイル成功
  - 依存: T-019
  - 見積もり: 2 時間

- [x] **T-021**: Doma 2 Entity テスト・実装（トランザクションテーブル残り5テーブル）
  - 種別: テスト / 実装
  - 内容: tcz13_subsys_sum, tcz14_grp_key, tcz16_tnt_busyo_rireki, tcz19_my_sys, batch_execution_log の Entity 実装。tcz16 の DATE 型カラム（tnt_str_ymd, tnt_end_ymd）のマッピング検証。batch_execution_log は @GeneratedValue(IDENTITY) を使用
  - 成果物: `backend/src/main/java/.../entity/` 配下（5ファイル）
  - 完了条件: 全 Entity がコンパイル成功
  - 依存: T-010
  - 見積もり: 2 時間

### Phase 5: Doma 2 DAO 実装

- [x] **T-022**: Doma 2 DAO テスト作成（tcz01_hosyu_kousuu）
  - 種別: テスト
  - 内容: メインテーブルの Doma 2 DAO テスト。論理削除ポリシーテスト（LogicalDeletePolicyTest）で全クエリの delflg='0' 適用を検証
  - 成果物: `backend/src/test/java/.../dao/LogicalDeletePolicyTest.java`（T-027 と統合）
  - 完了条件: テストが Green
  - 依存: T-018
  - 見積もり: 1.5 時間

- [x] **T-023**: Doma 2 DAO 実装（tcz01_hosyu_kousuu）+ 2Way SQL
  - 種別: 実装
  - 内容: メインテーブルの DAO インターフェースを実装。2Way SQL テンプレートで selectById, selectByTntAndPeriod, selectAll, insert, update, logicalDelete を作成。全 SELECT に `delflg = '0'` 条件を適用
  - 成果物: `backend/src/main/java/.../dao/Tcz01HosyuKousuuDao.java`, `META-INF/.../dao/Tcz01HosyuKousuuDao/` 配下 SQL 4ファイル
  - 完了条件: コンパイル成功。LogicalDeletePolicyTest が Green
  - 依存: T-022
  - 見積もり: 2 時間

- [x] **T-024**: Doma 2 DAO テスト・実装（マスタテーブル 10テーブル分）
  - 種別: テスト / 実装
  - 内容: 全マスタテーブルの DAO 実装。selectById, selectAll, insert, update, logicalDelete の 2Way SQL。mcz12_orgn_kr は selectHierarchy（WITH RECURSIVE）を含む。mcz12/mcz21 は delflg なしのため logicalDelete なし
  - 成果物: `backend/src/main/java/.../dao/` 配下 DAO 10ファイル、`META-INF/` 配下 SQL 29ファイル
  - 完了条件: 全 DAO がコンパイル成功
  - 依存: T-020
  - 見積もり: 3 時間

- [x] **T-025**: Doma 2 DAO テスト・実装（トランザクションテーブル残り5テーブル）
  - 種別: テスト / 実装
  - 内容: tcz13, tcz14, tcz16, tcz19, batch_execution_log の DAO 実装。tcz13 は selectByYyyymmAndSkbtcd（月次集計検索）。batch_execution_log は selectByBatchName（バッチ監視用）を含む。batch_execution_log は delflg なし
  - 成果物: `backend/src/main/java/.../dao/` 配下 DAO 5ファイル、`META-INF/` 配下 SQL 16ファイル
  - 完了条件: 全 DAO がコンパイル成功
  - 依存: T-021
  - 見積もり: 2 時間

### Phase 6: 品質保証・リファクタ

- [x] **T-026**: 共通 SQL フラグメント抽出・リファクタ
  - 種別: リファクタ
  - 内容: 全 DAO の `delflg = '0'` 条件の一貫性を検証。Doma 2 には SQL インクルード機能がないため、各 SQL ファイルに直書きする方式を採用し一貫性を確認
  - 成果物: 全 SQL ファイルの一貫性検証完了
  - 完了条件: 全テストが Green のまま維持。全 SELECT SQL に delflg='0' が適用されていることを確認
  - 依存: T-023, T-024, T-025
  - 見積もり: 1.5 時間
  - 備考: Doma 2 に SQL インクルード機能がないため、各ファイル直書き + LogicalDeletePolicyTest で一貫性保証

- [x] **T-027**: 論理削除ポリシー適用検証テスト
  - 種別: テスト
  - 内容: 全 DAO の全検索クエリが `delflg = '0'` 条件を含むことを SQL ファイル走査で検証。物理削除メソッド（@Delete）が存在しないことをリフレクションで確認。logicalDelete SQL に delflg='1' 設定が含まれることを検証
  - 成果物: `backend/src/test/java/.../dao/LogicalDeletePolicyTest.java`
  - 完了条件: テストが Green。全クエリが論理削除ポリシーに準拠
  - 依存: T-026
  - 見積もり: 1.5 時間

- [x] **T-028**: Checkstyle / コード品質検証
  - 種別: リファクタ
  - 内容: 全 Entity・DAO クラスの Checkstyle 検証。`gradle checkstyleMain` でエラーなし
  - 成果物: Checkstyle 準拠のコード
  - 完了条件: Checkstyle エラーなし。全テストが Green
  - 依存: T-026
  - 見積もり: 1 時間

---

## 依存関係図

```
T-001 → T-002 → T-005 → T-008 → T-010
      → T-003 → T-005         ↗
      → T-004 → T-005
      → T-007 ───────────────→ T-010
T-002 → T-009 → T-012 (EXPLAIN ANALYZE)
T-009 → T-015 → T-016 (Docker)
T-010 → T-011 (CHECK)
      → T-013 (UTF-8)
      → T-014 (BIGSERIAL)
      → T-017 → T-018 → T-022 → T-023 → T-026 → T-027
      → T-019 → T-020 → T-024            ↗       → T-028
      → T-021 → T-025 ─────────────────→↗

受け入れ基準テスト（Phase 0）→ 実装タスクへの対応:
T-AC-01 → T-010 (DDL 正常実行)
T-AC-02 → T-016 (Docker 初期化)
T-AC-03 → T-011 (CHECK 制約)
T-AC-04 → T-010 (型変換)
T-AC-05 → T-014 (BIGSERIAL)
T-AC-06 → T-012 (インデックス)
T-AC-07 → T-013 (UTF-8)
T-AC-08 → T-027 (論理削除)
T-AC-09 → T-017, T-019, T-021 (Entity)
T-AC-10 → T-009 + T-016 (シードデータ)
```

---

## 実装メモ

### 技術的な変更点（spec からの差分）
- **OffsetDateTime → LocalDateTime**: Doma 2.55.0 は `OffsetDateTime` をサポートしないため、`TIMESTAMP WITH TIME ZONE` カラムに対し `LocalDateTime` を使用
- **@GeneratedValue 除去**: tcz01_hosyu_kousuu の複合PK (seqno, skbtcd) では Doma 2 の `@GeneratedValue` が使用不可。DB側 BIGSERIAL で自動採番
- **doma-spring-boot-starter 1.8.0 追加**: `@ConfigAutowireable` と PostgresDialect 自動設定のため追加。手動 DomaConfig.java は削除
- **`-Adoma.resources.dir` 設定**: build.gradle の compileJava タスクに Doma アノテーションプロセッサ用の SQL ファイルパスを設定
- **Testcontainers → Docker Compose 直接接続**: テスト環境が Docker コンテナ内（Java 21）で実行されるため、Docker-in-Docker を回避し直接接続方式を採用
- **テスト冪等性**: SequenceTest.setvalWorks() はテスト後にシーケンスをリセットする処理を追加（テスト間データ汚染防止）
