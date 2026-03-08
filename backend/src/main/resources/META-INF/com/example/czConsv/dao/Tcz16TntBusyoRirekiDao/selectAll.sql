SELECT /*%expand*/*
  FROM tcz16_tnt_busyo_rireki
 WHERE delflg = '0'
 ORDER BY tnt_kubun, sknno, tnt_end_ymd
