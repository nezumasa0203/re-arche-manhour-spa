WITH RECURSIVE hierarchy AS (
    SELECT * FROM mcz12_orgn_kr WHERE sikcd = /* sikcd */'0000001'
    UNION ALL
    SELECT c.* FROM mcz12_orgn_kr c
      JOIN hierarchy h ON c.krijsikcd = h.sikcd
)
SELECT * FROM hierarchy ORDER BY sikcd
