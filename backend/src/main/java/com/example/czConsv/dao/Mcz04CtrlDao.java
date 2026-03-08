package com.example.czConsv.dao;

import com.example.czConsv.entity.Mcz04Ctrl;
import java.util.List;
import java.util.Optional;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.Update;
import org.seasar.doma.boot.ConfigAutowireable;

@Dao
@ConfigAutowireable
public interface Mcz04CtrlDao {

    @Select
    Optional<Mcz04Ctrl> selectById(String sysid, String yyyymm);

    @Select
    List<Mcz04Ctrl> selectAll();

    @Insert
    int insert(Mcz04Ctrl entity);

    @Update
    int update(Mcz04Ctrl entity);

    @Update(sqlFile = true)
    int logicalDelete(Mcz04Ctrl entity);
}
