-- ============================================
-- CZ マイグレーション — 初期データ投入
-- 開発環境用シードデータ
-- spec: .specify/features/01-database-schema/spec.md
-- ============================================

-- --------------------------------------------
-- mcz04_ctrl: コントロールマスタ初期データ
-- sysid='00': 管理モード (jinjiMode:false)
-- sysid='01': 人事モード (jinjiMode:true)
-- --------------------------------------------
INSERT INTO mcz04_ctrl (sysid, yyyymm, online_flg, renketsu_flg, gjkt_flg, data_sk_flg, initnt, updtnt, updpgid)
VALUES
    ('00', '202602', '0', '0', '0', '0', 'SYSTEM', 'SYSTEM', 'INIT'),
    ('00', '202601', '0', '0', '1', '1', 'SYSTEM', 'SYSTEM', 'INIT'),
    ('00', '202512', '0', '0', '1', '1', 'SYSTEM', 'SYSTEM', 'INIT'),
    ('01', '202602', '0', '0', '0', '0', 'SYSTEM', 'SYSTEM', 'INIT'),
    ('01', '202601', '0', '0', '1', '1', 'SYSTEM', 'SYSTEM', 'INIT'),
    ('01', '202512', '0', '0', '1', '1', 'SYSTEM', 'SYSTEM', 'INIT');

-- --------------------------------------------
-- mcz02_hosyu_kategori: 保守カテゴリマスタ サンプルデータ
-- --------------------------------------------
INSERT INTO mcz02_hosyu_kategori (hs_kategori, yukou_kaishiki, yukou_syuryoki, hs_syubetu, hs_unyou_kubun, hs_kategori_mei, hs_kategori_naiyo, initnt, updtnt, updpgid)
VALUES
    ('0001', '2020-04-01', '2099-12-31', '0', '0', '障害対応', '障害に関する保守作業', 'SYSTEM', 'SYSTEM', 'INIT'),
    ('0002', '2020-04-01', '2099-12-31', '1', '0', '機能追加', '新規機能の追加作業', 'SYSTEM', 'SYSTEM', 'INIT'),
    ('0003', '2020-04-01', '2099-12-31', '2', '0', '改善', 'システム改善作業', 'SYSTEM', 'SYSTEM', 'INIT'),
    ('0004', '2020-04-01', '2099-12-31', '3', '1', '運用保守', '定期的な運用保守作業', 'SYSTEM', 'SYSTEM', 'INIT'),
    ('0005', '2020-04-01', '2099-12-31', '4', '1', 'その他', 'その他の保守作業', 'SYSTEM', 'SYSTEM', 'INIT');

-- --------------------------------------------
-- mav01_sys: システム管理Noマスタ サンプルデータ
-- --------------------------------------------
INSERT INTO mav01_sys (skbtcd, sknno, sys_mei, sys_mei_kn, yukou_kaishiki, yukou_syuryoki, initnt, updtnt, updpgid)
VALUES
    ('01', '00000001', '基幹システム', 'キカンシステム', '20200401', '20991231', 'SYSTEM', 'SYSTEM', 'INIT'),
    ('01', '00000002', '人事給与システム', 'ジンジキュウヨシステム', '20200401', '20991231', 'SYSTEM', 'SYSTEM', 'INIT'),
    ('01', '00000003', '財務会計システム', 'ザイムカイケイシステム', '20200401', '20991231', 'SYSTEM', 'SYSTEM', 'INIT'),
    ('02', '00000001', 'WorkSys基幹', 'ワークシスキカン', '20200401', '20991231', 'SYSTEM', 'SYSTEM', 'INIT');

-- --------------------------------------------
-- mav03_subsys: サブシステムNoマスタ サンプルデータ
-- --------------------------------------------
INSERT INTO mav03_subsys (skbtcd, sknno, subsysno, aplid, subsys_mei, subsys_mei_kn, yukou_kaishiki, yukou_syuryoki, initnt, updtnt, updpgid)
VALUES
    ('01', '00000001', '00000001', '00000001', '受注管理', 'ジュチュウカンリ', '20200401', '20991231', 'SYSTEM', 'SYSTEM', 'INIT'),
    ('01', '00000001', '00000002', '00000002', '在庫管理', 'ザイコカンリ', '20200401', '20991231', 'SYSTEM', 'SYSTEM', 'INIT'),
    ('01', '00000002', '00000001', '00000003', '勤怠管理', 'キンタイカンリ', '20200401', '20991231', 'SYSTEM', 'SYSTEM', 'INIT'),
    ('01', '00000003', '00000001', '00000004', '仕訳入力', 'シワケニュウリョク', '20200401', '20991231', 'SYSTEM', 'SYSTEM', 'INIT');

-- --------------------------------------------
-- mcz15_ts_sys: 対象システムNoマスタ サンプルデータ
-- --------------------------------------------
INSERT INTO mcz15_ts_sys (tssknno, tssubsysno, yukou_kaishiki, yukou_syuryoki, aplid, tssysname, tssysname_kn, tssubsysname, tssubsysname_kn, initnt, updtnt, updpgid)
VALUES
    ('00000001', '00000001', '20201', '99992', '00000001', '基幹システム', 'キカンシステム', '受注管理', 'ジュチュウカンリ', 'SYSTEM', 'SYSTEM', 'INIT'),
    ('00000001', '00000002', '20201', '99992', '00000002', '基幹システム', 'キカンシステム', '在庫管理', 'ザイコカンリ', 'SYSTEM', 'SYSTEM', 'INIT'),
    ('00000002', '00000001', '20201', '99992', '00000003', '人事給与システム', 'ジンジキュウヨシステム', '勤怠管理', 'キンタイカンリ', 'SYSTEM', 'SYSTEM', 'INIT');

-- --------------------------------------------
-- mcz12_orgn_kr: 組織構造マスタ サンプルデータ（7階層）
-- --------------------------------------------
INSERT INTO mcz12_orgn_kr (sikcd, startymd, endymd, krikaisocd, hyojikn, hyojikj, hyojiryaku,
    sikcdhonb, honbhyojikn, honbhyojikj, honbhyojiryaku,
    sikcdkyk, kykhyojikn, kykhyojikj, kykhyojiryaku,
    sikcdsitu, situhyojikn, situhyojikj, situhyojiryaku,
    sikcdbu, buhyojikn, buhyojikj, buhyojiryaku,
    sikcdka, kahyojikn, kahyojikj, kahyojiryaku)
VALUES
    ('0000001', '2020-04-01', '2099-12-31', '01', 'ゼンシャ', '全社', '全社',
     '0000001', 'ゼンシャ', '全社', '全社',
     NULL, NULL, NULL, NULL,
     NULL, NULL, NULL, NULL,
     NULL, NULL, NULL, NULL,
     NULL, NULL, NULL, NULL),
    ('0001001', '2020-04-01', '2099-12-31', '02', 'ジョウホウホンブ', '情報本部', '情報本部',
     '0000001', 'ゼンシャ', '全社', '全社',
     '0001001', 'ジョウホウホンブ', '情報本部', '情報本部',
     NULL, NULL, NULL, NULL,
     NULL, NULL, NULL, NULL,
     NULL, NULL, NULL, NULL),
    ('0001011', '2020-04-01', '2099-12-31', '05', 'カイハツイチカ', '開発一課', '開発一課',
     '0000001', 'ゼンシャ', '全社', '全社',
     '0001001', 'ジョウホウホンブ', '情報本部', '情報本部',
     NULL, NULL, NULL, NULL,
     '0001010', 'カイハツブ', '開発部', '開発部',
     '0001011', 'カイハツイチカ', '開発一課', '開発一課');

-- --------------------------------------------
-- mcz24_tanka: 単価マスタ サンプルデータ
-- --------------------------------------------
INSERT INTO mcz24_tanka (yukou_kaishiki, yukou_syuryoki, skbtcd, tanka_kbn, tanka, initnt, updtnt, updpgid)
VALUES
    ('20201', '99992', '01', '01', 5000, 'SYSTEM', 'SYSTEM', 'INIT'),
    ('20201', '99992', '01', '02', 8000, 'SYSTEM', 'SYSTEM', 'INIT'),
    ('20201', '99992', '02', '01', 4500, 'SYSTEM', 'SYSTEM', 'INIT');

-- --------------------------------------------
-- mcz21_kanri_taisyo: 管理対象マスタ サンプルデータ
-- （代行入力: 管理担当者 E00002 が E00001 の代行可能）
-- --------------------------------------------
INSERT INTO mcz21_kanri_taisyo (kanritsy_esqid, kanritnt_esqid)
VALUES
    ('E00001', 'E00002'),
    ('E00003', 'E00002');

-- --------------------------------------------
-- tcz01_hosyu_kousuu: 保守工数 サンプルデータ
-- --------------------------------------------
INSERT INTO tcz01_hosyu_kousuu (skbtcd, hssgytnt_esqid, hssgytnt_name, year_half, sgyymd, taisyo_sknno, taisyo_subsysno, kenmei, hs_kategori, hs_syubetu, status, kousuu, initnt, updtnt, updpgid)
VALUES
    ('01', 'E00001', '山田太郎', '20262', '2026-02-03', '00000001', '00000001', '受注管理障害対応', '0001', '0', '0', 2.50, 'E00001', 'E00001', 'FORM010'),
    ('01', 'E00001', '山田太郎', '20262', '2026-02-04', '00000001', '00000002', '在庫管理改修', '0002', '1', '1', 4.00, 'E00001', 'E00001', 'FORM010'),
    ('01', 'E00002', '鈴木花子', '20262', '2026-02-03', '00000002', '00000001', '勤怠システム運用', '0004', '3', '2', 1.50, 'E00002', 'E00002', 'FORM010');
