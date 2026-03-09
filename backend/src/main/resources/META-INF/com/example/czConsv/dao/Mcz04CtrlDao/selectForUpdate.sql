SELECT /*%expand*/*
  FROM mcz04_ctrl
 WHERE sysid = /* sysid */'00'
   AND yyyymm = /* yyyymm */'202502'
   AND delflg = '0'
   FOR UPDATE
