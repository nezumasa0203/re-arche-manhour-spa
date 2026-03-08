SELECT /*%expand*/*
  FROM tcz13_subsys_sum
 WHERE yyyymm = /* yyyymm */'202601'
   AND sumkbn = /* sumkbn */'1'
   AND skbtcd = /* skbtcd */'01'
   AND sknno = /* sknno */'00000001'
   AND subsknno = /* subsknno */'00000001'
   AND hs_syubetu = /* hsSyubetu */'1'
   AND hs_unyou_kubun = /* hsUnyouKubun */'1'
   AND hs_kategori_id = /* hsKategoriId */'0001'
   AND delflg = '0'
