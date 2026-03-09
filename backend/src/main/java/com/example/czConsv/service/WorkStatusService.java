package com.example.czConsv.service;

import com.example.czConsv.dao.Tcz01HosyuKousuuDao;
import com.example.czConsv.entity.Tcz01HosyuKousuu;
import com.example.czConsv.exception.CzBusinessException;
import com.example.czConsv.exception.OptimisticLockException;
import com.example.czConsv.security.CzSecurityContext;
import com.example.czConsv.security.model.CzPermissions;
import com.example.czConsv.security.util.StatusMatrixResolver;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 作業ステータス管理サービス。
 *
 * <p>作業ステータスの検索（search）、工数インライン編集（updateHours）、
 * ステータス承認（approve: 1→2）、ステータス差戻（revert: 2→1）を提供する。
 *
 * <p>各操作は CzSecurityContext から認証済みユーザーの権限を取得し、
 * 適切な権限チェック（canManageReports / canFullManage）を実施してから処理を行う。
 */
@Service
public class WorkStatusService {

    private final Tcz01HosyuKousuuDao workHoursDao;
    private final ValidationService validationService;

    public WorkStatusService(Tcz01HosyuKousuuDao workHoursDao,
                             ValidationService validationService) {
        this.workHoursDao = workHoursDao;
        this.validationService = validationService;
    }

    /**
     * 作業ステータスを検索する。
     *
     * <p>yearMonth（yearHalf）、skbtcd でフィルタし、
     * staffId が指定されている場合はさらに担当者でフィルタする。
     * 論理削除済みレコード（delflg!="0"）は除外する。
     *
     * <p>注: SQL最適化は Phase 5 で実施予定。現時点では selectAll + フィルタ方式。
     *
     * @param yearMonth        対象年月（YYYYMM 形式）
     * @param organizationCode 種別コード（skbtcd）
     * @param staffId          担当者ID（nullable: null の場合は全担当者対象）
     * @return フィルタ済みレコードリスト
     */
    public List<Tcz01HosyuKousuu> search(String yearMonth,
                                          String organizationCode,
                                          String staffId) {
        List<Tcz01HosyuKousuu> all = workHoursDao.selectAll();

        return all.stream()
                .filter(r -> "0".equals(r.delflg))
                .filter(r -> yearMonth.equals(r.yearHalf))
                .filter(r -> organizationCode.equals(r.skbtcd))
                .filter(r -> staffId == null
                        || staffId.equals(r.hssgytntEsqid))
                .collect(Collectors.toList());
    }

    /**
     * 工数をインライン編集する（PATCH /work-status/{id}/hours）。
     *
     * <p>権限チェック（canManageReports or canFullManage）、
     * 楽観ロックチェック、バリデーション（VR-008〜VR-010）を実施した上で
     * kousuu フィールドを更新する。
     *
     * @param id                レコードの seqNo
     * @param skbtcd            種別コード
     * @param hours             時間（HH:mm 形式）
     * @param expectedUpdatedAt 楽観ロック用の期待更新日時
     * @return 更新後のエンティティ
     * @throws CzBusinessException       CZ-106: 権限不足、CZ-125: バリデーションエラー、
     *                                   CZ-300: レコード未存在
     * @throws OptimisticLockException   CZ-101: 楽観ロック競合
     */
    public Tcz01HosyuKousuu updateHours(Long id, String skbtcd,
                                         String hours,
                                         LocalDateTime expectedUpdatedAt) {
        // 権限チェック
        requireManagePermission();

        // レコード取得
        Tcz01HosyuKousuu record = findRecordOrThrow(id, skbtcd);

        // 楽観ロックチェック
        checkOptimisticLock(record, expectedUpdatedAt);

        // バリデーション
        int existingDailyMinutes = record.kousuu != null
                ? record.kousuu.intValue() : 0;
        // 自身の工数を除いた既存合計として 0 を渡す（自身分は置換されるため）
        List<ValidationError> errors = validationService.validateFieldUpdate(
                "hours", hours, 0, record.hsSyubetu);
        if (!errors.isEmpty()) {
            ValidationError firstError = errors.get(0);
            throw new CzBusinessException(
                    firstError.code(), firstError.message(),
                    firstError.field());
        }

        // 時間を分数に変換して更新
        int minutes = validationService.parseHoursToMinutes(hours);
        record.kousuu = new BigDecimal(minutes);
        record.upddate = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        workHoursDao.update(record);

        return record;
    }

    /**
     * ステータスを承認する（STATUS_1 → STATUS_2）。
     *
     * <p>権限チェック（canManageReports or canFullManage）を実施し、
     * 各レコードのステータスが "1" であることを検証してから "2" に更新する。
     * StatusMatrixResolver で statusUpdate が ENABLED であることも確認する。
     *
     * @param ids    対象レコードの seqNo リスト
     * @param skbtcd 種別コード
     * @return 更新件数
     * @throws CzBusinessException CZ-106: 権限不足、CZ-107: ステータス不正、
     *                             CZ-300: レコード未存在
     */
    public int approve(List<Long> ids, String skbtcd) {
        CzPermissions perms = requireManagePermission();

        int count = 0;
        for (Long id : ids) {
            Tcz01HosyuKousuu record = findRecordOrThrow(id, skbtcd);

            // ステータスチェック: "1" のみ承認可能
            if (!"1".equals(record.status)) {
                throw new CzBusinessException(
                        "CZ-107",
                        "承認できるのはステータス「確認済」のレコードのみです");
            }

            // StatusMatrix チェック
            String statusKey = record.status + "00";
            int statusUpdatePermission = StatusMatrixResolver.resolveOperation(
                    statusKey, perms.useTanSeries(),
                    StatusMatrixResolver.OP_STATUS_UPDATE);
            if (statusUpdatePermission != StatusMatrixResolver.ENABLED) {
                throw new CzBusinessException(
                        "CZ-107",
                        "このステータスのレコードは承認できません");
            }

            // ステータス更新
            record.status = "2";
            record.upddate = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
            workHoursDao.update(record);
            count++;
        }

        return count;
    }

    /**
     * ステータスを差し戻す（STATUS_2 → STATUS_1）。
     *
     * <p>権限チェック（canManageReports or canFullManage）を実施し、
     * 各レコードのステータスが "2" であることを検証してから "1" に更新する。
     *
     * @param ids    対象レコードの seqNo リスト
     * @param skbtcd 種別コード
     * @return 更新件数
     * @throws CzBusinessException CZ-106: 権限不足、CZ-108: ステータス不正、
     *                             CZ-300: レコード未存在
     */
    public int revert(List<Long> ids, String skbtcd) {
        requireManagePermission();

        int count = 0;
        for (Long id : ids) {
            Tcz01HosyuKousuu record = findRecordOrThrow(id, skbtcd);

            // ステータスチェック: "2" のみ差戻可能
            if (!"2".equals(record.status)) {
                throw new CzBusinessException(
                        "CZ-108",
                        "差戻できるのはステータス「承認済」のレコードのみです");
            }

            // ステータス更新
            record.status = "1";
            record.upddate = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
            workHoursDao.update(record);
            count++;
        }

        return count;
    }

    // ========================================================================
    // プライベートメソッド
    // ========================================================================

    /**
     * canManageReports() または canFullManage() 権限を要求する。
     *
     * @return CzPermissions（呼び出し元で使用する場合）
     * @throws CzBusinessException CZ-106: 権限不足
     */
    private CzPermissions requireManagePermission() {
        CzPermissions perms = CzSecurityContext.require().permissions();
        if (!perms.canManageReports() && !perms.canFullManage()) {
            throw new CzBusinessException("CZ-106", "権限がありません");
        }
        return perms;
    }

    /**
     * seqNo + skbtcd でレコードを取得し、見つからない場合は CZ-300 をスローする。
     *
     * @param id     seqNo
     * @param skbtcd 種別コード
     * @return Tcz01HosyuKousuu エンティティ
     * @throws CzBusinessException CZ-300: レコード未存在
     */
    private Tcz01HosyuKousuu findRecordOrThrow(Long id, String skbtcd) {
        return workHoursDao.selectById(id, skbtcd)
                .orElseThrow(() -> new CzBusinessException(
                        "CZ-300",
                        "レコードが見つかりません: seqNo=" + id
                                + ", skbtcd=" + skbtcd));
    }

    /**
     * 楽観ロックチェック。
     * エンティティの upddate と期待値が一致しない場合、
     * OptimisticLockException をスローする。
     *
     * @param record            対象エンティティ
     * @param expectedUpdatedAt 期待更新日時
     * @throws OptimisticLockException CZ-101: 楽観ロック競合
     */
    private void checkOptimisticLock(Tcz01HosyuKousuu record,
                                     LocalDateTime expectedUpdatedAt) {
        if (record.upddate == null && expectedUpdatedAt == null) {
            return;
        }
        if (record.upddate == null || !record.upddate.equals(expectedUpdatedAt)) {
            throw new OptimisticLockException();
        }
    }
}
