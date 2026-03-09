SELECT /*%expand*/*
  FROM mav03_subsys
 WHERE delflg = '0'
/*%if keyword != null && !keyword.isEmpty()*/
   AND (subsys_mei LIKE /* @prefix(keyword) */'サブシス%'
        OR aplid LIKE /* @prefix(keyword) */'AP%')
/*%end*/
 ORDER BY skbtcd, sknno, subsysno
