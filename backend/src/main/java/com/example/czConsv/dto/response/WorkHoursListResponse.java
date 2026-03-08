package com.example.czConsv.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * GET /work-hours レスポンス DTO。
 *
 * <p>工数一覧画面に必要な全データをラップする。
 */
public record WorkHoursListResponse(
        List<WorkHoursRecord> records,
        WorkHoursSummary summary,
        WorkHoursPermissions permissions,
        MonthControl monthControl
) {

    /**
     * 工数レコード 1 件分。
     */
    public record WorkHoursRecord(
            Long id,
            String yearMonth,
            String workDate,
            SubsystemInfo targetSubsystem,
            SubsystemInfo causeSubsystem,
            CategoryInfo category,
            String subject,
            String hours,
            String tmrNo,
            String workRequestNo,
            String workRequesterName,
            String status,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime updatedAt
    ) {
    }

    /**
     * サブシステム情報（対象/原因共通）。
     */
    public record SubsystemInfo(
            String subsystemNo,
            String subsystemName,
            String systemNo,
            String systemName
    ) {
    }

    /**
     * カテゴリ情報。
     */
    public record CategoryInfo(
            String categoryCode,
            String categoryName
    ) {
    }

    /**
     * 工数サマリ（月合計・日合計）。
     */
    public record WorkHoursSummary(
            BigDecimal monthlyTotal,
            BigDecimal dailyTotal
    ) {
    }

    /**
     * 操作権限フラグ。
     */
    public record WorkHoursPermissions(
            boolean canCreate,
            boolean canEdit,
            boolean canDelete,
            boolean canConfirm,
            boolean canRevert,
            boolean canCopy,
            boolean canTransfer
    ) {
    }

    /**
     * 月次管理状態。
     */
    public record MonthControl(
            String yearMonth,
            String status,
            boolean isLocked
    ) {
    }
}
