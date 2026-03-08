UPDATE tcz13_subsys_sum
   SET delflg = '1',
       upddate = CURRENT_TIMESTAMP,
       updtnt = /* entity.updtnt */'SYSTEM'
 WHERE yyyymm = /* entity.yyyymm */'202601'
   AND sumkbn = /* entity.sumkbn */'1'
   AND skbtcd = /* entity.skbtcd */'01'
   AND sknno = /* entity.sknno */'00000001'
   AND subsknno = /* entity.subsknno */'00000001'
   AND hs_syubetu = /* entity.hsSyubetu */'1'
   AND hs_unyou_kubun = /* entity.hsUnyouKubun */'1'
   AND hs_kategori_id = /* entity.hsKategoriId */'0001'
   AND delflg = '0'
