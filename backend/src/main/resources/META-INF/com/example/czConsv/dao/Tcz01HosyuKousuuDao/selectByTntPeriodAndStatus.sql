SELECT /*%expand*/*
  FROM tcz01_hosyu_kousuu
 WHERE hssgytnt_esqid = /* hssgytntEsqid */'E00001'
   AND year_half = /* yearHalf */'2026-01'
   AND status = /* status */'0'
   AND delflg = '0'
 ORDER BY sgyymd, seqno
