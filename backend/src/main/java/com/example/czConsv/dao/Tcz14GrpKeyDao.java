package com.example.czConsv.dao;

import com.example.czConsv.entity.Tcz14GrpKey;
import java.util.List;
import java.util.Optional;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.Update;
import org.seasar.doma.boot.ConfigAutowireable;

@Dao
@ConfigAutowireable
public interface Tcz14GrpKeyDao {

    @Select
    Optional<Tcz14GrpKey> selectById(String nendoHalf, String sknno);

    @Select
    List<Tcz14GrpKey> selectAll();

    @Insert
    int insert(Tcz14GrpKey entity);

    @Update
    int update(Tcz14GrpKey entity);

    @Update(sqlFile = true)
    int logicalDelete(Tcz14GrpKey entity);
}
