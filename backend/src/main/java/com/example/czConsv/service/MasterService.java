package com.example.czConsv.service;

import com.example.czConsv.dao.Mav01SysDao;
import com.example.czConsv.dao.Mav03SubsysDao;
import com.example.czConsv.dao.Mcz02HosyuKategoriDao;
import com.example.czConsv.dao.Mcz12OrgnKrDao;
import com.example.czConsv.dao.Tcz16TntBusyoRirekiDao;
import com.example.czConsv.entity.Mav01Sys;
import com.example.czConsv.entity.Mav03Subsys;
import com.example.czConsv.entity.Mcz02HosyuKategori;
import com.example.czConsv.entity.Mcz12OrgnKr;
import com.example.czConsv.entity.Tcz16TntBusyoRireki;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * マスタデータ参照サービス。
 *
 * <p>組織、システム、サブシステム、保守カテゴリ、担当者の
 * マスタデータを取得・フィルタリングする。
 *
 * <p>現時点では DAO の selectAll() を使用し、サービス層で
 * フィルタリングを行う。後続フェーズで 2Way SQL に最適化予定。
 */
@Service
public class MasterService {

    private final Mcz12OrgnKrDao organizationDao;
    private final Mav01SysDao systemDao;
    private final Mav03SubsysDao subsystemDao;
    private final Mcz02HosyuKategoriDao categoryDao;
    private final Tcz16TntBusyoRirekiDao staffDao;

    /**
     * コンストラクタ。
     *
     * @param organizationDao 組織階層DAO
     * @param systemDao       システムDAO
     * @param subsystemDao    サブシステムDAO
     * @param categoryDao     保守カテゴリDAO
     * @param staffDao        担当部署履歴DAO
     */
    public MasterService(Mcz12OrgnKrDao organizationDao,
                         Mav01SysDao systemDao,
                         Mav03SubsysDao subsystemDao,
                         Mcz02HosyuKategoriDao categoryDao,
                         Tcz16TntBusyoRirekiDao staffDao) {
        this.organizationDao = organizationDao;
        this.systemDao = systemDao;
        this.subsystemDao = subsystemDao;
        this.categoryDao = categoryDao;
        this.staffDao = staffDao;
    }

    /**
     * 組織一覧を取得する。
     *
     * <p>組織階層マスタには delflg が存在しないため、全件を返す。
     *
     * @return 組織のリスト
     */
    public List<Mcz12OrgnKr> getOrganizations() {
        return organizationDao.selectAll();
    }

    /**
     * システム一覧を取得する。
     *
     * <p>論理削除されていないレコードのみを返す。
     *
     * @return システムのリスト
     */
    public List<Mav01Sys> getSystems() {
        return systemDao.selectAll().stream()
                .filter(e -> "0".equals(e.delflg))
                .collect(Collectors.toList());
    }

    /**
     * サブシステム一覧を取得する。
     *
     * <p>キーワードが指定された場合、サブシステム名またはサブシステム名カナに
     * 部分一致するレコードのみを返す。論理削除されていないレコードのみが対象。
     *
     * @param keyword 検索キーワード（null または空の場合は全件返却）
     * @return サブシステムのリスト
     */
    public List<Mav03Subsys> getSubsystems(String keyword) {
        return subsystemDao.selectAll().stream()
                .filter(e -> "0".equals(e.delflg))
                .filter(e -> matchesKeyword(e, keyword))
                .collect(Collectors.toList());
    }

    /**
     * 保守カテゴリ一覧を取得する。
     *
     * <p>指定された会計年度の期間（4/1 〜 翌年3/31）と有効期間が重なる
     * カテゴリを返す。論理削除されていないレコードのみが対象。
     *
     * @param fiscalYear 会計年度（例: 2024）
     * @return 保守カテゴリのリスト
     */
    public List<Mcz02HosyuKategori> getCategories(int fiscalYear) {
        LocalDate fiscalStart = LocalDate.of(fiscalYear, 4, 1);
        LocalDate fiscalEnd = LocalDate.of(fiscalYear + 1, 3, 31);

        return categoryDao.selectAll().stream()
                .filter(e -> "0".equals(e.delflg))
                .filter(e -> overlaps(e.yukouKaishiki, e.yukouSyuryoki,
                        fiscalStart, fiscalEnd))
                .collect(Collectors.toList());
    }

    /**
     * 担当者を検索する。
     *
     * <p>担当部署コードによる部分一致検索を行う。
     * 論理削除されていないレコードのみが対象。
     *
     * @param name 検索文字列
     * @return 担当部署履歴のリスト
     */
    public List<Tcz16TntBusyoRireki> searchStaff(String name) {
        return staffDao.selectAll().stream()
                .filter(e -> "0".equals(e.delflg))
                .filter(e -> matchesName(e, name))
                .collect(Collectors.toList());
    }

    // ========================================================================
    // プライベートヘルパー
    // ========================================================================

    /**
     * サブシステムがキーワードに一致するかを判定する。
     *
     * @param entity  サブシステムエンティティ
     * @param keyword 検索キーワード
     * @return キーワードが null/空の場合は true、それ以外は部分一致判定
     */
    private boolean matchesKeyword(Mav03Subsys entity, String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return true;
        }
        boolean nameMatch = entity.subsysMei != null
                && entity.subsysMei.contains(keyword);
        boolean kanaMatch = entity.subsysMeiKn != null
                && entity.subsysMeiKn.contains(keyword);
        return nameMatch || kanaMatch;
    }

    /**
     * 2つの期間が重なるかを判定する。
     *
     * @param start1 期間1の開始日
     * @param end1   期間1の終了日
     * @param start2 期間2の開始日
     * @param end2   期間2の終了日
     * @return 重なる場合 true
     */
    private boolean overlaps(LocalDate start1, LocalDate end1,
                             LocalDate start2, LocalDate end2) {
        if (start1 == null || end1 == null) {
            return false;
        }
        return !start1.isAfter(end2) && !end1.isBefore(start2);
    }

    /**
     * 担当者レコードが検索名に一致するかを判定する。
     *
     * @param entity 担当部署履歴エンティティ
     * @param name   検索文字列
     * @return 一致する場合 true
     */
    private boolean matchesName(Tcz16TntBusyoRireki entity, String name) {
        if (name == null || name.isEmpty()) {
            return true;
        }
        boolean busyoMatch = entity.tntBusyo != null
                && entity.tntBusyo.contains(name);
        boolean sknnoMatch = entity.sknno != null
                && entity.sknno.contains(name);
        return busyoMatch || sknnoMatch;
    }
}
