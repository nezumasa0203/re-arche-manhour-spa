package com.example.czConsv.dao;

import com.example.czConsv.entity.Mav03Subsys;
import java.util.List;
import java.util.Optional;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.Update;
import org.seasar.doma.boot.ConfigAutowireable;

@Dao
@ConfigAutowireable
public interface Mav03SubsysDao {

    @Select
    Optional<Mav03Subsys> selectById(String skbtcd, String sknno, String subsysno);

    @Select
    List<Mav03Subsys> selectAll();

    @Insert
    int insert(Mav03Subsys entity);

    @Update
    int update(Mav03Subsys entity);

    @Update(sqlFile = true)
    int logicalDelete(Mav03Subsys entity);
}
