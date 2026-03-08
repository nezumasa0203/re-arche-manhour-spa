package com.example.czConsv.dao;

import com.example.czConsv.entity.Mcz02HosyuKategori;
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
public interface Mcz02HosyuKategoriDao {

    @Select
    Optional<Mcz02HosyuKategori> selectById(String hsKategori, LocalDate yukouKaishiki, LocalDate yukouSyuryoki);

    @Select
    List<Mcz02HosyuKategori> selectAll();

    @Insert
    int insert(Mcz02HosyuKategori entity);

    @Update
    int update(Mcz02HosyuKategori entity);

    @Update(sqlFile = true)
    int logicalDelete(Mcz02HosyuKategori entity);
}
