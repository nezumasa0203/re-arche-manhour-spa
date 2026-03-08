package com.example.czConsv.dao;

import com.example.czConsv.entity.Mcz15TsSys;
import java.util.List;
import java.util.Optional;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.Update;
import org.seasar.doma.boot.ConfigAutowireable;

@Dao
@ConfigAutowireable
public interface Mcz15TsSysDao {

    @Select
    Optional<Mcz15TsSys> selectById(String tssknno, String tssubsysno, String yukouSyuryoki);

    @Select
    List<Mcz15TsSys> selectAll();

    @Insert
    int insert(Mcz15TsSys entity);

    @Update
    int update(Mcz15TsSys entity);

    @Update(sqlFile = true)
    int logicalDelete(Mcz15TsSys entity);
}
