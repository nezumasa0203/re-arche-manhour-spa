package com.example.czConsv.service;

import com.example.czConsv.dao.Tcz01HosyuKousuuDao;
import com.example.czConsv.dto.request.WorkHoursCreateRequest;
import com.example.czConsv.entity.Tcz01HosyuKousuu;
import com.example.czConsv.exception.CzBusinessException;
import com.example.czConsv.exception.OptimisticLockException;
import com.example.czConsv.security.CzSecurityContext;
import com.example.czConsv.security.model.CzPrincipal;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 工数管理サービス。
 *
 * <p>保守工数レコード（tcz01_hosyu_kousuu）に対する CRUD 操作と、
 * 一括確定・差戻のビジネスロジックを提供する。
 *
 * <p>ステータス遷移:
 * <ul>
 *   <li>"0" (下書き) — 新規作成時、コピー時のデフォルト</li>
 *   <li>"1" (確定) — batchConfirm による一括確定</li>
 *   <li>"2" (承認済) — 別途承認フローで設定</li>
 * </ul>
 */
@Service
public class WorkHoursService {

    /** ステータス: 下書き。 */
    private static final String STATUS_DRAFT = "0";

    /** ステータス: 確定。 */
    private static final String STATUS_CONFIRMED = "1";

    /** 更新元プログラムID。 */
    private static final String UPDPGID = "WorkHoursService";

    /** 日付フォーマッタ。 */
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final Tcz01HosyuKousuuDao workHoursDao;
    private final ValidationService validationService;

    /**
     * コンストラクタ・インジェクション。
     *
     * @param workHoursDao      工数DAO
     * @param validationService バリデーションサービス
     */
    public WorkHoursService(Tcz01HosyuKousuuDao workHoursDao,
                            ValidationService validationService) {
        this.workHoursDao = workHoursDao;
        this.validationService = validationService;
    }

    /**
     * 月次工数一覧を取得する。
     *
     * @param staffId   担当者ESQID
     * @param yearMonth 対象年月（YYYY-MM 形式）
     * @return 工数レコードリスト
     */
    public List<Tcz01HosyuKousuu> fetchByMonth(String staffId,
                                                String yearMonth) {
        return workHoursDao.selectByTntAndPeriod(staffId, yearMonth);
    }

    /**
     * 工数レコードを新規作成する。
     *
     * <p>下書きモード: yearMonth のみ指定の場合、バリデーションをスキップし
     * 空の STATUS_0 レコードを作成する。
     * フルモード: 全フィールド指定の場合、バリデーションを実行した上で
     * STATUS_0 レコードを作成する。
     *
     * @param request 作成リクエスト
     * @return 作成されたエンティティ
     * @throws CzBusinessException バリデーションエラー時
     */
    public Tcz01HosyuKousuu create(WorkHoursCreateRequest request) {
        CzPrincipal principal = CzSecurityContext.require();
        String skbtcd = resolveSkbtcd(principal);
        boolean isDraft = isDraftMode(request);

        // フルモードの場合はバリデーション
        if (!isDraft) {
            List<ValidationError> errors = validationService.validateWorkHoursInput(
                    request.yearMonth(),
                    request.workDate(),
                    request.targetSubsystemNo(),
                    request.causeSubsystemNo(),
                    request.categoryCode(),
                    null, // hsSyubetu はリクエストに含まれない新規作成時
                    request.subject(),
                    request.hours(),
                    request.tmrNo(),
                    request.workRequestNo(),
                    request.workRequesterName(),
                    0);
            if (!errors.isEmpty()) {
                ValidationError first = errors.get(0);
                throw new CzBusinessException(
                        first.code(), first.message(), first.field());
            }
        }

        // エンティティ構築
        Tcz01HosyuKousuu entity = new Tcz01HosyuKousuu();
        entity.skbtcd = skbtcd;
        entity.hssgytntEsqid = principal.userId();
        entity.hssgytntName = principal.userName();
        entity.yearHalf = request.yearMonth();
        entity.status = STATUS_DRAFT;
        entity.delflg = "0";

        LocalDateTime now = LocalDateTime.now();
        entity.initnt = principal.userId();
        entity.inidate = now;
        entity.updtnt = principal.userId();
        entity.upddate = now;
        entity.updpgid = UPDPGID;

        if (!isDraft) {
            populateEntityFromRequest(entity, request);
        }

        workHoursDao.insert(entity);
        return entity;
    }

    /**
     * 単一フィールドを更新する（セル単位のインライン編集）。
     *
     * @param id               レコードのseqNo
     * @param skbtcd           識別コード
     * @param field            更新対象フィールド名
     * @param value            更新値
     * @param expectedUpdatedAt 楽観ロック用の期待更新日時
     * @return 更新後のエンティティ
     * @throws OptimisticLockException 楽観ロック衝突時
     * @throws CzBusinessException     レコード未存在・バリデーションエラー時
     */
    public Tcz01HosyuKousuu updateField(Long id, String skbtcd,
                                         String field, String value,
                                         LocalDateTime expectedUpdatedAt) {
        // レコード取得
        Tcz01HosyuKousuu entity = workHoursDao.selectById(id, skbtcd)
                .orElseThrow(() -> new CzBusinessException(
                        "CZ-104", "対象レコードが見つかりません"));

        // 楽観ロックチェック
        if (!entity.upddate.equals(expectedUpdatedAt)) {
            throw new OptimisticLockException();
        }

        // 同日の既存レコード合計分数（簡易実装: 当該レコード分を除外）
        int existingDailyMinutes = 0;

        // フィールドバリデーション
        List<ValidationError> errors = validationService.validateFieldUpdate(
                field, value, existingDailyMinutes, entity.hsSyubetu);
        if (!errors.isEmpty()) {
            ValidationError first = errors.get(0);
            throw new CzBusinessException(
                    first.code(), first.message(), first.field());
        }

        // フィールド更新
        applyFieldUpdate(entity, field, value);

        CzPrincipal principal = CzSecurityContext.require();
        entity.updtnt = principal.userId();
        entity.upddate = LocalDateTime.now();
        entity.updpgid = UPDPGID;

        workHoursDao.update(entity);
        return entity;
    }

    /**
     * STATUS_0 のレコードを一括削除（論理削除）する。
     *
     * @param ids    削除対象のseqNoリスト
     * @param skbtcd 識別コード
     * @return 削除件数
     * @throws CzBusinessException STATUS_0 以外のレコードが含まれる場合
     */
    public int delete(List<Long> ids, String skbtcd) {
        CzPrincipal principal = CzSecurityContext.require();

        // 全レコードを先に取得し、ステータスチェック
        List<Tcz01HosyuKousuu> entities = new ArrayList<>();
        for (Long id : ids) {
            Tcz01HosyuKousuu entity = workHoursDao.selectById(id, skbtcd)
                    .orElseThrow(() -> new CzBusinessException(
                            "CZ-104", "対象レコードが見つかりません"));
            if (!STATUS_DRAFT.equals(entity.status)) {
                throw new CzBusinessException(
                        "CZ-106",
                        "下書き以外のレコードは削除できません");
            }
            entities.add(entity);
        }

        // 全てSTATUS_0であることが確認できたら削除実行
        int count = 0;
        for (Tcz01HosyuKousuu entity : entities) {
            entity.updtnt = principal.userId();
            workHoursDao.logicalDelete(entity);
            count++;
        }

        return count;
    }

    /**
     * レコードを複製する。STATUS は "0" にリセットされる。
     *
     * @param ids    コピー元のseqNoリスト
     * @param skbtcd 識別コード
     * @return 複製されたエンティティリスト
     */
    public List<Tcz01HosyuKousuu> copy(List<Long> ids, String skbtcd) {
        CzPrincipal principal = CzSecurityContext.require();
        List<Tcz01HosyuKousuu> copied = new ArrayList<>();

        for (Long id : ids) {
            Tcz01HosyuKousuu source = workHoursDao.selectById(id, skbtcd)
                    .orElseThrow(() -> new CzBusinessException(
                            "CZ-104", "対象レコードが見つかりません"));

            Tcz01HosyuKousuu newEntity = copyEntity(source, principal);
            workHoursDao.insert(newEntity);
            copied.add(newEntity);
        }

        return copied;
    }

    /**
     * レコードを指定月へ繰越す。
     *
     * <p>元レコードをコピーし、yearHalf を対象月に変更する。
     * sgyymd（作業日）はクリアされる。
     *
     * @param ids          繰越元のseqNoリスト
     * @param skbtcd       識別コード
     * @param targetMonths 繰越先年月リスト（YYYY-MM 形式）
     * @return 繰越されたエンティティリスト
     */
    public List<Tcz01HosyuKousuu> transferNextMonth(List<Long> ids,
                                                     String skbtcd,
                                                     List<String> targetMonths) {
        CzPrincipal principal = CzSecurityContext.require();
        List<Tcz01HosyuKousuu> transferred = new ArrayList<>();

        for (Long id : ids) {
            Tcz01HosyuKousuu source = workHoursDao.selectById(id, skbtcd)
                    .orElseThrow(() -> new CzBusinessException(
                            "CZ-104", "対象レコードが見つかりません"));

            for (String targetMonth : targetMonths) {
                Tcz01HosyuKousuu newEntity = copyEntity(source, principal);
                newEntity.yearHalf = targetMonth;
                // 繰越時は作業日をクリア
                newEntity.sgyymd = null;
                workHoursDao.insert(newEntity);
                transferred.add(newEntity);
            }
        }

        return transferred;
    }

    /**
     * 指定月の全下書きレコードを一括確定する。
     *
     * <p>STATUS_0 のレコードに対して validateWorkHoursInput を実行し、
     * 最初のエラーで処理を中断する。全て通過した場合に STATUS_0→1 に更新する。
     *
     * @param yearMonth 対象年月（YYYY-MM 形式）
     * @return 確定件数
     * @throws CzBusinessException バリデーションエラー時（recordId 付き）
     */
    public int batchConfirm(String yearMonth) {
        CzPrincipal principal = CzSecurityContext.require();
        String staffId = principal.userId();

        List<Tcz01HosyuKousuu> drafts = workHoursDao
                .selectByTntPeriodAndStatus(staffId, yearMonth, STATUS_DRAFT);

        if (drafts.isEmpty()) {
            return 0;
        }

        // 全レコードのバリデーション（最初のエラーで停止）
        for (Tcz01HosyuKousuu record : drafts) {
            String workDate = record.sgyymd != null
                    ? record.sgyymd.format(DATE_FORMATTER)
                    : null;
            String hours = record.kousuu != null
                    ? formatMinutesToHours(record.kousuu)
                    : null;

            List<ValidationError> errors = validationService.validateWorkHoursInput(
                    yearMonth,
                    workDate,
                    record.taisyoSubsysno,
                    record.causeSubsysno,
                    record.hsKategori,
                    record.hsSyubetu,
                    record.kenmei,
                    hours,
                    record.tmrNo,
                    record.sgyIraisyoNo,
                    record.sgyIraisyaName,
                    0);

            if (!errors.isEmpty()) {
                ValidationError first = errors.get(0);
                throw new CzBusinessException(
                        first.code(), first.message(), first.field(),
                        first.params(), record.seqNo);
            }
        }

        // 全レコードのステータスを確定に更新
        int count = 0;
        for (Tcz01HosyuKousuu record : drafts) {
            record.status = STATUS_CONFIRMED;
            record.updtnt = principal.userId();
            record.upddate = LocalDateTime.now();
            record.updpgid = UPDPGID;
            workHoursDao.update(record);
            count++;
        }

        return count;
    }

    /**
     * 指定月の確定済みレコードを一括で下書きに戻す。
     *
     * <p>canManageReports または canFullManage 権限が必要。
     *
     * @param yearMonth 対象年月（YYYY-MM 形式）
     * @return 差戻件数
     * @throws CzBusinessException 権限不足時
     */
    public int batchRevert(String yearMonth) {
        CzPrincipal principal = CzSecurityContext.require();

        // 権限チェック
        if (!principal.permissions().canManageReports()
                && !principal.permissions().canFullManage()) {
            throw new CzBusinessException(
                    "CZ-106", "差戻権限がありません");
        }

        List<Tcz01HosyuKousuu> confirmed = workHoursDao
                .selectByTntPeriodAndStatus(
                        principal.userId(), yearMonth, STATUS_CONFIRMED);

        if (confirmed.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (Tcz01HosyuKousuu record : confirmed) {
            record.status = STATUS_DRAFT;
            record.updtnt = principal.userId();
            record.upddate = LocalDateTime.now();
            record.updpgid = UPDPGID;
            workHoursDao.update(record);
            count++;
        }

        return count;
    }

    // ========================================================================
    // プライベートメソッド
    // ========================================================================

    /**
     * 識別コードを解決する。人事モード時は "01"、通常は "00"。
     */
    private String resolveSkbtcd(CzPrincipal principal) {
        return principal.permissions().jinjiMode() ? "01" : "00";
    }

    /**
     * 下書きモード判定。yearMonth 以外のフィールドが全て null の場合 true。
     */
    private boolean isDraftMode(WorkHoursCreateRequest request) {
        return request.workDate() == null
                && request.targetSubsystemNo() == null
                && request.causeSubsystemNo() == null
                && request.categoryCode() == null
                && request.subject() == null
                && request.hours() == null
                && request.tmrNo() == null
                && request.workRequestNo() == null
                && request.workRequesterName() == null;
    }

    /**
     * リクエストからエンティティの各フィールドを設定する。
     */
    private void populateEntityFromRequest(Tcz01HosyuKousuu entity,
                                            WorkHoursCreateRequest request) {
        if (request.workDate() != null) {
            entity.sgyymd = LocalDate.parse(request.workDate(), DATE_FORMATTER);
        }
        entity.taisyoSubsysno = request.targetSubsystemNo();
        entity.causeSubsysno = request.causeSubsystemNo();
        entity.hsKategori = request.categoryCode();
        entity.kenmei = request.subject();
        if (request.hours() != null) {
            int minutes = validationService.parseHoursToMinutes(request.hours());
            entity.kousuu = BigDecimal.valueOf(minutes)
                    .setScale(2, RoundingMode.HALF_UP);
        }
        entity.tmrNo = request.tmrNo();
        entity.sgyIraisyoNo = request.workRequestNo();
        entity.sgyIraisyaName = request.workRequesterName();
    }

    /**
     * エンティティをコピーする。seqNo は null（DB自動採番）、STATUS は "0" にリセット。
     */
    private Tcz01HosyuKousuu copyEntity(Tcz01HosyuKousuu source,
                                         CzPrincipal principal) {
        Tcz01HosyuKousuu copy = new Tcz01HosyuKousuu();
        // seqNo は null（DB自動採番）
        copy.skbtcd = source.skbtcd;
        copy.hssgytntEsqid = source.hssgytntEsqid;
        copy.hssgytntName = source.hssgytntName;
        copy.yearHalf = source.yearHalf;
        copy.sgyymd = source.sgyymd;
        copy.sysKbn = source.sysKbn;
        copy.causeSysKbn = source.causeSysKbn;
        copy.taisyoSknno = source.taisyoSknno;
        copy.taisyoSubsysno = source.taisyoSubsysno;
        copy.taisyoAplid = source.taisyoAplid;
        copy.causeSysno = source.causeSysno;
        copy.causeSubsysno = source.causeSubsysno;
        copy.causeAplid = source.causeAplid;
        copy.kenmei = source.kenmei;
        copy.hsKategori = source.hsKategori;
        copy.hsSyubetu = source.hsSyubetu;
        copy.hsUnyouKubun = source.hsUnyouKubun;
        copy.tmrNo = source.tmrNo;
        copy.sgyIraisyoNo = source.sgyIraisyoNo;
        copy.sgyIraisyaEsqid = source.sgyIraisyaEsqid;
        copy.sgyIraisyaName = source.sgyIraisyaName;
        copy.status = STATUS_DRAFT;
        copy.kousuu = source.kousuu;
        copy.delflg = "0";

        LocalDateTime now = LocalDateTime.now();
        copy.initnt = principal.userId();
        copy.inidate = now;
        copy.updtnt = principal.userId();
        copy.upddate = now;
        copy.updpgid = UPDPGID;

        return copy;
    }

    /**
     * 単一フィールドの値をエンティティに適用する。
     */
    private void applyFieldUpdate(Tcz01HosyuKousuu entity,
                                   String field, String value) {
        switch (field) {
            case "workDate":
                entity.sgyymd = value != null && !value.isEmpty()
                        ? LocalDate.parse(value, DATE_FORMATTER)
                        : null;
                break;
            case "targetSubsystemNo":
                entity.taisyoSubsysno = value;
                break;
            case "causeSubsystemNo":
                entity.causeSubsysno = value;
                break;
            case "categoryCode":
                entity.hsKategori = value;
                break;
            case "subject":
                entity.kenmei = value;
                break;
            case "hours":
                if (value != null && !value.isEmpty()) {
                    int minutes = validationService.parseHoursToMinutes(value);
                    entity.kousuu = BigDecimal.valueOf(minutes)
                            .setScale(2, RoundingMode.HALF_UP);
                } else {
                    entity.kousuu = null;
                }
                break;
            case "tmrNo":
                entity.tmrNo = value;
                break;
            case "workRequestNo":
                entity.sgyIraisyoNo = value;
                break;
            case "workRequesterName":
                entity.sgyIraisyaName = value;
                break;
            case "hsSyubetu":
                entity.hsSyubetu = value;
                break;
            case "hsUnyouKubun":
                entity.hsUnyouKubun = value;
                break;
            default:
                throw new CzBusinessException(
                        "CZ-125",
                        "不明なフィールド: " + field, field);
        }
    }

    /**
     * 分数（BigDecimal）を HH:mm 形式の文字列に変換する。
     */
    private String formatMinutesToHours(BigDecimal kousuu) {
        int totalMinutes = kousuu.intValue();
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        return String.format("%d:%02d", hours, minutes);
    }
}
