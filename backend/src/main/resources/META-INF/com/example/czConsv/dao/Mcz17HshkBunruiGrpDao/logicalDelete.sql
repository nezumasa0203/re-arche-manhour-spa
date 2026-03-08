UPDATE mcz17_hshk_bunrui_grp
   SET delflg = '1',
       upddate = CURRENT_TIMESTAMP,
       updtnt = /* entity.updtnt */'SYSTEM'
 WHERE sknno = /* entity.sknno */'0001'
   AND yukou_kaishiki = /* entity.yukouKaishiki */'20260101'
   AND yukou_syuryoki = /* entity.yukouSyuryoki */'20991231'
   AND delflg = '0'
