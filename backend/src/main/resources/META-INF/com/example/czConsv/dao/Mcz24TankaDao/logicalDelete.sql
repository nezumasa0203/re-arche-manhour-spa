UPDATE mcz24_tanka
   SET delflg = '1',
       upddate = CURRENT_TIMESTAMP,
       updtnt = /* entity.updtnt */'SYSTEM'
 WHERE yukou_syuryoki = /* entity.yukouSyuryoki */'20991231'
   AND skbtcd = /* entity.skbtcd */'01'
   AND tanka_kbn = /* entity.tankaKbn */'01'
   AND delflg = '0'
