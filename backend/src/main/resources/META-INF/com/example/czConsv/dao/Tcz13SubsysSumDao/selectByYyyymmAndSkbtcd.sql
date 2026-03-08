SELECT /*%expand*/*
  FROM tcz13_subsys_sum
 WHERE yyyymm = /* yyyymm */'202601'
   AND skbtcd = /* skbtcd */'01'
   AND delflg = '0'
 ORDER BY sumkbn, sknno, subsknno
