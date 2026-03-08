SELECT /*%expand*/*
  FROM batch_execution_log
 WHERE batch_name = /* batchName */'MONTHLY_AGGREGATE'
 ORDER BY started_at DESC
