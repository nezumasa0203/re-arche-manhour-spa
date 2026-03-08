package com.example.czConsv.dao;

import com.example.czConsv.entity.Tcz19MySys;
import java.util.List;
import java.util.Optional;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.Update;
import org.seasar.doma.boot.ConfigAutowireable;

@Dao
@ConfigAutowireable
public interface Tcz19MySysDao {

    @Select
    Optional<Tcz19MySys> selectById(String tntEsqid, String sknno);

    @Select
    List<Tcz19MySys> selectAll();

    @Insert
    int insert(Tcz19MySys entity);

    @Update
    int update(Tcz19MySys entity);

    @Update(sqlFile = true)
    int logicalDelete(Tcz19MySys entity);
}
