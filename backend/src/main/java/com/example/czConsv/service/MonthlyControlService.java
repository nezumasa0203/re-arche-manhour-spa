package com.example.czConsv.service;

import com.example.czConsv.dao.Mcz04CtrlDao;
import com.example.czConsv.entity.Mcz04Ctrl;
import com.example.czConsv.exception.CzBusinessException;
import com.example.czConsv.security.CzSecurityContext;
import com.example.czConsv.security.model.CzPermissions;
import org.springframework.stereotype.Service;

/**
 * 月次制御サービス。
 *
 * <p>月次確認（monthlyConfirm）、月次集約（monthlyAggregate）、
 * 月次確認取消（monthlyUnconfirm）、コントロール情報取得（getControl）を提供する。
 *
 * <p>各操作は CzSecurityContext から認証済みユーザーの権限を取得し、
 * 適切な権限チェックを実施してから処理を行う。
 */
@Service
public class MonthlyControlService {

    private final Mcz04CtrlDao ctrlDao;

    public MonthlyControlService(Mcz04CtrlDao ctrlDao) {
        this.ctrlDao = ctrlDao;
    }

    /**
     * 月次確認を実行する。
     *
     * <p>gjktFlg を "1"（確認済）に設定し、dataSkFlg を "0"（未集約）にリセットする。
     * canInputPeriodCondition() 権限が必要。
     *
     * @param yearMonth        対象年月（YYYYMM 形式）
     * @param organizationCode 組織コード
     * @return 更新後の Mcz04Ctrl エンティティ
     * @throws CzBusinessException CZ-106: 権限不足、CZ-300: レコード未存在
     */
    public Mcz04Ctrl monthlyConfirm(String yearMonth,
                                    String organizationCode) {
        CzPermissions perms = CzSecurityContext.require().permissions();
        if (!perms.canInputPeriodCondition()) {
            throw new CzBusinessException("CZ-106", "権限がありません");
        }

        String sysid = deriveSysid(perms);
        Mcz04Ctrl ctrl = findCtrlOrThrow(sysid, yearMonth);

        ctrl.gjktFlg = "1";
        ctrl.dataSkFlg = "0";
        ctrlDao.update(ctrl);

        return ctrl;
    }

    /**
     * 月次集約を実行する。
     *
     * <p>dataSkFlg を "1"（集約済）に設定する。
     * canAggregatePeriod() 権限が必要。
     * 事前に月次確認（gjktFlg="1"）が完了している必要がある。
     *
     * @param yearMonth        対象年月（YYYYMM 形式）
     * @param organizationCode 組織コード
     * @return 更新後の Mcz04Ctrl エンティティ
     * @throws CzBusinessException CZ-106: 権限不足、CZ-108: 未確認、
     *                             CZ-300: レコード未存在
     */
    public Mcz04Ctrl monthlyAggregate(String yearMonth,
                                      String organizationCode) {
        CzPermissions perms = CzSecurityContext.require().permissions();
        if (!perms.canAggregatePeriod()) {
            throw new CzBusinessException("CZ-106", "権限がありません");
        }

        String sysid = deriveSysid(perms);
        Mcz04Ctrl ctrl = findCtrlOrThrow(sysid, yearMonth);

        if (!"1".equals(ctrl.gjktFlg)) {
            throw new CzBusinessException(
                    "CZ-108", "月次確認が完了していません");
        }

        ctrl.dataSkFlg = "1";
        ctrlDao.update(ctrl);

        return ctrl;
    }

    /**
     * 月次確認取消を実行する。
     *
     * <p>gjktFlg と dataSkFlg を共に "0" にリセットする。
     * canInputPeriodCondition() 権限が必要。
     *
     * @param yearMonth        対象年月（YYYYMM 形式）
     * @param organizationCode 組織コード
     * @return 更新後の Mcz04Ctrl エンティティ
     * @throws CzBusinessException CZ-106: 権限不足、CZ-300: レコード未存在
     */
    public Mcz04Ctrl monthlyUnconfirm(String yearMonth,
                                      String organizationCode) {
        CzPermissions perms = CzSecurityContext.require().permissions();
        if (!perms.canInputPeriodCondition()) {
            throw new CzBusinessException("CZ-106", "権限がありません");
        }

        String sysid = deriveSysid(perms);
        Mcz04Ctrl ctrl = findCtrlOrThrow(sysid, yearMonth);

        ctrl.gjktFlg = "0";
        ctrl.dataSkFlg = "0";
        ctrlDao.update(ctrl);

        return ctrl;
    }

    /**
     * コントロール情報を取得する。
     *
     * @param yearMonth 対象年月（YYYYMM 形式）
     * @return Mcz04Ctrl エンティティ
     * @throws CzBusinessException CZ-300: レコード未存在
     */
    public Mcz04Ctrl getControl(String yearMonth) {
        CzPermissions perms = CzSecurityContext.require().permissions();
        String sysid = deriveSysid(perms);
        return findCtrlOrThrow(sysid, yearMonth);
    }

    // ========================================================================
    // プライベートメソッド
    // ========================================================================

    /**
     * jinjiMode に基づいて sysid を導出する。
     *
     * @param perms CzPermissions
     * @return "01"（人事モード）or "00"（管理モード）
     */
    private String deriveSysid(CzPermissions perms) {
        return perms.jinjiMode() ? "01" : "00";
    }

    /**
     * sysid + yyyymm でコントロールレコードを取得し、
     * 見つからない場合は CZ-300 システムエラーをスローする。
     *
     * @param sysid   システムID
     * @param yyyymm  対象年月
     * @return Mcz04Ctrl エンティティ
     * @throws CzBusinessException CZ-300: レコード未存在
     */
    private Mcz04Ctrl findCtrlOrThrow(String sysid, String yyyymm) {
        return ctrlDao.selectById(sysid, yyyymm)
                .orElseThrow(() -> new CzBusinessException(
                        "CZ-300",
                        "コントロールレコードが見つかりません: sysid="
                                + sysid + ", yyyymm=" + yyyymm));
    }
}
