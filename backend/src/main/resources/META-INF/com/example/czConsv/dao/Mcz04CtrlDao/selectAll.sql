SELECT /*%expand*/*
  FROM mcz04_ctrl
 WHERE delflg = '0'
 ORDER BY sysid, yyyymm
