UPDATE mcz04_ctrl
   SET delflg = '1',
       upddate = CURRENT_TIMESTAMP,
       updtnt = /* entity.updtnt */'SYSTEM'
 WHERE sysid = /* entity.sysid */'00'
   AND yyyymm = /* entity.yyyymm */'202601'
   AND delflg = '0'
