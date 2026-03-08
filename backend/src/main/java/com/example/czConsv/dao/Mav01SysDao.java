package com.example.czConsv.dao;

import com.example.czConsv.entity.Mav01Sys;
import java.util.List;
import java.util.Optional;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.Update;
import org.seasar.doma.boot.ConfigAutowireable;

@Dao
@ConfigAutowireable
public interface Mav01SysDao {

    @Select
    Optional<Mav01Sys> selectById(String skbtcd, String sknno);

    @Select
    List<Mav01Sys> selectAll();

    @Insert
    int insert(Mav01Sys entity);

    @Update
    int update(Mav01Sys entity);

    @Update(sqlFile = true)
    int logicalDelete(Mav01Sys entity);
}
