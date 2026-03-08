UPDATE mav03_subsys
   SET delflg = '1',
       upddate = CURRENT_TIMESTAMP,
       updtnt = /* entity.updtnt */'SYSTEM'
 WHERE skbtcd = /* entity.skbtcd */'01'
   AND sknno = /* entity.sknno */'0001'
   AND subsysno = /* entity.subsysno */'001'
   AND delflg = '0'
