package com.example.czConsv.dto.response;

import com.example.czConsv.dto.response.WorkHoursListResponse.CategoryInfo;
import com.example.czConsv.dto.response.WorkHoursListResponse.MonthControl;
import com.example.czConsv.dto.response.WorkHoursListResponse.SubsystemInfo;
import com.example.czConsv.dto.response.WorkHoursListResponse.WorkHoursPermissions;
import com.example.czConsv.dto.response.WorkHoursListResponse.WorkHoursRecord;
import com.example.czConsv.dto.response.WorkHoursListResponse.WorkHoursSummary;
import com.example.czConsv.entity.Tcz01HosyuKousuu;
import com.example.czConsv.security.model.CzPrincipal;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;


/**
 * Tcz01HosyuKousuu エンティティ → WorkHoursListResponse DTO 変換マッパー。
 */
public final class WorkHoursMapper {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private WorkHoursMapper() {
    }

    /**
     * エンティティリストから一覧レスポンスを構築する。
     */
    public static WorkHoursListResponse toListResponse(
            List<Tcz01HosyuKousuu> entities,
            String yearMonth,
            CzPrincipal principal) {

        List<WorkHoursRecord> records = entities.stream()
                .map(WorkHoursMapper::toRecord)
                .toList();

        WorkHoursSummary summary = computeSummary(entities);
        WorkHoursPermissions permissions = resolvePermissions(principal);
        MonthControl monthControl = new MonthControl(yearMonth, "OPEN", false);

        return new WorkHoursListResponse(records, summary, permissions, monthControl);
    }

    /**
     * 単一エンティティを WorkHoursRecord DTO に変換する。
     */
    public static WorkHoursRecord toRecord(Tcz01HosyuKousuu entity) {
        return new WorkHoursRecord(
                entity.seqNo,
                entity.yearHalf,
                entity.sgyymd != null ? entity.sgyymd.format(DATE_FMT) : null,
                new SubsystemInfo(
                        entity.taisyoSubsysno,
                        null,
                        entity.taisyoSknno,
                        null),
                new SubsystemInfo(
                        entity.causeSubsysno,
                        null,
                        entity.causeSysno,
                        null),
                new CategoryInfo(entity.hsKategori, null),
                entity.kenmei,
                formatMinutesToHours(entity.kousuu),
                entity.tmrNo,
                entity.sgyIraisyoNo,
                entity.sgyIraisyaName,
                entity.status,
                entity.upddate
        );
    }

    /**
     * エンティティから WorkHoursUpdateResponse を構築する。
     */
    public static WorkHoursUpdateResponse toUpdateResponse(
            Tcz01HosyuKousuu entity,
            String field,
            String oldValue,
            String newValue,
            List<Tcz01HosyuKousuu> allRecords) {

        WorkHoursSummary summary = computeSummary(allRecords);
        return new WorkHoursUpdateResponse(entity.seqNo, field, oldValue, newValue, summary);
    }

    private static WorkHoursSummary computeSummary(List<Tcz01HosyuKousuu> entities) {
        BigDecimal monthlyTotal = entities.stream()
                .map(e -> e.kousuu != null ? e.kousuu : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new WorkHoursSummary(monthlyTotal, BigDecimal.ZERO);
    }

    private static WorkHoursPermissions resolvePermissions(CzPrincipal principal) {
        boolean canReport = principal.permissions().canReport();
        boolean canManage = principal.permissions().canManageReports();
        boolean canFull = principal.permissions().canFullManage();

        return new WorkHoursPermissions(
                canReport || canManage || canFull,   // canCreate
                canReport || canManage || canFull,   // canEdit
                canReport || canManage || canFull,   // canDelete
                canReport || canManage || canFull,   // canConfirm
                canManage || canFull,                // canRevert
                canReport || canManage || canFull,   // canCopy
                canReport || canManage || canFull    // canTransfer
        );
    }

    private static String formatMinutesToHours(BigDecimal kousuu) {
        if (kousuu == null) return null;
        int totalMinutes = kousuu.intValue();
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        return String.format("%d:%02d", hours, minutes);
    }
}
