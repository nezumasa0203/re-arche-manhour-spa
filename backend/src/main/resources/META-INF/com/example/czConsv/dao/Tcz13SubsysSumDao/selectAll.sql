SELECT /*%expand*/*
  FROM tcz13_subsys_sum
 WHERE delflg = '0'
 ORDER BY yyyymm, sumkbn, sknno, subsknno
