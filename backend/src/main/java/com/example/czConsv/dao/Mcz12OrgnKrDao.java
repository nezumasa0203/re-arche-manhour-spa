package com.example.czConsv.dao;

import com.example.czConsv.entity.Mcz12OrgnKr;
import java.util.List;
import java.util.Optional;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.Update;
import org.seasar.doma.boot.ConfigAutowireable;

@Dao
@ConfigAutowireable
public interface Mcz12OrgnKrDao {

    @Select
    Optional<Mcz12OrgnKr> selectById(String sikcd);

    @Select
    List<Mcz12OrgnKr> selectAll();

    @Select
    List<Mcz12OrgnKr> selectHierarchy(String sikcd);

    @Insert
    int insert(Mcz12OrgnKr entity);

    @Update
    int update(Mcz12OrgnKr entity);
}
