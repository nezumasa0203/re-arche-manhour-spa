SELECT /*%expand*/*
  FROM tcz13_subsys_sum
 WHERE yyyymm IN /* yyyymmList */('202501', '202502')
   AND skbtcd = /* skbtcd */'SKB1'
   AND delflg = '0'
 ORDER BY hs_kategori_id, sknno, subsknno, yyyymm
