UPDATE mcz02_hosyu_kategori
   SET delflg = '1',
       upddate = CURRENT_TIMESTAMP,
       updtnt = /* entity.updtnt */'SYSTEM'
 WHERE hs_kategori = /* entity.hsKategori */'01'
   AND yukou_kaishiki = /* entity.yukouKaishiki */'2026-01-01'
   AND yukou_syuryoki = /* entity.yukouSyuryoki */'2099-12-31'
   AND delflg = '0'
