UPDATE tcz01_hosyu_kousuu
   SET delflg = '1',
       upddate = CURRENT_TIMESTAMP,
       updtnt = /* entity.updtnt */'SYSTEM'
 WHERE seqno = /* entity.seqNo */1
   AND skbtcd = /* entity.skbtcd */'01'
   AND delflg = '0'
