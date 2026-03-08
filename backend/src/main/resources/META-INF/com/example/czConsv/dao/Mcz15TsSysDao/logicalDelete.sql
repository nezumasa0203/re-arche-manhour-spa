UPDATE mcz15_ts_sys
   SET delflg = '1',
       upddate = CURRENT_TIMESTAMP,
       updtnt = /* entity.updtnt */'SYSTEM'
 WHERE tssknno = /* entity.tssknno */'0001'
   AND tssubsysno = /* entity.tssubsysno */'001'
   AND yukou_syuryoki = /* entity.yukouSyuryoki */'20991231'
   AND delflg = '0'
