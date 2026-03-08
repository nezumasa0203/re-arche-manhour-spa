SELECT /*%expand*/*
  FROM mcz24_tanka
 WHERE delflg = '0'
 ORDER BY yukou_syuryoki, skbtcd, tanka_kbn
