SELECT /*%expand*/*
  FROM mav03_subsys
 WHERE delflg = '0'
 ORDER BY skbtcd, sknno, subsysno
