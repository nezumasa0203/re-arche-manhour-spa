SELECT /*%expand*/*
  FROM mcz02_hosyu_kategori
 WHERE hs_kategori = /* hsKategori */'01'
   AND yukou_kaishiki = /* yukouKaishiki */'2026-01-01'
   AND yukou_syuryoki = /* yukouSyuryoki */'2099-12-31'
   AND delflg = '0'
