-- ============================================
-- CZ マイグレーション — PostgreSQL 16 スキーマ
-- Oracle → PostgreSQL 移行
-- spec: .specify/features/01-database-schema/spec.md
-- GAP-ID: GAP-B01, GAP-D01〜D06, GAP-D09, GAP-N08
-- ============================================

-- UUID 生成用の拡張（T-001）
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- マスタテーブル（参照先を先に作成）
-- ============================================

-- --------------------------------------------
-- M1: mcz02_hosyu_kategori（保守カテゴリマスタ）
-- Oracle: MCZ02HOSYUKATEGORIMST
-- GAP-D06: yukou_kaishiki/yukou_syuryoki を DATE 型に変換
-- --------------------------------------------
CREATE TABLE mcz02_hosyu_kategori (
    hs_kategori         VARCHAR(4)      NOT NULL,
    yukou_kaishiki      DATE            NOT NULL,
    yukou_syuryoki      DATE            NOT NULL,
    hs_syubetu          VARCHAR(1),
    hs_unyou_kubun      VARCHAR(1),
    hs_kategori_mei     VARCHAR(100),
    hs_kategori_naiyo   VARCHAR(500),
    initnt              VARCHAR(6),
    inidate             TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updtnt              VARCHAR(6),
    upddate             TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updpgid             VARCHAR(20),
    delflg              VARCHAR(1)      NOT NULL DEFAULT '0',

    CONSTRAINT pk_mcz02 PRIMARY KEY (hs_kategori, yukou_kaishiki, yukou_syuryoki),
    CONSTRAINT chk_mcz02_delflg CHECK (delflg IN ('0','1'))
);

COMMENT ON TABLE  mcz02_hosyu_kategori IS '保守カテゴリマスタ（有効期間付き）';

-- --------------------------------------------
-- M2: mcz04_ctrl（コントロールマスタ）
-- Oracle: MCZ04CTRLMST
-- --------------------------------------------
CREATE TABLE mcz04_ctrl (
    sysid               VARCHAR(2)      NOT NULL,
    yyyymm              VARCHAR(6)      NOT NULL,
    online_flg          VARCHAR(1)      NOT NULL DEFAULT '0',
    renketsu_flg        VARCHAR(1)      DEFAULT '0',
    gjkt_flg            VARCHAR(1)      DEFAULT '0',
    data_sk_flg         VARCHAR(1)      DEFAULT '0',
    initnt              VARCHAR(6),
    inidate             TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updtnt              VARCHAR(6),
    upddate             TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updpgid             VARCHAR(20),
    delflg              VARCHAR(1)      NOT NULL DEFAULT '0',

    CONSTRAINT pk_mcz04 PRIMARY KEY (sysid, yyyymm),
    CONSTRAINT chk_mcz04_online CHECK (online_flg IN ('0','1')),
    CONSTRAINT chk_mcz04_gjkt   CHECK (gjkt_flg IN ('0','1')),
    CONSTRAINT chk_mcz04_datask CHECK (data_sk_flg IN ('0','1')),
    CONSTRAINT chk_mcz04_delflg CHECK (delflg IN ('0','1'))
);

COMMENT ON TABLE  mcz04_ctrl IS 'コントロールマスタ（月次処理制御）';
COMMENT ON COLUMN mcz04_ctrl.sysid      IS 'システムID（00=管理モード/jinjiMode:false, 01=人事モード/jinjiMode:true）';
COMMENT ON COLUMN mcz04_ctrl.online_flg IS 'オンラインフラグ（0=利用可, 1=停止）';
COMMENT ON COLUMN mcz04_ctrl.gjkt_flg   IS '月次確認フラグ（0=未確認, 1=確認済）';
COMMENT ON COLUMN mcz04_ctrl.data_sk_flg IS 'データ集約フラグ（0=未集約, 1=集約済）';

-- --------------------------------------------
-- M3: mav01_sys（システム管理Noマスタ）
-- Oracle: TBMAV01_SKNNO
-- --------------------------------------------
CREATE TABLE mav01_sys (
    skbtcd              VARCHAR(2)      NOT NULL,
    sknno               VARCHAR(8)      NOT NULL,
    sys_mei             VARCHAR(100),
    sys_mei_kn          VARCHAR(100),
    yukou_kaishiki      VARCHAR(8),
    yukou_syuryoki      VARCHAR(8),
    initnt              VARCHAR(6),
    inidate             TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updtnt              VARCHAR(6),
    upddate             TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updpgid             VARCHAR(20),
    delflg              VARCHAR(1)      NOT NULL DEFAULT '0',

    CONSTRAINT pk_mav01 PRIMARY KEY (skbtcd, sknno),
    CONSTRAINT chk_mav01_delflg CHECK (delflg IN ('0','1'))
);

COMMENT ON TABLE mav01_sys IS 'システム管理Noマスタ';

-- --------------------------------------------
-- M4: mav03_subsys（サブシステムNoマスタ）
-- Oracle: TBMAV03_SUBSYSTEMNO
-- --------------------------------------------
CREATE TABLE mav03_subsys (
    skbtcd              VARCHAR(2)      NOT NULL,
    sknno               VARCHAR(8)      NOT NULL,
    subsysno            VARCHAR(8)      NOT NULL,
    aplid               VARCHAR(8),
    subsys_mei          VARCHAR(100),
    subsys_mei_kn       VARCHAR(100),
    yukou_kaishiki      VARCHAR(8),
    yukou_syuryoki      VARCHAR(8),
    initnt              VARCHAR(6),
    inidate             TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updtnt              VARCHAR(6),
    upddate             TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updpgid             VARCHAR(20),
    delflg              VARCHAR(1)      NOT NULL DEFAULT '0',

    CONSTRAINT pk_mav03 PRIMARY KEY (skbtcd, sknno, subsysno),
    CONSTRAINT chk_mav03_delflg CHECK (delflg IN ('0','1'))
);

COMMENT ON TABLE mav03_subsys IS 'サブシステムNoマスタ';

-- --------------------------------------------
-- M5: mcz15_ts_sys（対象システムNoマスタ）
-- Oracle: TBMCZ15_TSSYSNOMST
-- yukou_kaishiki/yukou_syuryoki は VARCHAR(5) YYYYX 半期形式（GAP-D05 維持）
-- --------------------------------------------
CREATE TABLE mcz15_ts_sys (
    tssknno             VARCHAR(8)      NOT NULL,
    tssubsysno          VARCHAR(8)      NOT NULL,
    yukou_kaishiki      VARCHAR(5),
    yukou_syuryoki      VARCHAR(5)      NOT NULL,
    aplid               VARCHAR(8),
    tssysname           VARCHAR(128),
    tssysname_kn        VARCHAR(128),
    tssubsysname        VARCHAR(128),
    tssubsysname_kn     VARCHAR(128),
    initnt              VARCHAR(6),
    inidate             TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updtnt              VARCHAR(6),
    upddate             TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updpgid             VARCHAR(20),
    delflg              VARCHAR(1)      NOT NULL DEFAULT '0',

    CONSTRAINT pk_mcz15 PRIMARY KEY (tssknno, tssubsysno, yukou_syuryoki),
    CONSTRAINT chk_mcz15_delflg CHECK (delflg IN ('0','1'))
);

COMMENT ON TABLE mcz15_ts_sys IS '対象システムNoマスタ';

-- --------------------------------------------
-- M6: mcz03_apl_bunrui_grp（アプリ分類グループマスタ）
-- Oracle: MCZ03APLBUNRUIGRPMST
-- yukou_kaishiki/yukou_syuryoki は VARCHAR(5) YYYYX 半期形式（GAP-D05 維持）
-- --------------------------------------------
CREATE TABLE mcz03_apl_bunrui_grp (
    sknno               VARCHAR(8)      NOT NULL,
    yukou_kaishiki      VARCHAR(5)      NOT NULL,
    yukou_syuryoki      VARCHAR(5)      NOT NULL,
    aplbunruicode       VARCHAR(4),
    initnt              VARCHAR(6),
    inidate             TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updtnt              VARCHAR(6),
    upddate             TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updpgid             VARCHAR(20),
    delflg              VARCHAR(1)      NOT NULL DEFAULT '0',

    CONSTRAINT pk_mcz03 PRIMARY KEY (sknno, yukou_kaishiki, yukou_syuryoki),
    CONSTRAINT chk_mcz03_delflg CHECK (delflg IN ('0','1'))
);

COMMENT ON TABLE mcz03_apl_bunrui_grp IS 'アプリ分類グループマスタ';

-- --------------------------------------------
-- M7: mcz17_hshk_bunrui_grp（保守報告分類グループマスタ）
-- Oracle: MCZ17_HSHKBUNRUIGRPMST
-- yukou_kaishiki/yukou_syuryoki は VARCHAR(5) YYYYX 半期形式（GAP-D05 維持）
-- --------------------------------------------
CREATE TABLE mcz17_hshk_bunrui_grp (
    sknno               VARCHAR(8)      NOT NULL,
    yukou_kaishiki      VARCHAR(5)      NOT NULL,
    yukou_syuryoki      VARCHAR(5)      NOT NULL,
    bunruicode          VARCHAR(4),
    initnt              VARCHAR(6),
    inidate             TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updtnt              VARCHAR(6),
    upddate             TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updpgid             VARCHAR(20),
    delflg              VARCHAR(1)      NOT NULL DEFAULT '0',

    CONSTRAINT pk_mcz17 PRIMARY KEY (sknno, yukou_kaishiki, yukou_syuryoki),
    CONSTRAINT chk_mcz17_delflg CHECK (delflg IN ('0','1'))
);

COMMENT ON TABLE mcz17_hshk_bunrui_grp IS '保守報告分類グループマスタ';

-- --------------------------------------------
-- M8: mcz12_orgn_kr（組織構造マスタ）
-- Oracle: MCZ12ORGNKRSPRD
-- 7階層組織展開テーブル（全社→本部→局→室→部→課）
-- GAP-D06: endymd/startymd を DATE 型に変換
-- バッチ #11（spec #10）で外部人事システムから UPSERT 同期
-- OrganizationScopeResolver（spec #2 セクション 6.3）が参照
-- --------------------------------------------
CREATE TABLE mcz12_orgn_kr (
    sikcd               VARCHAR(7)      NOT NULL,
    endymd              DATE,
    startymd            DATE,
    krikaisocd          VARCHAR(2),
    krijsikcd           VARCHAR(7),
    egsyocd             VARCHAR(2),
    showno              VARCHAR(14),
    jsikhyojijun        VARCHAR(2),
    hyojikn             VARCHAR(40),
    hyojikj             VARCHAR(60),
    hyojiryaku          VARCHAR(40),
    iocd                VARCHAR(7),
    sikcdhonb           VARCHAR(7),
    honbhyojikn         VARCHAR(40),
    honbhyojikj         VARCHAR(60),
    honbhyojiryaku      VARCHAR(40),
    sikcdkyk            VARCHAR(7),
    kykhyojikn          VARCHAR(40),
    kykhyojikj          VARCHAR(60),
    kykhyojiryaku       VARCHAR(40),
    sikcdsitu           VARCHAR(7),
    situhyojikn         VARCHAR(40),
    situhyojikj         VARCHAR(60),
    situhyojiryaku      VARCHAR(40),
    sikcdbu             VARCHAR(7),
    buhyojikn           VARCHAR(40),
    buhyojikj           VARCHAR(60),
    buhyojiryaku        VARCHAR(40),
    sikcdka             VARCHAR(7),
    kahyojikn           VARCHAR(40),
    kahyojikj           VARCHAR(60),
    kahyojiryaku        VARCHAR(40),

    CONSTRAINT pk_mcz12 PRIMARY KEY (sikcd)
);

COMMENT ON TABLE  mcz12_orgn_kr IS '組織構造マスタ（全社→本部→局→室→部→課）';
COMMENT ON COLUMN mcz12_orgn_kr.sikcdhonb IS '組織コード（本部）— 階層: HONBU';
COMMENT ON COLUMN mcz12_orgn_kr.sikcdkyk  IS '組織コード（局）— 階層: KYOKU';
COMMENT ON COLUMN mcz12_orgn_kr.sikcdsitu IS '組織コード（室）— 階層: SHITSU';
COMMENT ON COLUMN mcz12_orgn_kr.sikcdbu   IS '組織コード（部）— 階層: BU';
COMMENT ON COLUMN mcz12_orgn_kr.sikcdka   IS '組織コード（課）— 階層: KA';

-- --------------------------------------------
-- M9: mcz24_tanka（単価マスタ）
-- Oracle: TBMCZ24_TANKAMST
-- --------------------------------------------
CREATE TABLE mcz24_tanka (
    yukou_kaishiki      VARCHAR(5),
    yukou_syuryoki      VARCHAR(5)      NOT NULL,
    skbtcd              VARCHAR(2)      NOT NULL,
    tanka_kbn           VARCHAR(2)      NOT NULL,
    tanka               NUMERIC(13,0),
    initnt              VARCHAR(6),
    inidate             TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updtnt              VARCHAR(6),
    upddate             TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updpgid             VARCHAR(20),
    delflg              VARCHAR(1)      NOT NULL DEFAULT '0',

    CONSTRAINT pk_mcz24 PRIMARY KEY (yukou_syuryoki, skbtcd, tanka_kbn),
    CONSTRAINT chk_mcz24_delflg CHECK (delflg IN ('0','1'))
);

COMMENT ON TABLE mcz24_tanka IS '単価マスタ（工数単価情報）';

-- --------------------------------------------
-- M10: mcz21_kanri_taisyo（管理対象マスタ）
-- Oracle: TBMCZ21_KANRITAISYO
-- --------------------------------------------
CREATE TABLE mcz21_kanri_taisyo (
    kanritsy_esqid      VARCHAR(6)      NOT NULL,
    kanritnt_esqid      VARCHAR(6)      NOT NULL,

    CONSTRAINT pk_mcz21 PRIMARY KEY (kanritsy_esqid, kanritnt_esqid)
);

COMMENT ON TABLE mcz21_kanri_taisyo IS '管理対象マスタ（代行入力の管理対象者↔管理担当者）';

-- ============================================
-- トランザクションテーブル
-- ============================================

-- --------------------------------------------
-- T1: tcz01_hosyu_kousuu（保守工数）— メインテーブル
-- Oracle: TCZ01HOSYUKOUSUU
-- GAP-D04: 採番マスタ → BIGSERIAL
-- GAP-D06: sgyymd を DATE 型に変換
-- --------------------------------------------
CREATE TABLE tcz01_hosyu_kousuu (
    seqno               BIGSERIAL       NOT NULL,
    skbtcd              VARCHAR(2)      NOT NULL DEFAULT '01',
    hssgytnt_esqid      VARCHAR(6)      NOT NULL,
    hssgytnt_name       VARCHAR(40),
    year_half           VARCHAR(5)      NOT NULL,
    sgyymd              DATE            NOT NULL,
    sys_kbn             VARCHAR(1),
    cause_sys_kbn       VARCHAR(1),
    taisyo_sknno        VARCHAR(8),
    taisyo_subsysno     VARCHAR(8),
    taisyo_aplid        VARCHAR(8),
    cause_sysno         VARCHAR(8),
    cause_subsysno      VARCHAR(8),
    cause_aplid         VARCHAR(8),
    kenmei              VARCHAR(128),
    hs_kategori         VARCHAR(4),
    hs_syubetu          VARCHAR(1),
    hs_unyou_kubun      VARCHAR(1),
    tmr_no              VARCHAR(5),
    sgy_iraisyo_no      VARCHAR(7),
    sgy_iraisya_esqid   VARCHAR(6),
    sgy_iraisya_name    VARCHAR(40),
    status              VARCHAR(1)      NOT NULL DEFAULT '0',
    kousuu              NUMERIC(7,2),
    initnt              VARCHAR(6),
    inidate             TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updtnt              VARCHAR(6),
    upddate             TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updpgid             VARCHAR(20),
    delflg              VARCHAR(1)      NOT NULL DEFAULT '0',

    CONSTRAINT pk_tcz01 PRIMARY KEY (seqno, skbtcd),
    CONSTRAINT chk_tcz01_status CHECK (status IN ('0','1','2','9')),
    CONSTRAINT chk_tcz01_skbtcd CHECK (skbtcd IN ('01','02')),
    CONSTRAINT chk_tcz01_delflg CHECK (delflg IN ('0','1')),
    CONSTRAINT chk_tcz01_kenmei_len CHECK (octet_length(kenmei) <= 128)
);

COMMENT ON TABLE  tcz01_hosyu_kousuu IS '保守工数（メイン業務テーブル）';
COMMENT ON COLUMN tcz01_hosyu_kousuu.seqno   IS '連番（BIGSERIAL 自動採番。GAP-D04: 採番マスタ→SEQUENCE）';
COMMENT ON COLUMN tcz01_hosyu_kousuu.skbtcd  IS '識別コード（01=ISID, 02=WorkSys）';
COMMENT ON COLUMN tcz01_hosyu_kousuu.status  IS 'ステータス（0:作成中, 1:確認, 2:確定, 9:非表示）';
COMMENT ON COLUMN tcz01_hosyu_kousuu.kousuu  IS '工数（時間単位, 小数表現。例: 2時間30分 = 2.50）';
COMMENT ON COLUMN tcz01_hosyu_kousuu.upddate IS '更新日時（楽観ロック CZ-101 に使用）';

-- --------------------------------------------
-- T2: tcz13_subsys_sum（サブシステム集計）
-- Oracle: TCZ13SUBSYSSUMTBL
-- --------------------------------------------
CREATE TABLE tcz13_subsys_sum (
    yyyymm              VARCHAR(6)      NOT NULL,
    nendo_half          VARCHAR(5)      NOT NULL,
    month               VARCHAR(2)      NOT NULL,
    sumkbn              VARCHAR(1)      NOT NULL,
    sys_kbn             VARCHAR(1),
    sknno               VARCHAR(8)      NOT NULL,
    subsknno            VARCHAR(8)      NOT NULL,
    aplid               VARCHAR(8),
    hs_syubetu          VARCHAR(1)      NOT NULL,
    hs_unyou_kubun      VARCHAR(1)      NOT NULL,
    hs_kategori_id      VARCHAR(4)      NOT NULL,
    skbtcd              VARCHAR(2)      NOT NULL,
    hs_kousuu           NUMERIC(10,2),
    initnt              VARCHAR(6),
    inidate             TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updtnt              VARCHAR(6),
    upddate             TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updpgid             VARCHAR(20),
    delflg              VARCHAR(1)      NOT NULL DEFAULT '0',

    CONSTRAINT pk_tcz13 PRIMARY KEY (yyyymm, sumkbn, skbtcd, sknno, subsknno, hs_syubetu, hs_unyou_kubun, hs_kategori_id),
    CONSTRAINT chk_tcz13_delflg CHECK (delflg IN ('0','1'))
);

COMMENT ON TABLE tcz13_subsys_sum IS 'サブシステム集計（バッチ集計結果）';

-- --------------------------------------------
-- T3: tcz14_grp_key（グループキー）
-- Oracle: TCZ14GRPKEYTBL
-- --------------------------------------------
CREATE TABLE tcz14_grp_key (
    nendo_half          VARCHAR(5)      NOT NULL,
    sknno               VARCHAR(8)      NOT NULL,
    hshk_bunrui_code    VARCHAR(10),
    is_kyk_hs_tnt_bs_code VARCHAR(10),
    kh_tnt_bs_code      VARCHAR(10),
    sys_kan_k_tbs_code  VARCHAR(10),
    initnt              VARCHAR(6),
    inidate             TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updtnt              VARCHAR(6),
    upddate             TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updpgid             VARCHAR(20),
    delflg              VARCHAR(1)      NOT NULL DEFAULT '0',

    CONSTRAINT pk_tcz14 PRIMARY KEY (nendo_half, sknno),
    CONSTRAINT chk_tcz14_delflg CHECK (delflg IN ('0','1'))
);

COMMENT ON TABLE tcz14_grp_key IS 'グループキー（グルーピング集計）';

-- --------------------------------------------
-- T4: tcz19_my_sys（MYシステム）
-- Oracle: TCZ19MYSYSTBL
-- --------------------------------------------
CREATE TABLE tcz19_my_sys (
    tnt_esqid           VARCHAR(6)      NOT NULL,
    sknno               VARCHAR(8)      NOT NULL,
    initnt              VARCHAR(6),
    inidate             TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updtnt              VARCHAR(6),
    upddate             TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updpgid             VARCHAR(20),
    delflg              VARCHAR(1)      NOT NULL DEFAULT '0',

    CONSTRAINT pk_tcz19 PRIMARY KEY (tnt_esqid, sknno),
    CONSTRAINT chk_tcz19_delflg CHECK (delflg IN ('0','1'))
);

COMMENT ON TABLE tcz19_my_sys IS 'MYシステム（お気に入り）';

-- --------------------------------------------
-- T5: tcz16_tnt_busyo_rireki（担当部署履歴）
-- Oracle: TCZ16_TNTBUSYORIREKI
-- GAP-D06: tnt_str_ymd/tnt_end_ymd を DATE 型に変換
-- --------------------------------------------
CREATE TABLE tcz16_tnt_busyo_rireki (
    tnt_kubun           VARCHAR(1)      NOT NULL,
    sknno               VARCHAR(8)      NOT NULL,
    tnt_str_ymd         DATE,
    tnt_end_ymd         DATE            NOT NULL,
    tnt_busyo           VARCHAR(7),
    initnt              VARCHAR(6),
    inidate             TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updtnt              VARCHAR(6),
    upddate             TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updpgid             VARCHAR(20),
    delflg              VARCHAR(1)      NOT NULL DEFAULT '0',

    CONSTRAINT pk_tcz16 PRIMARY KEY (tnt_kubun, sknno, tnt_end_ymd),
    CONSTRAINT chk_tcz16_delflg CHECK (delflg IN ('0','1'))
);

COMMENT ON TABLE  tcz16_tnt_busyo_rireki IS '担当部署履歴（人事異動）';
COMMENT ON COLUMN tcz16_tnt_busyo_rireki.tnt_kubun IS '担当区分（0=IS担当, 2=管理担当）';

-- --------------------------------------------
-- T6: batch_execution_log（バッチ実行履歴）— 新規追加
-- GAP-O03: 旧システムには存在しない新規テーブル
-- spec #10 batch-processing で定義
-- --------------------------------------------
CREATE TABLE batch_execution_log (
    id                  BIGSERIAL       NOT NULL,
    batch_name          VARCHAR(50)     NOT NULL,
    started_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at         TIMESTAMP WITH TIME ZONE,
    status              VARCHAR(10)     NOT NULL,
    records_affected    INTEGER         DEFAULT 0,
    error_message       TEXT,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_batch_log PRIMARY KEY (id),
    CONSTRAINT chk_batch_status CHECK (status IN ('RUNNING','SUCCESS','FAILED'))
);

COMMENT ON TABLE  batch_execution_log IS 'バッチ実行履歴（監視・障害調査用）';
COMMENT ON COLUMN batch_execution_log.batch_name IS 'バッチ名（例: subsys-aggregation, grpkey-aggregation）';

-- ============================================
-- インデックス（T-008）
-- ============================================
CREATE INDEX idx_tcz01_tnt_yyyymm ON tcz01_hosyu_kousuu (hssgytnt_esqid, year_half);
CREATE INDEX idx_tcz01_sgyymd     ON tcz01_hosyu_kousuu (sgyymd);
CREATE INDEX idx_tcz01_taisyo     ON tcz01_hosyu_kousuu (taisyo_sknno, taisyo_subsysno);
CREATE INDEX idx_tcz01_status     ON tcz01_hosyu_kousuu (status, delflg);
CREATE INDEX idx_tcz13_yyyymm     ON tcz13_subsys_sum (yyyymm, skbtcd);
CREATE INDEX idx_mav03_search     ON mav03_subsys (skbtcd, sknno);
