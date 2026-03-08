package com.example.czConsv.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * GET /work-status レスポンス DTO。
 *
 * <p>工数状況一覧画面のデータをラップする。
 */
public record WorkStatusListResponse(
        List<WorkStatusRecord> records,
        MonthlyControlInfo monthlyControl,
        WorkStatusPermissions permissions
) {

    /**
     * 担当者別工数ステータスレコード。
     */
    public record WorkStatusRecord(
            String staffId,
            String staffName,
            String organizationCode,
            String organizationName,
            String yearMonth,
            String status,
            BigDecimal totalHours,
            int recordCount
    ) {
    }

    /**
     * 月次管理情報。
     */
    public record MonthlyControlInfo(
            String yearMonth,
            String organizationCode,
            String status,
            boolean isConfirmed,
            boolean isAggregated
    ) {
    }

    /**
     * 工数状況操作権限。
     */
    public record WorkStatusPermissions(
            boolean canApprove,
            boolean canRevert,
            boolean canMonthlyConfirm,
            boolean canMonthlyAggregate,
            boolean canMonthlyUnconfirm
    ) {
    }
}
