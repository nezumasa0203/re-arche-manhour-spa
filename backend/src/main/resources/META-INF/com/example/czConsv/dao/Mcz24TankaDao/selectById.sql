SELECT /*%expand*/*
  FROM mcz24_tanka
 WHERE yukou_syuryoki = /* yukouSyuryoki */'20991231'
   AND skbtcd = /* skbtcd */'01'
   AND tanka_kbn = /* tankaKbn */'01'
   AND delflg = '0'
