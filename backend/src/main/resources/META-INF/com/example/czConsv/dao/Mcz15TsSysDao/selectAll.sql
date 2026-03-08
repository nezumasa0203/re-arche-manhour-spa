SELECT /*%expand*/*
  FROM mcz15_ts_sys
 WHERE delflg = '0'
 ORDER BY tssknno, tssubsysno, yukou_syuryoki
