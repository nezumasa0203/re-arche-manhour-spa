package com.example.czConsv.service;

import com.example.czConsv.dao.Tcz19MySysDao;
import com.example.czConsv.entity.Tcz19MySys;
import com.example.czConsv.exception.CzBusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MYシステム管理サービス。
 *
 * <p>ユーザーの「お気に入り」システム登録・参照・削除を提供する。
 */
@Service
public class MySystemService {

    private final Tcz19MySysDao mySysDao;

    /**
     * コンストラクタ。
     *
     * @param mySysDao MYシステムDAO
     */
    public MySystemService(Tcz19MySysDao mySysDao) {
        this.mySysDao = mySysDao;
    }

    /**
     * 指定ユーザーのMYシステム一覧を取得する。
     *
     * <p>論理削除されていないレコードのみを返す。
     *
     * @param userId ユーザーID（担当者社員ID）
     * @return MYシステムのリスト
     */
    public List<Tcz19MySys> getMySystemList(String userId) {
        return mySysDao.selectAll().stream()
                .filter(e -> userId.equals(e.tntEsqid))
                .filter(e -> "0".equals(e.delflg))
                .collect(Collectors.toList());
    }

    /**
     * MYシステムを登録する。
     *
     * <p>既に同一ユーザー・同一システム番号の組み合わせが存在する場合は
     * CZ-132 エラーをスローする。
     *
     * @param userId   ユーザーID（担当者社員ID）
     * @param systemNo システム番号
     * @return 登録されたMYシステムエンティティ
     * @throws CzBusinessException CZ-132: 既に登録済みの場合
     */
    @Transactional
    public Tcz19MySys registerMySystem(String userId, String systemNo) {
        Optional<Tcz19MySys> existing = mySysDao.selectById(userId, systemNo);
        if (existing.isPresent()) {
            throw new CzBusinessException("CZ-132",
                    "このシステムは既にMYシステムに登録されています");
        }

        Tcz19MySys entity = new Tcz19MySys();
        entity.tntEsqid = userId;
        entity.sknno = systemNo;
        entity.initnt = userId;
        entity.inidate = LocalDateTime.now();
        entity.updtnt = userId;
        entity.upddate = LocalDateTime.now();
        entity.updpgid = "MySystem";
        entity.delflg = "0";

        mySysDao.insert(entity);
        return entity;
    }

    /**
     * MYシステムを削除する（論理削除）。
     *
     * <p>指定ユーザー・システム番号の組み合わせが見つからない場合は
     * CZ-300 エラーをスローする。
     *
     * @param userId   ユーザーID（担当者社員ID）
     * @param systemNo システム番号
     * @throws CzBusinessException CZ-300: 対象レコードが存在しない場合
     */
    @Transactional
    public void removeMySystem(String userId, String systemNo) {
        Optional<Tcz19MySys> existing = mySysDao.selectById(userId, systemNo);
        if (existing.isEmpty()) {
            throw new CzBusinessException("CZ-300",
                    "対象のMYシステムが見つかりません");
        }

        Tcz19MySys entity = existing.get();
        entity.updtnt = userId;
        entity.upddate = LocalDateTime.now();
        mySysDao.logicalDelete(entity);
    }
}
