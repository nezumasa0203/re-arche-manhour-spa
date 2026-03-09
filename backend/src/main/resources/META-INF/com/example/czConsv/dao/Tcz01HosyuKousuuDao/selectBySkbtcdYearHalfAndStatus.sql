SELECT /*%expand*/*
  FROM tcz01_hosyu_kousuu
 WHERE skbtcd = /* skbtcd */'SKB1'
   AND year_half = /* yearHalf */'202502'
   AND status = /* status */'0'
   AND delflg = '0'
 ORDER BY hssgytnt_esqid, sgyymd, seqno
