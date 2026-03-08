UPDATE tcz16_tnt_busyo_rireki
   SET delflg = '1',
       upddate = CURRENT_TIMESTAMP,
       updtnt = /* entity.updtnt */'SYSTEM'
 WHERE tnt_kubun = /* entity.tntKubun */'1'
   AND sknno = /* entity.sknno */'00000001'
   AND tnt_end_ymd = /* entity.tntEndYmd */'2026-03-31'
   AND delflg = '0'
