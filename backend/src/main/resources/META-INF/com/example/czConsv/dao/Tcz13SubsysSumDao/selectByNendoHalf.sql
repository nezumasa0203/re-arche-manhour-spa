SELECT /*%expand*/*
  FROM tcz13_subsys_sum
 WHERE nendo_half = /* nendoHalf */'20161'
   AND delflg = '0'
 ORDER BY hs_kategori_id, sknno, subsknno, yyyymm
