package com.example.czConsv.dao;

import com.example.czConsv.entity.Mcz24Tanka;
import java.util.List;
import java.util.Optional;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.Update;
import org.seasar.doma.boot.ConfigAutowireable;

@Dao
@ConfigAutowireable
public interface Mcz24TankaDao {

    @Select
    Optional<Mcz24Tanka> selectById(String yukouSyuryoki, String skbtcd, String tankaKbn);

    @Select
    List<Mcz24Tanka> selectAll();

    @Insert
    int insert(Mcz24Tanka entity);

    @Update
    int update(Mcz24Tanka entity);

    @Update(sqlFile = true)
    int logicalDelete(Mcz24Tanka entity);
}
