SELECT /*%expand*/*
  FROM mcz02_hosyu_kategori
 WHERE delflg = '0'
/*%if nendo != null*/
   AND nendo = /* nendo */'2025'
/*%end*/
 ORDER BY kategori_id
