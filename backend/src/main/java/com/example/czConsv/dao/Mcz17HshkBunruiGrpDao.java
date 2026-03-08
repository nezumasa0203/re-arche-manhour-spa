package com.example.czConsv.dao;

import com.example.czConsv.entity.Mcz17HshkBunruiGrp;
import java.util.List;
import java.util.Optional;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.Update;
import org.seasar.doma.boot.ConfigAutowireable;

@Dao
@ConfigAutowireable
public interface Mcz17HshkBunruiGrpDao {

    @Select
    Optional<Mcz17HshkBunruiGrp> selectById(String sknno, String yukouKaishiki, String yukouSyuryoki);

    @Select
    List<Mcz17HshkBunruiGrp> selectAll();

    @Insert
    int insert(Mcz17HshkBunruiGrp entity);

    @Update
    int update(Mcz17HshkBunruiGrp entity);

    @Update(sqlFile = true)
    int logicalDelete(Mcz17HshkBunruiGrp entity);
}
