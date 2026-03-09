package com.example.czConsv.dao;

import com.example.czConsv.entity.Tcz01HosyuKousuu;
import java.util.List;
import java.util.Optional;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.Update;
import org.seasar.doma.boot.ConfigAutowireable;

@Dao
@ConfigAutowireable
public interface Tcz01HosyuKousuuDao {

    @Select
    Optional<Tcz01HosyuKousuu> selectById(Long seqno, String skbtcd);

    @Select
    List<Tcz01HosyuKousuu> selectByTntAndPeriod(String hssgytntEsqid, String yearHalf);

    @Select
    List<Tcz01HosyuKousuu> selectByTntPeriodAndStatus(
            String hssgytntEsqid, String yearHalf, String status);

    @Select
    List<Tcz01HosyuKousuu> selectBySkbtcdAndYearHalf(String skbtcd, String yearHalf);

    @Select
    List<Tcz01HosyuKousuu> selectBySkbtcdYearHalfAndStatus(String skbtcd, String yearHalf, String status);

    @Select
    List<Tcz01HosyuKousuu> selectAll();

    @Insert
    int insert(Tcz01HosyuKousuu entity);

    @Update
    int update(Tcz01HosyuKousuu entity);

    @Update(sqlFile = true)
    int logicalDelete(Tcz01HosyuKousuu entity);
}
