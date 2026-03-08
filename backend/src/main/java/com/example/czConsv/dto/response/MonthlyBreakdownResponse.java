package com.example.czConsv.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * GET /monthly-breakdown/* レスポンス DTO。
 *
 * <p>月別内訳データを返す。
 * HalfTrendsResponse と同構造だが月単位の明細を含む。
 */
public record MonthlyBreakdownResponse(
        List<BreakdownRow> rows,
        BreakdownContext context
) {

    /**
     * 内訳行。
     */
    public record BreakdownRow(
            String key,
            String label,
            List<MonthValue> months,
            BigDecimal total
    ) {
    }

    /**
     * 月別値。
     */
    public record MonthValue(
            String yearMonth,
            BigDecimal value
    ) {
    }

    /**
     * 絞り込みコンテキスト。
     */
    public record BreakdownContext(
            String yearMonth,
            String systemNo,
            String subsystemNo,
            String categoryCode
    ) {
    }
}
