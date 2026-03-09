package com.example.czConsv.dao;

import com.example.czConsv.entity.Tcz13SubsysSum;
import java.util.List;
import java.util.Optional;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.Update;
import org.seasar.doma.boot.ConfigAutowireable;

@Dao
@ConfigAutowireable
public interface Tcz13SubsysSumDao {

    @Select
    Optional<Tcz13SubsysSum> selectById(String yyyymm, String sumkbn, String skbtcd, String sknno, String subsknno, String hsSyubetu, String hsUnyouKubun, String hsKategoriId);

    @Select
    List<Tcz13SubsysSum> selectByYyyymmAndSkbtcd(String yyyymm, String skbtcd);

    @Select
    List<Tcz13SubsysSum> selectAll();

    @Select
    List<Tcz13SubsysSum> selectByNendoHalf(String nendoHalf);

    @Select
    List<Tcz13SubsysSum> selectByYyyymmRangeAndSkbtcd(java.util.List<String> yyyymmList, String skbtcd);

    @Insert
    int insert(Tcz13SubsysSum entity);

    @Update
    int update(Tcz13SubsysSum entity);

    @Update(sqlFile = true)
    int logicalDelete(Tcz13SubsysSum entity);
}
