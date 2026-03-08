package com.example.czConsv.dao;

import com.example.czConsv.entity.Mcz21KanriTaisyo;
import java.util.List;
import java.util.Optional;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.Update;
import org.seasar.doma.boot.ConfigAutowireable;

@Dao
@ConfigAutowireable
public interface Mcz21KanriTaisyoDao {

    @Select
    Optional<Mcz21KanriTaisyo> selectById(String kanritsyEsqid, String kanritntEsqid);

    @Select
    List<Mcz21KanriTaisyo> selectAll();

    @Insert
    int insert(Mcz21KanriTaisyo entity);

    @Update
    int update(Mcz21KanriTaisyo entity);
}
