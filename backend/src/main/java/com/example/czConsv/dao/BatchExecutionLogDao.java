package com.example.czConsv.dao;

import com.example.czConsv.entity.BatchExecutionLog;
import java.util.List;
import java.util.Optional;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.Update;
import org.seasar.doma.boot.ConfigAutowireable;

@Dao
@ConfigAutowireable
public interface BatchExecutionLogDao {

    @Select
    Optional<BatchExecutionLog> selectById(Long id);

    @Select
    List<BatchExecutionLog> selectByBatchName(String batchName);

    @Select
    List<BatchExecutionLog> selectAll();

    @Insert
    int insert(BatchExecutionLog entity);

    @Update
    int update(BatchExecutionLog entity);
}
