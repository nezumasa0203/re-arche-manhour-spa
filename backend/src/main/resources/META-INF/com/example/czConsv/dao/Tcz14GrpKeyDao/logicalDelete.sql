UPDATE tcz14_grp_key
   SET delflg = '1',
       upddate = CURRENT_TIMESTAMP,
       updtnt = /* entity.updtnt */'SYSTEM'
 WHERE nendo_half = /* entity.nendoHalf */'20261'
   AND sknno = /* entity.sknno */'00000001'
   AND delflg = '0'
