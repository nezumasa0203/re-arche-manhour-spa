UPDATE tcz19_my_sys
   SET delflg = '1',
       upddate = CURRENT_TIMESTAMP,
       updtnt = /* entity.updtnt */'SYSTEM'
 WHERE tnt_esqid = /* entity.tntEsqid */'E00001'
   AND sknno = /* entity.sknno */'00000001'
   AND delflg = '0'
