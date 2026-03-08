package com.example.czConsv.dao;

import com.example.czConsv.entity.Mcz03AplBunruiGrp;
import java.util.List;
import java.util.Optional;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.Update;
import org.seasar.doma.boot.ConfigAutowireable;

@Dao
@ConfigAutowireable
public interface Mcz03AplBunruiGrpDao {

    @Select
    Optional<Mcz03AplBunruiGrp> selectById(String sknno, String yukouKaishiki, String yukouSyuryoki);

    @Select
    List<Mcz03AplBunruiGrp> selectAll();

    @Insert
    int insert(Mcz03AplBunruiGrp entity);

    @Update
    int update(Mcz03AplBunruiGrp entity);

    @Update(sqlFile = true)
    int logicalDelete(Mcz03AplBunruiGrp entity);
}
