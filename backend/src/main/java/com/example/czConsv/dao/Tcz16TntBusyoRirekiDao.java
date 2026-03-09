package com.example.czConsv.dao;

import com.example.czConsv.entity.Tcz16TntBusyoRireki;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.Update;
import org.seasar.doma.boot.ConfigAutowireable;

@Dao
@ConfigAutowireable
public interface Tcz16TntBusyoRirekiDao {

    @Select
    Optional<Tcz16TntBusyoRireki> selectById(String tntKubun, String sknno, LocalDate tntEndYmd);

    @Select
    List<Tcz16TntBusyoRireki> selectAll();

    @Select
    List<Tcz16TntBusyoRireki> selectBySkbtcd(String skbtcd);

    @Insert
    int insert(Tcz16TntBusyoRireki entity);

    @Update
    int update(Tcz16TntBusyoRireki entity);

    @Update(sqlFile = true)
    int logicalDelete(Tcz16TntBusyoRireki entity);
}
